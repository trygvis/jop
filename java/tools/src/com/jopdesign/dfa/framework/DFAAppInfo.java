/*
 * This file is part of JOP, the Java Optimized Processor
 * see <http://www.jopdesign.com/>
 *
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

package com.jopdesign.dfa.framework;

import com.jopdesign.common.AppInfo;
import com.jopdesign.common.ClassInfo;
import com.jopdesign.common.MethodInfo;
import com.jopdesign.common.code.CallString;
import com.jopdesign.common.code.Context;
import com.jopdesign.common.code.ContextMap;
import com.jopdesign.common.tools.ClinitOrder;
import com.jopdesign.dfa.analyses.LoopBounds;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.Type;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DFAAppInfo {

	private static final long serialVersionUID = 1L;

	private static final String mainName = "main";
	private static final String mainSig = "([Ljava/lang/String;)V";

	private static final String clinitName = "<clinit>";
	private static final String clinitSig = "()V";

	private static final String prologueName = "<prologue>";
	private static final String prologueSig = "()V";

    private AppInfo appInfo;

	private List<InstructionHandle> statements;
	private Flow flow;
	private Map<InstructionHandle, ContextMap<CallString, Set<String>>> receivers;

	private LoopBounds loopBounds;

	public DFAAppInfo() {
        this.appInfo = AppInfo.getSingleton();
		this.statements = new LinkedList<InstructionHandle>();
		this.flow = new Flow();
		this.receivers = null;
	}

	public void load() throws IOException, ClassNotFoundException {
		// find oredering for class initializers
		List<String> clinits = new LinkedList<String>();
		
		ClinitOrder c = new ClinitOrder();
		appInfo.iterate(c);

		List<ClassInfo> order = c.findOrder();
        for (ClassInfo cls : order) {
            clinits.add(cls.getClassName() + "." + clinitName + clinitSig);
        }

		// create prologue
		buildPrologue(mainClass, statements, flow, clinits);	
	}
	
	private void buildPrologue(String mainClass, List<InstructionHandle> statements, Flow flow, List<String> clinits) {

		// we use a prologue sequence for startup
		InstructionList prologue = new InstructionList();
		ConstantPoolGen prologueCP = new ConstantPoolGen();

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
		idx = prologueCP.addMethodref("java.lang.System", "init", "()V");
		instr = new INVOKESTATIC(idx);
		prologue.append(instr);

		// add class initializers
        for (String clinit : clinits) {
            String className = clinit.substring(0, clinit.lastIndexOf("."));
            idx = prologueCP.addMethodref(className, clinitName, clinitSig);
            instr = new INVOKESTATIC(idx);
            prologue.append(instr);
        }

		// add main method
		instr = new ACONST_NULL();
		prologue.append(instr);
		idx = prologueCP.addMethodref(mainClass, mainName, mainSig);
		instr = new INVOKESTATIC(idx);
		prologue.append(instr);
		
//		// invoke startMission() to ensure analysis of threads
//		idx = prologueCP.addMethodref("joprt.RtThread", "startMission", "()V");
//		instr = new INVOKESTATIC(idx);
//		prologue.append(instr);

		instr = new NOP();
		prologue.append(instr);

		prologue.setPositions(true);

// 		System.out.println(prologue);

		// add prologue to program structure
		for (Iterator l = prologue.iterator(); l.hasNext(); ) {
			InstructionHandle handle = (InstructionHandle)l.next();
			statements.add(handle);
			if (handle.getInstruction() instanceof GOTO) {
				GOTO g = (GOTO)handle.getInstruction();
				flow.addEdge(new FlowEdge(handle, g.getTarget(), FlowEdge.NORMAL_EDGE));
			} else if (handle.getNext() != null) {
				flow.addEdge(new FlowEdge(handle, handle.getNext(), FlowEdge.NORMAL_EDGE));
			}
		}
		
		MethodGen mg = new MethodGen(Constants.ACC_PRIVATE, Type.VOID, Type.NO_ARGS, null, mainClass+"."+prologueName+prologueSig, "", prologue, prologueCP);
		MethodInfo mi = new MethodInfo(cliMap.get(mainClass), prologueName+prologueSig);
		mi.setMethodGen(mg);
		appInfo.getClassInfo(mainClass).getMethodInfoMap().put(prologueName+prologueSig, mi);
	}

	@SuppressWarnings({"unchecked"})
    public Map runAnalysis(Analysis analysis) {

		Interpreter interpreter = new Interpreter(analysis, this);

		try {
			MethodInfo prologue = getMethod(mainClass+"."+prologueName+prologueSig);

			Context context = new Context();
			context.stackPtr = 0;
			context.syncLevel = 0;
			context.constPool = new ConstantPoolGen(prologue.getMethod().getConstantPool());
			context.method = prologue.methodId;
			
			MethodInfo main = getMethod(mainClass+"."+mainName+mainSig);
			analysis.initialize(main, context);

			InstructionHandle entry = prologue.getMethodGen().getInstructionList().getStart();
			interpreter.interpret(context, entry, new HashMap(), true);
		} catch (Throwable thr) {
			thr.printStackTrace();
		}
		
		return analysis.getResult();
	}
	
	public <K,V> 
	Map runLocalAnalysis(Analysis<K,V> analysis, String methodName) {

		Interpreter<K,V> interpreter = new Interpreter<K,V>(analysis, this);

		try {
			MethodInfo start = getMethod(methodName);
			if(start == null) throw new AssertionError("No such method: "+methodName);
			Context context = new Context();
			context.stackPtr = start.getMethodGen().getMaxLocals();
			context.constPool = new ConstantPoolGen(start.getMethod().getConstantPool());
			context.method = start.getFQMethodName();

			analysis.initialize(start, context);
			InstructionHandle entry = start.getMethodGen().getInstructionList().getStart();
			interpreter.interpret(context, entry, new HashMap<InstructionHandle, ContextMap<K, V>>(), true);
		} catch (Throwable thr) {
			thr.printStackTrace();
		}
		
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
		for (Iterator<CallString> i = map.keySet().iterator(); i.hasNext(); ) {
			CallString c = i.next();
			if (c.hasSuffix(cs)) {
				retval.addAll(map.get(c));
			}
		}
		return retval;
	}
		
	public void setReceivers(Map<InstructionHandle, ContextMap<CallString, Set<String>>> receivers) {
		this.receivers = receivers;
	}
	
	public MethodInfo getMethod(String methodName) {
		String className = methodName.contains(".") ? methodName.substring(0, methodName.lastIndexOf(".")) : mainClass;
		String signature = methodName.substring(methodName.lastIndexOf(".")+1, methodName.length());
		DFAClassInfo cli = (DFAClassInfo)cliMap.get(className);
		return cli.getMethodInfo(signature);
	}

	public boolean containsField(String fieldName) {
		return classForField(fieldName) != null;
	}

	public String classForField(String fieldName) {
		String className = fieldName.substring(0, fieldName.lastIndexOf("."));
		String signature = fieldName.substring(fieldName.lastIndexOf(".")+1, fieldName.length());
		DFAClassInfo cli = (DFAClassInfo)cliMap.get(className);
		
		while (cli != null) {
// 			System.out.println("contains: "+cli+" vs "+fieldName);
			DFAClassInfo sup = (DFAClassInfo)cli.superClass;
			if (cli.getFields().contains(signature)
				&& !sup.getFields().contains(signature)) {
				return cli.clazz.getClassName();
			}
			cli = sup;
		}

		return null;
	}

	public LoopBounds getLoopBounds() {
		return loopBounds;
	}
	
	public void setLoopBounds(LoopBounds lb) {
		this.loopBounds = lb;
	}
	
}
