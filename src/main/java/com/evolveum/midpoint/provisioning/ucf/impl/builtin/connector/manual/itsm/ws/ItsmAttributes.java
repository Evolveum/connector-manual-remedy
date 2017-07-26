package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public enum ItsmAttributes {
	INCIDENT_STATUS("IncidentStatus"), 
	INCIDENT_NUMBER("Incident_Number"),
	DESCRIPTION("Description", 100),
	DETAILED_DESCRIPTION("Detailed_Description"), // no limit set in specification
	CI_NAME("CI_Name", 254),
	
	PRIORITY("Priority", "3"),
	INCIDENT_TYPE("Incident_Type", "1"),
	REPORTED_SOURCE("Reported_Source", "10000"),
	SERVICE_TYPE("Service_Type", "1"),
	LAST_NAME("Last_Name", "IDM"),
	FIRST_NAME("First_Name", "Integration"),
	MESSAGE_ID("Message_ID", "CREATE");

	private String name;
	private Integer maxLength;
	private String defaultValue;

	private ItsmAttributes(String name) {
		this(name, null, null);
	}

	private ItsmAttributes(String name, Integer maxLength) {
		this(name, maxLength, null);
	}

	private ItsmAttributes(String name, String defaultValue) {
		this(name, null, defaultValue);
	}

	private ItsmAttributes(String name, Integer maxLength, String defaultValue) {
		this.name = name;
		this.maxLength = maxLength;
		this.defaultValue=defaultValue;
	}
	
	public String trimToMaxLength(String value) {
		String result = value;
		if(maxLength != null && isNotBlank(value) && value.length() > maxLength) {
			result = value.substring(0, maxLength);
		}
		return result;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getName() {
		return name;
	}
}
