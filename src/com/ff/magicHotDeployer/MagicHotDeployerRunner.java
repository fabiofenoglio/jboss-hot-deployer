package com.ff.magicHotDeployer;

import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.ff.magicHotDeployer.configuration.ConfigurationProvider;
import com.ff.magicHotDeployer.engine.MagicHotDeployerEngine;
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

	private static void run(String[] args) throws IOException {
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
        
		ConfigurationProvider cfg = new ConfigurationProvider(cmd);
		try {
			cfg.reload();
		} catch (Exception e) {
			throw new RuntimeException("Can't load configuration", e);
		}
		
		Logger.info("loading engine");
		MagicHotDeployerEngine engine = new MagicHotDeployerEngine(cfg);
		
		Logger.info("starting engine");
		engine.run();
		
		Logger.info("engine stopped");
	}
	
}
