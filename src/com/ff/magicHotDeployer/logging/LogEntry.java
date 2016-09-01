package com.ff.magicHotDeployer.logging;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class LogEntry {

	private LogLevel level;
	private String message;
	private HashMap<String, Object> params;
	private Date time;
	
	public LogEntry() {
		this.clear();
	}

	public LogEntry(String message) {
		this.clear();
		this.message = message;
		this.params = new HashMap<>();
	}

	public LogEntry(LogLevel level, String message) {
		this.clear();
		this.level = level;
		this.message = message;
	}

	public LogEntry(LogLevel level, String message, Object param) {
		this.clear();
		this.level = level;
		this.message = message;
		this.params.put("P0", param);
	}
	
	public void clear() {
		this.level = Logger.LEVEL_INFO;
		this.message = null;
		this.params = new HashMap<>();
		this.time = Calendar.getInstance().getTime();
	}

	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
	public LogLevel getLevel() {
		return level;
	}
	public void setLevel(LogLevel level) {
		this.level = level;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public HashMap<String, Object> getParams() {
		return params;
	}
	public void setParams(HashMap<String, Object> params) {
		this.params = params;
	}
	
	
}
