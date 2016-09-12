package com.ff.magicHotDeployer.engine;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.nio.file.LinkOption.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import com.ff.magicHotDeployer.configuration.ConfigurationProvider;
import com.ff.magicHotDeployer.logging.Logger;

public class MagicHotDeployerEngine {

	private ConfigurationProvider cfg;
	
	private String instanceName;
	private WatchService watcher;
    private Map<WatchKey, Path> keys = new HashMap<WatchKey, Path>();
    private Path targetFolder;
    private Boolean recursive;
    private String targetInnerPath = "";
    private Long counter = 0L;
    
	public MagicHotDeployerEngine(ConfigurationProvider cfg) {
		this.cfg = cfg;
		this.recursive = cfg.getRecursive();
		this.targetInnerPath = cfg.getJbossDeployedSubpath();
		this.instanceName = "[ " + cfg.getName() + " ] ";
	}
	
	public void registerFolder(Path folder) throws IOException {
		WatchKey key = folder.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		keys.put(key, folder);
		Logger.trace(this.instanceName + "folder registered to watchService: " + folder.toAbsolutePath().toString());
	}
	
	private void registerAllFolders(Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
            	registerFolder(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
	
	@SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
	
	private Path getTargetFolder() {
		if (targetFolder != null) {
        	if (!Files.exists(targetFolder)) {
        		Logger.debug(this.instanceName + "cached path is no longer available");
        		targetFolder = null;
        	}
        }
        
        if (targetFolder == null) {
        	Logger.debug(this.instanceName + "running a new deployment path search");
        	
        	if (cfg.getFixedTarget() != null) {
        		targetFolder = Paths.get(URI.create("file:///" + cfg.getFixedTarget()));
        	}
        	else {
        		targetFolder = JbossDeployer.findDeploymentPath(
        			cfg.getJbossHome(), 
        			cfg.getJbossDeployedPackagePrefix()
    			);
        	}
        	
        	Logger.debug(this.instanceName + "new deployment path is " + targetFolder.toAbsolutePath().toString());
        }
        
        return targetFolder;
	}
	
	public void run() throws IOException {
		watcher = FileSystems.getDefault().newWatchService();
	
		Path sourcePath = Paths.get(URI.create("file:///" + cfg.getSourceFolder()));
		
		if (recursive) {
			registerAllFolders(sourcePath);
		}
		else {
			registerFolder(sourcePath);
		}
		
		Logger.debug(this.instanceName + "instance is UP and RUNNING");
		
		while (true) {

            WatchKey key;
		    try {
		        key = watcher.take();
		    } catch (InterruptedException x) {
		        throw new RuntimeException("main watch loop interrupted", x);
		    }

            Path dir = keys.get(key);
            if (dir == null) {
                Logger.error("watchkey not recognized " + key.toString());
                continue;
            }
            
		    for (WatchEvent<?> event: key.pollEvents()) {
		        WatchEvent.Kind<?> kind = event.kind();
		        
		        if (kind == OVERFLOW) {
		        	Logger.warn(this.instanceName + "OVERFLOW EVENT RECEIVED - resources are probably out of sync");
		            continue;
		        }

		        // The filename is the context of the event.
		        WatchEvent<Path> ev = cast(event);
		        
		        Path filename = ev.context();

		        // Verify that the new file is a text file.
		        Path filePath;
		        try {
		        	filePath = dir.resolve(filename);
		        } catch (Exception e) {
		            Logger.error(this.instanceName + "error resolving changed file " + filename.toString(), e);
		            continue;
		        }

		        Logger.debug(this.instanceName + "event #" + (this.counter) + " : " + kind.toString() + " " + filePath);
		        this.counter ++;
		        
		        if (recursive && (kind == ENTRY_CREATE)) {
                    try {
                        if (Files.isDirectory(filePath, NOFOLLOW_LINKS)) {
                        	registerAllFolders(filePath);
                        }
                    } catch (IOException x) {
                        Logger.error(this.instanceName + "a folder has been created but recursive registering failed: " + filePath.toString());
                    }
                }
		        
		        try {
		        	JbossDeployer.processEvent(cfg, filePath, kind, sourcePath, getTargetFolder(), targetInnerPath);
		        }
		        catch (Throwable e) {
		        	Logger.error(this.instanceName + "cannot process event " + kind + " : " + filePath.toAbsolutePath().toString(), e);
		        }
		        Logger.trace("--------------------------------------");
		    }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
		}
	}
	
}
