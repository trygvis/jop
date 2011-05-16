/*
 * This file is part of JOP, the Java Optimized Processor
 *   see <http://www.jopdesign.com/>
 *
 * Copyright (C) 2010, Stefan Hepp (stefan@stefant.org).
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

package com.jopdesign.common;

import com.jopdesign.common.KeyManager.CustomKey;
import com.jopdesign.common.KeyManager.KeyType;
import com.jopdesign.common.bcel.StackMapTable;
import com.jopdesign.common.code.CallString;
import com.jopdesign.common.code.ControlFlowGraph;
import com.jopdesign.common.code.ControlFlowGraph.CFGNode;
import com.jopdesign.common.code.ControlFlowGraph.InvokeNode;
import com.jopdesign.common.code.InvokeSite;
import com.jopdesign.common.code.LoopBound;
import com.jopdesign.common.logger.LogConfig;
import com.jopdesign.common.misc.AppInfoError;
import com.jopdesign.common.misc.BadGraphError;
import com.jopdesign.common.misc.BadGraphException;
import com.jopdesign.common.misc.HashedString;
import com.jopdesign.common.misc.JavaClassFormatError;
import com.jopdesign.common.misc.MiscUtils;
import com.jopdesign.common.processormodel.ProcessorModel;
import com.jopdesign.common.type.FieldRef;
import com.jopdesign.common.type.MethodRef;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.CPInstruction;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.FieldOrMethod;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LineNumberGen;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Stefan Hepp (stefan@stefant.org)
 */
public class MethodCode {

    /**
     * A key used to attach an {@link InvokeSite} object to an InstructionHandle.
     */
    private static final Object KEY_INVOKESITE = new HashedString("MethodInfo.InvokeSite");
    // Keys to attach custom values to InstructionHandles
    private static final Object KEY_CUSTOMVALUES = new HashedString("MethodCode.CustomValues");
    // Keys to attach values directly to InstructionHandles, which are not handled by KeyManager
    private static final Object KEY_LINENUMBER = new HashedString("MethodCode.LineNumber");
    private static final Object KEY_SOURCEFILE = new HashedString("MethodCode.SourceFile");
    // We attach the LoopBounds as CustomKeys, so we can use the KeyManager to clear/copy/.. them.
    // TODO we could also attach them directly to InstructionHandles to save one map access (but then make this key private!)
    public static final CustomKey KEY_LOOPBOUND;

    static {
        KEY_LOOPBOUND = KeyManager.getSingleton().registerKey(KeyType.CODE, "MethodCode.LoopBound");
    }

    // We do not include KEY_INVOKESITE because it should not be copied when the handle is copied
    private static final Object[] MANAGED_KEYS = {KEY_LINENUMBER, KEY_SOURCEFILE};

    private static final Logger logger = Logger.getLogger(LogConfig.LOG_CODE+".MethodCode");

    private final MethodInfo methodInfo;
    private final MethodGen methodGen;
    private ControlFlowGraph cfg;

    /**
     * Only to be used by MethodInfo.
     *
     * @param methodInfo reference to the method containing the code.
     */
    MethodCode(MethodInfo methodInfo) {
        this.methodInfo = methodInfo;
        methodGen = methodInfo.getInternalMethodGen();
    }

    public AppInfo getAppInfo() {
        return methodInfo.getAppInfo();
    }

    public ClassInfo getClassInfo() {
        return methodInfo.getClassInfo();
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public ConstantPoolGen getConstantPoolGen() {
        return methodInfo.getConstantPoolGen();
    }

    //////////////////////////////////////////////////////////////////////////////
    // Various wrappers to BCEL methods
    //////////////////////////////////////////////////////////////////////////////

    public int getMaxStack() {
        return methodGen.getMaxStack();
    }

    public int getMaxLocals() {
        return methodGen.getMaxLocals();
    }

    public LocalVariableGen addLocalVariable(String name, Type type, int slot, InstructionHandle start, InstructionHandle end) {
        return methodGen.addLocalVariable(name, type, slot, start, end);
    }

    public LocalVariableGen addLocalVariable(String name, Type type, InstructionHandle start, InstructionHandle end) {
        return methodGen.addLocalVariable(name, type, start, end);
    }

    public void removeLocalVariable(LocalVariableGen l) {
        methodGen.removeLocalVariable(l);
    }

    public void removeLocalVariables() {
        methodGen.removeLocalVariables();
    }

    public LocalVariableGen[] getLocalVariables() {
        return methodGen.getLocalVariables();
    }

    public Attribute[] getAttributes() {
        return methodGen.getCodeAttributes();
    }

    public void addAttribute(Attribute a) {
        methodGen.addCodeAttribute(a);
    }

    public void removeAttribute(Attribute a) {
        methodGen.removeCodeAttribute(a);
    }

    public void removeAttributes() {
        methodGen.removeCodeAttributes();
    }


    //////////////////////////////////////////////////////////////////////////////
    // Line number handling
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Get a table of linenumber entries.
     * If you want to get the line number of an instruction, use {@link #getLineNumber(InstructionHandle)} instead.
     *
     * @return a table of linenumber entries.
     */
    public LineNumberGen[] getLineNumbers() {
        return methodGen.getLineNumbers();
    }

    /**
     * Get the linenumber table attribute.
     * If you want to get the line number of an instruction, use {@link #getLineNumber(InstructionHandle)} instead.
     *
     * @return the linenumbers attribute.
     */
    public LineNumberTable getLineNumberTable() {
        return methodGen.getLineNumberTable(getConstantPoolGen());
    }

    /**
     * Remove a line number entry.
     * @param l the entry to remove.
     */
    public void removeLineNumber(LineNumberGen l) {
        methodGen.removeLineNumber(l);
    }

    /**
     * Remove all line numbers.
     */
    public void removeLineNumbers() {
        methodGen.removeLineNumbers();
    }

    public LineNumberGen setLineNumber(InstructionHandle ih, int src_line) {
        LineNumberGen lg = getLineNumberEntry(ih, false);
        if (lg != null) {
            lg.setSourceLine(src_line);
            return lg;
        }
        return methodGen.addLineNumber(ih, src_line);
    }

    public LineNumberGen getLineNumberEntry(InstructionHandle ih, boolean checkPrevious) {
        InstructionHandle prev = ih.getPrev();
        while (prev != null) {
            InstructionTargeter[] targeter = ih.getTargeters();
            if (targeter != null) {
                for (InstructionTargeter t : targeter) {
                    if (t instanceof LineNumberGen) {
                        // found a linenumber attached to this
                        return (LineNumberGen) t;
                    }
                }
            }
            // no match found
            if (checkPrevious) {
                prev = prev.getPrev();
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * Get the line number of the instruction. If instruction handle attributes are not used,
     * the positions of the instruction handles must be uptodate.
     *
     * @see #getSourceFileName(InstructionHandle)
     * @param ih the instruction to check.
     * @return the line number of the instruction, or -1 if unknown.
     */
    public int getLineNumber(InstructionHandle ih) {
        return getLineNumberTable().getSourceLine(ih.getPosition());
    }

    public void setSourceFileName(InstructionHandle ih, String filename) {
        ih.addAttribute(KEY_SOURCEFILE, filename);
    }

    public String getSourceFileName(InstructionHandle ih) {
        // We cannot store SourceFile info in Tables, so we always check the InstructionHandle..
        String source = (String) ih.getAttribute(KEY_SOURCEFILE);
        if (source != null) {
            return source;
        }
        return methodInfo.getClassInfo().getSourceFileName();
    }

    //////////////////////////////////////////////////////////////////////////////
    // Exception handling
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Add an exception which can be thrown by this method
     * @param class_name classname of the exception
     */
    public void addException(String class_name) {
        methodGen.addException(class_name);
    }

    /**
     * Remove an exception which is no longer thrown by this method.
     * @param c classname of the exception to remove
     */
    public void removeException(String c) {
        methodGen.removeException(c);
    }

    /**
     * This method does not throw any exceptions anymore, remove all exceptions which are thrown from this method.
     */
    public void removeExceptions() {
        methodGen.removeExceptions();
    }

    /**
     * @return a list of classnames of all exceptions which this method can throw
     */
    public String[] getExceptions() {
        return methodGen.getExceptions();
    }

    public CodeExceptionGen[] getExceptionHandlers() {
        return methodGen.getExceptionHandlers();
    }

    public CodeExceptionGen addExceptionHandler(InstructionHandle start_pc, InstructionHandle end_pc, InstructionHandle handler_pc, ObjectType catch_type) {
        return methodGen.addExceptionHandler(start_pc, end_pc, handler_pc, catch_type);
    }

    public void removeExceptionHandler(CodeExceptionGen c) {
        methodGen.removeExceptionHandler(c);
    }

    public void removeExceptionHandlers() {
        methodGen.removeExceptionHandlers();
    }

    public boolean isExceptionRangeStart(InstructionHandle ih) {
        return !getStartingExceptionRanges(ih).isEmpty();
    }

    public boolean isExceptionRangeEnd(InstructionHandle ih) {
        return !getEndingExceptionRanges(ih).isEmpty();
    }

    public boolean isExceptionRangeTarget(InstructionHandle ih) {
        return !getTargetingExceptionRanges(ih).isEmpty();
    }

    /**
     * Get a list of all exception ranges which start at this instruction.
     * @param ih the instruction to check
     * @return a list of all exception ranges with this instruction as start instruction, or an empty list.
     */
    public List<CodeExceptionGen> getStartingExceptionRanges(InstructionHandle ih) {
        List<CodeExceptionGen> list = new ArrayList<CodeExceptionGen>();
        InstructionTargeter[] targeters = ih.getTargeters();
        if (targeters == null) return list;
        for (InstructionTargeter t : targeters) {
            if (t instanceof CodeExceptionGen) {
                CodeExceptionGen ceg = (CodeExceptionGen) t;
                if (ceg.getStartPC().equals(ih)) {
                    list.add(ceg);
                }
            }
        }
        return list;
    }

    public List<CodeExceptionGen> getEndingExceptionRanges(InstructionHandle ih) {
        List<CodeExceptionGen> list = new ArrayList<CodeExceptionGen>();
        InstructionTargeter[] targeters = ih.getTargeters();
        if (targeters == null) return list;
        for (InstructionTargeter t : targeters) {
            if (t instanceof CodeExceptionGen) {
                CodeExceptionGen ceg = (CodeExceptionGen) t;
                if (ceg.getEndPC().equals(ih)) {
                    list.add(ceg);
                }
            }
        }
        return list;
    }

    public List<CodeExceptionGen> getTargetingExceptionRanges(InstructionHandle ih) {
        List<CodeExceptionGen> list = new ArrayList<CodeExceptionGen>();
        InstructionTargeter[] targeters = ih.getTargeters();
        if (targeters == null) return list;
        for (InstructionTargeter t : targeters) {
            if (t instanceof CodeExceptionGen) {
                CodeExceptionGen ceg = (CodeExceptionGen) t;
                if (ceg.getHandlerPC().equals(ih)) {
                    list.add(ceg);
                }
            }
        }
        return list;
    }

    //////////////////////////////////////////////////////////////////////////////
    // Code Access, Instruction Lists and CFG
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Get the instruction list of this code. If {@link #getControlFlowGraph(boolean)} has been used before, the CFG
     * will be compiled and removed first.
     * <p>
     * Do not call {@code dispose()} for the returned instruction list, or you will remove the instructions.
     * </p>
     *
     * @see #compile()
     * @return the instruction list of this code.
     */
    public InstructionList getInstructionList() {
        return getInstructionList(true, true);
    }

    /**
     * Get the instruction list of this code.
     *
     * @see #compile()
     * @see #removeCFG()
     * @param compileCFG if true, compile an existing CFG first, else ignore any changes made to CFG.
     * @param removeCFG if true, dispose the CFG of this method. If you want to modify the instruction list,
     *        you should set this to true to avoid inconsistencies. 
     * @return the instruction list of this code.
     */
    public InstructionList getInstructionList(boolean compileCFG, boolean removeCFG) {
        InstructionList list;
        if (compileCFG) {
            list = prepareInstructionList();
        } else {
            list = methodGen.getInstructionList();
        }

        if (removeCFG) {
            // If one only uses the IList to analyze code but does not modify it, we could keep an existing CFG.
            // Unfortunately, there is no 'const InstructionList' or 'UnmodifiableInstructionList', so we
            // can never be sure what the user will do with the list, so we kill the CFG to avoid inconsistencies.
            removeCFG();
        }

        return list;
    }

    /**
     * Set a new instruction list to this code.
     * <p>
     * IMPORTANT: If you use this method, make sure to update all targeters using
     * {@link #retarget(InstructionHandle, InstructionHandle)} first, else the various tables will link
     * to invalid handlers.
     * </p>
     * @see #getInstructionList()
     * @param il the new list
     */
    public void setInstructionList(InstructionList il) {
        methodGen.getInstructionList().dispose();
        methodGen.setInstructionList(il);
        removeCFG();
    }

    public InstructionHandle getInstructionHandle(int pos) {
        InstructionList il = getInstructionList();
        InstructionHandle ih = il.getStart();
        for (int i = 0; i < pos; i++) {
            ih = ih.getNext();
        }
        return ih;
    }

    /**
     * Retarget all targeters (jumps, branches, exception ranges, linenumbers,..) of a handle to a new handle.
     * @param oldHandle the old target
     * @param newHandle the new target
     */
    public void retarget(InstructionHandle oldHandle, InstructionHandle newHandle) {
        InstructionTargeter[] it = oldHandle.getTargeters();
        if (it == null) return;
        for (InstructionTargeter targeter : it) {
            targeter.updateTarget(oldHandle, newHandle);
        }
    }

    public void retarget(TargetLostException e, InstructionHandle newTarget) {
        InstructionHandle[] targets = e.getTargets();
        for (InstructionHandle target : targets) {
            InstructionTargeter[] targeters = target.getTargeters();
            for (InstructionTargeter targeter : targeters) {
                targeter.updateTarget(target, newTarget);
            }
        }
    }

    /**
     * Replace instructions in this code with an instruction list.
     * If the number of instructions to replace differs from the number of source instructions, instruction
     * handles will be removed or inserted appropriately and the targets will be updated.
     * <p>
     * The source instructions must use the constant pool of this method. Custom values will be copied.
     * </p>
     *
     * @param replaceStart the first instruction in this code to replace
     * @param replaceCount the number of instructions in this code to replace
     * @param source the instructions to use as replacement.
     * @return the first handle in the target list after the inserted code, or null if the last instruction in this
     *         list has been replaced.
     */
    public InstructionHandle replace(InstructionHandle replaceStart, int replaceCount, InstructionList source) {
        return replace(replaceStart, replaceCount, null, source, source.getStart(), source.getLength(), true);
    }

    /**
     * Replace instructions in this code with an instruction list.
     * If the number of instructions to replace differs from the number of source instructions, instruction
     * handles will be removed or inserted appropriately and the targets will be updated.
     * <p>
     * The source instructions must use the constant pool of this method.
     * </p>
     *
     * @param replaceStart the first instruction in this code to replace
     * @param replaceCount the number of instructions in this code to replace
     * @param source the instructions to use as replacement.
     * @param copyCustomKeys if true copy the custom values from the source.
     * @return the first handle in the target list after the inserted code, or null if the last instruction in this
     *         list has been replaced.
     */
    public InstructionHandle replace(InstructionHandle replaceStart, int replaceCount, InstructionList source,
                                     boolean copyCustomKeys)
    {
        return replace(replaceStart, replaceCount, null, source, source.getStart(), source.getLength(), copyCustomKeys);
    }

    /**
     * Replace instructions in this code with an instruction list.
     * If the number of instructions to replace differs from the number of source instructions, instruction
     * handles will be removed or inserted appropriately and the targets will be updated.
     *
     * @param replaceStart the first instruction in this code to replace
     * @param replaceCount the number of instructions in this code to replace
     * @param sourceInfo the MethodInfo containing the source instruction. If non-null, the instructions will be copied
     *                   using the constant pool from the given MethodInfo. If null, the instructions will not be copied.
     * @param source the instructions to use as replacement.
     * @param copyCustomKeys if true copy the custom values from the source.
     * @return the first handle in the target list after the inserted code, or null if the last instruction in this
     *         list has been replaced.
     */
    public InstructionHandle replace(InstructionHandle replaceStart, int replaceCount,
                                     MethodInfo sourceInfo, InstructionList source, boolean copyCustomKeys)
    {
        return replace(replaceStart, replaceCount, sourceInfo, source, source.getStart(), source.getLength(), copyCustomKeys);
    }


    /**
     * Replace instructions in this code with an instruction list or a part of it.
     * If the number of instructions to replace differs from the number of source instructions, instruction
     * handles will be removed or inserted appropriately and the targets will be updated.
     *
     * @param replaceStart the first instruction in this code to replace
     * @param replaceCount the number of instructions in this code to replace
     * @param sourceInfo the MethodInfo containing the source instruction. If non-null, the instructions will be copied
     *                   using the constant pool from the given MethodInfo. If null, the instructions will not be copied.
     * @param source the instructions to use as replacement.
     * @param sourceStart the first instruction in the source list to use for replacing the code.
     * @param sourceCount the number of instructions to use from the source.
     * @param copyCustomValues if true copy the custom values from the source.
     * @return the first handle in the target list after the inserted code, or null if the last instruction in this
     *         list has been replaced.
     */
    public InstructionHandle replace(InstructionHandle replaceStart, int replaceCount,
                                     MethodInfo sourceInfo, InstructionList source,
                                     InstructionHandle sourceStart, int sourceCount, boolean copyCustomValues)
    {
        InstructionList il = getInstructionList();

        InstructionHandle current = replaceStart;
        InstructionHandle currSource = sourceStart;

        // update the common prefix
        int cnt = Math.min(replaceCount, sourceCount);
        for (int i = 0; i < cnt; i++) {
            Instruction instr;
            if (sourceInfo != null) {
                instr = copyFrom(sourceInfo.getClassInfo(), currSource.getInstruction());
            } else {
                instr = currSource.getInstruction();
            }
            current.setInstruction(instr);

            if (copyCustomValues) {
                copyCustomValues(current, currSource);
            }

            current = current.getNext();
            currSource = currSource.getNext();
        }

        InstructionHandle next = current;

        // Case 1: delete unused handles, update targets to next instruction
        if (replaceCount > sourceCount) {
            int rest = replaceCount - sourceCount;
            for (int i=1; i < rest; i++) {
                next = next.getNext();
            }

            InstructionHandle end = next;
            next = next.getNext();

            try {
                // we cannot use next.getPrev, since next might be null
                il.delete(current, end);
            } catch (TargetLostException e) {
                retarget(e, next);
            }
        }

        // Case 2: insert new handles for rest of source
        if (replaceCount < sourceCount) {
            int rest = sourceCount - replaceCount;
            for (int i=0; i < rest; i++) {
                Instruction instr;
                if (sourceInfo != null) {
                    instr = copyFrom(sourceInfo.getClassInfo(), currSource.getInstruction());
                } else {
                    instr = currSource.getInstruction();
                }
                if (next == null) {
                    current = il.append(instr);
                } else {
                    current = il.insert(next, instr);
                }
                if (copyCustomValues) {
                    copyCustomValues(current, currSource);
                }
                currSource = currSource.getNext();
            }

        }

        return next;
    }


    /**
     * Create a copy of an instruction and if it uses the constantpool, copy and update the constantpool
     * reference too.
     * <p>Note that branch targets are not updated, so they will most likely be incorrect and need to be updated
     * manually.</p>                                                                                 d
     *
     * @param sourceClass the class containing the constantpool the instruction uses.
     * @param instruction the instruction to copy
     * @return a new copy of the instruction.
     */
    public Instruction copyFrom(ClassInfo sourceClass, Instruction instruction) {
        Instruction newInstr = instruction.copy();

        if (instruction instanceof CPInstruction) {
            int oldIndex = ((CPInstruction)instruction).getIndex();
            int newIndex = getClassInfo().addConstantInfo(sourceClass.getConstantInfo(oldIndex));
            ((CPInstruction)newInstr).setIndex(newIndex);
        }

        return newInstr;
    }

    public boolean isInvokeSite(InstructionHandle ih) {
        return ih.getInstruction() instanceof InvokeInstruction ||
               getAppInfo().getProcessorModel().isImplementedInJava(methodInfo, ih.getInstruction());
    }

    /**
     * Get the InvokeSite for an instruction handle from the code of this method.
     * This does not check if the given instruction is an invoke instruction.
     *
     * @param ih an instruction handle from this code
     * @return the InvokeSite associated with this instruction or a new one.
     */
    public InvokeSite getInvokeSite(InstructionHandle ih) {
        InvokeSite is = (InvokeSite) ih.getAttribute(KEY_INVOKESITE);
        if (is == null) {
            is = new InvokeSite(ih, this.getMethodInfo());
            ih.addAttribute(KEY_INVOKESITE, is);
        }
        return is;
    }

    /**
     * Get a list of invoke sites in this method code. This also returns invoke sites for special
     * instructions implemented in java.
     *
     * @return a list of all invoke sites in this code.
     */
    public Set<InvokeSite> getInvokeSites() {
        Set<InvokeSite> invokes = new HashSet<InvokeSite>();
        if (hasCFG()) {
            for (CFGNode node : cfg.getGraph().vertexSet()) {
                if (node instanceof InvokeNode) {
                    invokes.add( ((InvokeNode)node).getInvokeSite() );
                }
            }
        } else {
            for (InstructionHandle ih : methodGen.getInstructionList().getInstructionHandles()) {
                if (isInvokeSite(ih)) {
                    invokes.add( getInvokeSite(ih) );
                }
            }
        }
        return invokes;
    }

    public MethodRef getInvokeeRef(InstructionHandle ih) {
        return getInvokeSite(ih).getInvokeeRef();
    }
    
    public FieldRef getFieldRef(InstructionHandle ih) {
        return getFieldRef((FieldInstruction) ih.getInstruction());
    }
    
    public FieldRef getFieldRef(FieldInstruction instr) {
        ConstantPoolGen cpg = getConstantPoolGen();

        String classname = getReferencedClassName(instr);

        return getAppInfo().getFieldRef(classname, instr.getFieldName(cpg));
    }

    /**
     * Get the classname or array-type name referenced by an invoke- or field instruction in this code.
     *
     * @param instr the instruction to check, using this methods constantpool.
     * @return the referenced classname or array-typename, to be used for a ClassRef or AppInfo getter. 
     */
    public String getReferencedClassName(FieldOrMethod instr) {
        ConstantPoolGen cpg = getConstantPoolGen();

        ReferenceType refType = instr.getReferenceType(cpg);
        String classname;
        if (refType instanceof ObjectType) {
            classname = ((ObjectType)refType).getClassName();
        } else if (refType instanceof ArrayType) {
            // need to call array.<method>, which class should we use? Let's decide later..
            String msg = "Calling a method of an array: " +
                    refType.getSignature()+"#"+ instr.getName(cpg) + " in "+methodInfo;
            logger.debug(msg);
            classname = refType.getSignature();
        } else {
            // Hu??
            throw new JavaClassFormatError("Unknown reference type " +refType);
        }
        return classname;
    }


    public void removeNOPs() {
        prepareInstructionList();
        methodGen.removeNOPs();
    }

    /**
     * Remove attributes related to debugging (e.g. variable name mappings, stack maps, ..)
     * except the linenumber table.
     */
    public void removeDebugAttributes() {

        removeLocalVariables();

        for (Attribute a : getAttributes()) {
            if (a instanceof StackMapTable ||
                a instanceof StackMap)
            {
                removeAttribute(a);
                continue;
            }
            // Quick hack, just remove it (not yet supported by framework, we have no type for this attribute)
            ConstantUtf8 name = (ConstantUtf8) getConstantPoolGen().getConstant(a.getNameIndex());
            if ("LocalVariableTypeTable".equals(name.getBytes())) {
                removeAttribute(a);
            }
            // TODO also remove stuff like Annotations,..?
        }
    }

    /**
     * Get the control flow graph associated with this method code or create a new one.
     * @param clean if true, compile and recreate the graph if {@link ControlFlowGraph#isClean()} returns false.
     * @return the CFG for this method.
     */
    public ControlFlowGraph getControlFlowGraph(boolean clean) {

        // clean/isClean could be made more general (like the CallGraphConfiguration)

        if ( cfg != null && clean && !cfg.isClean()) {
            cfg.compile();
            cfg = null;
        }
        if ( this.cfg == null ) {
            try {
                cfg = new ControlFlowGraph(this.getMethodInfo());
                for (AppEventHandler ah : AppInfo.getSingleton().getEventHandlers()) {
                    ah.onCreateControlFlowGraph(cfg, clean);
                }
            } catch (BadGraphException e) {
                throw new BadGraphError("Unable to create CFG for " + methodInfo, e);
            }
        }

        return cfg;
    }

    public boolean hasCFG() {
        return cfg != null;
    }

    /**
     * Remove the CFG linked to this MethodCode, dismissing all changes made to it.
     */
    public void removeCFG() {
        if (cfg != null) {
            cfg.dispose();
            cfg = null;
        }
    }

    /**
     * Compile all changes, and update maxStack and maxLocals.
     */
    public void compile() {
        prepareInstructionList();
        methodGen.setMaxLocals();
        methodGen.setMaxStack();
    }

    /**
     * @see ProcessorModel#getNumberOfBytes(MethodInfo, Instruction) 
     * @param il the instruction list to get the size for
     * @return the number of bytes for a given instruction list on the target.
     */
    public int getNumberOfBytes(InstructionList il) {
        int sum = 0;
        ProcessorModel pm = getAppInfo().getProcessorModel();

        for (InstructionHandle ih : il.getInstructionHandles()) {
            sum += pm.getNumberOfBytes(methodInfo, ih.getInstruction());
        }

        return sum;
    }

    /**
     * Get the number of bytes of the code for the target architecture.
     * @see #getNumberOfBytes(boolean)
     * @see ControlFlowGraph#getNumberOfBytes()
     * @return the number of bytes of the code.
     */
    public int getNumberOfBytes() {
        return getNumberOfBytes(true);
    }

    /**
     * Get the length of the code attribute. This needs to compile the CFG first.
     * @see Code#length
     * @see ControlFlowGraph#getNumberOfBytes()
     * @param targetSize if true, use the processor model to get the code size. If false, the
     *        control flow graph needs to be compiled first.
     * @return the number of bytes of the code
     */
    public int getNumberOfBytes(boolean targetSize) {
        if (cfg == null || !targetSize) {
            InstructionList il = prepareInstructionList();
            if (!targetSize) {
                return methodGen.getMethod().getCode().getCode().length;
            } else {
                return getNumberOfBytes(il);
            }
        } else {
            return cfg.getNumberOfBytes();
        }
    }

    public int getNumberOfWords() {
        return MiscUtils.bytesToWords(getNumberOfBytes());
    }

    //////////////////////////////////////////////////////////////////////////////
    // LoopBound methods
    //////////////////////////////////////////////////////////////////////////////

    /**
     * Get the loopbound set to an instruction.
     * <p>
     * Loopbounds should be set to the last instruction of the loop head basic block
     * </p>
     *
     * @param ih the instruction handle to check.
     * @return the loopbound set for this instruction, or null if none has been set.
     */
    public LoopBound getLoopBound(InstructionHandle ih) {
        return (LoopBound) getCustomValue(ih, KEY_LOOPBOUND);
    }

    /**
     * Set a new loopbound. Overwrites the existing loopbound.
     * <p>
     * Loopbounds should be set to the last instruction of the loop head basic block
     * </p>
     *
     * @see #updateLoopBound(InstructionHandle, LoopBound)
     * @see LoopBound#boundedAbove(long)
     * @param ih the instruction handle to update.
     * @param loopBound the new loopbound to set, or null to clear it.
     */
    public void setLoopBound(InstructionHandle ih, LoopBound loopBound) {
        setCustomValue(ih, KEY_LOOPBOUND, loopBound);
        // Note: we do not return the old loopbound so that there is no confusion with updateLoopBound()
    }

    public LoopBound updateLoopBound(InstructionHandle ih, long upperBound) {
        if (upperBound < 0) {
            return getLoopBound(ih);
        }
        return updateLoopBound(ih, LoopBound.boundedAbove(upperBound));
    }

    /**
     * Tighten the existing loopbound with the given bound.
     * <p>
     * Loopbounds should be set to the last instruction of the loop head basic block
     * </p>
     * 
     * @param ih the instruction to update.
     * @param loopBound the loopbound used to tighten the existing one (can be null)
     * @return the new loopbound
     */
    public LoopBound updateLoopBound(InstructionHandle ih, LoopBound loopBound) {
        LoopBound oldLb = getLoopBound(ih);
        if (loopBound == null) {
            // nothing to update
            return oldLb;
        }
        if (oldLb == null) {
            // no old value, use new value
            setLoopBound(ih, loopBound);
            return loopBound;
        }

        // TODO tighten the old bound with the new bound

        // TODO we might want to know where the loopbounds came from so that we can generate messages
        // if the new loopbound is larger/smaller than the old one, so maybe we want to store the origin in LoopBound

        throw new AppInfoError("Implement me ..");
    }

    //////////////////////////////////////////////////////////////////////////////
    // Get and set CustomValues
    //////////////////////////////////////////////////////////////////////////////

    public Object setCustomValue(InstructionHandle ih, CustomKey key, Object value) {
        if (value == null) {
            return clearCustomKey(ih, key);
        }
        @SuppressWarnings({"unchecked"})
        Map<CustomKey,Object> map = (Map<CustomKey, Object>) ih.getAttribute(KEY_CUSTOMVALUES);
        if (map == null) {
            map = new HashMap<CustomKey, Object>(1);
            ih.addAttribute(KEY_CUSTOMVALUES, map);
        }
        return map.put(key, value);
    }

    public Object getCustomValue(InstructionHandle ih, CustomKey key) {
        @SuppressWarnings({"unchecked"})
        Map<CustomKey,Object> map = (Map<CustomKey, Object>) ih.getAttribute(KEY_CUSTOMVALUES);
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    public Object setCustomValue(InstructionHandle ih, CustomKey key, CallString context, Object value) {
        // TODO implement
        throw new AppInfoError("Not yet implemented.");
    }

    public Object getCustomValue(InstructionHandle ih, CustomKey key, CallString context, boolean checkSuffixes) {
        // TODO implement
        throw new AppInfoError("Not yet implemented.");
    }

    public Object clearCustomKey(InstructionHandle ih, CustomKey key) {
        @SuppressWarnings({"unchecked"})
        Map<CustomKey,Object> map = (Map<CustomKey, Object>) ih.getAttribute(KEY_CUSTOMVALUES);
        if (map == null) {
            return null;
        }
        Object value = map.remove(key);
        if (map.size() == 0) {
            ih.removeAttribute(KEY_CUSTOMVALUES);
        }
        return value;
    }

    public void copyCustomValues(InstructionHandle to, InstructionHandle from) {
        Object value;
        for (Object key : MANAGED_KEYS) {
            value = from.getAttribute(key);
            if (value != null) to.addAttribute(key, value);
            else to.removeAttribute(key);
        }

        @SuppressWarnings({"unchecked"})
        Map<CustomKey,Object> map = (Map<CustomKey, Object>) from.getAttribute(KEY_CUSTOMVALUES);
        if (map == null) {
            to.removeAttribute(KEY_CUSTOMVALUES);
            return;
        }
        Map<CustomKey,Object> newMap = new HashMap<CustomKey, Object>(map);
        to.addAttribute(KEY_CUSTOMVALUES, newMap);
    }


    //////////////////////////////////////////////////////////////////////////////
    // Private area. For staff only..
    //////////////////////////////////////////////////////////////////////////////

    private InstructionList prepareInstructionList() {
        if ( cfg != null ) {
            cfg.compile();
        }
        return methodGen.getInstructionList();
    }
}
