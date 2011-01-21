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

package com.jopdesign.common.config;

import com.jopdesign.common.config.Config.BadConfigurationException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Stefan Hepp (stefan@stefant.org)
 */
public class OptionGroup {

    public static final String CMD_KEY = "cmd";

    private OptionChecker checker;
    private Config config;
    private String prefix;

    /**
     * List of all options in this group.
     */
    private List<Option<?>> optionList;

    /**
     * Map of options to their respective keys (excluding group prefix).
     */
    private Map<String, Option<?>> optionSet;

    /**
     * Map of subgroups and their OptionGroups.
     */
    private Map<String, OptionGroup> subGroups;
    /**
     * Set of all subgroup names which are handled as commands
     */
    private Set<String> commands;

    public OptionGroup(Config config) {
        this(config, null);
    }

    public OptionGroup(Config config, String prefix) {
        this.config = config;
        this.prefix = prefix;
        optionList = new LinkedList<Option<?>>();
        optionSet = new HashMap<String, Option<?>>();
        subGroups = new HashMap<String, OptionGroup>(1);
        commands = new HashSet<String>(0);
    }

    public Config getConfig() {
        return config;
    }

    public OptionChecker getChecker() {
        return checker;
    }

    public void setChecker(OptionChecker checker) {
        this.checker = checker;
    }

    public String getPrefix() {
        return prefix;
    }

    public Set<String> availableSubgroups() {
        return subGroups.keySet();
    }

    public boolean hasCommands() {
        return commands.size() > 0;
    }

    public OptionGroup addGroup(String prefix, boolean isCommand) {
        OptionGroup grp = new OptionGroup(config, prefix);
        subGroups.put(prefix, grp);
        if (isCommand) commands.add(prefix);
        return grp;
    }

    public OptionGroup getGroup(String group) {
        return subGroups.get(group);
    }

    public String selectedCommand() {
        return config.getValue(getConfigKey(CMD_KEY));
    }

    /*
    * Setup Options
    * ~~~~~~~~~~~~~
    */

    public List<Option<?>> availableOptions() {
        return optionList;
    }

    public void addOption(Option option) {
        if (optionSet.containsKey(option.getKey())) {
            for (Iterator<Option<?>> it = optionList.iterator(); it.hasNext();) {
                Option opt = it.next();
                if (opt.getKey().equals(option.getKey())) {
                    it.remove();
                    break;
                }
            }
        }
        optionSet.put(option.getKey(), option);

        // we keep the options in an additional list to have them sorted in the same way they are added.
        optionList.add(option);
    }

    public void addOptions(Option[][] options) {
        for (Option[] optList : options) {
            addOptions(optList);
        }
    }

    public void addOptions(Option[] options) {
        for (Option opt : options) {
            addOption(opt);
        }
    }

    public Option getShortOptionKey(char shortKey) {
        if (shortKey == Option.SHORT_NONE) {
            return null;
        }
        for (Option o : optionList) {
            if (o.getShortKey() == shortKey) {
                return o;
            }
        }
        return null;
    }

    public Option getOptionSpec(String key) {
        return optionSet.get(key);
    }

    public boolean containsOption(Option option) {
        return optionSet.containsKey(option.getKey());
    }

    public boolean containsOption(String key) {
        return optionSet.containsKey(key);
    }

    public String getConfigKey(Option<?> option) {
        return getConfigKey(option.getKey());
    }

    public String getConfigKey(String key) {
        return prefix == null ? key : prefix + '.' + key;
    }

    /*
     * Access Option Values
     * ~~~~~~~~~~~~~~~~~~~~ 
     */

    /**
     * Check if option has been assigned a value in the config.
     *
     * @param option the option to check.
     * @return true if option has been explicitly set.
     * @see Option#isEnabled(OptionGroup)
     * @see #hasValue(Option)
     */
    public boolean isSet(Option<?> option) {
        return config.isSet(getConfigKey(option));
    }

    /**
     * Check if we have any value for this option (either set explicitly or some default value).
     * Does not check if the value can be parsed.
     *
     * @param option the option to check.
     * @return true if there is some value available for this option.
     * @see Option#isEnabled(OptionGroup)
     * @see #isSet(Option)
     */
    public boolean hasValue(Option<?> option) {
        String val = config.getValue(getConfigKey(option));
        return val != null || option.getDefaultValue(this) != null;
    }

    /**
     * Try to get the value of an option, or its default value.
     * If no value and no default value is available, this returns null, even
     * if the option is not optional.
     * <p/>
     * Note that after {@link #checkOptions()} has been called, this method
     * should not throw any errors.
     *
     * @param option the option to query.
     * @param <T>    Type of the option
     * @return The value, the default value, or null if no value is available.
     * @throws IllegalArgumentException if the config-value cannot be parsed or is not valid.
     */
    public <T> T tryGetOption(Option<T> option) throws IllegalArgumentException {

        String val;
        // if this optiongroup has a prefix and the option is not defined here, try to
        // look it up in the root group
        if (prefix != null && !optionSet.containsKey(option.getKey())) {
            val = config.getValue(option.getKey());
        } else {
            val = config.getValue(getConfigKey(option));
        }

        if (val == null) {
            return option.getDefaultValue(this);
        } else {
            return option.parse(this, val);
        }
    }

    /**
     * Get the parsed default value from the config or from the option if not set.
     *
     * @param option the option to get the default value for.
     * @param <T>    the type of the value
     * @return the default value, or null if no default is set in neither the config nor the option.
     */
    public <T> T getDefaultValue(Option<T> option) {
        String val = config.getDefaultValue(getConfigKey(option));
        if (val == null) {
            return option.getDefaultValue(this);
        } else {
            return option.parse(this, val);
        }
    }

    /**
     * Get the default value from the config or from the option if not set, but do not parse it or
     * replace any keywords.
     *
     * @param option the option to get the default value for.
     * @return the default value as set in the config file or the option.
     */
    public String getDefaultValueText(Option option) {
        String val = config.getDefaultValue(getConfigKey(option));
        if (val == null) {
            Object def = option.getDefaultValue();
            return def != null ? def.toString() : null;
        } else {
            return val;
        }
    }

    public <T> T getOption(Option<T> option, T defaultVal) throws IllegalArgumentException {
        T val = tryGetOption(option);
        return val != null ? val : defaultVal;
    }

    /**
     * Try to get the value of an option, or return null for optional options with no default value.
     * An exception is thrown if no value (and no default value) is given and the option is not optional,
     * or if the config-value cannot be parsed.
     * <p/>
     * Note that after {@link #checkOptions()} has been called, this method
     * should not throw any errors.
     *
     * @param option the option to get the value for.
     * @param <T>    type of the option.
     * @return the option value or null if
     * @throws Config.BadConfigurationError if the config-value cannot be parsed or if the option is null but required.
     */
    public <T> T getOption(Option<T> option) throws Config.BadConfigurationError {
        T opt;
        try {
            opt = tryGetOption(option);
        } catch (IllegalArgumentException e) {
            throw new Config.BadConfigurationError("Error parsing option '" + getConfigKey(option) + "': " + e.getMessage(), e);
        }
        if (opt == null && !option.isOptional()) {
            throw new Config.BadConfigurationError("Missing required option: " + getConfigKey(option));
        }
        return opt;
    }

    public <T> void checkPresent(Option<T> option) throws Config.BadConfigurationException {
        if (getOption(option) == null) {
            throw new Config.BadConfigurationException("Missing option: " + getConfigKey(option));
        }
    }

    public <T> void setOption(Option<T> option, T value) {
        config.setProperty(getConfigKey(option), value.toString());
    }

    /*
     * Parse, check and dump options
     * ~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Consume all command line options and turn them into properties.<br/>
     * <p/>
     * <p>The arguments are processed as follows: If an argument is of the form
     * "-option" or "--option", it is considered to be an option.
     * If an argument is an option, the next argument is considered to be the parameter,
     * unless the option is boolean and the next argument is missing or an option as well.
     * We add the pair to our properties, consuming both arguments.
     * </p><p>
     * If an argument starts with @, the rest of it is considered as a property file name,
     * which is then loaded and added to the configuration.
     * The first non-option or the argument string {@code --} terminates the option list.
     * </p>
     *
     * @param args The argument list
     * @return An array of unconsumed arguments
     * @throws Config.BadConfigurationException if an argument is malformed.
     */
    public String[] consumeOptions(String[] args) throws Config.BadConfigurationException {
        int i = 0;

        while (i < args.length) {

            if (commands.contains(args[i])) {
                config.setProperty(getConfigKey(CMD_KEY), args[i]);
                OptionGroup cmdGroup = subGroups.get(args[i]);
                return cmdGroup.consumeOptions(Arrays.copyOfRange(args, i + 1, args.length));
            }

            // handle custom config files
            if (args[i].startsWith("@")) {
                String filename = args[i].substring(1);
                try {
                    InputStream is = new BufferedInputStream(new FileInputStream(filename));
                    config.addProperties(is, prefix);
                    is.close();
                } catch (FileNotFoundException e) {
                    throw new Config.BadConfigurationException("Configuration file '" + filename + "' not found!", e);
                } catch (IOException e) {
                    throw new Config.BadConfigurationException("Error reading file '" + filename + "': " + e.getMessage(), e);
                }
                i++;
                continue;
            }

            // break if this is not an option argument, return rest
            if (!args[i].startsWith("-")) break;
            if ("-".equals(args[i]) || "--".equals(args[i])) {
                i++;
                break;
            }

            String key = null;
            if (args[i].charAt(1) == '-') key = args[i].substring(2);
            else {
                // for something of form '-<char>', try short option,
                if (args[i].length() == 2) {
                    Option shortOption = getShortOptionKey(args[i].charAt(1));
                    if (shortOption != null) {
                        key = shortOption.getKey();
                    }
                    // for something of form '-<longtext>' try normal key for compatibility
                } else {
                    key = args[i].substring(1);
                }
            }

            i = parseOption(args, key, i);
            i++;
        }
        return Arrays.copyOfRange(args, i, args.length);
    }

    protected int parseOption(String[] args, String key, int pos) throws BadConfigurationException {

        Option spec = getOptionSpec(key);

        if (spec != null) {
            String val = null;
            if (pos + 1 < args.length) {
                if (spec.isValue(args[pos + 1])) {
                    val = args[pos + 1];
                }
            }
            int i = pos;
            if (spec instanceof BooleanOption && val == null) {
                val = "true";
            } else if (val == null) {
                throw new Config.BadConfigurationException("Missing argument for option: " + spec);
            } else {
                i++;
            }
            config.setProperty(getConfigKey(spec), val);

            return i;
        }

        // maybe a boolean option, check for --no-<key>
        if (key.startsWith("no-")) {
            spec = getOptionSpec(key.substring(3));
            if (spec != null && spec instanceof BooleanOption) {
                config.setProperty(getConfigKey(spec), "false");
            }
        } else {
            // or maybe a sub-option
            int j = key.indexOf('.');
            OptionGroup group = subGroups.get(key.substring(0,j));
            if (group != null) {
                return group.parseOption(args, key.substring(j+1), pos);
            }
        }

        if (spec == null) {
            throw new Config.BadConfigurationException("Unknown option: " + key);
        }

        return pos;
    }

    public void checkOptions() throws Config.BadConfigurationException {

        boolean skipCheck = false;

        // first, check if we can parse all options and if we need to call the OptionChecker
        for (Option<?> option : optionList) {

            // check if we can parse
            try {
                tryGetOption(option);
            } catch (IllegalArgumentException e) {
                throw new Config.BadConfigurationException("Error parsing option '" + getConfigKey(option) + "': " + e.getMessage(), e);
            }

            if (option.doSkipChecks() && option.isEnabled(this)) {
                skipCheck = true;
            }
        }

        // run the OptionChecker if required
        if (!skipCheck) {

            // check for required options
            for (Option<?> option : optionList) {
                if (!option.isOptional() && !hasValue(option)) {
                    throw new Config.BadConfigurationException("Missing required option '" + getConfigKey(option) + '"');
                }
            }

            // run OptionChecker
            if (checker != null) checker.check(this);
        }
    }

    /**
     * Dump configuration of all options for debugging purposes
     *
     * @param p      a writer to print the options to
     * @param indent indent used for keys
     * @return a set of all printed keys
     */
    public Collection<String> printOptions(PrintStream p, int indent) {
        Set<String> keys = new HashSet<String>();

        for (Option<?> o : availableOptions()) {
            String key = getConfigKey(o);
            Object val = tryGetOption(o);
            keys.add(key);
            Config.printOption(p, indent, key, val);
        }

        for (OptionGroup group : subGroups.values()) {
            keys.addAll(group.printOptions(p, indent));
        }

        return keys;
    }
}
