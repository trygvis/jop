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
package com.jopdesign.wcet.analysis;

import com.jopdesign.common.AppInfo;
import com.jopdesign.common.ClassInfo;
import com.jopdesign.common.MethodInfo;
import com.jopdesign.common.code.BasicBlock;
import com.jopdesign.common.code.ControlFlowGraph;
import com.jopdesign.common.code.ControlFlowGraph.BasicBlockNode;
import com.jopdesign.common.code.ControlFlowGraph.CFGNode;
import com.jopdesign.wcet.WCETProcessorModel;
import com.jopdesign.wcet.WCETTool;
import com.jopdesign.wcet.analysis.RecursiveAnalysis.CacheKey;
import com.jopdesign.wcet.ipet.CostProvider;
import com.jopdesign.wcet.ipet.IPETBuilder;
import com.jopdesign.wcet.ipet.IPETConfig;
import com.jopdesign.wcet.report.ClassReport;
import org.apache.log4j.Logger;
import org.jgrapht.DirectedGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Simple and fast local analysis, with the possibility to use more expensive analysis
 * methods (global IPET for miss-once fit-all, UPPAAL) for parts of the program.
 *
 * @author Benedikt Huber (benedikt.huber@gmail.com)
 *
 */

public class RecursiveWcetAnalysis<Context extends AnalysisContext>
             extends RecursiveAnalysis<Context, WcetCost> {

	/** Visitor for computing the WCET of CFG nodes */
	private class LocalWcetVisitor extends WcetVisitor {
		Context ctx;
		public LocalWcetVisitor(WCETTool project, Context ctx) {
			super(project);
			this.ctx = ctx;
		}
		@Override
		public void visitSummaryNode(ControlFlowGraph.SummaryNode n) {
			cost.addCost(runWCETComputation("summary",n.getSubGraph(),ctx).getCost());
		}
		@Override
		public void visitInvokeNode(ControlFlowGraph.InvokeNode n) {

			// FIXME: [Bug #3] Hackish implementation of callgraph pruning
			if(n.getVirtualNode() != null) {
				Set<MethodInfo> actuallyReachable =
					n.getVirtualNode().getImplementedMethods(ctx.getCallString());
				if(! actuallyReachable.contains(n.getImplementedMethod())) return;
			}
			
			cost.addLocalCost(processor.getExecutionTime(ctx.getExecutionContext(n),n.getInstructionHandle()));
			if(n.isVirtual()) {
				throw new AssertionError("Invoke node "+n.getReferenced()+" without implementation in WCET analysis - did you preprocess virtual methods ?");
			}
			cost.addCost(RecursiveWcetAnalysis.this.recursiveWCET.recursiveCost(RecursiveWcetAnalysis.this, n, ctx));
		}
		@Override
		public void visitBasicBlockNode(BasicBlockNode n) {
			cost.addLocalCost(project.getWCETProcessorModel().basicBlockWCET(ctx.getExecutionContext(n),n.getBasicBlock()));
		}
	}

	/** Solution to local WCET problem */
	public class LocalWCETSolution {
		private DirectedGraph<CFGNode, ControlFlowGraph.CFGEdge> graph;

		private long lpCost;
		private WcetCost cost;
		private Map<CFGNode,Long> nodeFlow;
		private Map<ControlFlowGraph.CFGEdge,Long> edgeFlow;
		
		private Map<CFGNode, WcetCost> nodeCosts;

		public LocalWCETSolution(DirectedGraph<CFGNode,ControlFlowGraph.CFGEdge> g, Map<CFGNode,WcetCost> nodeCosts) {

			this.graph = g;
			this.nodeCosts= nodeCosts;
		}
		public Map<CFGNode, WcetCost> getNodeCostMap() {
			return nodeCosts;
		}
		public void setSolution(long lpCost, Map<IPETBuilder.ExecutionEdge, Long> executionEdgeFlow) {
			this.lpCost = lpCost;
			this.edgeFlow = RecursiveWcetAnalysis.executionToProgramFlow(graph, executionEdgeFlow);
			this.nodeFlow = edgeToNodeFlow(graph, edgeFlow);
			computeCost();
		}
		public long getLpCost() {
			return lpCost;
		}
		public WcetCost getCost() {
			return cost;
		}
		public WcetCost getTotalCost() {
			WcetCost tCost = cost.clone();
			tCost.moveLocalToGlobalCost();
			return tCost;
		}
		public long getNodeFlow(CFGNode n) {
			return getNodeFlow().get(n);
		}
		public Map<CFGNode, Long> getNodeFlow() {
			return nodeFlow;
		}
		public Map<ControlFlowGraph.CFGEdge, Long> getEdgeFlow() {
			return edgeFlow;
		}
		/** Safety check: compare flow*cost to actual solution */
		public void checkConsistentency() {
			if(cost.getCost() != lpCost) {
				throw new AssertionError("The solution implies that the flow graph cost is "
										 + cost.getCost() + ", but the ILP solver reported "+lpCost);
			}
		}
		/* Compute cost, separating local and non-local cost */
		private void computeCost() {
			cost = new WcetCost();
			for(CFGNode n : graph.vertexSet()) {
				long flow = nodeFlow.get(n);
				cost.addLocalCost(flow * nodeCosts.get(n).getLocalCost());
				cost.addCacheCost(flow * nodeCosts.get(n).getCacheCost());
				cost.addNonLocalCost(flow * nodeCosts.get(n).getNonLocalCost());
			    cost.addPotentialCacheFlushes((int)flow * nodeCosts.get(n).getPotentialCacheFlushes());
			}
		}
	}
	/** Provide execution cost using a node->cost table
	 */
	public static class WcetCostProvider<T> implements CostProvider<T> {
		private Map<T, WcetCost> costMap;
		private WcetCost defCost;
		public WcetCostProvider(Map<T,WcetCost> costMap) {
			this.costMap = costMap;
			this.defCost = null;
		}
		public WcetCostProvider(Map<T,WcetCost> costMap, WcetCost defCost) {
			this.costMap = costMap;
			this.defCost = defCost;
		}
		public long getCost(T obj) {
			WcetCost cost = costMap.get(obj);
			if(cost == null) {
				if(defCost == null) {
					throw new AssertionError("Missing entry for "+obj+" in cost map");
				} else {
					return defCost.getCost();
				}
			} else {
				return cost.getCost();
			}
		}
	}

	static final Logger logger = Logger.getLogger(WCETTool.LOG_WCET_ANALYSIS+".RecursiveWcetAnalysis");
	private AppInfo appInfo;
	private WCETProcessorModel processor;
	private RecursiveAnalysis.RecursiveStrategy<Context, WcetCost> recursiveWCET;
	private Set<MethodInfo> costsPerLineReported = new HashSet<MethodInfo>();

	public RecursiveWcetAnalysis(WCETTool project,
			RecursiveAnalysis.RecursiveStrategy<Context, WcetCost> recursiveStrategy) {
		this(project, new IPETConfig(project.getConfig()), recursiveStrategy);
	}

	public RecursiveWcetAnalysis(WCETTool project,
			                 IPETConfig ipetConfig,
			                 RecursiveAnalysis.RecursiveStrategy<Context,WcetCost> recursiveStrategy) {
		super(project, ipetConfig);
		this.appInfo = project.getAppInfo();
		this.processor = project.getWCETProcessorModel();

		this.recursiveWCET = recursiveStrategy;
	}
	
	/**
	 * WCET analysis of the given method, using some strategy for recursive WCET calculation and cache
	 * approximation.cache approximation scheme.
	 * @param m the method to be analyzed
	 * @return
	 *
	 * <p>FIXME: Logging/Report is somewhat broken and messy </p>
	 */
	public WcetCost computeCost(MethodInfo m, Context ctx) {
		/* use memoization to speed up analysis */
		CacheKey key = new CacheKey(m,ctx);
		if(isCached(key)) return getCached(key);
		/* compute solution */
		LocalWCETSolution sol = runWCETComputation(key.toString(), getWCETTool().getFlowGraph(m), ctx);
		sol.checkConsistentency();
		recordCost(key, sol.getCost());
		/* Logging and Report */
		logger.debug("WCET for " + key + ": "+sol.getCost());
		if(getWCETTool().reportGenerationActive()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Report generation active: "+m+" in context "+ctx);
			}
			updateReport(key, sol);
		}

		return sol.getTotalCost();
	}

	public LocalWCETSolution runWCETComputation(
			String key,
			ControlFlowGraph cfg, 
			Context ctx) {
		Map<CFGNode,WcetCost> nodeCosts = buildNodeCostMap(cfg,ctx);
		CostProvider<CFGNode> costProvider = getCostProvider(nodeCosts);

		Map<IPETBuilder.ExecutionEdge, Long> edgeFlowOut = new HashMap<IPETBuilder.ExecutionEdge, Long>();
		long maxCost = runLocalComputation(key, cfg, ctx, costProvider, edgeFlowOut );
		LocalWCETSolution sol = new LocalWCETSolution(cfg.getGraph(),nodeCosts);
		sol.setSolution(maxCost, edgeFlowOut);
		return sol;
	}
	
	// FIXME: [recursive-wcet-analysis] Report generation is a big mess
	// FIXME: [recursive-wcet-analysis] For now, we only add line costs once per method
	private void updateReport(CacheKey key, LocalWCETSolution sol) {
		MethodInfo m = key.m;
		HashMap<CFGNode, String> nodeFlowCostDescrs = new HashMap<CFGNode, String>();
		updateClassReport(key, sol);
		Map<String,Object> stats = new HashMap<String, Object>();
		stats.put("WCET",sol.getCost());
		stats.put("mode",key.ctx);
		stats.put("all-methods-fit-in-cache", getWCETTool().getWCETProcessorModel().getMethodCache().allFit(m,null));
		getWCETTool().getReport().addDetailedReport(m,"WCET_"+key.ctx.toString(),stats,nodeFlowCostDescrs,sol.getEdgeFlow());
	}

	/**
	 * Update class report (cost per line number)
	 * @param key
	 * @param sol
	 * FIXME: Currently only reported once per method
	 */
	private void updateClassReport(CacheKey key, LocalWCETSolution sol) {
		MethodInfo m = key.m;
		if(costsPerLineReported .contains(m)) return;
		costsPerLineReported.add(m);
		
		Map<CFGNode,WcetCost> nodeCosts = sol.getNodeCostMap();
		HashMap<CFGNode, String> nodeFlowCostDescrs = new HashMap<CFGNode, String>();

		for(Entry<CFGNode, WcetCost> entry: nodeCosts.entrySet()) {
			CFGNode n = entry.getKey();
			WcetCost cost = entry.getValue();
			if(sol.getNodeFlow(n) > 0) {
				nodeFlowCostDescrs.put(n,cost.toString());
				BasicBlock basicBlock = n.getBasicBlock();
				/* prototyping */
				if(basicBlock != null) {
					TreeSet<Integer> lineRange = basicBlock.getSourceLineRange();
					if(lineRange.isEmpty()) {
						logger.error("No source code lines associated with basic block ! ");
					}
					ClassInfo cli = basicBlock.getClassInfo();
					ClassReport cr = getWCETTool().getReport().getClassReport(cli);
					Long oldCost = (Long) cr.getLineProperty(lineRange.first(), "cost");
					if(oldCost == null) oldCost = 0L;
					long newCost = sol.getNodeFlow(n)*nodeCosts.get(n).getCost();

					if(logger.isTraceEnabled()) {
						logger.trace("Attaching cost "+oldCost + " + " + 
								newCost+" ( " + sol.getNodeFlow(n)+ " * " + nodeCosts.get(n).getCost() + " )" + 
								" to line "+lineRange.first() + " in " + basicBlock.getMethodInfo());
					}
					
					cr.addLineProperty(lineRange.first(), "cost", oldCost + newCost);
					for(int i : lineRange) {
						cr.addLineProperty(i, "color", "red");
					}
				}
			} else {
				nodeFlowCostDescrs.put(n, ""+nodeCosts.get(n).getCost());
			}
		}		
	}

	@Override
	public WcetCost computeCostOfNode(CFGNode n ,Context ctx) {
		WcetVisitor wcetVisitor = new LocalWcetVisitor(getWCETTool(), ctx);
		return wcetVisitor.computeCost(n);
	}

	@Override
	protected CostProvider<CFGNode> getCostProvider(
			Map<CFGNode, WcetCost> nodeCosts) {
		return new WcetCostProvider<CFGNode>(nodeCosts);
	}

	// currently unused
	@Override
	protected WcetCost extractSolution(ControlFlowGraph cfg,
			Map<CFGNode, WcetCost> nodeCosts,
			long maxCost,
			Map<IPETBuilder.ExecutionEdge, Long> edgeFlowOut) {
		LocalWCETSolution sol = new LocalWCETSolution(cfg.getGraph(),nodeCosts);
		sol.setSolution(maxCost, edgeFlowOut);
		return sol.getCost();
	}

	/**
	 * Extract CFGEdge execution frequencies from IPET ExecutionEdge flow.
	 * Eliminates context dependencies and low level models.
	 * @param graph
	 * @param executionEdgeFlow
	 * @return
	 */
	public static Map<ControlFlowGraph.CFGEdge, Long> executionToProgramFlow(
			DirectedGraph<CFGNode, ControlFlowGraph.CFGEdge> graph,
			Map<IPETBuilder.ExecutionEdge, Long> executionEdgeFlow) {
		Map<ControlFlowGraph.CFGEdge, Long> cfgEdgeFlow = new HashMap<ControlFlowGraph.CFGEdge, Long>();
		for(Entry<IPETBuilder.ExecutionEdge, Long> entry : executionEdgeFlow.entrySet()) {

			ControlFlowGraph.CFGEdge cfgEdge = entry.getKey().getModelledEdge();
			if(cfgEdge == null) continue;
			Long oldFlow = cfgEdgeFlow.get(cfgEdge);
			if(oldFlow == null) oldFlow = 0L;
			cfgEdgeFlow.put(cfgEdge, oldFlow + entry.getValue());
		}
		return cfgEdgeFlow;
	}

	public static Map<CFGNode, Long> edgeToNodeFlow(DirectedGraph<CFGNode,ControlFlowGraph.CFGEdge> graph, Map<ControlFlowGraph.CFGEdge, Long> cfgEdgeFlow) {
		
		HashMap<CFGNode, Long> nodeFlow = new HashMap<CFGNode, Long>();
		for(CFGNode n : graph.vertexSet()) {
		
			if(graph.inDegreeOf(n) == 0) nodeFlow.put(n, 0L); // ENTRY and DEAD CODE (no flow)
			else {
				long flow = 0;
				for(ControlFlowGraph.CFGEdge inEdge : graph.incomingEdgesOf(n)) {
					flow+=cfgEdgeFlow.get(inEdge);
				}
				nodeFlow.put(n, flow);
			}
		}

		return nodeFlow;
	}

}
