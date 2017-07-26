package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPException;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.http.HttpStatus;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;

import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ItsmManualConnectorConfiguration;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ConfigurableServiceFactory {
	
	private static final Trace LOGGER = TraceManager.getTrace(ConfigurableServiceFactory.class);

	private static final String LOG_REQUEST_FILENAME_DEFAULT = "itsm-io.log.xml";
	private static final String LOG_RESPONSE_FILENAME_DEFAULT = "itsm-io.log.xml";

	public static <S> S createService(final Class<S> seiClass, ItsmManualConnectorConfiguration configuration) {
		final ClientProxyFactoryBean factory = new JaxWsProxyFactoryBean();   // a new instance must be used for each service
		final Path soapLogTargetPath = configuration.getSoapLogBasedirPath();
		if (soapLogTargetPath != null) {
			try {
				final Path targetForRequests  = soapLogTargetPath.resolve(LOG_REQUEST_FILENAME_DEFAULT);
				final Path targetForResponses = soapLogTargetPath.resolve(LOG_RESPONSE_FILENAME_DEFAULT);
				final URL targetPathURLForRequests  = targetForRequests .toUri().toURL();
				final URL targetPathURLForResponses = targetForResponses.toUri().toURL();
				factory.getFeatures().add(new LoggingFeature(targetPathURLForResponses.toString(),
															 targetPathURLForRequests.toString(),
															 100_000,
															 true)); //pretty
			} catch (MalformedURLException ex) {
				LOGGER.warn("Couldn't initialize logging of SOAP messages.", ex);
			}
		}

		factory.setAddress (configuration.getWsUrl());
		factory.setUsername(configuration.getUsername());
		factory.setPassword(configuration.getPassword());
		factory.setServiceClass(seiClass);		
		
		final S result = (S) factory.create();
		
		setWsseUsernameToken(result, configuration);
		if(isNotBlank(configuration.getProxyUrl())) {
			setProxy(result, configuration);
		}

		if(tamperSsl(configuration)) {
			// HTTPS settings - you can override jre trust settings here
			final Client client = ClientProxy.getClient(result);
			final HTTPConduit http = (HTTPConduit) client.getConduit();
			final TLSClientParameters tlsParameters = ofNullable(http.getTlsClientParameters()).orElse(new TLSClientParameters());
			{
				tlsParameters.setKeyManagers(new KeyManager[0]);
				tlsParameters.setTrustManagers(new TrustManager[] { new NonValidatingTM() });
				tlsParameters.setDisableCNCheck(Boolean.valueOf(configuration.getSslDisableCnCheck()));
			}
			http.setTlsClientParameters(tlsParameters);
		}
		
		return result;
	}
	
	public static void closeQuietly(final Object service) {
		LOGGER.debug("Closing Itsm web service");
		try {
			if (service != null) {
				final Client client = ClientProxy.getClient(service);
				if (client != null) {
					client.destroy();
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error closing ws service, ignoring as non fatal.", e);
		}
	}

	private static boolean tamperSsl(ItsmManualConnectorConfiguration configuration) {
		return configuration.getSslTrustManager() != null || configuration.getSslDisableCnCheck() != null;
	}

	private static class NonValidatingTM implements X509TrustManager {

		    public X509Certificate[] getAcceptedIssuers() {
		      return new X509Certificate[0];
		    }

		    public void checkClientTrusted(X509Certificate[] certs, String authType) {
		    }

		    public void checkServerTrusted(X509Certificate[] certs, String authType) {
		    }
		  }

	private static void setProxy(Object cxfService, ItsmManualConnectorConfiguration configuration) {
		final Client client = ClientProxy.getClient(cxfService);
		HTTPConduit httpConduit = (HTTPConduit)client.getConduit();
		HTTPClientPolicy httpClient = httpConduit.getClient();

		httpClient.setProxyServer(configuration.getProxyUrl());
		httpClient.setProxyServerPort(configuration.getProxyPort());
		httpClient.setProxyServerType(ProxyServerType.fromValue(configuration.getProxyType()));
	}
	
	private static void setWsseUsernameToken(Object cxfService, ItsmManualConnectorConfiguration configuration) {
		final Client client = ClientProxy.getClient(cxfService);
		HashMap<String, Object> wsSec = new HashMap<String, Object>(); {

			wsSec.put(WSHandlerConstants.ALLOW_NAMESPACE_QUALIFIED_PASSWORD_TYPES, "true");
			wsSec.put(WSHandlerConstants.HANDLE_CUSTOM_PASSWORD_TYPES, "true");

			wsSec.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
			wsSec.put(WSHandlerConstants.USER, configuration.getUsername());
			wsSec.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
			wsSec.put(WSHandlerConstants.PW_CALLBACK_REF, configuration);
		}
		client.getOutInterceptors().add(new WSS4JOutInterceptor(wsSec));
	}
	
	public static void handleCreateOpSoapFault(final SOAPFaultException ex) {
		// TODO HANDLE HOW? there is no error state description from itsm
		throw ex;
	}
	
	public static void handleWebServiceException(final WebServiceException exception) throws CommunicationException {
		final Throwable cause = exception.getCause();
		if (cause instanceof HTTPException) {
			final HTTPException httpException = (HTTPException) cause;
			final int responseCode = httpException.getResponseCode();
			switch (responseCode) {
				case HttpStatus.SC_UNAUTHORIZED:
					throw new CommunicationException("Unauthorized", exception);
				case HttpStatus.SC_REQUEST_TIMEOUT:
					throw new CommunicationException("Timeout", exception);
			}
		} else if (cause instanceof SocketTimeoutException) {
			throw new CommunicationException("Timeout", exception);
		}
	}
}
