package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm;

import static com.evolveum.midpoint.prism.polystring.PolyString.fromOrig;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.HANDLED_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.IN_PROGRESS;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.PARTIAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.SUCCESS;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.UNKNOWN;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinitionImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ItsmManualConnectorIT {

	private static String COMMON_NS = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";

	private static ItsmManualConnector connector;

	private static ItsmManualConnectorConfiguration configuration;

	@BeforeClass
	public static void setUpConnector() {
		connector = new ItsmManualConnector();
		configuration = new ItsmManualConnectorConfiguration();
		
		configuration.setWsUrl("http://localhost:8088/mockRemedy");
		configuration.setUsername("admin");
		configuration.setPassword("myadminpassword");
		configuration.setProfileName("IDMIncident");

		configuration.setSoapLogBasedir(".");
		configuration.setTestIncidentNumber("TT0000000676689");
		configuration.setCIName("SYSTEM-NAME");
		
		configuration.setDescriptionTemplate("IDM request: ${operation} account on ${cIName}");
		configuration.setDetailTemplate("Account $!{identifier} details :\n${accountChanges}");
		
		configuration.setSslTrustManager(ItsmManualConnectorConfiguration.NON_VALIDATING_TRUST_MANAGER);
		configuration.setSslDisableCnCheck("true");
		
		configuration.setPriority("1");
		
		connector.setConfiguration(configuration);
		
		connector.connect(new OperationResult("connect"));
	}
	
	@Before
	public void cleanConnector() {
	}
	
	@Test
	public void connectorTestTest() {
		OperationResult result = new OperationResult("test");
		
		connector.test(result);
		
		assertEquals(SUCCESS, result.getComputeStatus());
	}

	@Test
	public void createTicketTest() throws Exception {
		OperationResult result = new OperationResult("test create");
		
		PrismObject<ShadowType> testObject = new PrismObject<>(
				new QName( COMMON_NS, "AccountShadowType"),
				ShadowType.class); {
				ResourceAttributeContainer attrContainer = new ResourceAttributeContainer(ShadowType.F_ATTRIBUTES, null, null);
				testObject.add(attrContainer);
				
				addAttribute(attrContainer, "type", "goat");
				addAttribute(attrContainer, "name", "ěščřžýáíéůŮĚŠČŘŽÝÁÍÉ");
		}

		String ticketId = connector.createTicketAdd(
				testObject, 
				null, 
				result);
		
		assertEquals(IN_PROGRESS, result.getComputeStatus());
		assertTrue(StringUtils.isNotBlank(ticketId));
	}

	private void addAttribute(ResourceAttributeContainer attrContainer, String name, String value)
			throws SchemaException {
		
		ResourceAttribute<PolyString> attribute = new ResourceAttribute<>(new QName(name), null , null);
		attribute.add(new PrismPropertyValue<>(fromOrig(value)));
		attrContainer.add(attribute);
	}

	@Test
	public void modifyTicketTest() throws Exception {
		OperationResult result = new OperationResult("test modify");
		
		PrismContext prismContext = null;
		PropertyDelta<Object> propertyDelta = new PropertyDelta<>(
				new PrismPropertyDefinitionImpl<>(
						new QName("test:foo","sizeOfTrousers"),
						null, 
						prismContext), 
				prismContext);
		
		propertyDelta.addValueToAdd(new PrismPropertyValue<>("XXXL"));
		propertyDelta.addValueToDelete(new PrismPropertyValue<>("XL"));
		
		String ticketId = connector.createTicketModify(
				null, 
				asList(resourceAttribute(COMMON_NS, "uid", new PrismPropertyValue<>(fromOrig("chuck")))), 
				asList(new PropertyModificationOperation<>( propertyDelta)),
				result);
		
		assertEquals(IN_PROGRESS, result.getComputeStatus());
		System.out.println("Ticket id is " + ticketId);
		assertTrue(isNotBlank(ticketId));
	}
	
	@Test
	public void deleteTicketTest() throws Exception {
		OperationResult result = new OperationResult("test modify");
		
		String ticketId = connector.createTicketDelete(
				null, 
				asList(resourceAttribute(COMMON_NS, "uid", new PrismPropertyValue<>(fromOrig("chuck")))), 
				result);
		
		assertEquals(IN_PROGRESS, result.getComputeStatus());
		assertTrue(StringUtils.isNotBlank(ticketId));
	}
	
	@Test
	public void queryTicketTest() throws Exception {
		OperationResult result = new OperationResult("test modify");
		
		OperationResultStatus queryOperationStatus = connector.queryOperationStatus(configuration.getTestIncidentNumber(), result);
		
		assertTrue(asList(IN_PROGRESS, SUCCESS).contains(result.getComputeStatus()));
		assertNotNull(queryOperationStatus);
		assertFalse(asList(FATAL_ERROR, HANDLED_ERROR, PARTIAL_ERROR, UNKNOWN).contains(queryOperationStatus));
	}

	private <T> ResourceAttribute<T> resourceAttribute(String namespace, String attrName,
			PrismPropertyValue<T> value) throws SchemaException {
		ResourceAttribute<T> result = new ResourceAttribute<>(
						new QName(namespace, attrName), 
						new ResourceAttributeDefinitionImpl<T>(null, null, null), 
						null);
		result.setValue(value);
		return result;
	}
}
