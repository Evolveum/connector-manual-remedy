package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io;

/*
 * Values available for templating the ticket attribute values.
 */
public class TemplateContext {
	
	private String cIName;
	private String operation;
	private String accountChanges;
	private String identifier;
	
	public String getcIName() {
		return cIName;
	}
	public void setcIName(String cIName) {
		this.cIName = cIName;
	}
	public String getOperation() {
		return operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	public String getAccountChanges() {
		return accountChanges;
	}
	public void setAccountChanges(String accountValue) {
		this.accountChanges = accountValue;
	}
	public String getIdentifier() {
		return this.identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}
