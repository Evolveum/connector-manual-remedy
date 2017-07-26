package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io;

public interface TemplateRepository {
	
	public static final String TEMPLATE_DETAIL_ID = "detail";
	public static final String TEMPLATE_DESCRIPTION_ID = "description";

	String getTemplate(String id);
}
