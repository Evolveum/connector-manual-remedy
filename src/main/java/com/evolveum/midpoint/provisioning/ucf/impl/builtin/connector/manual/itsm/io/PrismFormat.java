package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PasswordChangeOperation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Converts various prism types used in connector to Strings usable in itsm ticket.
 * 
 * @author Arnost Starosta
 */
public class PrismFormat {
	
    private static ResourceBundle messages = ResourceBundle.getBundle("ItsmMessages");

	public static String propertyValue(PrismPropertyValue<?> propertyValue) {
		return propertyValue.getValue().toString();
	}
	
	public static String valueCollection(String msg, Collection<?> values) {
		if(isNotEmpty(values)) {
			
			return "  " + msg + " [" + 
			        joinedDistinctStringValues(values) +
					"]";
		} else {
			return "";
		}
	}

    private static String joinedDistinctStringValues(Collection<?> values) {
        if(values == null) {
            return "";
        }
        
        return stringValues(values)
                .distinct()
                .collect(joining(","));
    }

    private static Stream<String> stringValues(Collection<?> values) {
        return values.stream()
        .map(value -> value instanceof PrismPropertyValue<?> ? 
                propertyValue((PrismPropertyValue<?>)value) 
                : value.toString());
    }

	public static String deltaOperation(Operation operation) {
		if(operation == null) {
			return "null";
		} else {
			if(operation instanceof ExecuteProvisioningScriptOperation) {
				return msg("delta.executeScript") + " : " + operation.toString();
			} else if(operation instanceof PasswordChangeOperation) {
				return msg("delta.changePassword") + " : " + operation.toString();
			} else if(operation instanceof PropertyModificationOperation) {
				
				PropertyDelta<?> d = ((PropertyModificationOperation<?>)operation).getPropertyDelta(); 
				StringBuffer result = new StringBuffer();
				result.append(d.getElementName().getLocalPart());
				result.append(" : \n");
				
				if(hasNoValue(d)) {
					result.append("  " + msg("delta.emptyValue"));
				} else {
					append(result, msg("delta.addValues"), d.getValuesToAdd());
					append(result, msg("delta.deleteValues"), d.getValuesToDelete());
					append(result, msg("delta.replaceValues"), d.getValuesToReplace());
				}				
				
				return result.toString();
			} else {
				return operation.debugDump();
			}
		}
	}

	private static boolean hasNoValue(PropertyDelta<?> d) {
		return d == null || (
				isEmpty(d.getValuesToAdd())
				&& isEmpty(d.getValuesToDelete())
				&& isEmpty(d.getValuesToReplace()));
	}

	private static void append(StringBuffer buffer, String msg, Collection<?> values) {
		if(values != null && ! values.isEmpty()) {
			buffer.append(valueCollection(msg, values));
			buffer.append("\n");
		}
	}

	private static String msg(String key) {
		return messages.getString(key);
	}
	
	// create account attributes formatting
	public static List<String> attributesList(PrismObject<? extends ShadowType> object) {
        AttributesCollectingVisitor attributeCollector = new AttributesCollectingVisitor();

		PrismContainer<Containerable> attributesContainer = object.findContainer(ShadowType.F_ATTRIBUTES);
        attributesContainer.accept(attributeCollector);
		
        List<String> result = attributeCollector.getAttributes();

        PrismContainer<Containerable> activationContainer = object.findContainer(ShadowType.F_ACTIVATION);
		if(activationContainer != null) {
	        Item<PrismValue, ItemDefinition> adminStatus = activationContainer.findItem(ActivationType.F_ADMINISTRATIVE_STATUS);
	        if(adminStatus != null) {
	            result.add(attribute(
	                        msg("operation.create.statusAttribute"), 
	                        joinedDistinctStringValues(adminStatus.getValues())));
	        }
		}

        return result;
	}

	public static <V, T extends ResourceAttribute<V>> String resourceAttributeValue(T attr) {
		V realValue = attr.getRealValue();
		if(realValue instanceof PolyString) {
			return ((PolyString)realValue).getOrig();
		}
		return realValue.toString();
	}

	public static String compoundIdentifier(Collection<? extends ResourceAttribute<?>> identifiers) {
		return identifiers.stream()
				.map(PrismFormat::resourceAttributeValue)
				.collect(Collectors.joining(","));
	}
	
	public static String operations(Collection<Operation> changes) {
		return changes.stream()
				.map(PrismFormat::deltaOperation)
				.collect(Collectors.joining("\n"));
	}

    private static String attribute(String key, Object attributeValues) {
        return key + ":\t" + attributeValues;
    }

    private static final class AttributesCollectingVisitor implements Visitor {
        private final List<String> attributes = new ArrayList<String>();

        @Override
        public void visit(Visitable visitable) {
            if(visitable instanceof ResourceAttribute) {
                ResourceAttribute<?> attr = (ResourceAttribute<?>)visitable;
                
                Object attributeValues = attr.getValues().stream().map(value ->
                    value instanceof PrismPropertyValue ? 
                            PrismFormat.propertyValue((PrismPropertyValue<?>)value) :
                            value.toString()
                ).collect(Collectors.joining(","));
                
                attributes.add(attribute(attr.getElementName().getLocalPart(), attributeValues));
            }
        }

        public List<String> getAttributes() {
            return attributes;
        }
    }
}
