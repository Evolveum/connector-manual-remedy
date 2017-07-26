package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io;

public class StaticTemplater implements Templater {

	@Override
	public String fillFrom(String templateId, TemplateContext templateContext) {
		
		switch(templateId) {
			case "description" : 
				
				return "IDM request: " 
					+ templateContext.getOperation() + " account on "
					+ templateContext.getcIName();
				
			case "detail" : 
			return "Account details are:\n" + templateContext.getAccountChanges();
				
			default : return "Missing template for " + templateId;
		}
	}
}
