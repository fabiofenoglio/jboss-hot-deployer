package com.ff.magicHotDeployer.configuration;

import java.io.IOException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.ini4j.InvalidFileFormatException;

import com.ff.magicHotDeployer.logging.LogLevel;
import com.ff.magicHotDeployer.logging.Logger;

public class ConfigurationProvider {

	public final static String DEFAULT_NODE = "config";
	
	public final static String PARAM_SOURCE_FOLDER = "source";
	public final static String PARAM_JBOSS_HOME = "jbossHome";
	public final static String PARAM_DEST_SUB_FOLDER = "destSub";
	public final static String PARAM_DEST_PACKAGE_PREFIX = "destPackagePrefix";
	public final static String PARAM_RECURSIVE = "recursive";
	public final static String PARAM_FILTER = "filter";
	public final static String PARAM_LOG_LEVEL = "logLevel";
	public final static String PARAM_INSTANCE_NAME = "name";
	public final static String PARAM_DEST_ABSOLUTE = "fixedTarget";
	
	public static Integer instanceIndex = 0;
	
	private Preferences cfgCache = null;
	private Preferences cfgCacheDefault = null;
	
	private CommandLine cmdLineOptions = null;
	private String sourceFolder = null;
	private String jbossHome = null;
	private String jbossDeployedPackagePrefix = null;
	private String jbossDeployedSubpath = null;
	private Boolean recursive = true;
	private String filter = null;
	private String name = null;
	private Pattern filterPattern;
	private String fixedTarget = null;
	
	public ConfigurationProvider(CommandLine cmdLineOptions, Preferences node, Preferences defaultNode) {
		this.cmdLineOptions = cmdLineOptions;
		this.cfgCache = node;
		this.cfgCacheDefault = defaultNode;
	}
	
	public static String getCurrentPath() {
		return System.getProperty("user.dir");
	}
	public static String getConfigFilePath() {
		return getCurrentPath() + "/config.ini";
	}
	
	private String readFromPrioritizedSource(String key) {
		return readFromPrioritizedSource(key, null);
	}
	private String readFromPrioritizedSource(String key, String def) {
		if (cmdLineOptions.hasOption(key)) {
			Logger.debug("reading " + key + " parameter from commandLine override");
			return cmdLineOptions.getOptionValue(key);
		}
		if (cfgCache == null) return def;
		String raw = cfgCache.get(key, null);
		if (raw == null && cfgCacheDefault != null) {
			raw = cfgCacheDefault.get(key, def);
		}
		return raw;
	}
	
	public void reload() throws InvalidFileFormatException, IOException {
		sourceFolder = readFromPrioritizedSource(PARAM_SOURCE_FOLDER);
		if (sourceFolder == null) throw new RuntimeException("no sourceFolders in configuration");
		
		jbossHome = readFromPrioritizedSource(PARAM_JBOSS_HOME);
		if (jbossHome == null) throw new RuntimeException("no jbossHome in configuration");

		jbossDeployedPackagePrefix = readFromPrioritizedSource(PARAM_DEST_PACKAGE_PREFIX);
		if (jbossDeployedPackagePrefix == null) throw new RuntimeException("no jbossDeployedPackagePrefix in configuration");

		jbossDeployedSubpath = readFromPrioritizedSource(PARAM_DEST_SUB_FOLDER);
		if (jbossDeployedSubpath == null) jbossDeployedSubpath = "";
		
		fixedTarget = readFromPrioritizedSource(PARAM_DEST_ABSOLUTE);
		if (fixedTarget != null && "".equals(fixedTarget)) fixedTarget = null;
		
		name = readFromPrioritizedSource(PARAM_INSTANCE_NAME);
		if (name == null) name = "I" + (instanceIndex ++);
		
		filter = readFromPrioritizedSource(PARAM_FILTER);
		// filter can be null
		if (filter != null && !"".equals(filter)) {
			filterPattern = Pattern.compile(filter);
		}
		
		String recursive = readFromPrioritizedSource(PARAM_RECURSIVE);
		if (recursive != null) {
			if (!"true".equalsIgnoreCase(recursive)) {
				Logger.debug("single folder specified (not recursive)");
				this.recursive = false;
			}
			else {
				Logger.debug("folder will be scanned recursively");
				this.recursive = true;
			}
		}
		
		String logLevel = readFromPrioritizedSource(PARAM_LOG_LEVEL);
		if (logLevel != null) {
			Logger.debug("changing logLevel as of configuration to " + logLevel);
			LogLevel level = Logger.getLevelFromCode(logLevel);
			if (level == null) throw new RuntimeException("invalid loglevel");
			Logger.setFilterLevel(level);
		}
		
		Logger.debug("parsed configuration voices");
		
		if (Logger.isEnabled(Logger.LEVEL_DEBUG)) {
			Logger.debug("specified source path: " + sourceFolder);
			
			if (getFixedTarget() != null) {
				Logger.debug("mapping to fixed target: " + getFixedTarget());
			}
			else {
				Logger.debug("specified jboss home: " + jbossHome);
				Logger.debug("specified jboss package: " + jbossDeployedPackagePrefix + "*");
				Logger.debug("specified jboss inner path: " + jbossDeployedSubpath);
			}
		}
	}

	public static Options buildCmdLineOptions() {
		Options options = new Options();

        Option a1 = new Option("s", PARAM_SOURCE_FOLDER, true, "source folders");
        a1.setRequired(false);
        options.addOption(a1);

        Option a2 = new Option("j", PARAM_JBOSS_HOME, true, "jboss home (e.g. /...path.../standalone");
        a2.setRequired(false);
        options.addOption(a2);

        Option a3 = new Option("p", PARAM_DEST_PACKAGE_PREFIX, true, "deploy package prefix (e.g. scadeweb-web-1.0.0.war)");
        a3.setRequired(false);
        options.addOption(a3);

        Option a4 = new Option("l", PARAM_LOG_LEVEL, true, "log level (one of trace, debug, info, warn, error, shut-up, every-single-shit");
        a4.setRequired(false);
        options.addOption(a4);
        
        Option a5 = new Option("z", PARAM_DEST_SUB_FOLDER, true, "relative path inner to jboss deployed structure");
        a5.setRequired(false);
        options.addOption(a5);

        Option a6 = new Option("r", PARAM_RECURSIVE, true, "recursively watch source folder subfolders");
        a6.setRequired(false);
        options.addOption(a6);

        Option a7 = new Option("f", PARAM_FILTER, true, "filter to apply to files");
        a7.setRequired(false);
        options.addOption(a7);
        
        Option a8= new Option("w", PARAM_DEST_ABSOLUTE, true, "don't deploy to jboss but to this directory");
        a8.setRequired(false);
        options.addOption(a8);
        
        return options;
	}
	
	public String getFixedTarget() {
		return fixedTarget;
	}

	public void setFixedTarget(String fixedTarget) {
		this.fixedTarget = fixedTarget;
	}

	public Pattern getFilterPattern() {
		return filterPattern;
	}

	public void setFilterPattern(Pattern filterPattern) {
		this.filterPattern = filterPattern;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public String getJbossDeployedSubpath() {
		return jbossDeployedSubpath;
	}

	public void setJbossDeployedSubpath(String jbossDeployedSubpath) {
		this.jbossDeployedSubpath = jbossDeployedSubpath;
	}

	public Boolean getRecursive() {
		return recursive;
	}

	public void setRecursive(Boolean recursive) {
		this.recursive = recursive;
	}

	public String getSourceFolder() {
		return sourceFolder;
	}

	public void setSourceFolder(String sourceFolder) {
		this.sourceFolder = sourceFolder;
	}

	public String getJbossHome() {
		return jbossHome;
	}

	public void setJbossHome(String jbossHome) {
		this.jbossHome = jbossHome;
	}

	public String getJbossDeployedPackagePrefix() {
		return jbossDeployedPackagePrefix;
	}

	public void setJbossDeployedPackagePrefix(String jbossDeployedPackagePrefix) {
		this.jbossDeployedPackagePrefix = jbossDeployedPackagePrefix;
	}
	
}
