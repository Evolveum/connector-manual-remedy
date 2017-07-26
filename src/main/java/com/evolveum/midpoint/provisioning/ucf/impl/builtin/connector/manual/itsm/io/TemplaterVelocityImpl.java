package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;

public class TemplaterVelocityImpl implements Templater {

	private TemplateRepository templateRepository; 
	
	public TemplaterVelocityImpl(TemplateRepository templateRepository) {
		this.templateRepository = templateRepository;
	}
	
	@Override
	public String fillFrom(String templateId, TemplateContext templateContext) {
		String template = templateRepository.getTemplate(templateId);
		
		Context ctx = new VelocityContext();
		
		// set nl as newline to avoid the ugly formatting in xml
		ctx.put("nl", "\n");
		
		fill(ctx, templateContext);
		StringWriter out = new StringWriter();
		Velocity.evaluate(ctx, out, templateId, new StringReader(template));
		return out.toString();
	}

	private void fill(Context ctx, TemplateContext templateContext) {
		ctx.put("accountChanges", templateContext.getAccountChanges());
		ctx.put("cIName", templateContext.getcIName());
		ctx.put("operation", templateContext.getOperation());
		ctx.put("identifier", templateContext.getIdentifier());
	}

}
