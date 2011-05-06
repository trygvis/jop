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

package com.jopdesign.jcopter.inline;

import com.jopdesign.common.MethodInfo;
import com.jopdesign.common.config.BooleanOption;
import com.jopdesign.common.config.Config;
import com.jopdesign.common.config.EnumOption;
import com.jopdesign.common.config.OptionGroup;
import com.jopdesign.common.config.StringOption;

import java.util.List;

/**
 * @author Stefan Hepp (stefan@stefant.org)
 */
public class InlineConfig {

    public enum JVMInline { NONE, SAFE, ALL }

    public static final StringOption EXCLUDE =
            new StringOption("exclude", "Comma separated list of methods (without descriptor), classes and packages to exclude from inlining.", true);

    public static final BooleanOption ALLOW_CODEMODIFY =
            new BooleanOption("allow-codemodify", "Allow making methods public and renaming methods if required for inlining.", true);

    public static final BooleanOption SKIP_NP_CHECKS =
            new BooleanOption("skip-np-checks", "Do not generate any nullpointer checks", false);

    public static final EnumOption<JVMInline> JVM_INLINE =
            new EnumOption<JVMInline>("jvm-calls",
                    "Allow inlining of JVM calls: disabled, only if result is verifiable, all calls",
                    JVMInline.SAFE);

    private final OptionGroup options;
    private final List<String> ignorePrefix;

    public static void registerOptions(OptionGroup options) {
        options.addOption(EXCLUDE);
        options.addOption(ALLOW_CODEMODIFY);
        options.addOption(SKIP_NP_CHECKS);
        options.addOption(JVM_INLINE);
    }

    public InlineConfig(OptionGroup options) {
        this.options = options;
        this.ignorePrefix = Config.splitStringList(options.getOption(EXCLUDE));
    }

    public boolean allowChangeAccess() {
        return options.getOption(ALLOW_CODEMODIFY);
    }

    public boolean allowRename() {
        // TODO currently not supported by InlineHelper.prepareInvoke
        // return options.getOption(ALLOW_CODEMODIFY);
        return false;
    }

    public boolean doExcludeMethod(MethodInfo method) {
        String className = method.getClassName();

        // NOTICE maybe separate configs for ignore from and ignore to?
        for (String prefix : ignorePrefix) {
            if ( className.startsWith(prefix+".") || className.equals(prefix)
                 || prefix.equals(className+"."+method.getShortName())
                 || prefix.equals(className+"#"+method.getShortName())) {
                return false;
            }
        }
        return true;
    }

    public boolean skipNullpointerChecks() {
        return options.getOption(SKIP_NP_CHECKS);
    }

    public JVMInline allowJVMCalls() {
        return options.getOption(JVM_INLINE);
    }
}
