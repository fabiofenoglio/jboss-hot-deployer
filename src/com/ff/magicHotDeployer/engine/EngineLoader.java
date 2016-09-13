package com.ff.magicHotDeployer.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

import com.ff.magicHotDeployer.configuration.ConfigurationProvider;
import com.ff.magicHotDeployer.logging.Logger;

public class EngineLoader {

	
	public static List<RunnableEngineInstance> loadEngines(CommandLine cmd) throws InvalidFileFormatException, IOException {

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
		
		return instances;
	}
}
