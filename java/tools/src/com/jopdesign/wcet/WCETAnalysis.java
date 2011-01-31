/*
  This file is part of JOP, the Java Optimized Processor
    see <http://www.jopdesign.com/>

  Copyright (C) 2006-2008, Martin Schoeberl (martin@jopdesign.com)
  Copyright (C) 2008-2009, Benedikt Huber (benedikt.huber@gmail.com)

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

/* Notes: WCET times reported by JOP (noted if JopSIM+cache-timing differs)
 * Method.java: 12039
 * StartKfl.java: 3048-11200
 * StartLift.java: 4638-4772 (JopSIM: 4636-4774)
 */

package com.jopdesign.wcet;

import com.jopdesign.common.AppInfo;
import com.jopdesign.common.AppSetup;
import com.jopdesign.common.MethodInfo;
import com.jopdesign.common.code.CallString;
import com.jopdesign.common.config.Config;
import com.jopdesign.common.misc.MiscUtils;
import com.jopdesign.dfa.DFATool;
import com.jopdesign.wcet.analysis.AnalysisContextLocal;
import com.jopdesign.wcet.analysis.GlobalAnalysis;
import com.jopdesign.wcet.analysis.LocalAnalysis;
import com.jopdesign.wcet.analysis.RecursiveAnalysis.RecursiveStrategy;
import com.jopdesign.wcet.analysis.RecursiveWcetAnalysis;
import com.jopdesign.wcet.analysis.TreeAnalysis;
import com.jopdesign.wcet.analysis.UppaalAnalysis;
import com.jopdesign.wcet.analysis.WcetCost;
import com.jopdesign.wcet.ipet.IPETConfig;
import com.jopdesign.wcet.ipet.IPETConfig.StaticCacheApproximation;
import com.jopdesign.wcet.ipet.LpSolveWrapper;
import com.jopdesign.wcet.uppaal.UppAalConfig;
import com.jopdesign.wcet.uppaal.model.DuplicateKeyException;
import com.jopdesign.wcet.uppaal.model.XmlSerializationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

import static com.jopdesign.wcet.ExecHelper.timeDiff;

/**
 * WCET Analysis for JOP - Executable
 */
public class WCETAnalysis {
    private static final String CONFIG_FILE_NAME = "wcetanalysis.properties";
    private static final boolean CALCULATE_MINIMUM_CACHE_COST = false;

    public static void main(String[] args) {

        // We set a different output path for this tool if invoked by cmdline
        // Note that WCETTool could also override defaults, but we do not want to change the
        // default value of outdir if WCETTool is invoked from another tool
        Properties defaultProps = new Properties();
        defaultProps.put("outdir", "java/target/wcet");

        AppSetup setup = new AppSetup(defaultProps, false);
        setup.setVersionInfo("1.0.1");
        // We do not load a config file automatically, user has to specify it explicitly to avoid
        // unintentional misconfiguration
        //setup.setConfigFilename(CONFIG_FILE_NAME);
        setup.setUsageInfo("WCETAnalysis", "WCET Analysis tool");

        WCETTool wcetTool = new WCETTool();
        DFATool dfaTool = new DFATool();

        setup.registerTool("dfa", dfaTool, true, false);
        setup.registerTool("wcet", wcetTool);

        AppInfo appInfo = setup.initAndLoad(args, true, false, false);

        if (setup.useTool("dfa")) {
            wcetTool.setDfaTool(dfaTool);
        }

        ExecHelper exec = new ExecHelper(setup.getConfig(), Logger.getLogger(WCETAnalysis.class));
        exec.dumpConfig();           /* Load config */
        exec.checkLibs();                /* check environment */

        WCETAnalysis inst = new WCETAnalysis(wcetTool, exec);

        if(! inst.run()) exec.bail("Worst Case Analysis failed");
        else             exec.info("Worst Case Analysis finished");        	
    }

    private Config config;
    private WCETTool project;
    private ExecHelper exec;
    private WcetCost wcet;
    private WcetCost alwaysMissCost;
    private WcetCost alwaysHitCost;
    private WcetCost minCacheCost;
    private IPETConfig ipetConfig;

    public WCETAnalysis(WCETTool wcetTool, ExecHelper e) {
        this.project = wcetTool;
        this.config = wcetTool.getConfig();
        this.exec   = e;
    }

    private boolean run() {
        /* Initialize */
        try {
            project.setTopLevelLogger(exec.getExecLogger());
            exec.info("Loading project");
            project.initialize();
            MethodInfo largestMethod = project.getWCETProcessorModel().getMethodCache().checkCache();
            int minWords = MiscUtils.bytesToWords(largestMethod.getCode().getLength());
            reportMetric("min-cache-size",largestMethod.getFQMethodName(),minWords);
        } catch (Exception e) {
            exec.logException("Loading project", e);
            return false;
        }

        // project.getLinkerInfo().dump(System.out);
        // new ConstantCache(project).build().dumpStats();

        /* Perf-Test */
//        for(int i = 0; i < 50; i++) {
//            RecursiveAnalysis<StaticCacheApproximation> an =
//                new RecursiveAnalysis<StaticCacheApproximation>(project,new RecursiveAnalysis.LocalIPETStrategy());
//            an.computeWCET(project.getTargetMethod(),StaticCacheApproximation.ALWAYS_HIT);
//        }
//        System.err.println("Total solver time (50): "+LpSolveWrapper.getSolverTime());
//        System.exit(1);

        
        if(project.getProjectConfig().doObjectCacheAnalysis()) {
            ObjectCacheAnalysis oca = new ObjectCacheAnalysis(project);
            return oca.run();
        } else {
            return runWCETAnalysis();
        }
    }
    
    private boolean runWCETAnalysis() {
        /* Run */
        ipetConfig = new IPETConfig(config);
        boolean succeed = true;
        // FIXME: Report generation is a BIG MESS
        // bh wants to fix this soon
        try {
            /* Analysis */
            computeMetrics(); /* some metrics, and some cheap analysis for comparison */
            exec.info("Starting precise WCET analysis");
            computeWCET();
        } catch (Exception e) {
            exec.logException("analysis", e);
            succeed = false;
        }
        if (!project.getProjectConfig().doGenerateReport()) {
            exec.info("Ommiting HTML report");
            return succeed;
        }
        try {
            /* Report */
            exec.info("Generating info pages");
            project.getReport().generateInfoPages();
            exec.info("Generating result document");
            project.writeReport();
            exec.info("Generated files are in " + project.getProjectConfig().getOutDir());
        } catch (Exception e) {
            exec.logException("Report generation", e);
            succeed = false;
        }
        return succeed;
    }

        
    private void computeMetrics() throws Exception {
        StaticCacheApproximation preciseApprox = IPETConfig.getPreciseCacheApprox(config);
        project.setGenerateWCETReport(false); /* generate reports later (except preciseApprox does not support reports) */

        exec.info("Cyclomatic complexity: " + project.computeCyclomaticComplexity(project.getTargetMethod()));
        if(! project.getWCETProcessorModel().hasMethodCache()) preciseApprox = StaticCacheApproximation.ALWAYS_MISS;
        /* Perform a few standard analysis (MIN_CACHE_COST, ALWAYS_HIT, ALWAYS_MISS) without call strings */
        if(project.getWCETProcessorModel().hasMethodCache()) {
            long start,stop;

            /* Tree based WCET analysis - has to be equal to ALWAYS_MISS */
            {
                start = System.nanoTime();
                TreeAnalysis treeAna = new TreeAnalysis(project, false);
                long treeWCET = treeAna.computeWCET(project.getTargetMethod());
                stop = System.nanoTime();
                reportMetric("progress-measure",treeAna.getMaxProgress(project.getTargetMethod()));
                reportSpecial("wcet.tree",WcetCost.totalCost(treeWCET),start,stop,0.0);
            }

            RecursiveWcetAnalysis<AnalysisContextLocal> an =
                new RecursiveWcetAnalysis<AnalysisContextLocal>(
                        project, ipetConfig,
                        new LocalAnalysis(project,ipetConfig));

            /* FIXME: We don't have  report generation for UPPAAL and global analysis yet,
             * therefore we generate our report here */
            if(project.getProjectConfig().useUppaal() || preciseApprox.needsInterProcIPET()) {
                project.setGenerateWCETReport(true);
            }
            /* always miss */
            start = System.nanoTime();
            alwaysMissCost = an.computeCost(project.getTargetMethod(),new AnalysisContextLocal(StaticCacheApproximation.ALWAYS_MISS));
            stop  = System.nanoTime();
            reportSpecial("always-miss",alwaysMissCost,start,stop,LpSolveWrapper.getSolverTime());
            project.setGenerateWCETReport(false);

            /* always hit */
            LpSolveWrapper.resetSolverTime();
            start = System.nanoTime();
            alwaysHitCost = an.computeCost(project.getTargetMethod(), new AnalysisContextLocal(StaticCacheApproximation.ALWAYS_HIT));
            stop  = System.nanoTime();
            reportSpecial("always-hit",alwaysHitCost,start,stop,LpSolveWrapper.getSolverTime());

            /* minimal cache cost (too expensive for large problems) */
            if(CALCULATE_MINIMUM_CACHE_COST)  {                
                IPETConfig mmcConfig = ipetConfig.clone();
                mmcConfig.setAssumeMissOnceOnInvoke(true);
                GlobalAnalysis gb = new GlobalAnalysis(project, mmcConfig);
                LpSolveWrapper.resetSolverTime();
                start = System.nanoTime();
                AnalysisContextLocal initialContext = new AnalysisContextLocal(StaticCacheApproximation.GLOBAL_ALL_FIT, CallString.EMPTY);
                minCacheCost = gb.computeWCET(project.getTargetMethod(), initialContext);
                stop  = System.nanoTime();
                reportSpecial("min-cache-cost",minCacheCost, start, stop, LpSolveWrapper.getSolverTime());
            }
        }        
    }

    private void computeWCET() throws IOException, DuplicateKeyException, XmlSerializationException, Config.BadConfigurationException {
        StaticCacheApproximation preciseApprox = IPETConfig.getPreciseCacheApprox(config);
        project.setGenerateWCETReport(false);

        if(project.getProjectConfig().useUppaal()) {
            UppaalAnalysis an = new UppaalAnalysis(exec.getExecLogger(),project,project.getOutDir("uppaal"));
            config.checkPresent(UppAalConfig.UPPAAL_VERIFYTA_BINARY);

            /* Run uppaal analysis */
            long start = System.nanoTime();
            wcet = an.computeWCET(project.getTargetMethod(),alwaysMissCost.getCost());
            long stop  = System.nanoTime();
            reportUppaal(wcet,start,stop,an.getSearchtime(),an.getSolvertimemax());
        } else if(preciseApprox == StaticCacheApproximation.ALL_FIT_REGIONS) {
            RecursiveStrategy<AnalysisContextLocal, WcetCost> recStrategy =
                new GlobalAnalysis.GlobalIPETStrategy(ipetConfig);
            RecursiveWcetAnalysis<AnalysisContextLocal> an =
                new RecursiveWcetAnalysis<AnalysisContextLocal>(
                        project,
                        ipetConfig,
                        recStrategy);

            /* Run global analysis */
            LpSolveWrapper.resetSolverTime();
            long start = System.nanoTime();
            wcet = an.computeCost(project.getTargetMethod(),
                                  new AnalysisContextLocal(preciseApprox));
            long stop  = System.nanoTime();
            report(wcet,start,stop,LpSolveWrapper.getSolverTime());
        } else {
            AnalysisContextLocal initialContext = new AnalysisContextLocal(preciseApprox);
            RecursiveStrategy<AnalysisContextLocal, WcetCost> recStrategy =
                new LocalAnalysis(project, ipetConfig);
            RecursiveWcetAnalysis<AnalysisContextLocal> an =
                new RecursiveWcetAnalysis<AnalysisContextLocal>(project,ipetConfig,recStrategy);

            /* Run local analysis */
            project.setGenerateWCETReport(true);
            LpSolveWrapper.resetSolverTime();
            long start = System.nanoTime();
            wcet = an.computeCost(project.getTargetMethod(),initialContext);
            long stop  = System.nanoTime();
            report(wcet,start,stop,LpSolveWrapper.getSolverTime());
        }
        exec.info("WCET analysis finished: "+wcet);
    }

    private void reportMetric(String metric, Object... args) {
        project.recordMetric(metric, args);
        System.out.print(metric+":");
        for(Object o : args) System.out.print(" "+o);
        System.out.println("");
    }

    private void report(WcetCost wcet, long start, long stop,double solverTime) {
        String key = "wcet";
        System.out.println(key+": "+wcet);
        System.out.println(key+".time: " + timeDiff(start,stop));
        System.out.println(key+".solvertime: " + solverTime);
        project.recordResult(wcet,timeDiff(start,stop),solverTime);
        project.getReport().addStat(key, wcet.toString());
    }

    private void reportUppaal(WcetCost wcet, long start, long stop, double searchtime, double solvertimemax) {
        String key = "wcet";
        System.out.println(key+": "+wcet);
        System.out.println(key+".time: " + timeDiff(start,stop));
        System.out.println(key+".searchtime: " + searchtime);
        System.out.println(key+".solvertimemax: " + solvertimemax);
        project.recordResultUppaal(wcet,timeDiff(start,stop),searchtime,solvertimemax);
        project.getReport().addStat(key, wcet.toString());
    }

    private void reportSpecial(String metric, WcetCost cost, long start, long stop, double solverTime) {
        String key = "wcet."+metric;
        System.out.println(key+": "+cost);
        if(start != stop) System.out.println(key+".time: " + timeDiff(start,stop));
        if(solverTime != 0) System.out.println(key+".solvertime: " + solverTime);
        project.recordSpecialResult(metric,cost);
        project.getReport().addStat(key, cost.toString());
    }
}
