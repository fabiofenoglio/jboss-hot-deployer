package com.ff.magicHotDeployer;

import java.io.IOException;
import java.util.List;
import java.util.prefs.BackingStoreException;
import org.apache.commons.cli.CommandLine;
import com.ff.magicHotDeployer.configuration.ConfigurationProvider;
import com.ff.magicHotDeployer.engine.EngineLoader;
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
		
        CommandLine cmd = ConfigurationProvider.parseCommandLine(args);
        List<RunnableEngineInstance> instances = EngineLoader.loadEngines(cmd);
		
		Logger.debug(instances.size() + " instances configured and ready to go");
		
		for (RunnableEngineInstance instance : instances) {
			instance.start();
		}
		
		while (true) {
			monitorEnginesPause();
			monitorEngines(instances);
		}
	}
	
	private static void monitorEngines(List<RunnableEngineInstance> instances) {
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
	
	private static void monitorEnginesPause() {
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
