/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>

  Copyright (C) 2008, Benedikt Huber (benedikt.huber@gmail.com)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.jopdesign.wcet08;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedMap;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.log4j.Logger;

import com.jopdesign.build.AppInfo;
import com.jopdesign.build.AppVisitor;
import com.jopdesign.build.ClassInfo;
import com.jopdesign.build.MethodInfo;
import com.jopdesign.build.WcetPreprocess;
import com.jopdesign.dfa.analyses.LoopBounds;
import com.jopdesign.dfa.analyses.ReceiverTypes;
import com.jopdesign.wcet08.frontend.CallGraph;
import com.jopdesign.wcet08.frontend.FlowGraph;
import com.jopdesign.wcet08.frontend.WcetAppInfo;
import com.jopdesign.wcet08.frontend.SourceAnnotations;
import com.jopdesign.wcet08.frontend.SourceAnnotations.LoopBound;
import com.jopdesign.wcet08.report.Report;

/** WCET 'project', information on which method in which class to analyse etc. */
public class Project {
	public static class UnsupportedFeatureException extends Exception {
		private static final long serialVersionUID = 1L;
		public UnsupportedFeatureException(String msg) {
			super(msg);
		}
	}
	/**
	 * Remove NOPs in all reachable classes
	 */
	public static class RemoveNops extends AppVisitor {
		public RemoveNops(AppInfo ai) {
			super(ai);
		}
		@Override
		public void visitJavaClass(JavaClass clazz) {
			super.visitJavaClass(clazz);
			ClassInfo cli = super.getCli();
			for(MethodInfo m : cli.getMethods()) {
				m.getMethodGen().removeNOPs();
				m.updateMethodFromGen();
			}
		}		
	}
	/**
	 * Set {@link MethodGen} in all reachable classes 
	 */
	public static class CreateMethodGenerators extends AppVisitor {
		public CreateMethodGenerators(AppInfo ai) {
			super(ai);
		}
		public void visitJavaClass(JavaClass clazz) {
			super.visitJavaClass(clazz);
			ConstantPoolGen cpg = new ConstantPoolGen(clazz.getConstantPool());
			Method[] methods = clazz.getMethods();
			for(int i=0; i < methods.length; i++) {
				if(!(methods[i].isAbstract() || methods[i].isNative())) {
					Method m = methods[i];
			        MethodInfo mi = getCli().getMethodInfo(m.getName()+m.getSignature());
			        mi.setMethodGen(new MethodGen(m,
			        							  mi.getCli().clazz.getClassName(),
			        							  cpg));
				}
			}
		}
	}

	public static final Logger logger = Logger.getLogger(Project.class);
	private Logger topLevelLogger = Logger.getLogger(Project.class); /* special logger */
	public void setTopLevelLooger(Logger tlLogger) {
		this.topLevelLogger = tlLogger;		
	}

	private Config config;

	private String className;
	private String methodName;

	private WcetAppInfo wcetAppInfo;
	private CallGraph callGraph;

	private Report results;
		
	private Map<ClassInfo, SortedMap<Integer, LoopBound>> annotationMap;

	private LoopBounds dfaLoopBounds;

	public CallGraph getCallGraph() {
		return callGraph;
	}
	public ClassInfo getRootClass() {
		return callGraph.getRootClass();
	}
	public MethodInfo getRootMethod() {
		return callGraph.getRootMethod();
	}
	public Report getReport() { return results; }
	
	public Project() {
		this.config = Config.instance();
		this.className = config.getRootClassName();
		this.methodName = config.getRootMethodName();
		this.results = new Report(this);
	}
	
	public static AppInfo loadApp() throws IOException {
		AppInfo appInfo;
		Config config = Config.instance();
		if(config.doDataflowAnalysis()) {
			appInfo = new com.jopdesign.dfa.framework.AppInfo(
							new com.jopdesign.dfa.framework.ClassInfo());
		} else {
			appInfo = new AppInfo(ClassInfo.getTemplate());
		}
		appInfo.configure(config.getClassPath(),
		                  config.getSourcePath(),
		                  config.getRootClassName(),
		                  config.getRootMethodName());
		appInfo.addClass(WcetAppInfo.JVM_CLASS);
		if(config.doDataflowAnalysis()) {			
			appInfo.load();
			appInfo.iterate(new RemoveNops(appInfo));
		} else {
			appInfo.load();
			WcetPreprocess.preprocess(appInfo);
			appInfo.iterate(new CreateMethodGenerators(appInfo));			
		}
		return appInfo;
	}
	
	public void load() throws Exception  {
		AppInfo appInfo = loadApp();
		wcetAppInfo = new WcetAppInfo(appInfo);

		/* run dataflow analysis */
		if(Config.instance().doDataflowAnalysis()) {
			topLevelLogger.info("Starting DFA analysis");
			dataflowAnalysis();
			topLevelLogger.info("DFA analysis finished");
		}
		
		/* build callgraph */
		callGraph = CallGraph.buildCallGraph(wcetAppInfo,className,methodName);

		/* Load source code annotations */
		annotationMap = new Hashtable<ClassInfo, SortedMap<Integer,LoopBound>>();
		SourceAnnotations sourceAnnotations = new SourceAnnotations(config);
		for(ClassInfo ci : callGraph.getClassInfos()) {
			annotationMap.put(ci,sourceAnnotations.calculateWCA(ci));
		}
		/* Analyse control flow graphs */
		wcetAppInfo.analyseFlowGraphs(this, this.callGraph.getImplementedMethods());
	}

	public WcetAppInfo getWcetAppInfo() {
		return this.wcetAppInfo;
	}
	public FlowGraph getRootFlowGraph() {
		return wcetAppInfo.getFlowGraph(this.getRootMethod());
	}	
	public SortedMap<Integer, LoopBound> getAnnotations(ClassInfo cli) {
		return this.annotationMap.get(cli);
	}
	
	/* Data flow analysis
	 * ------------------
	 */
	
	public com.jopdesign.dfa.framework.AppInfo getDfaProgram() {
		return (com.jopdesign.dfa.framework.AppInfo) this.wcetAppInfo.getAppInfo();
	}
	
	public void dataflowAnalysis() {
		com.jopdesign.dfa.framework.AppInfo program = getDfaProgram();
		topLevelLogger.info("Receiver analysis");
		ReceiverTypes recTys = new ReceiverTypes();
		Map receiverResults = program.runAnalysis(recTys);
		program.setReceivers(receiverResults);
		topLevelLogger.info("Loop bound analysis");
		dfaLoopBounds = new LoopBounds();
		program.runAnalysis(dfaLoopBounds);
	}
	
	public LoopBounds getDfaLoopBounds() {
		return this.dfaLoopBounds;
	}
}
