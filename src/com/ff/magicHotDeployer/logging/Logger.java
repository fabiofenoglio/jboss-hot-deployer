package com.ff.magicHotDeployer.logging;

public class Logger {

	public static LogLevel LEVEL_ERROR = new LogLevel(500, "ERROR", "red");
	public static LogLevel LEVEL_WARNING = new LogLevel(200, "WARN", "orange");
	public static LogLevel LEVEL_INFO = new LogLevel(100, "info", "#333377");
	public static LogLevel LEVEL_DEBUG = new LogLevel(30, "debug", "#773333");
	public static LogLevel LEVEL_TRACE = new LogLevel(10, "trace", "#555555");
	
	public static LogLevel LEVEL_SHUT_UP = new LogLevel(9999, null, null);
	public static LogLevel LEVEL_EVERY_SINGLE_SHIT = new LogLevel(0, null, null);
	
	public static LogLevel filterLevel = LEVEL_DEBUG;

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
			line = "[" + level.getName() + "] " + line;
			System.out.println(line);
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
