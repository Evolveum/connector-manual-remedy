package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws;

public enum MessageType {
	CREATE("Create"), GET_ENTRY("GetEntry");
	
	private String value;

	private MessageType(String value) {
		this.value = value;
	}
	
	public String value() {
		return this.value;
	}
}
