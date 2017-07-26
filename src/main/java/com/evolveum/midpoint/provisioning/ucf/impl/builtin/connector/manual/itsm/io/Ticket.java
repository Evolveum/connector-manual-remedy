package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws.ItsmAttributes;

public class Ticket {

	private Map<String, String> attributes = new HashMap<>();
	
	{
		// default ticket attributes given by itsm specification
		add(ItsmAttributes.PRIORITY);
		add(ItsmAttributes.INCIDENT_TYPE);
		add(ItsmAttributes.REPORTED_SOURCE);
		add(ItsmAttributes.SERVICE_TYPE);
		add(ItsmAttributes.LAST_NAME);
		add(ItsmAttributes.FIRST_NAME);
		add(ItsmAttributes.MESSAGE_ID);
	}

	private void add(ItsmAttributes attr) {
		add(attr, attr.getDefaultValue());
	}

	/**
	 * Adds attribute key with value to ticket. 
	 * Does nothing if value is null to preserve default values.
	 */
	public void add(ItsmAttributes key, String value) {
		if(value == null) {
			return;
		}
		
		String mapKey = key.getName();
		if(attributes.containsKey(mapKey)) {
			attributes.remove(mapKey);
		}
		attributes.put(mapKey, key.trimToMaxLength(value));
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

}
