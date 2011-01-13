/*
 * This file is part of JOP, the Java Optimized Processor
 *   see <http://www.jopdesign.com/>
 *
 * Copyright (C) 2008, Benedikt Huber (benedikt.huber@gmail.com)
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

import com.jopdesign.common.AppInfo;
import com.jopdesign.common.processormodel.ProcessorModel.Model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Configuration container, based on String-properties.
 * Option handling and -parsing is done by the {@link OptionGroup} class.
 *
 * @author Benedikt Huber <benedikt.huber@gmail.com>
 * @author Stefan Hepp (stefan@stefant.org)
 */
public class Config {

    /**
     * This is the default value; use {@link AppInfo#getClinitSignature(String)}.
     */
    public static final String DEFAULT_CLINIT_NAME = "<clinit>";
    public static final String DEFAULT_CLINIT_DESCRIPTOR = "()V";

    public static final String DEFAULT_NATIVE = "com.jopdesign.sys.Native";
    public static final String[] JOP_SYSTEM_CLASSES = {
            "com.jopdesign.sys.JVM",
            "com.jopdesign.sys.JVMHelp",
            "com.jopdesign.sys.Startup"
        };

    /* Options which are always present
     * TODO maybe move the class loading and entry-point related options to AppInfo?
     */

    public static final BooleanOption SHOW_HELP =
            new BooleanOption("help", "show help", 'h', true);

    public static final BooleanOption SHOW_VERSION =
            new BooleanOption("version", "show version number", Option.SHORT_NONE, true);

    public static final BooleanOption SHOW_CONFIG =
            new BooleanOption("showconfig", "print current configuration values", Option.SHORT_NONE, true);

    public static final BooleanOption DEBUG =
            new BooleanOption("debug", "show debug level messages", 'd', false);

    public static final BooleanOption QUIET =
            new BooleanOption("quiet", "only show warnings and errors. Overruled by '-d'", 'q', false);

    public static final BooleanOption VERBOSE =
            new BooleanOption("verbose", "use a more detailed log format and show stacktraces. Can be used with '-d' or '-q'", 'v', false);

    public static final StringOption CLASSPATH =
            new StringOption("classpath", "classpath of the classes to load", 'c', "java/target/dist/classes");

    public static final StringOption MAIN_METHOD_NAME =
            new StringOption("mm", "method name of the entry method", "main");

    public static final EnumOption<Model> PROCESSOR_MODEL =
            new EnumOption<Model>("arch", "The processor model to use", Model.JOP);

    public static final StringOption LIBRARY_CLASSES =
            new StringOption("libraries", "comma-separated list of library classes and packages", "");

    public static final StringOption IGNORE_CLASSES =
            new StringOption("ignore", "comma-separated list of classes and packages to ignore", "");

    public static final BooleanOption EXCLUDE_LIBRARIES =
            new BooleanOption("exclude-libs", "do not load library classes", false);

    public static final BooleanOption LOAD_NATIVES =
            new BooleanOption("load-natives", "load native classes too", false);

    public static final StringOption ROOTS =
            new StringOption("roots", "comma-separated list of additional root classes", "");

    public static final IntegerOption CALLSTRING_LENGTH =
	    new IntegerOption("callstring-length", "Length of the callstring", 0);

    public static final StringOption WRITE_PATH =
            new StringOption("outdir", "base path for output directories (classdir,reportdir,..)", 'o', "java/target/dist");

    public static final StringOption WRITE_CLASSPATH =
            new StringOption("classdir", "output path for generated class files", "${outdir}/classes");

    public static final StringOption REPORTDIR =
        new StringOption("reportdir", "the directory to write reports into", "${outdir}/report");

    public static final StringOption ERROR_LOG_FILE =
        new StringOption("error-log","the error log file, placed in the report dir","error.log.html");

    public static final StringOption INFO_LOG_FILE =
        new StringOption("info-log","the info log file, placed in the report dir","info.log.html");

    public static final Option<?>[] standardOptions =
            { SHOW_HELP, SHOW_VERSION, SHOW_CONFIG, DEBUG, QUIET, VERBOSE };


    /*
    * Exception classes
    * ~~~~~~~~~~~~~~~~~
    */
    @SuppressWarnings({"UncheckedExceptionClass"})
    public static class BadConfigurationError extends Error {
		private static final long serialVersionUID = 1L;
		public BadConfigurationError(String msg) { super(msg); }

        public BadConfigurationError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class BadConfigurationException extends Exception {
		private static final long serialVersionUID = 1L;
		public BadConfigurationException(String message) {
			super(message);
		}
		public BadConfigurationException(String message, Exception e) {
			super(message,e);
		}
	}

    private Properties defaultProps, props;
    private OptionGroup options;

    public Config() {
        this(new Properties());
    }

    public Config(Properties defaultProps) {
        this.defaultProps = defaultProps;
        props = new Properties(defaultProps);
        options = new OptionGroup(this);
    }

    /**
     * Take a comma-separated list of strings and split them into an array,
     * avoiding empty strings, and trim all entries.
     *
     * @param list a comma-separated list.
     * @return trimmed entries of list.
     */
    public static String[] splitStringList(String list) {
        String[] parts = list.split(",");
        List<String> newList = new LinkedList<String>();
        for (String part : parts) {
            part = part.trim();
            if ( "".equals(part) ) {
                continue;
            }
            newList.add(part);
        }
        return newList.toArray(new String[newList.size()]);
    }

    /**
     * Check whether the given file is a directory, possibly creating it if
	 * non existing
	 * @param outDir the path to the directory
	 * @param createIfNonExist whether the directory should be created, if it doesn't exist yet
	 * @throws BadConfigurationException if the file is not a directory or does not exist and is not created.
	 */
	public static void checkDir(File outDir, boolean createIfNonExist) throws BadConfigurationException {
		if(outDir.exists()) {
			if(! outDir.isDirectory()) {
				throw new BadConfigurationException("Not a directory: "+outDir);
			}
		} else if(createIfNonExist) {
			if (!outDir.mkdirs()) {
                throw new BadConfigurationException("Directory could not be created: "+outDir);
            }
		} else {
			throw new BadConfigurationException("Directory does not exist: "+outDir);
		}
	}


    public OptionGroup getOptions() {
        return options;
    }

    public Properties getProperties() {
        return props;
    }

    /**
     * Load a properties configuration file and append its content to the current configuration.
     * Existing keys are replaced.
     *
     * @param propStream an open InputStream serving the properties
     * @throws IOException if loading fails.
     */
    public void addProperties(InputStream propStream) throws IOException {
        Properties p = new Properties();
        p.load(propStream);
        props.putAll(p);
    }

    /**
     * Load a properties configuration file and append its content to the current configuration.
     * Existing keys are replaced.
     *
     * @param propStream an open InputStream serving the properties
     * @param prefix a prefix to append to all property keys in the stream before adding them to the configuration.
     * @throws IOException if loading fails.
     */
    public void addProperties(InputStream propStream, String prefix) throws IOException {
        Properties p = new Properties();
        p.load(propStream);
        String pfx = prefix == null || "".equals(prefix) ? "" : prefix + ".";
        for (Map.Entry<Object,Object> e : p.entrySet()) {
            props.put(pfx + e.getKey(), e.getValue() );
        }
    }

    /**
     * Get the default value used to indent the descriptions or values of options.
     * @return the default indent for help texts.
     */
    public int getDefaultIndent() {
        int i = 1;
        for (Option o : options.availableOptions()) {
            i = Math.max(i, o.getKey().length());
        }
        return Math.max(8, Math.min(i, 25));
    }

    /**
     * Parse configuration options.
     *
     * @see OptionGroup#consumeOptions(String[])
     * @param args arguments to parse
     * @return string-arguments after the last known argument.
     * @throws BadConfigurationException if arguments or current properties cannot be parsed.
     */
    public String[] parseArguments(String[] args) throws BadConfigurationException {
        return options.consumeOptions(args);
    }

    /**
     * Check all options for correctness (missing required options, if options can be parsed, .. ).
     *
     * @throws BadConfigurationException if an option is missing or cannot be parsed.
     */
    public void checkOptions() throws BadConfigurationException {
        options.checkOptions();
    }

    public void checkPresent(Option<?> option) throws BadConfigurationException {
        if(getOption(option) == null) {
            throw new BadConfigurationException("Missing option: "+option);
        }
    }

    /**
     * Set a new set of default values, replaces the old default values.
     * @param defaultProps the new default values.
     */
    public void setDefaults(Properties defaultProps) {
        this.defaultProps = defaultProps;

        Properties oldProps = props;
        props = new Properties(defaultProps);
        props.putAll(oldProps);
    }

    /**
     * Add a set of properties to the default properties.
     *
     * @param defaults a set of default properties.
     * @param override if true, override existing default props
     */
    public void addDefaults(Properties defaults, boolean override) {
        //noinspection unchecked
        if ( override ) {
            defaultProps.putAll(defaults);
        } else {
            for (Object key : defaults.entrySet()) {
                if ( !defaultProps.containsKey(key) ) {
                    defaultProps.put(key, defaults.get(key));
                }
            }
        }
    }

    /**
     * Clear all set properties, but not the default values.
     */
    public void clearValues() {
        props.clear();
    }

    /**
     * Set a new value for a key.
     *
     * @param key the key to of the value to set.
     * @param value the new value to set.
     * @param setDefault if true, set the default value instead of the value.
     * @return the old value or old default value.
     */
    public String setProperty(String key, String value, boolean setDefault) {
        Object val;
        if ( setDefault ) {
            val = defaultProps.setProperty(key, value);
        } else {
            val = props.setProperty(key, value);
        }
        return val != null ? val.toString() : null;
    }

    public String setProperty(String key, String value) {
        return setProperty(key, value, false);
    }

    /**
     * Check if a key is set (ignoring default options).
     *
     * @see #isPresent(String)
     * @param key the key of the property to check.
     * @return true if set.
     */
    public boolean isSet(String key) {
        return props.containsKey(key);
    }

    /**
     * Check if a key is set or has a default value.
     *
     * @see #isSet(String)
     * @param key the key of the property to check.
     * @return true if it has a value or default not equal to null.
     */
    public boolean isPresent(String key) {
        return props.getProperty(key) != null;
    }

    public String getValue(String key) {
        return props.getProperty(key);
    }

    public String getValue(String key, String defaultVal) {
        return props.getProperty(key, defaultVal);
    }

    public String getDefaultValue(String key) {
        return defaultProps.getProperty(key);
    }

    /**
     * This is a shortcut to add an option to the main option group.
     *
     * @see OptionGroup#addOption(Option)
     * @param option the option to add.
     */
    public void addOption(Option<?> option) {
        options.addOption(option);
    }

    /**
     * This is a shortcut to add a list of options to the main option group.
     *
     * @see OptionGroup#addOptions(Option[])
     * @param options the options to add.
     */
    public void addOptions(Option<?>[] options) {
        this.options.addOptions(options);
    }

    public boolean hasOption(Option<?> option) {
        return this.options.containsOption(option);
    }

    /**
     * This is a shortcut to get an option from the main option group.
     *
     * @see OptionGroup#getOption(Option)
     * @param option the option to read.
     * @return the value of the option
     * @throws Config.BadConfigurationError if the format of the option is invalid if required and not set.
     */
    public <T> T getOption(Option<T> option) throws BadConfigurationError {
        return options.getOption(option);
    }

    /**
     * This is a shortcut to get an option from the main option group.
     *
     * @see OptionGroup#getOption(Option, Object)
     * @param option the option to read.
     * @param defaultVal the default value to use if no other value is found.
     * @return the value of the option
     * @throws IllegalArgumentException if the format of the option is invalid
     */
    public <T> T getOption(Option<T> option, T defaultVal) throws IllegalArgumentException {
        return options.getOption(option, defaultVal);
    }

    public <T> T getDefaultValue(Option<T> option) {
        return options.getDefaultValue(option);
    }

    public <T> void setOption(Option<T> option, T value) {
        options.setOption(option, value);
    }

    /**
     * This is a shortcut to get an option from the main option group.
     *
     * @see OptionGroup#tryGetOption(Option)
     * @param option the option to read.
     * @return the value of the option or null if not set, even if required.
     * @throws IllegalArgumentException if the format of the option is invalid
     */
    public <T> T tryGetOption(Option<T> option) throws IllegalArgumentException {
        return options.tryGetOption(option);
    }

    /**
     * Dump configuration of all user-set properties for debugging purposes.
     * To print a list of all options with their values,
     * use {@link #printConfiguration(int)}.
     *
     * @see #printConfiguration(int)
     * @param indent indent used for keys
     * @return a dump of all options with their respective values.
     */
    public String dumpConfiguration(int indent) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(os);

        for (Map.Entry<Object,Object> e : props.entrySet()) {
            printOption(p, indent, e.getKey().toString(), e.getValue());
        }

        return os.toString();
    }

    public void printConfiguration(int indent) {
        Set<String> keys = new HashSet<String>();

        keys.addAll(options.printOptions(System.out, indent));

        System.out.println();
        System.out.println("Other configuration values:");

        for (Map.Entry<Object,Object> e : props.entrySet()) {
            if ( keys.contains(e.getKey().toString()) ) {
                continue;
            }
            printOption(System.out, indent, e.getKey().toString(), e.getValue());
        }

        System.out.println();
    }

    protected static void printOption(PrintStream p, int indent, String key, Object value) {
        p.println(String.format("%4s%-"+(indent+1)+"s ==> %s", "", key,
                  value == null ? "<not set>": value));
    }

}
