package com.ff.magicHotDeployer.engine;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;

import com.ff.magicHotDeployer.configuration.ConfigurationProvider;
import com.ff.magicHotDeployer.logging.Logger;

public class JbossDeployer {
	
	public static Boolean processEvent(
			ConfigurationProvider cfg,
			Path eventFilePath, 
			WatchEvent.Kind<?> eventType, 
			Path baseSourcePath, 
			Path baseTargetFolder,
			String targetInnerPath
		) throws IOException
	{
		Integer attempt = 1;
		
		while (true) {
			try {
				trySingleProcessEvent(cfg, eventFilePath, eventType, baseSourcePath, baseTargetFolder, targetInnerPath);
			}
			catch (Exception e) {
				Logger.debug("event process attempt #" + attempt + " failed");
				Logger.error("event process attempt #" + attempt + " failed", e);
				
				if (attempt > cfg.getMaxRetries()) {
					Logger.warn("max retries reached for event process. aborting");
					throw e;
				}
				else {
					attempt ++;
					Logger.debug("retrying - attempt #" + attempt);
					
					try {
						Thread.sleep(cfg.getRetryDelay());
					} catch (InterruptedException e2) {
						e2.printStackTrace();
					}
				}
			}
		}
	}
	
	public static Boolean trySingleProcessEvent(
		ConfigurationProvider cfg,
		Path eventFilePath, 
		WatchEvent.Kind<?> eventType, 
		Path baseSourcePath, 
		Path baseTargetFolder,
		String targetInnerPath
	) throws IOException {

		if (targetInnerPath != null && !"".equals(targetInnerPath)) {
			baseTargetFolder = Paths.get(baseTargetFolder.toAbsolutePath().toString(), targetInnerPath);
		}
		
		Path targetPath = reflectSourceToTargetPath(
			eventFilePath.toAbsolutePath().toString(), 
			baseSourcePath.toAbsolutePath().toString(), 
			baseTargetFolder.toAbsolutePath().toString()
		);

		Boolean isFolder = (
			targetPath.toFile().exists() ? 
					targetPath.toFile().isDirectory() :
					eventFilePath.toFile().isDirectory()
		);

		if (!isFolder && cfg.getFilterPattern() != null) {
			if (!cfg.getFilterPattern().matcher(eventFilePath.toAbsolutePath().toString()).find()) {
				Logger.trace("file excluded by filter : " + eventFilePath.toAbsolutePath().toString());
				return false;
			}
		}
		
		if (eventType == ENTRY_CREATE) {
			if (isFolder) {
				hotDeployNewFolder(eventFilePath.toFile(), targetPath.toFile());
			}
			else {
				// file created
				hotDeployFile(eventFilePath.toFile(), targetPath.toFile());
			}
		}
		else if (eventType == ENTRY_DELETE) {
			if (isFolder) {
				// folder deleted
				hotUndeployFolder(targetPath.toFile());
			}
			else {
				// file deleted
				hotUndeployFile(targetPath.toFile());
			}
		}
		else if (eventType == ENTRY_MODIFY) {
			if (isFolder) {
				Logger.trace("skipping folder modify event (nothing to do)");
				return true;
			}
			else {
				// file edited
				hotDeployFile(eventFilePath.toFile(), targetPath.toFile());
			}
		}
		else {
			Logger.warn("invalid event type " + eventType.toString());
			return false;
		}
		
		return true;
	}
	
	public static Boolean hotDeployNewFolder(File source, File target) throws IOException {
		// register directory and sub-directories

		final File sourceFinal = source;
		final File targetFinal = target;
		
        Files.walkFileTree(source.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
            	Logger.trace("new file in created folder to deploy : " + file.toAbsolutePath().toString());
            	
            	Path targetPath = reflectSourceToTargetPath(
            		file.toFile().getAbsolutePath(), 
            		sourceFinal.getAbsolutePath(), 
            		targetFinal.getAbsolutePath()
        		);
            	
            	hotDeployFile(file.toFile(), targetPath.toFile());
            	
            	return FileVisitResult.CONTINUE;
            }
        });
        
        return true;
	}
	
	public static Boolean hotDeployFile(File source, File target) throws IOException {
		Logger.trace("executing hotDeployFile from " + source.getAbsolutePath() + " to " + target.getAbsolutePath());
		if (!source.exists()) {
			Logger.trace("skipping (missing)");
			return false;
		}
		FileUtils.copyFile(source, target);
		return true;
	}

	public static Boolean hotUndeployFile(File target) throws IOException {
		if (!target.exists()) {
			Logger.trace("skipping (missing)");
			return false;
		}
		Logger.trace("executing hotUndeployFile from " + target.getAbsolutePath());
		target.delete();
		return true;
	}

	public static Boolean hotUndeployFolder(File target) throws IOException {
		if (!target.exists()) {
			Logger.trace("skipping (missing)");
			return false;
		}
		Logger.trace("executing hotUndeployFolder from " + target.getAbsolutePath());
		FileUtils.deleteDirectory(target);
		return true;
	}

	public static Path reflectSourceToTargetPath(String sourcePath, String sourceBase, String targetBase) {
		if (Logger.isEnabled(Logger.LEVEL_TRACE)) {
			Logger.trace("reflecting STT path from " + sourcePath);
			Logger.trace("based at " + sourceBase);
			Logger.trace("to " + targetBase);
		}
		
		String relative = new File(sourceBase).toURI().relativize(new File(sourcePath).toURI()).getPath();
		Path targetPath = Paths.get(targetBase, relative);
		
		if (Logger.isEnabled(Logger.LEVEL_TRACE)) {
			Logger.trace("reflected to " + targetPath.toAbsolutePath().toString());
		}
		
		return targetPath;
	}
	
	public static Path findDeploymentPath(String jbossHome, String matchPrefix) throws IOException {
		
		Logger.trace("running deployment path serch from " + jbossHome + ", matching " + matchPrefix);
		
		Path path = Paths.get(jbossHome, "tmp/vfs/deployment");
		
		File file = new File(path.toAbsolutePath().toString());
		String[] directories = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory() && name.startsWith("deployment");
			}
		});
		
		String picked = null;
		
		if (directories.length < 1) {
			throw new RuntimeException("no base deployment directory found in " + path.toAbsolutePath().toString());
		}
		
		if (directories.length > 1) {
			picked = pickMostRecentDeploymentFolder(path, directories);
		}
		else {
			picked = directories[0];
		}
		
		Logger.trace("found first deployment dir: " + path);
		
		file = path.resolve(picked).toFile();
		directories = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		
		Logger.trace("found " + directories.length + " exploded packages");
		
		String found = null;
		for (String d : directories) {
			if (d.startsWith(matchPrefix)) {
				found = d;
				break;
			}
		}
		
		if (found != null) {
			Logger.trace("found matching package " + found);
			return Paths.get(file.getPath(), found);
		}
		
		throw new RuntimeException("no deployed package found in " + file.getAbsolutePath());
	}
	
	private static String pickMostRecentDeploymentFolder(Path path, String[] folders) throws IOException {
		// throw new RuntimeException("cannot proceed : MULTIPLE DEPLOYMENT FOLDERS found in " + path.toAbsolutePath().toString());
		
		String mostRecent = null;
		BasicFileAttributes mostRecentAttr = null;
		
		Logger.trace("running deployment folder picking procedure on creation time");
		
		for (String sub : folders) {
			Path subPath = path.resolve(sub);
			BasicFileAttributes attr = Files.readAttributes(subPath, BasicFileAttributes.class);
			
			Logger.trace("folder " + sub + " created at " + attr.creationTime().toString());
			
			if (mostRecent == null) {
				mostRecent = sub;
				mostRecentAttr = attr;
				continue;
			}
			
			if (attr.creationTime().compareTo(mostRecentAttr.creationTime()) > 0) {
				mostRecentAttr = attr;
				mostRecent = sub;
			}
		}
		
		Logger.trace("most recent is " + mostRecent);
		return mostRecent;
	}
}
