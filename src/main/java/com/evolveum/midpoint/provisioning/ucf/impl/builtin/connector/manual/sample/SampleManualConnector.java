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
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.sample;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnector;
import com.evolveum.midpoint.provisioning.ucf.api.ManagedConnectorConfiguration;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManualConnectorInstance;
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

/**
 * Manual connector for Remedy. This connector allows to create tickets
 * in Remedy for manual account management.
 * 
 * @author Arnost Starosta
 * @author Martin Lizner
 * @author Radovan Semancik
 *
 */
@ManagedConnector(type="SampleManualConnector")
public class SampleManualConnector extends AbstractManualConnectorInstance {
	
	private static final Trace LOGGER = TraceManager.getTrace(SampleManualConnector.class);
	
	private SampleManualConnectorConfiguration configuration;
	
	@ManagedConnectorConfiguration
	public SampleManualConnectorConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(SampleManualConnectorConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	protected String createTicketAdd(PrismObject<? extends ShadowType> object,
			Collection<Operation> additionalOperations, OperationResult result) throws CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String createTicketModify(ObjectClassComplexTypeDefinition objectClass,
			Collection<? extends ResourceAttribute<?>> identifiers, Collection<Operation> changes,
			OperationResult result) throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String createTicketDelete(ObjectClassComplexTypeDefinition objectClass,
			Collection<? extends ResourceAttribute<?>> identifiers, OperationResult result)
			throws ObjectNotFoundException, CommunicationException, GenericFrameworkException,
			SchemaException, ConfigurationException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public OperationResultStatus queryOperationStatus(String asyncronousOperationReference) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void connect(OperationResult result) {
		// TODO
	}

	@Override
	public void test(OperationResult parentResult) {
		OperationResult connectionResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_CONNECTION.getOperation());
		connectionResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, SampleManualConnector.class);
		connectionResult.addContext("connector", getConnectorObject());
		
		// TODO: connection test
		
		connectionResult.recordSuccess();
	}
	
	@Override
	public void dispose() {
		// TODO
	}

}
