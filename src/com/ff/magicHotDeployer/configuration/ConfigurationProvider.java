package com.ff.magicHotDeployer.configuration;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.ini4j.InvalidFileFormatException;

import com.ff.magicHotDeployer.logging.LogLevel;
import com.ff.magicHotDeployer.logging.Logger;

public class ConfigurationProvider {

	public final static String DEFAULT_NODE = "config";
	
	private Preferences cfgCache = null;
	
	private CommandLine cmdLineOptions = null;
	private String[] sourceFolders = null;
	private String jbossHome = null;
	private String jbossDeployedPackagePrefix = null;
	private String jbossDeployedSubpath = null;
	private Boolean recursive = true;
	
	public ConfigurationProvider(CommandLine cmdLineOptions) {
		this.cmdLineOptions = cmdLineOptions;
	}
	
	public String getCurrentPath() {
		return System.getProperty("user.dir");
	}

	public String getConfigFilePath() {
		return getCurrentPath() + "/config.ini";
	}
	
	public Preferences getLoadedNode() {
		if (cfgCache == null) return null;
		return cfgCache.node(DEFAULT_NODE);
	}
	
	public static Options buildCmdLineOptions() {
		Options options = new Options();

        Option a1 = new Option("s", "sourceFolders", true, "source folders");
        a1.setRequired(false);
        options.addOption(a1);

        Option a2 = new Option("d", "jbossHome", true, "jboss home (e.g. /...path.../standalone");
        a2.setRequired(false);
        options.addOption(a2);

        Option a3 = new Option("p", "jbossDeployedPackagePrefix", true, "deploy package prefix (e.g. scadeweb-web-1.0.0.war)");
        a3.setRequired(false);
        options.addOption(a3);

        Option a4 = new Option("l", "logLevel", true, "log level (one of trace, debug, info, warn, error, shut-up, every-single-shit");
        a4.setRequired(false);
        options.addOption(a4);
        
        Option a5 = new Option("z", "jbossDeployedSubpath", true, "relative path inner to jboss deployed structure");
        a5.setRequired(false);
        options.addOption(a5);
        	
        return options;
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
		return getLoadedNode().get(key, def);
	}
	
	public void reload() throws InvalidFileFormatException, IOException {
		String path = getConfigFilePath();
		Logger.debug("looking for configuration file " + path + ", node = " + DEFAULT_NODE);
		File iniFile = new File(path);
		
		if (iniFile.exists()) {
			Logger.debug("loading configuration from ini file");
			Ini ini = new Ini(iniFile);
			cfgCache = new IniPreferences(ini);
			Logger.debug("loaded raw configuration from ini file");
		}
		else {
			Logger.debug("not loading configuration from ini file (missing file)");
		}
		
		String sourceFoldersList = readFromPrioritizedSource("sourceFolders");
		if (sourceFoldersList == null) throw new RuntimeException("no sourceFolders in configuration");
		sourceFolders = sourceFoldersList.split("\\|");
		
		jbossHome = readFromPrioritizedSource("jbossHome");
		if (jbossHome == null) throw new RuntimeException("no jbossHome in configuration");

		jbossDeployedPackagePrefix = readFromPrioritizedSource("jbossDeployedPackagePrefix");
		if (jbossDeployedPackagePrefix == null) throw new RuntimeException("no jbossDeployedPackagePrefix in configuration");

		jbossDeployedSubpath = readFromPrioritizedSource("jbossDeployedSubpath");
		if (jbossDeployedSubpath == null) throw new RuntimeException("no jbossDeployedSubpath in configuration");
		
		String recursive = readFromPrioritizedSource("recursive");
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
		
		String logLevel = readFromPrioritizedSource("logLevel");
		if (logLevel != null) {
			Logger.debug("changing logLevel as of configuration to " + logLevel);
			LogLevel level = Logger.getLevelFromCode(logLevel);
			if (level == null) throw new RuntimeException("invalid loglevel");
			Logger.setFilterLevel(level);
		}
		
		Logger.debug("parsed configuration voices");
		
		if (Logger.isEnabled(Logger.LEVEL_DEBUG)) {
			for (String sf : sourceFolders) {
				Logger.debug("specified source path: " + sf);
			}
			Logger.debug("specified jboss home: " + jbossHome);
		}
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

	public String[] getSourceFolders() {
		return sourceFolders;
	}

	public void setSourceFolders(String[] sourceFolders) {
		this.sourceFolders = sourceFolders;
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
