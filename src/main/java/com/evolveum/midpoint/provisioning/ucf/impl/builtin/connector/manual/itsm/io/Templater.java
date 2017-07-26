package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io;

public interface Templater {

	String fillFrom(String templateId, TemplateContext templateContext);

}
