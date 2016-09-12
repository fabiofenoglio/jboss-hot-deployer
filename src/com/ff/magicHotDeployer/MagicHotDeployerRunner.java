package com.ff.magicHotDeployer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.ini4j.Profile.Section;

import com.ff.magicHotDeployer.configuration.ConfigurationProvider;
import com.ff.magicHotDeployer.engine.RunnableEngineInstance;
import com.ff.magicHotDeployer.logging.Logger;

public class MagicHotDeployerRunner {

	public static void main(String[] args) {
		try {
			run(args);
		}
		catch (Throwable e) {
			Logger.error("error running application", e);
		}
	}

	private static void run(String[] args) throws IOException, BackingStoreException {
		Logger.setFilterLevel(Logger.LEVEL_EVERY_SINGLE_SHIT);
		Logger.info("starting");
		
		Options options = ConfigurationProvider.buildCmdLineOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        
        try {
            cmd = parser.parse(options, args);
        } catch (Exception e) {
            formatter.printHelp("magicHotDeployer", options);
            throw new RuntimeException("Can't load command line arguments", e);
        }
        
        String path = ConfigurationProvider.getConfigFilePath();
		Logger.debug("looking for configuration file " + path);
		File iniFile = new File(path);
		List<Preferences> configNodes = new ArrayList<Preferences>();
		Preferences rootConfigNode = null;
		Ini ini = null;
		IniPreferences cfgCacheBase = null;
		
		if (iniFile.exists()) {
			Logger.debug("loading configuration from ini file");
			ini = new Ini(iniFile);
			cfgCacheBase = new IniPreferences(ini);
			Logger.debug("loaded raw configuration from ini file");
		}
		else {
			Logger.debug("not loading configuration from ini file (missing file)");
		}
		
		if (ini != null) {
			for (Section section : ini.values()) {
				String sectionName = section.getName();
				if (sectionName.equals("config") || sectionName.equals("default") || sectionName.equals("root")) {
					rootConfigNode = cfgCacheBase.node(sectionName);
				}
				else {
					configNodes.add(cfgCacheBase.node(sectionName));
				}
			}
		}
		
		List<RunnableEngineInstance> instances = new ArrayList<RunnableEngineInstance>();
		
		for (Preferences node : configNodes) {
			
			ConfigurationProvider cfg = new ConfigurationProvider(
				cmd, node, rootConfigNode
			);
			
			Logger.debug("configuring instance " + node.name());
			
			try {
				cfg.reload();
			} catch (Exception e) {
				throw new RuntimeException("Can't load configuration", e);
			}
			
			RunnableEngineInstance instanceRunnerThread = new RunnableEngineInstance(cfg);
			instances.add(instanceRunnerThread);
		}
		
		Logger.debug(instances.size() + " instances configured and ready to go");
		for (RunnableEngineInstance instance : instances) {
			instance.start();
		}
		
		while (true) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Boolean atLeastOneAlive = false;
			
			for (RunnableEngineInstance instance : instances) {
				if (instance.isAlive()) {
					atLeastOneAlive = true;
					break;
				}
			}
			
			if (!atLeastOneAlive) {
				Logger.info("no alive instances, exiting");
				System.exit(1);
			}
		}
	}
	
}
