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

package com.jopdesign.jcopter;

import com.jopdesign.common.AppInfo;
import com.jopdesign.common.AppSetup;
import com.jopdesign.common.EmptyTool;
import com.jopdesign.common.config.Config;
import com.jopdesign.common.config.Config.BadConfigurationException;
import com.jopdesign.common.config.OptionGroup;
import com.jopdesign.dfa.DFATool;
import com.jopdesign.wcet.WCETTool;
import org.apache.log4j.Logger;

/**
 * User: Stefan Hepp (stefan@stefant.org)
 * Date: 18.05.2010
 */
public class JCopter extends EmptyTool<JCopterManager> {

    public static final String VERSION = "0.1";

    public static final String CONFIG_FILE_NAME = "jcopter.properties";

    public static final String LOG_ROOT = "jcopter";
    public static final String LOG_OPTIMIZER = "jcopter.optimizer";
    public static final String LOG_INLINE = "jcopter.inline";

    private static final Logger logger = Logger.getLogger(LOG_ROOT + ".JCopter");

    private final JCopterManager manager;
    private final AppInfo appInfo;

    private JCopterConfig config;
    private PhaseExecutor executor;
    private DFATool dfaTool;
    private WCETTool wcetTool;

    public JCopter() {
        super(VERSION);
        manager = new JCopterManager();
        appInfo = AppInfo.getSingleton();
    }

    @Override
    public JCopterManager getEventHandler() {
        return manager;
    }

    @Override
    public void registerOptions(Config config) {
        OptionGroup options = config.getOptions();
        JCopterConfig.registerOptions(options);
        // TODO add options/profiles/.. to this so that only a subset of
        //      optimizations/analyses are initialized ? Overwrite PhaseExecutor for this?
        //      Or user simply uses phaseExecutor directly
        PhaseExecutor.registerOptions(options);
    }

    @Override
    public void onSetupConfig(AppSetup setup) throws Config.BadConfigurationException {

        OptionGroup options = setup.getConfig().getOptions();
        
        config = new JCopterConfig(options);
        executor = new PhaseExecutor(this, options);
    }

    @Override
    public void onSetupAppInfo(AppSetup setup, AppInfo appInfo) throws BadConfigurationException {
        config.checkOptions();
    }

    public JCopterConfig getJConfig() {
        return config;
    }

    public PhaseExecutor getExecutor() {
        return executor;
    }

    public DFATool getDfaTool() {
        return dfaTool;
    }

    public void setDfaTool(DFATool dfaTool) {
        this.dfaTool = dfaTool;
    }

    public WCETTool getWcetTool() {
        return wcetTool;
    }

    public void setWcetTool(WCETTool wcetTool) {
        this.wcetTool = wcetTool;
    }

    public boolean useDFA() {
        return dfaTool != null;
    }

    public boolean useWCET() {
        return wcetTool != null;
    }

    /**
     * Run all configured optimizations and perform the required analyses.
     */
    public void optimize() {

        // - (optional) perform receiver type DFA: reduce callgraph, maybe eliminate
        //   some nullpointer-checks. This is used only for the SimpleInliner since this will
        //   invalidate the results (see Callgraph#merge). We skip this for -O1 to save some time.
        boolean firstPassDFA = useDFA() && !config.doOptimizeFastOnly();
        if (firstPassDFA) {
            executor.dataflowAnalysis(false);
        }

        // - build callgraph: uses DFA results if available, else do some simple thinning
        executor.buildCallGraph(firstPassDFA);

        executor.dumpCallgraph("callgraph");


        // - Kill'em all, since we do not use them and we do not update them so they will get out-of-date
        executor.removeDebugAttributes();

        // - perform simple, guaranteed optimizations (everything to reduce code size!)
        //   (inline 2/3 byte methods, load/store eliminate, peephole, dead-code elimination, constant folding, ..)
        //   Be aware that we only have DFA receiver analysis data here (no loopbounds)!
        executor.cleanupMethodCode();

        // - perform simple inlining: guaranteed not to increase worst case
        executor.performSimpleInline();

        // - Rebuild callgraph and rerun DFA analyses since SimpleInliner changed the callstrings
        //   and we do not have an implementation for Callgraph#merge and a framework to notify analyses
        //   of callstring/callgraph changes (yet..)
        if (useDFA()) {
            executor.dataflowAnalysis(true);
        }

        executor.buildCallGraph(useDFA());

        // - Now we have full DFA results (if enabled) and an updated callgraph, now would be the time
        //   for some cleanup optimizations before we start the WCA (but we may not have Loopbounds yet)

        // - perform WCET analysis, select methods for inlining
        if (useWCET()) {
            // First, rebuild the WCET-Tool callgraph, since we modified the appInfo graph already
            wcetTool.rebuildCallGraph();

            // TODO call WCET analysis, use WCET-oriented inline selector


        } else {
            logger.info("WCA tool is disabled, not performing WCET-driven optimizations.");

            // use non-WCET-based inline selector

        }

        // - perform inlining (check previous analysis results to avoid creating nullpointer checks),
        //   duplicate/rename/.. methods, perform method extraction/splitting too?
        executor.performInline();

        // - perform code cleanup optimizations (load/store/param-passing, constantpool cleanup,
        //   remove unused members, constant folding, dead-code elimination (remove some more NP-checks,..),
        //   remove NOPs, ... )
        executor.cleanupMethodCode();

        executor.removeUnusedMembers();

        executor.relinkInvokesuper();

        executor.cleanupConstantPool();
    }


    public static void main(String[] args) {

        // setup some defaults
        AppSetup setup = new AppSetup();
        setup.setUsageInfo("jcopter", "A WCET driven Java bytecode optimizer.");
        setup.setVersionInfo(VERSION);
        // We do not load a config file automatically, user has to specify it explicitly to avoid
        // unintentional misconfiguration
        //setup.setConfigFilename(CONFIG_FILE_NAME);

        DFATool dfaTool = new DFATool();
        WCETTool wcetTool = new WCETTool();
        JCopter jcopter = new JCopter();

        setup.registerTool("dfa", dfaTool, true, false);
        setup.registerTool("wca", wcetTool, true, true);
        setup.registerTool("jcopter", jcopter);

        setup.initAndLoad(args, true, true, true);

        if (setup.useTool("dfa")) {
            wcetTool.setDfaTool(dfaTool);
            jcopter.setDfaTool(dfaTool);
        }
        if (setup.useTool("wca")) {
            jcopter.setWcetTool(wcetTool);
        }

        // run optimizations
        jcopter.optimize();

        // write results
        setup.writeClasses();
    }

}
