package com.ff.magicHotDeployer.logging;

import java.util.concurrent.locks.ReentrantLock;

import com.diogonunes.jcdp.color.ColoredPrinter;
import com.diogonunes.jcdp.color.api.Ansi.Attribute;
import com.diogonunes.jcdp.color.api.Ansi.BColor;
import com.diogonunes.jcdp.color.api.Ansi.FColor;

public class Logger {

	public static LogLevel LEVEL_ERROR = new LogLevel(500, "ERROR", "red");
	public static LogLevel LEVEL_WARNING = new LogLevel(200, " WARN", "orange");
	public static LogLevel LEVEL_INFO = new LogLevel(100, " info", "#333377");
	public static LogLevel LEVEL_DEBUG = new LogLevel(30, "debug", "#773333");
	public static LogLevel LEVEL_TRACE = new LogLevel(10, "trace", "#555555");
	
	public static LogLevel LEVEL_SHUT_UP = new LogLevel(9999, null, null);
	public static LogLevel LEVEL_EVERY_SINGLE_SHIT = new LogLevel(0, null, null);
	
	public static LogLevel filterLevel = LEVEL_DEBUG;

	public static ReentrantLock lock = new ReentrantLock();
	
	public static ColoredPrinter printer = 
			new ColoredPrinter.Builder(1, false)
            .foreground(FColor.WHITE)
            .background(BColor.BLACK)
            .build();
	
	public static LogLevel getLevelFromCode(String code) {
		switch (code) {
			case "error": return LEVEL_ERROR;
			case "warn": return LEVEL_WARNING;
			case "info": return LEVEL_INFO;
			case "debug": return LEVEL_DEBUG;
			case "trace": return LEVEL_TRACE;
			case "shut-up": return LEVEL_SHUT_UP;
			case "every-single-shit": return LEVEL_EVERY_SINGLE_SHIT;
			default: return null;
		}
	}
	
	public static void log(String message, LogLevel level) {
		if (level.getPriority() >= getFilterLevel().getPriority()) {
			
			String line = message;
			String pre = level.getName();
		
			Logger.lock.lock();
			
			if (level == LEVEL_DEBUG) {
				printer.print(
					"[" + pre + "]", 
					Attribute.NONE, FColor.CYAN, BColor.BLACK
				);
				printer.println(
					" " + line, 
					Attribute.NONE, FColor.WHITE, BColor.BLACK
				);
			}
			else if (level == LEVEL_TRACE) {
				printer.print(
					"[" + pre + "]", 
					Attribute.NONE, FColor.MAGENTA, BColor.BLACK
				);
				printer.println(
					" " + line, 
					Attribute.NONE, FColor.WHITE, BColor.BLACK
				);
			}
			else if (level == LEVEL_INFO) {
				printer.print(
					"[" + pre + "]", 
					Attribute.NONE, FColor.WHITE, BColor.BLACK
				);
				printer.println(
					" " + line, 
					Attribute.NONE, FColor.WHITE, BColor.BLACK
				);
			}
			else if (level == LEVEL_ERROR) {
				printer.print(
					"[" + pre + "]", 
					Attribute.NONE, FColor.WHITE, BColor.RED
				);
				printer.println(
					" " + line, 
					Attribute.NONE, FColor.WHITE, BColor.RED
				);
			}
			else if (level == LEVEL_WARNING) {
				printer.print(
					"[" + pre + "]", 
					Attribute.NONE, FColor.WHITE, BColor.YELLOW
				);
				printer.println(
					" " + line, 
					Attribute.NONE, FColor.WHITE, BColor.YELLOW
				);
			}
			else {
				printer.println(
					"[" + pre + "] " + line, 
					Attribute.NONE, FColor.WHITE, BColor.BLACK
				);	
			}
			System.out.println("");
			
			Logger.lock.unlock();
		}
	}

	public static boolean isEnabled(LogLevel level) {
		if (level.getPriority() >= getFilterLevel().getPriority()) {
			return true;
		}
		return false;
	}

	public static LogLevel getFilterLevel() {
		return filterLevel;
	}

	public static void setFilterLevel(LogLevel filterLevel) {
		Logger.filterLevel = filterLevel;
	}
	
	public static void info(String message) {
		log(message, LEVEL_INFO);		
	}
	public static void info(String message, Object object) {
		log(message, LEVEL_INFO);
		log(object.toString(), LEVEL_INFO);
	}
	public static void debug(String message) {
		log(message, LEVEL_DEBUG);		
	}
	public static void debug(String message, Object object) {
		log(message, LEVEL_DEBUG);
		log(object.toString(), LEVEL_DEBUG);
	}
	public static void trace(String message) {
		log(message, LEVEL_TRACE);
	}
	public static void trace(String message, Object object) {
		log(message, LEVEL_TRACE);
		log(object.toString(), LEVEL_TRACE);
	}
	public static void warn(String message) {
		log(message, LEVEL_WARNING);
	}
	public static void warn(String message, Object object) {
		log(message, LEVEL_WARNING);
		log(object.toString(), LEVEL_WARNING);
	}
	public static void error(String message) {
		log(message, LEVEL_ERROR);
	}
	public static void error(Throwable e) {
		log(e.getMessage(), LEVEL_ERROR);
		if (getFilterLevel().getPriority() <= LEVEL_ERROR.getPriority()) {
			e.printStackTrace();
		}
	}
	public static void error(String message, Throwable e) {
		log(message, LEVEL_ERROR);
		log(e.getMessage(), LEVEL_ERROR);
		if (getFilterLevel().getPriority() <= LEVEL_ERROR.getPriority()) {
			e.printStackTrace();
		}
	}
}
