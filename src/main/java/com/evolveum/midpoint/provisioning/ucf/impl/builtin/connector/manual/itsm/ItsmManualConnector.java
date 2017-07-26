/**
 * Copyright (c) 2017 Evolveum, AMI Praha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm;

import static com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws.ConnectorAttributeUtils.getAttributeValue;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnector;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnectorConfiguration;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManualConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io.PrismFormat;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io.TemplateContext;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io.TemplateRepository;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io.Templater;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io.TemplaterVelocityImpl;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io.Ticket;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws.ConfigurableServiceFactory;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws.ConnectorAttributeUtils;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws.ItsmAttributes;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws.MessageType;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws.TicketStatus;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import eu.domainname.integrator.AttributeType;
import eu.domainname.integrator.AttributesType;
import eu.domainname.integrator.IntegrationEndpoint;
import eu.domainname.integrator.IntegrationOperationInput;
import eu.domainname.integrator.IntegrationOperationOutput;
import eu.domainname.integrator.StatusEnumType;

/**
 * Manual connector for Itsm ticketing system. This connector allows to create tickets
 * in itsm for manual account management.
 * 
 * @author Arnost Starosta
 * @author Martin Lizner
 * @author Radovan Semancik
 *
 */
@ManagedConnector(type="ItsmManualConnector")
public class ItsmManualConnector extends AbstractManualConnectorInstance {
	
	private static final Trace LOGGER = TraceManager.getTrace(ItsmManualConnector.class);
	
	private static ResourceBundle messages = ResourceBundle.getBundle("ItsmMessages");

	private ItsmManualConnectorConfiguration configuration;

	private Templater templater; // thread-safe templating impl

	private IntegrationEndpoint itsmService; // not thread-safe, synchronize access

	@ManagedConnectorConfiguration
	public ItsmManualConnectorConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(ItsmManualConnectorConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	protected String createTicketAdd(
			
			PrismObject<? extends ShadowType> object,
			Collection<Operation> additionalOperations, 
			OperationResult result) throws CommunicationException, GenericFrameworkException, 
									SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		
		TemplateContext templateContext = templateContextOperation(msg("operation.create"));

		List<String> attributes = PrismFormat.attributesList(object);
		templateContext.setAccountChanges(String.join("\n", attributes));
		
		return sendTicket(templateContext, result);
	}


	@Override
	protected String createTicketModify(
			
			ObjectClassComplexTypeDefinition objectClass,
			Collection<? extends ResourceAttribute<?>> identifiers, 
			Collection<Operation> changes,
			OperationResult result) throws ObjectNotFoundException, CommunicationException,
									GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, 
									ConfigurationException {

		TemplateContext templateContext = templateContextOperation(msg("operation.modify"));
		
		templateContext.setIdentifier(PrismFormat.compoundIdentifier(identifiers));
		templateContext.setAccountChanges(PrismFormat.operations(changes).toString());
		
		return sendTicket(templateContext, result);
	}

	@Override
	protected String createTicketDelete(
			
			ObjectClassComplexTypeDefinition objectClass,
			Collection<? extends ResourceAttribute<?>> identifiers, 
			OperationResult result) throws ObjectNotFoundException, CommunicationException, 
											GenericFrameworkException, SchemaException, ConfigurationException {
		
		TemplateContext templateContext = templateContextOperation(msg("operation.delete"));
		
		templateContext.setAccountChanges(PrismFormat.compoundIdentifier(identifiers));
		
		return sendTicket(templateContext, result);
	}

	public OperationResultStatus queryOperationStatus(
			String ticketId, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		// create request from ticket
		IntegrationOperationInput queryRequest = new IntegrationOperationInput();
		queryRequest.setMessageType(MessageType.GET_ENTRY.value());
		queryRequest.setProfileName(configuration.getProfileName());

		AttributesType attrs = new AttributesType();
		attrs.getAttribute().add(ConnectorAttributeUtils.wsAttribute(ItsmAttributes.INCIDENT_NUMBER, ticketId));
		queryRequest.setAttributes(attrs);
		
		TicketStatus ticketStatus = null;
		try {
			IntegrationOperationOutput response = ioCall(queryRequest);
			if(response.getStatus() == StatusEnumType.ERROR) {
				fillResultOnError(parentResult, response);
			} else {
				String wsTicketStatus = getAttributeValue(ItsmAttributes.INCIDENT_STATUS, response.getAttributes().getAttribute());
				ticketStatus = ConnectorAttributeUtils.fromWsCode(wsTicketStatus);
				fillResultOnSuccess(parentResult, response, ticketStatus);
			}
		} catch (CommunicationException e) {
			throw new IllegalStateException(e);
		}
		
		return mapTicketStatus(ticketStatus);
	}
	
	@Override
	protected synchronized void connect(OperationResult result) {

		try {
			configuration.validate();
		} catch (ConfigurationException e) {
			// just recording fatalError is ignored by callers and the exceptions is not reported
			throw new IllegalStateException("Configuration is not valid", e);
		}
	
		ConfigurableServiceFactory.closeQuietly(itsmService);

		LOGGER.debug("Creating Itsm web service");
		itsmService = ConfigurableServiceFactory.createService(IntegrationEndpoint.class, configuration);

		this.templater = new TemplaterVelocityImpl(configuration);
		result.recordSuccess();
	}
	
	@Override
	public synchronized void dispose() {
		ConfigurableServiceFactory.closeQuietly(itsmService);
		itsmService = null;
	}

	@Override
	public void test(OperationResult parentResult) {
		OperationResult connectionResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
		connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ItsmManualConnector.class);
		connectionResult.addContext("connector", getConnectorObject());
		
		String testIncidentNumber = "TT0000000000001";
		boolean testConnectionOnly = isBlank(configuration.getTestIncidentNumber());
		if( ! testConnectionOnly) {
			testIncidentNumber = configuration.getTestIncidentNumber();
		}
		
		try {
			LOGGER.info("Testing itsm connection");
			configuration.validate();
			
			IntegrationOperationInput request = new IntegrationOperationInput(); {
				request.setProfileName(configuration.getProfileName());
				request.setMessageType(MessageType.GET_ENTRY.value());
				AttributesType attrs = new AttributesType();
				attrs.getAttribute().add(ConnectorAttributeUtils.wsAttribute(ItsmAttributes.INCIDENT_NUMBER, testIncidentNumber));
				request.setAttributes(attrs);
			}
			
			IntegrationOperationOutput response = null;
			synchronized (itsmService) {
				response = itsmService.integrationOperation(request);
			}

			if(testConnectionOnly && response.getStatus() != null) {
				recordSuccess(response, connectionResult);
			} else if( ! testConnectionOnly && response.getStatus() == StatusEnumType.OK) {
				recordSuccess(response, connectionResult);
			} else {
				throw new CommunicationException("Calling web service failed, response message is : " 
							+ response != null ? response.getMessage() : " <response is null>");
			}
		} catch (Exception e) {
			throw new IllegalStateException("Test failed", e);
		}
	}

	private void recordSuccess(IntegrationOperationOutput response, OperationResult connectionResult) {
		connectionResult.recordSuccess();
		LOGGER.info("Itsm test ok - response status {}, message {}, attributes [{}]", 
				response.getStatus(), response.getMessage(), 
				response.getAttributes() != null ? 
						prettyPrint(response.getAttributes().getAttribute()) 
						: "null");
	}
	
	private String sendTicket(TemplateContext templateContext, OperationResult result) 
			throws CommunicationException {

		// ticket with default values
		Ticket ticket = new Ticket();
		MessageType messageType = MessageType.CREATE;
		
		ticket.add(ItsmAttributes.DESCRIPTION, 
				templater.fillFrom(TemplateRepository.TEMPLATE_DESCRIPTION_ID, templateContext));
		ticket.add(ItsmAttributes.DETAILED_DESCRIPTION, 
				templater.fillFrom(TemplateRepository.TEMPLATE_DETAIL_ID, templateContext));
		ticket.add(ItsmAttributes.CI_NAME, configuration.getCIName());
		ticket.add(ItsmAttributes.PRIORITY, configuration.getPriority());

		// create request from ticket
		IntegrationOperationInput addRequest = new IntegrationOperationInput();
		addRequest.setMessageType(messageType.value());
		addRequest.setProfileName(configuration.getProfileName());
		addRequest.setAttributes(ConnectorAttributeUtils.from(ticket));
		
		IntegrationOperationOutput response = ioCall(addRequest);
		if(response.getStatus() == StatusEnumType.ERROR) {
			fillResultOnError(result, response);
			throw new CommunicationException("Service responds with ERROR : " + response.getMessage());
		} else {
			// return IN_PROGRESS as
			// 1) the status is not returned 2) UNKNOWN signals an error 3) asking seems not worth it(?)
			fillResultOnSuccess(result, response, TicketStatus.IN_PROGRESS);
			return getAttributeValue(ItsmAttributes.INCIDENT_NUMBER, response.getAttributes().getAttribute());
		}
	}

	private IntegrationOperationOutput ioCall(IntegrationOperationInput request) throws CommunicationException {
		try {
			// Access to cxf service is synchronized as a poor man's 
			// thread safety - single connection pooling
			synchronized (itsmService) {
				return itsmService.integrationOperation(request);
			}
			
		} catch (WebServiceException ex) {
			if (ex instanceof SOAPFaultException) {
				ConfigurableServiceFactory.handleCreateOpSoapFault((SOAPFaultException) ex);
			}
			ConfigurableServiceFactory.handleWebServiceException(ex);
			throw ex;
		}
	}

	private TemplateContext templateContextOperation(String operationName) {
		TemplateContext templateContext = new TemplateContext();
		templateContext.setOperation(operationName);
		templateContext.setcIName(configuration.getCIName());
		return templateContext;
	}

	private void fillResultOnSuccess(OperationResult result, IntegrationOperationOutput response, TicketStatus ticketStatus) {
		OperationResult subresult = new OperationResult("Itsm integration operation result - " + response.getStatus());
		subresult.setStatus(mapResultStatus(response.getStatus(), ticketStatus));
		result.addSubresult(subresult);
	}
	
	private void fillResultOnError(OperationResult result, IntegrationOperationOutput response) {

		OperationResult subresult = new OperationResult("Itsm integration operation result - error");
		
		String resultMessage = "";
		if(response == null || response.getStatus() == null) {
			resultMessage = "Operation response or response status is null.";
		} else {
			switch(response.getStatus()) {
				case OK :
				case WARNING :
					throw new IllegalStateException("Response status is OK or WARNING, this is not an error state?");
				case ERROR:
					resultMessage = "Error calling service.";
					break;
				default:
					resultMessage = "Service responds with unknown status " + response.getStatus();
				}
		}
		resultMessage += " Response msg is : " + response.getMessage();
		subresult.setStatus(mapResultStatus(response.getStatus()));
		subresult.setMessage(resultMessage);
		result.addSubresult(subresult);
	}
	
	private OperationResultStatus mapResultStatus(StatusEnumType messageStatus) {
		return mapResultStatus(messageStatus, null);
	}
	
	private OperationResultStatus mapResultStatus(StatusEnumType messageStatus, TicketStatus ticketStatus) {
		if(messageStatus == null) {
			return OperationResultStatus.FATAL_ERROR;
		}
		
		switch(messageStatus) {
			case OK:
			case WARNING:
				return mapTicketStatus(ticketStatus);
			case ERROR:
			default:
				LOGGER.debug("Result status is " + messageStatus + ", returning FATAL_ERROR");
				return OperationResultStatus.FATAL_ERROR;
		}
	}

	private OperationResultStatus mapTicketStatus(TicketStatus ticketStatus) {

		if(ticketStatus == null) {
			LOGGER.info("Status is null, can't convert to OperationResultStatus, returning UNKNOWN");
			return OperationResultStatus.UNKNOWN;
		}

		switch(ticketStatus) {
			case NEW:
			case ASSIGNED:
			case IN_PROGRESS:
			case PENDING:
				return OperationResultStatus.IN_PROGRESS;
				
			case RESOLVED:
			case CLOSED:
				return OperationResultStatus.SUCCESS;
				
			case CANCELLED:
				return OperationResultStatus.FATAL_ERROR;
				
			default :
				LOGGER.error("Status " + ticketStatus + " is not specified, can't convert to OperationResultStatus, returning FATAL_ERROR");
				return OperationResultStatus.FATAL_ERROR;
		}
	}
	
	private String prettyPrint(List<AttributeType> attributes) {
		
		return String.join(", ", 
				attributes.stream()
					.map(attr -> String.join(":", attr.getName(), attr.getValue()))
					.collect(toList()));
	}

	private static String msg(String key) {
		return messages.getString(key);
	}
}
