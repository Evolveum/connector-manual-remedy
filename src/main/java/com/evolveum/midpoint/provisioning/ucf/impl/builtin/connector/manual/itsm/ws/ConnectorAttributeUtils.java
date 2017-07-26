package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws;

import java.util.List;

import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io.Ticket;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import eu.domainname.integrator.AttributeType;
import eu.domainname.integrator.AttributesType;

public class ConnectorAttributeUtils {

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorAttributeUtils.class);
	
	public static String getAttributeValue(ItsmAttributes attribute, List<AttributeType> attributes) {
		return attributes.stream()
				.filter(attr -> attribute.getName().equals(attr.getName()))
				.findFirst()
				.map(AttributeType::getValue)
				.orElse(null);
	}

	public static AttributeType wsAttribute(ItsmAttributes attribute, String value) {
		AttributeType attr = new AttributeType(); {
			attr.setName(attribute.getName());
			attr.setValue(value);
		}

		return attr;
	}

	/**
	 * Maps ticket attributes to soap attributes.
	 * @param ticket
	 * @return
	 */
	public static AttributesType from(Ticket ticket) {
		if(ticket.getAttributes() == null) {
			return null;
		}
		
		AttributesType result = new AttributesType();
		List<AttributeType> attributesList = result.getAttribute();
		
		ticket.getAttributes().entrySet().stream()
			.map(entry -> attr(entry.getKey(), entry.getValue()))
			.forEach(attributesList::add);

		return result;
	}

	private static AttributeType attr(String name, String value) {
		AttributeType attr = new AttributeType(); {
			attr.setName(name);
			attr.setValue(value);
		}

		return attr;
	}
	
	public static TicketStatus fromWsCode(String code) {
		for(TicketStatus ts : TicketStatus.values()) {
			if (ts.getCode().equals(code)) {
				return ts;
			}
		}
		LOGGER.error("Ticket status " + code + " from ws response is not specified, returning null status!");
		return null;
	}
}
