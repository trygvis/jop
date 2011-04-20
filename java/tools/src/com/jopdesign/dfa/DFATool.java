/*
 * This file is part of JOP, the Java Optimized Processor
 * see <http://www.jopdesign.com/>
 *
 * Copyright (C) 2010, Stefan Hepp (stefan@stefant.org).
 * Copyright (C) 2008, Wolfgang Puffitsch
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jopdesign.dfa;

import com.jopdesign.common.AppEventHandler;
import com.jopdesign.common.AppInfo;
import com.jopdesign.common.AppSetup;
import com.jopdesign.common.ClassInfo;
import com.jopdesign.common.EmptyTool;
import com.jopdesign.common.FieldInfo;
import com.jopdesign.common.KeyManager.CustomKey;
import com.jopdesign.common.KeyManager.KeyType;
import com.jopdesign.common.MemberInfo.AccessType;
import com.jopdesign.common.MethodCode;
import com.jopdesign.common.MethodInfo;
import com.jopdesign.common.code.CallString;
import com.jopdesign.common.config.Config;
import com.jopdesign.common.config.Config.BadConfigurationException;
import com.jopdesign.common.config.StringOption;
import com.jopdesign.common.graphutils.Pair;
import com.jopdesign.common.misc.MethodNotFoundException;
import com.jopdesign.common.tools.ClinitOrder;
import com.jopdesign.common.type.Descriptor;
import com.jopdesign.common.type.MemberID;
import com.jopdesign.dfa.analyses.CallStringReceiverTypes;
import com.jopdesign.dfa.analyses.LoopBounds;
import com.jopdesign.dfa.analyses.ValueMapping;
import com.jopdesign.dfa.framework.Analysis;
import com.jopdesign.dfa.framework.AnalysisResultSerialization;
import com.jopdesign.dfa.framework.Context;
import com.jopdesign.dfa.framework.ContextMap;
import com.jopdesign.dfa.framework.Flow;
import com.jopdesign.dfa.framework.FlowEdge;
import com.jopdesign.dfa.framework.Interpreter;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Select;
import org.apache.bcel.generic.UnconditionalBranch;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Tool for dataflow analysis
 */
public class DFATool extends EmptyTool<AppEventHandler> {

    private static final String prologueName = "<prologue>";
    private static final String prologueSig = "()V";

    // Root logger
    public static final String LOG_DFA = "dfa";
    public static final String LOG_DFA_ANALYSES = "dfa.analyses";

    private static final Logger logger = Logger.getLogger(LOG_DFA+".DFATool");

    private AppInfo appInfo;

    private List<InstructionHandle> statements;
    private Flow flow;
    private Map<InstructionHandle, ContextMap<CallString, Set<String>>> receivers;

    private LoopBounds loopBounds;

    /**
     * This key is used to attach a NOP instruction handle to each method which is used as handle for
     * the result state of a method.
     */
    private CustomKey KEY_NOP;
	private byte[] digest = null;
	private File cacheDir = null; 

    public DFATool() {
        super("head");
        this.appInfo = AppInfo.getSingleton();
        this.statements = new LinkedList<InstructionHandle>();
        this.flow = new Flow();
        this.receivers = null;
    }

    public AppInfo getAppInfo() {
        return appInfo;
    }

    @Override
    public void registerOptions(Config config) {
    	config.addOption(OPT_DFA_CACHE_DIR);
    }

    @Override
    public void onSetupConfig(AppSetup setup) throws Config.BadConfigurationException {

    	if(setup.getConfig().getOption(OPT_DFA_CACHE_DIR) != null) {
    		this.cacheDir = new File(setup.getConfig().getOption(OPT_DFA_CACHE_DIR));
    	}
    }

    @Override
    public void onSetupAppInfo(AppSetup setup, AppInfo appInfo) throws BadConfigurationException {
    	
        // We do not call load() here, because some other tool might want to modify the code before
        // running the DFA the first tool..
        KEY_NOP = appInfo.getKeyManager().registerKey(KeyType.STRUCT, "dfa.nop");
    }

    /**
     * Load the methods into the internal DFA structures and initialize the DFA tool for a new analysis.
     * You need to call this method before starting the first analysis and before starting an analysis after
     * the code has been modified.
     */
    public void load() {

        // find ordering for class initializers
        ClinitOrder c = new ClinitOrder();
        appInfo.iterate(c);

        List<ClassInfo> order = c.findOrder();

        MethodInfo mainClass = appInfo.getMainMethod();

        // Also compute SHA-1 checksum for this DFA problem
        // (for caching purposes)
        // TODO: Maybe this is interesting for other parties as well,
        // and we should move checksums to common
        MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			md = null;
		}
        
        // create prologue
        MethodInfo prologue =
            buildPrologue(mainClass, statements, flow, order);
        updateChecksum(prologue, md);

        // Now we need to process all classes (for DFA's internal flow graph)
        for (ClassInfo ci : appInfo.getClassInfos()) {
        	updateCheckSum(ci.getConstantPoolGen().getConstantPool(), md);
            for (MethodInfo mi : ci.getMethods()) {
                if (mi.hasCode()) {
                    loadMethod(mi);
                    updateChecksum(mi, md);
                }
            }
        }
        this.digest = md.digest();
        logger.info("DFA problem has checksum: "+this.getDigestString());
    }

    /* constant pool to checksum */
    private void updateCheckSum(ConstantPool cp, MessageDigest md) {
    	
    	if(md == null) return;
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	DataOutputStream dos = new DataOutputStream(bos);
    	try {
    		cp.dump(dos);
    	} catch (IOException e) {
    		logger.error("Dumping the constant pool (checksum calculation) failed: "+
    				e.getMessage());
    		throw new RuntimeException(e);
    	}
    	md.update(bos.toByteArray());		
	}

	private static void updateChecksum(MethodInfo mi, MessageDigest md) {
    	if(md == null) return;
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	OutputStreamWriter writer = new OutputStreamWriter(bos);
    	try {
			writer.append(mi.getFQMethodName());
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    	md.update(bos.toByteArray());
    	// finally, also add the code
    	md.update(mi.getCode().getInstructionList().getByteCode());
	}

	/**
     * Remove all helper objects. You need to run {@link #load()} again before performing a new analysis.
     */
    public void cleanup() {
        appInfo.getKeyManager().clearAllValues(KEY_NOP);
    }

    private void loadMethod(MethodInfo method) {

        MethodCode mcode = method.getCode();
        // TODO is there a better way to get an instruction handle? Do we need to keep the list somehow?
        InstructionHandle exit = new InstructionList(new NOP()).getStart();
        this.getStatements().add(exit);

        // We do not modify the code, so we leave existing CFGs alone, just make sure the instruction list is uptodate
        for (Iterator<?> l = mcode.getInstructionList(true, false).iterator(); l.hasNext();) {
            InstructionHandle handle = (InstructionHandle) l.next();
            this.getStatements().add(handle);

            Instruction instr = handle.getInstruction();
            if (instr instanceof BranchInstruction) {
                if (instr instanceof Select) {
                    Select s = (Select) instr;
                    InstructionHandle[] target = s.getTargets();
                    for (InstructionHandle aTarget : target) {
                        this.getFlow().addEdge(new FlowEdge(handle, aTarget,
                            FlowEdge.TRUE_EDGE));
                    }
                    this.getFlow().addEdge(new FlowEdge(handle, s.getTarget(),
                            FlowEdge.FALSE_EDGE));
                } else {
                    BranchInstruction b = (BranchInstruction) instr;
                    this.getFlow().addEdge(new FlowEdge(handle, b.getTarget(),
                            FlowEdge.TRUE_EDGE));
                }
            }
            if (handle.getNext() != null
                    && !(instr instanceof UnconditionalBranch
                    || instr instanceof Select || instr instanceof ReturnInstruction)) {
                if (instr instanceof BranchInstruction) {
                    this.getFlow().addEdge(new FlowEdge(handle, handle.getNext(),
                            FlowEdge.FALSE_EDGE));
                } else {
                    this.getFlow().addEdge(new FlowEdge(handle, handle.getNext(),
                            FlowEdge.NORMAL_EDGE));
                }
            }
            if (instr instanceof ReturnInstruction) {
                this.getFlow().addEdge(new FlowEdge(handle, exit,
                            FlowEdge.NORMAL_EDGE));
            }
        }

        // We do not really want to modify the REAL instruction list and append exit
        // (SH) Fixed:) Yep, we need the NOP somewhere, else doInvoke() will collect the wrong result state.
        //      But we can eliminate the NOP by adding the instruction not to the list, but instead to the
        //      MethodInfo, and also retrieve it from there.
        method.setCustomValue(KEY_NOP, exit);
    }

    public InstructionHandle getEntryHandle(MethodInfo method) {
        // for symmetry, lets also provide a getter for the first instruction ..
        MethodCode mCode = method.getCode();
        // since we already compiled any existing CFG in load(), there is no need to do it again here
        return mCode.getInstructionList(false, false).getStart();
    }

    public InstructionHandle getExitHandle(MethodInfo method) {
        return (InstructionHandle) method.getCustomValue(KEY_NOP); 
    }

    private MethodInfo buildPrologue(MethodInfo mainMethod, List<InstructionHandle> statements, Flow flow, List<ClassInfo> clinits) {

        // we use a prologue sequence for startup
        InstructionList prologue = new InstructionList();
        ConstantPoolGen prologueCP = mainMethod.getConstantPoolGen();

        Instruction instr;
        int idx;

        // add magic initializers to prologue sequence
        instr = new ICONST(0);
        prologue.append(instr);
        instr = new ICONST(0);
        prologue.append(instr);
        idx = prologueCP.addMethodref("com.jopdesign.sys.GC", "init", "(II)V");
        instr = new INVOKESTATIC(idx);
        prologue.append(instr);

        // Not in prologue anymore
        //        idx = prologueCP.addMethodref("java.lang.System", "<init>", "()V");
        //        instr = new INVOKESTATIC(idx);
        //        prologue.append(instr);

        // add class initializers
        for (ClassInfo clinit : clinits) {
            MemberID cSig = appInfo.getClinitSignature(clinit.getClassName());
            idx = prologueCP.addMethodref(cSig.getClassName(), cSig.getMemberName(),
                    cSig.getDescriptor().toString());
            instr = new INVOKESTATIC(idx);
            prologue.append(instr);
        }

        // add main method
        instr = new ACONST_NULL();
        prologue.append(instr);
        idx = prologueCP.addMethodref(mainMethod.getClassName(), mainMethod.getShortName(),
                mainMethod.getDescriptor().toString());
        instr = new INVOKESTATIC(idx);
        prologue.append(instr);

//      // invoke startMission() to ensure analysis of threads
//      idx = prologueCP.addMethodref("joprt.RtThread", "startMission", "()V");
//      instr = new INVOKESTATIC(idx);
//      prologue.append(instr);

        instr = new NOP();
        prologue.append(instr);

        prologue.setPositions(true);

//      System.out.println(prologue);

        // add prologue to program structure
        for (Iterator l = prologue.iterator(); l.hasNext();) {
            InstructionHandle handle = (InstructionHandle) l.next();
            statements.add(handle);
            if (handle.getInstruction() instanceof GOTO) {
                GOTO g = (GOTO) handle.getInstruction();
                flow.addEdge(new FlowEdge(handle, g.getTarget(), FlowEdge.NORMAL_EDGE));
            } else if (handle.getNext() != null) {
                flow.addEdge(new FlowEdge(handle, handle.getNext(), FlowEdge.NORMAL_EDGE));
            }
        }

        MemberID pSig = new MemberID(prologueName, Descriptor.parse(prologueSig));
        MethodInfo mi = mainMethod.getClassInfo().createMethod(pSig, null, prologue);

        mi.setAccessType(AccessType.ACC_PRIVATE);
        return mi;
    }

    public Map<InstructionHandle, ContextMap<CallString, Set<String>>>  runReceiverAnalysis(int callstringLength) {
        CallStringReceiverTypes recTys = new CallStringReceiverTypes(callstringLength);
        @SuppressWarnings({"unchecked"})
        Map<InstructionHandle, ContextMap<CallString, Set<String>>> receiverResults = runAnalysis(recTys);

        setReceivers(receiverResults);
        return receiverResults;
    }

    public void runLoopboundAnalysis(int callstringLength) {
        LoopBounds dfaLoopBounds = new LoopBounds(callstringLength);
        runAnalysis(dfaLoopBounds);
        setLoopBounds(dfaLoopBounds);        
    }

    @SuppressWarnings("unchecked")
    public Map runAnalysis(Analysis analysis) {
	
    	/* use cached results if possible */
    	Map results;
    	if((results = getCachedResults(analysis)) != null) {
    		logger.warn("Analysis "+analysis.getId()+": Using cached DFA analysis results");
    		return results;
    	}

    	Interpreter interpreter = new Interpreter(analysis, this);

        MethodInfo main = appInfo.getMainMethod();
        MethodInfo prologue = main.getClassInfo().getMethodInfo(prologueName + prologueSig);

        Context context = new Context();
        context.stackPtr = 0;
        context.syncLevel = 0;
        context.setMethodInfo(prologue);

        analysis.initialize(main, context);

        InstructionHandle entry = prologue.getCode().getInstructionList().getStart();
        interpreter.interpret(context, entry, new HashMap(), true);

        /* cache results if requested */
        writeCachedResults(analysis);
        return analysis.getResult();
    }

	public <K, V>
    Map runLocalAnalysis(Analysis<K, V> analysis, MethodInfo start) {

        Interpreter<K, V> interpreter = new Interpreter<K, V>(analysis, this);

        if (start == null) throw new AssertionError("No such method: " + start);
        Context context = new Context();
        context.stackPtr = start.getCode().getMaxLocals();
        context.setMethodInfo(start);

        analysis.initialize(start, context);
        InstructionHandle entry = start.getCode().getInstructionList().getStart();
        interpreter.interpret(context, entry, new HashMap<InstructionHandle, ContextMap<K, V>>(), true);

        return analysis.getResult();
    }


    public List<InstructionHandle> getStatements() {
        return statements;
    }

    public Flow getFlow() {
        return flow;
    }

    public Set<String> getReceivers(InstructionHandle stmt, CallString cs) {
        ContextMap<CallString, Set<String>> map = receivers.get(stmt);
        if (map == null) {
            return null;
        }
        Set<String> retval = new HashSet<String>();
        for (CallString c : map.keySet()) {
            if (c.hasSuffix(cs)) {
                retval.addAll(map.get(c));
            }
        }
        return retval;
    }

    public Set<MethodInfo> getReceiverMethods(InstructionHandle stmt, CallString cs) {
        Set<String> receivers = getReceivers(stmt, cs);
        Set<MethodInfo> methods = new HashSet<MethodInfo>(receivers.size());
        for (String rcv : receivers) {
            MemberID mID = MemberID.parse(rcv);
            methods.add(appInfo.getMethodInfoInherited(mID));
        }
        return methods;
    }

    public void setReceivers(Map<InstructionHandle, ContextMap<CallString, Set<String>>> receivers) {
        this.receivers = receivers;
    }

    public LoopBounds getLoopBounds() {
        return loopBounds;
    }

    public void setLoopBounds(LoopBounds lb) {
        this.loopBounds = lb;
    }

    /**
     * Helper method to find the actually invoked method given the
     * dynamic type and the method signature
     *
     * @param recvStr the dynamic type of the receiver
     * @param sigStr  the signature (without class) of the method.
     * @return the actually invoked method, or {@code null} if not found
     */
    public MethodInfo getMethod(String recvStr, String sigStr) {
        return appInfo.getMethodInfoInherited(recvStr, sigStr);
    }

    /**
     * Helper method to find the actually invoked method given the
     * dynamic type and the method signature
     *
     * @param signature FQ signature of the method
     * @return the invoked method, or {@code null} if not found
     */
    public MethodInfo getMethod(String signature) {
        return appInfo.getMethodInfoInherited(MemberID.parse(signature, true));
    }

    public boolean containsField(String fieldName) {
        MemberID s = MemberID.parse(fieldName, true);
        return classForField(s.getClassName(), s.getMemberName()) != null;
    }

    public ClassInfo classForField(String className, String fieldName) {
        ClassInfo cls = getAppInfo().getClassInfo(className);
        if (cls == null) {
            logger.info("Unknown class as potential receiver of field access" + className);
            return null;
        }
        // TODO maybe we *do* want to check access here...
        FieldInfo field = cls.getFieldInfoInherited(fieldName, false);
        return field != null ? field.getClassInfo() : null;
    }

    public String dumpDFA(MethodInfo method) {
        if (getLoopBounds() == null) return "n/a";
        if (method.isAbstract()) {
            return "n/a";
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream os = new PrintStream( baos );
        Map<InstructionHandle, ContextMap<CallString, Pair<ValueMapping, ValueMapping>>> results = getLoopBounds().getResult();
        if (results == null) return "n/a";

        AnalysisResultSerialization<Pair<ValueMapping,ValueMapping>> printer = 
        	new AnalysisResultSerialization<Pair<ValueMapping,ValueMapping>>();
        for(Entry<InstructionHandle, ContextMap<CallString, Pair<ValueMapping, ValueMapping>>> ihEntry : 
        	results.entrySet()) {
        	for(Entry<CallString, Pair<ValueMapping, ValueMapping>> csEntry : 
        		ihEntry.getValue().entrySet()) {
        			if(ihEntry.getValue().getContext().getMethodInfo().equals(method)) {
        				printer.addResult(method, ihEntry.getKey().getPosition(), csEntry.getKey(), csEntry.getValue());
        			}
        	}
        }
        printer.dump(os);
        try {
			return baos.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return baos.toString();
		}
    }
    
    /* Caching DFA results */
    /* ------------------- */
    
    public static final StringOption OPT_DFA_CACHE_DIR = 
    	new StringOption("dfa-cache-dir","If dataflow analysis results should " +
    			"be cached, specify a cache dir to store the results in",true);
	
	/** If caching is enabled, safe the cached results for the given analysis*/
    private void writeCachedResults(Analysis analysis) {
    	if(cacheDir == null) return;
    	try {
        	analysis.serializeResult(getCacheFile(analysis));
		} catch (IOException e) {
			logger.error("Failed to serialize analysis results: "+e);
		}
	}

    /** If caching is enabled, look for cached results for the given analysis*/
	private Map getCachedResults(Analysis analysis) {
		if(cacheDir == null) return null;
		File cacheFile = getCacheFile(analysis);
		try {
			if(! cacheFile.exists()) return null;
			return analysis.deSerializeResult(appInfo, cacheFile);
		} catch(IOException ex) {
			logger.error("Deserialization of " + analysis.getId() + " result failed",ex);
		} catch (ClassNotFoundException ex) {
			logger.error("Deserialization of " + analysis.getId() + " result failed",ex);
		} catch (MethodNotFoundException ex) {
			logger.error("Deserialization of " + analysis.getId() + " result failed",ex);
		}
		return null;
	}

    private File getCacheFile(Analysis analysis) {
    	if(cacheDir == null) {
    		throw new AssertionError("Invariant violated: getCacheFile should only be called if cacheDir is non-null");
		}
    	String key = analysis.getId() + "-" + getDigestString();
		String cacheFile = "dfa-" + key + ".dat";
		return new File(cacheDir , cacheFile);
	}
    
	private static final char[] digits = "0123456789abcdef".toCharArray();
    private String getDigestString() {
    	StringBuffer sb = new StringBuffer();
    	for(byte b : digest) {
    		int v = b < 0 ? (256 + b) : b;
    		sb.append(digits[v >> 4]);
    		sb.append(digits[v & 0xF]);
    	}
    	return sb.toString();
    }

}
