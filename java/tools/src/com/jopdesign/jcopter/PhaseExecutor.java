/*
 * This file is part of JOP, the Java Optimized Processor
 *   see <http://www.jopdesign.com/>
 *
 * Copyright (C) 2011, Stefan Hepp (stefan@stefant.org).
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
import com.jopdesign.common.config.BooleanOption;
import com.jopdesign.common.config.Config;
import com.jopdesign.common.config.Config.BadConfigurationError;
import com.jopdesign.common.config.Config.BadConfigurationException;
import com.jopdesign.common.config.Option;
import com.jopdesign.common.config.OptionGroup;
import com.jopdesign.common.config.StringOption;
import com.jopdesign.common.graphutils.InvokeDot;
import com.jopdesign.common.misc.AppInfoError;
import com.jopdesign.common.tools.ConstantPoolRebuilder;
import com.jopdesign.common.tools.UsedCodeFinder;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This is just a helper class to execute various optimizations and analyses.
 *
 * @author Stefan Hepp (stefan@stefant.org)
 */
public class PhaseExecutor {

    public static final Logger logger = Logger.getLogger(JCopter.LOG_ROOT + ".PhaseExecutor");

    public static final BooleanOption REMOVE_UNUSED_MEMBERS =
            new BooleanOption("remove-unused-members", "Remove unreachable code", true);

    public static final BooleanOption DUMP_CALLGRAPH =
            new BooleanOption("dump-callgraph", "Dump the callgraph", false);

    public static final StringOption CALLGRAPH_DIR =
            new StringOption("cgdir", "Directory to put the callgraph files into", "${outdir}/callgraph");


    public static final Option[] options = {
            REMOVE_UNUSED_MEMBERS,
            DUMP_CALLGRAPH, CALLGRAPH_DIR
            };

    private final JCopter jcopter;
    private final AppInfo appInfo;

    public PhaseExecutor(JCopter jcopter) {
        this.jcopter = jcopter;
        appInfo = AppInfo.getSingleton();
    }

    public Config getConfig() {
        return jcopter.getConfig().getConfig();
    }

    @SuppressWarnings({"AccessStaticViaInstance"})
    public void registerOptions(OptionGroup options) {
        options.addOptions(this.options);
    }

    public void dumpCallgraph(String graphName) {
        if (!getConfig().getOption(DUMP_CALLGRAPH)) return;
        
        try {
            File outDir = getConfig().getOutDir(CALLGRAPH_DIR);
            File dotFile = new File(outDir, graphName+".dot");
            File pngFile = new File(outDir, graphName+".png");
            FileWriter writer = new FileWriter(dotFile);

            appInfo.getCallGraph().exportDOT(writer);
            InvokeDot.invokeDot(getConfig(), dotFile, pngFile);

        } catch (BadConfigurationException e) {
            throw new BadConfigurationError("Could not create output dir "+getConfig().getOption(CALLGRAPH_DIR), e);
        } catch (IOException e) {
            throw new AppInfoError("Unable to export to .dot file", e);
        }
    }

    /**
     * Reduce the callgraph stored with AppInfo.
     * {@link AppInfo#buildCallGraph(boolean)} must have been called first.
     */
    public void reduceCallGraph() {
        // TODO perform callgraph thinning analysis
        // logger.info("Starting callgraph reduction");

        // logger.info("Finished callgraph reduction");
    }

    /**
     * Mark all InvokeSites which are safe to inline, or store info
     * about what needs to be done in order to inline them.
     * To get better results, reduce the callgraph first as much as possible.
     */
    public void markInlineCandidates() {
        // TODO call invoke candidate finder
    }

    /**
     * Inline all methods which do not increase the code size.
     * {@link #markInlineCandidates()} must have been run first.
     */
    public void performSimpleInline() {
    }

    /**
     * Inline all InvokeSites which are marked for inlining by an inline strategy.
     */
    public void performInline() {
    }

    /**
     * Run some simple optimizations to cleanup the bytecode without increasing its size.
     */
    public void cleanupMethodCode() {
        // TODO optimize load/store
        // TODO perform some simple peephole optimizations
        // (more complex optimizations (dead-code elimination, constant-folding,..) should
        //  go into another method..)
    }

    /**
     * Find and remove unused classes, methods and fields
     */
    public void removeUnusedMembers() {
        logger.info("Starting removal of unused members");

        UsedCodeFinder ucf = new UsedCodeFinder();
        ucf.resetMarks();
        ucf.markUsedMembers();
        ucf.removeUnusedMembers();

        logger.info("Finished removal of unused members");
    }

    /**
     * Rebuild all constant pools.
     */
    public void cleanupConstantPool() {
        logger.info("Starting cleanup of constant pools");

        appInfo.iterate(new ConstantPoolRebuilder());

        logger.info("Finished cleanup of constant pools");
    }
}
