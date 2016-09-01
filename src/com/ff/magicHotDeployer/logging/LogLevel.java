package com.ff.magicHotDeployer.logging;

public class LogLevel {

	private Integer priority;
	private String name;
	private String color;
	
	public LogLevel() {
		this.clear();
	}
	
	public LogLevel(Integer priority, String name, String color) {
		this.clear();
		this.priority = priority;
		this.name = name;
		this.color = color;
	}
	
	public void clear() {
		this.priority = 100;
		this.name = "LOGLEVEL";
		this.color = "black";
	}

	public String getHtmlPrefix() {
		return "<font color='" + color + "'>[" + name + "]</font> ";
	}

	public Integer getPriority() {
		return priority;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
}
