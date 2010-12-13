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
import com.jopdesign.common.JopTool;
import com.jopdesign.common.config.BoolOption;
import com.jopdesign.common.config.Config;
import com.jopdesign.common.config.OptionGroup;
import com.jopdesign.common.config.StringOption;

import java.io.IOException;
import java.util.Properties;

/**
 * User: Stefan Hepp (stefan@stefant.org)
 * Date: 18.05.2010
 */
public class JCopter implements JopTool<JCopterManager> {

    public static final String VERSION = "0.1";

    public static final StringOption LIBRARY_CLASSES =
            new StringOption("libraries", "comma-separated list of library classes and packages", "");

    public static final StringOption IGNORE_CLASSES =
            new StringOption("ignore", "comma-separated list of classes and packages to ignore", "");

    public static final BoolOption ALLOW_INCOMPLETE_APP =
            new BoolOption("allow-incomplete", "Ignore missing classes", false);

    public static final BoolOption USE_DFA =
            new BoolOption("useDFA", "run and use results of the DFA tool", true);

    public static final BoolOption USE_WCET =
            new BoolOption("useWCET", "run and use results of the WCET analysis tool", true);


    private final JCopterManager manager;

    public JCopter() {
        manager = new JCopterManager();
    }

    public String getToolVersion() {
        return VERSION;
    }

    public JCopterManager getEventHandler() {
        return manager;
    }

    public Properties getDefaultProperties() throws IOException {
        return AppSetup.loadResourceProps(JCopter.class, "defaults.properties");
    }

    public void registerOptions(OptionGroup options) {
        options.addOption( ALLOW_INCOMPLETE_APP );
        options.addOption( USE_DFA );
        options.addOption( USE_WCET );
    }

    public void onSetupConfig(AppSetup setup) throws Config.BadConfigurationException {
        Config config = setup.getConfig();
        AppInfo appInfo = AppInfo.getSingleton();

        if ( config.getOption(ALLOW_INCOMPLETE_APP) ) {
            appInfo.setIgnoreMissingClasses(true);
        }

    }


    public void run(AppSetup setup) {

        if ( setup.getConfig().getOption(USE_DFA) ) {

        }

        if ( setup.getConfig().getOption(USE_WCET) ) {

        }

        
    }


    public static void main(String[] args) {

        // TODO create and register wcet and dfa tools, pass to JCopter
        JCopter jcopter = new JCopter();

        // setup some defaults
        AppSetup setup = new AppSetup();
        setup.setUsageInfo("jcopter", "A WCET driven Java bytecode optimizer.");

        setup.addStandardOptions(true, true);
        setup.addPackageOptions(true);
        setup.addWriteOptions(true);
        setup.setConfigFilename("jcopter.properties");

        setup.registerTool("jcopter", jcopter);

        // parse options and config, setup everything, load application classes
        String[] rest = setup.setupConfig(args);

        setup.setupLogger(true);
        setup.setupAppInfo(rest, true);

        // run optimizations
        jcopter.run(setup);

        // write results
        setup.writeClasses();

    }

}
