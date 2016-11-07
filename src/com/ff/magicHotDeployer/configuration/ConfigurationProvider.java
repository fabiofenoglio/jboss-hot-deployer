package com.ff.magicHotDeployer.configuration;

import java.io.IOException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
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
	public final static String PARAM_WATCH_FROM = "watchFrom";
	public final static String PARAM_MAX_RETRIES = "maxRetries";
	public final static String PARAM_RETRY_DELAY = "retryDelay";
	
	// for jboss 4 support
	public final static String PARAM_DEPLOY_MODE = "deployMode";
	
	public final static String PARAM_VAL_DEPLOY_MODE_JBOSS4 = "jboss4";
	
	public final static Integer DEFAULT_MAX_RETRIES = 3;
	public final static Integer DEFAULT_RETRY_DELAY = 100;
	
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
	private String watchFrom = null;
	private Integer maxRetries = null;
	private Integer retryDelay = null;
	private String deployMode = null;
	
	public static String getCurrentPath() {
		return System.getProperty("user.dir");
	}
	public static String getConfigFilePath() {
		return getCurrentPath() + "/config.ini";
	}

	public ConfigurationProvider(CommandLine cmdLineOptions, Preferences node, Preferences defaultNode) {
		this.cmdLineOptions = cmdLineOptions;
		this.cfgCache = node;
		this.cfgCacheDefault = defaultNode;
	}

	public static CommandLine parseCommandLine(String[] args) {
		Options options = ConfigurationProvider.buildCmdLineOptions();
        CommandLine cmd = null;
        
        try {
        	CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (Exception e) {
        	HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("magicHotDeployer", options);
            System.exit(0);
            return null;
        }
        
        return cmd;
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

		jbossDeployedPackagePrefix = readFromPrioritizedSource(PARAM_DEST_PACKAGE_PREFIX);

		jbossDeployedSubpath = readFromPrioritizedSource(PARAM_DEST_SUB_FOLDER);
		if (jbossDeployedSubpath == null) jbossDeployedSubpath = "";
		
		fixedTarget = readFromPrioritizedSource(PARAM_DEST_ABSOLUTE);
		if (fixedTarget != null && "".equals(fixedTarget)) fixedTarget = null;
		
		name = readFromPrioritizedSource(PARAM_INSTANCE_NAME);
		if (name == null) name = "I" + (instanceIndex ++);
		
		watchFrom = readFromPrioritizedSource(PARAM_WATCH_FROM);
		if (watchFrom != null && "".equals(watchFrom)) watchFrom = null;

		String maxRetriesStr = readFromPrioritizedSource(PARAM_MAX_RETRIES);
		if (maxRetriesStr != null && "".equals(maxRetriesStr)) maxRetriesStr = null;
		if (maxRetriesStr == null) {
			maxRetries = DEFAULT_MAX_RETRIES;
		}
		else {
			maxRetries = Integer.valueOf(maxRetriesStr);
		}

		String retryDelayStr = readFromPrioritizedSource(PARAM_RETRY_DELAY);
		if (retryDelayStr != null && "".equals(retryDelayStr)) retryDelayStr = null;
		if (retryDelayStr == null) {
			retryDelay = DEFAULT_RETRY_DELAY;
		}
		else {
			retryDelay = Integer.valueOf(retryDelayStr);
		}
		
		filter = readFromPrioritizedSource(PARAM_FILTER);
		// filter can be null
		if (filter != null && !"".equals(filter)) {
			filterPattern = Pattern.compile(filter);
		}
		
		deployMode = readFromPrioritizedSource(PARAM_DEPLOY_MODE);
		
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

        Option a9= new Option("wf", PARAM_WATCH_FROM, true, "place watcher in this directory instead of the watched one");
        a9.setRequired(false);
        options.addOption(a9);
        
        return options;
	}
	
	public Boolean isJboss4() {
		return (deployMode != null && deployMode.equals(PARAM_VAL_DEPLOY_MODE_JBOSS4));
	}
	
	public String getDeployMode() {
		return deployMode;
	}
	public void setDeployMode(String deployMode) {
		this.deployMode = deployMode;
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
	public String getWatchFrom() {
		return watchFrom;
	}
	public void setWatchFrom(String watchFrom) {
		this.watchFrom = watchFrom;
	}
	public Integer getMaxRetries() {
		return maxRetries;
	}
	public void setMaxRetries(Integer maxRetries) {
		this.maxRetries = maxRetries;
	}
	public Integer getRetryDelay() {
		return retryDelay;
	}
	public void setRetryDelay(Integer retryDelay) {
		this.retryDelay = retryDelay;
	}
	
}
