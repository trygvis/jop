package com.jopdesign.wcet.uppaal.translator;

import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;

import com.jopdesign.build.MethodInfo;
import com.jopdesign.wcet.Project;
import com.jopdesign.wcet.analysis.RecursiveAnalysis;
import com.jopdesign.wcet.analysis.WcetCost;
import com.jopdesign.wcet.frontend.ControlFlowGraph;
import com.jopdesign.wcet.frontend.SuperGraph;
import com.jopdesign.wcet.frontend.ControlFlowGraph.CFGNode;
import com.jopdesign.wcet.frontend.ControlFlowGraph.InvokeNode;
import com.jopdesign.wcet.frontend.SourceAnnotations.LoopBound;
import com.jopdesign.wcet.frontend.SuperGraph.SuperInvokeEdge;
import com.jopdesign.wcet.frontend.SuperGraph.SuperReturnEdge;
import com.jopdesign.wcet.graphutils.MiscUtils;
import com.jopdesign.wcet.graphutils.Pair;
import com.jopdesign.wcet.ipet.IpetConfig.StaticCacheApproximation;
import com.jopdesign.wcet.uppaal.UppAalConfig;
import com.jopdesign.wcet.uppaal.model.DuplicateKeyException;
import com.jopdesign.wcet.uppaal.model.Location;
import com.jopdesign.wcet.uppaal.model.Transition;

public class JavaOneProcessPerSupergraphTranslator extends JavaTranslator {

	public class InvokeViaCallStackBuilder extends InvokeBuilder {

		public InvokeViaCallStackBuilder(JavaTranslator mt, TemplateBuilder tBuilder) {
			super(mt,tBuilder, mt.cacheSim);
		}
		/* when syncing via callstack, we connect the invoke node to the entry, and the exit to
		 * to the return node. The former pushes the invoked method on the callstack, while the
		 * latter pops it if we have a match (pushdown automata).
		 * (non-Javadoc)
		 * @see com.jopdesign.wcet.uppaal.translator.InvokeBuilder#translateInvoke(com.jopdesign.wcet.uppaal.translator.MethodBuilder, com.jopdesign.wcet.frontend.ControlFlowGraph.InvokeNode, long)
		 */
		@Override
		public SubAutomaton translateInvoke(MethodBuilder mBuilder, InvokeNode n, long staticWCET) {
			int invokedID = callSiteIDs.get(n);
			String suffix = "_"+invokedID+"_"+n.getId();
			/* location for executing the code */
			SubAutomaton basicBlock = mBuilder.createBasicBlock(n.getId(),staticWCET);
			Location startInvokeNode = basicBlock.getEntry();
			Location finishInvokeNode;
			Location basicBlockNode = basicBlock.getExit();
			/* location for waiting */
			if(javaTranslator.getCacheSim().isDynamic()) {
				Location callNode   = tBuilder.createCommitedLocation("CALL"+suffix);
				Location returnNode = tBuilder.createLocation("RETURN"+suffix);
				finishInvokeNode    = tBuilder.createCommitedLocation("FINISH"+suffix);
				/* miss nodes */
				Location invokeMissNode = tBuilder.createLocation("INVOKE_MISS"+suffix);
				Location returnMissNode = tBuilder.createLocation("RETURN_MISS"+suffix);
				/* invoke access on incoming bb, insert miss node */
				Transition toInvokeHit    = tBuilder.createTransition(basicBlockNode, callNode);
				Transition toInvokeMiss   = tBuilder.createTransition(basicBlockNode, invokeMissNode);
	                                        tBuilder.createTransition(invokeMissNode, callNode);
				simulateCacheAccess(
						n.receiverFlowGraph(),true,
						basicBlockNode,  /* access cache on ingoing transitions */
						toInvokeHit,     /* if hit transition */
						toInvokeMiss,    /* if miss transition */
						invokeMissNode); /* miss node */

				simulateMethodInvocation(callNode,returnNode,invokedID, n);

				Transition toReturnHit    = tBuilder.createTransition(returnNode, finishInvokeNode);
				Transition toReturnMiss   = tBuilder.createTransition(returnNode, returnMissNode);
	            							tBuilder.createTransition(returnMissNode, finishInvokeNode);
				simulateCacheAccess(
						n.invokerFlowGraph(),false,
						returnNode,       /* access cache on ingoing transitions */
						toReturnHit,      /* if hit transition */
						toReturnMiss,     /* if miss transition */
						returnMissNode); /* miss node */
			} else {
				finishInvokeNode = tBuilder.createLocation("RETURN_"+invokedID+"_"+n.getId());
				simulateMethodInvocation(basicBlockNode,finishInvokeNode,invokedID, n);
			}
			return new SubAutomaton(startInvokeNode, finishInvokeNode);
		}
		private void simulateMethodInvocation(
				Location startInvokeNode, 
				Location endInvokeNode,
				int invokedID,
				InvokeNode n) {
			MethodInfo invoked = n.getImplementedMethod();
			if(n.receiverFlowGraph().isLeafMethod() && config.collapseLeaves) {
				RecursiveAnalysis<StaticCacheApproximation> ilpAn = 
					new RecursiveAnalysis<StaticCacheApproximation>(project,new RecursiveAnalysis.LocalIPETStrategy());
				WcetCost wcet = ilpAn.computeWCET(n.getImplementedMethod(), StaticCacheApproximation.ALWAYS_HIT);
				tBuilder.waitAtLocation(endInvokeNode, wcet.getCost());
				tBuilder.createTransition(startInvokeNode, endInvokeNode);
			} else {
				endInvokeNode.setCommited();
				Transition i = tBuilder.createTransition(startInvokeNode, this.javaTranslator.getMethodAutomaton(invoked).getEntry());
				Transition r = tBuilder.createTransition(javaTranslator.getMethodAutomaton(invoked).getExit(),endInvokeNode);
				i.getAttrs().appendUpdate("pushCallStack("+invokedID+")");
				r.getAttrs().appendGuard("matchCallStack("+invokedID+")");
				r.getAttrs().appendUpdate("popCallStack()");				
			}
		}
	}

	private HashMap<MethodInfo, Integer> methodMNDs;
	private SuperGraph superGraph;
	private HashMap<InvokeNode, Integer> callSiteIDs;

	public JavaOneProcessPerSupergraphTranslator(UppAalConfig c, Project p,MethodInfo root) {
		super(c, p, root);
		this.superGraph =  new SuperGraph(project.getWcetAppInfo(), project.getFlowGraph(root));
	}

	@Override
	protected void translate() {
		computeCallSiteIDs();
		systemBuilder.addCallStack(root,callSiteIDs.size());
		/* Create one template for root method */
		TemplateBuilder tBuilder = new TemplateBuilder(config,"Process",0,"t_local");
		tBuilder.addClock("t_local");
		SubAutomaton mRoot = tBuilder.getTemplateAutomaton();
		addMethodAutomaton(root,mRoot);
		recordLoops(tBuilder);
		/* Create start and end nodes for other methods */
		for(int i = 1; i < this.methodInfos.size(); i++) {
			MethodInfo mi = methodInfos.get(i);
			if(project.getCallGraph().isLeafNode(mi) && config.collapseLeaves) continue;

			SubAutomaton mAuto = tBuilder.createSubAutomaton(MiscUtils.qEncode(mi.getFQMethodName()));
			addMethodAutomaton(mi,mAuto);
		}
		int i = 0;
		for(MethodInfo mi : methodInfos) {
			if(project.getCallGraph().isLeafNode(mi) && config.collapseLeaves) continue;
			translateMethod(tBuilder, 
					        getMethodAutomaton(mi),
					        i++, 
					        mi, 
					        new InvokeViaCallStackBuilder(this,tBuilder));
		}
		tBuilder.getInitial().setCommited();
		addProgessMeasure(tBuilder);
		tBuilder.addPostEnd();
		try {
			systemBuilder.addTemplate(0,0, tBuilder.getFinalTemplate());
		} catch (DuplicateKeyException e) {
			throw new AssertionError("Unexpected exception: "+e);
		}
	}


	private void addProgessMeasure(TemplateBuilder tBuilder) {
		if(tBuilder.getLoopVarBounds().isEmpty()) return;
		Vector<String> progressSummands = new Vector<String>();
		Vector<Integer> lvbs = tBuilder.getLoopVarBounds();
		long multiplicator = 1;
		for(int i = lvbs.size() - 1; i >= 0; i--) {
			progressSummands.add(""+multiplicator+" * M0."+TemplateBuilder.loopVarName(i));
			multiplicator *= lvbs.get(i);
		}	
		// progress measure are not as safe as I hoped them to be :(
		// systemBuilder.addProgressMeasure(MiscUtils.joinStrings(progressSummands, " + "));
	}

	// Global maximal nesting depth is given by the equation
	//   node.gmnd = node.method.gmnd + (node.loop ? node.loop.nestingDepth : 0)
	//   method.gmnd = max { cs.method.gmnd + cs.gmnd | cs <- method.callsites }
	// Example: 
	//  main() { for() for() X: f(); }
	//  f() { for() for(HOL) }
	//  nesting depth of HOL is 2
	//  gmnd of f is gmnd of X = 2 + gmnd of main = 2
	//  gmnd of HOL is 4
	private void recordLoops(TemplateBuilder tBuilder) {
		computeMethodNestingDepths();
		for(MethodInfo m : methodInfos) {
			ControlFlowGraph cfg = project.getFlowGraph(m);
			for( Entry<CFGNode, LoopBound> entry : cfg.getLoopBounds().entrySet()) {
				CFGNode hol = entry.getKey();
				LoopBound lb = entry.getValue();
				int nesting = cfg.getLoopColoring().getLoopColor(hol).size();
				int gmnd = nesting + methodMNDs.get(m);
				tBuilder.addLoop(hol, gmnd, lb);
			}
		}
		if(config.debug) tBuilder.dumpLoops();
	}

	private void computeMethodNestingDepths() {
		this.methodMNDs = new HashMap<MethodInfo,Integer>();
		/* for methods in topological order */
		for(MethodInfo m : project.getCallGraph().getImplementedMethods(project.getTargetMethod())) {
			int maxCaller = 0;
			ControlFlowGraph cfgInvoked = project.getFlowGraph(m);
			for(Pair<SuperInvokeEdge,SuperReturnEdge> callSite : superGraph.getCallSites(cfgInvoked)) {
				InvokeNode callSiteNode = callSite.fst().getInvokeNode();
				ControlFlowGraph cfgInvoker = callSiteNode.invokerFlowGraph();
				int callerRootDepth = methodMNDs.get(cfgInvoker.getMethodInfo());
				int nestingDepth    = cfgInvoker.getLoopColoring().getLoopColor(callSiteNode).size();
				maxCaller = Math.max(maxCaller, callerRootDepth + nestingDepth);
			}
			methodMNDs.put(m,maxCaller);
		}
		if(config.debug) MiscUtils.printMap(System.out, methodMNDs,30);
	}
	private void computeCallSiteIDs() {
		this.callSiteIDs = new HashMap<InvokeNode,Integer>();
		int i = 0;
		for(SuperInvokeEdge e : superGraph.getSuperEdgePairs().keySet()) {
			callSiteIDs.put(e.getInvokeNode(), i++);
		}
	}

}
