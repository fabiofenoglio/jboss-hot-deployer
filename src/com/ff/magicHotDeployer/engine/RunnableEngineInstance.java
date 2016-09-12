package com.ff.magicHotDeployer.engine;

import com.ff.magicHotDeployer.configuration.ConfigurationProvider;
import com.ff.magicHotDeployer.logging.Logger;

public class RunnableEngineInstance extends Thread {

	private ConfigurationProvider cfg;
	
	public RunnableEngineInstance(ConfigurationProvider cfg) {
		this.cfg = cfg;
	}

	public void run() {
    	
		Logger.info("[instance] loading engine " + cfg.getName());
		MagicHotDeployerEngine engine = new MagicHotDeployerEngine(cfg);
		
		Logger.info("[instance] starting engine " + cfg.getName());
		
		try {
			engine.run();
		} catch (Throwable e) {
			Logger.error("engine instance error", e);
			throw new RuntimeException(e);
		}
		
		Logger.info("[instance] engine stopped " + cfg.getName());
    }  
 
}
