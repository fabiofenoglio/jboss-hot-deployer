package com.ff.magicHotDeployer.updater;

public class UpdateFeedInfo {
	
	private Integer enable;
	private String version;
	private String message;
	private Integer mandatory;
	
	public Integer getMandatory() {
		return mandatory;
	}
	public void setMandatory(Integer mandatory) {
		this.mandatory = mandatory;
	}
	public Integer getEnable() {
		return enable;
	}
	public void setEnable(Integer enable) {
		this.enable = enable;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	
}
