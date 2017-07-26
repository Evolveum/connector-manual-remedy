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

import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wss4j.common.ext.WSPasswordCallback;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationProperty;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.io.TemplateRepository;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 *
 */

public class ItsmManualConnectorConfiguration implements CallbackHandler, TemplateRepository {

	private static final Trace LOGGER = TraceManager.getTrace(ItsmManualConnectorConfiguration.class);
	
	public static final String NON_VALIDATING_TRUST_MANAGER = "NonValidatingTM";

	private String wsUrl;
	private String username;
	private String password;
	private Long timeout = 60000L;
	private String soapLogBasedirString;
	private Path soapLogBasedirPath;
	private String testIncidentNumber;
	private String profileName;
	private String cIName;
	
	private String proxyUrl;
	private Integer proxyPort;
	private String proxyType;

	private String descriptionTemplate;
	private String detailTemplate;

	private String sslTrustManager;
	private String sslDisableCnCheck;

	private String priority;

	@ConfigurationProperty
	public String getWsUrl() {
		return wsUrl;
	}

	public void setWsUrl(String wsUrl) {
		this.wsUrl = wsUrl;
	}

	@ConfigurationProperty
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@ConfigurationProperty
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Long getTimeout() {
		return timeout;
	}

	public void setTimeout(Long timeout) {
		this.timeout = timeout;
	}
	
	@ConfigurationProperty
	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	@ConfigurationProperty
	public String getSoapLogBasedir() {
		return soapLogBasedirString;
	}
	
	public void setSoapLogBasedir(final String soapLogBasedir) {
		soapLogBasedirString = soapLogBasedir;

		if (soapLogBasedirString == null) {
			this.soapLogBasedirPath = null;
		} else {
			try {
				this.soapLogBasedirPath = Paths.get(soapLogBasedirString);
				soapLogBasedirString = this.soapLogBasedirPath.toString(); //normalized
			} catch (InvalidPathException ex) {
				LOGGER.error("The SOAP log basedir is not a valid path.", ex);
				this.soapLogBasedirPath = null;
			}
		}
	}

	public Path getSoapLogBasedirPath() {
		return soapLogBasedirPath;
	}

	@ConfigurationProperty
	public String getTestIncidentNumber() {
		return testIncidentNumber;
	}

	public void setTestIncidentNumber(String testIncidentNumber) {
		this.testIncidentNumber = testIncidentNumber;
	}

	@ConfigurationProperty
	public String getCIName() {
		return cIName;
	}

	public void setCIName(String cIName) {
		this.cIName = cIName;
	}

	@ConfigurationProperty
	public String getProxyUrl() {
		return proxyUrl;
	}

	public void setProxyUrl(String proxyUrl) {
		this.proxyUrl = proxyUrl;
	}

	@ConfigurationProperty
	public Integer getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

	@ConfigurationProperty
	// cxf ProxyServerType as String - HTTP or SOCKS
	public String getProxyType() {
		return proxyType;
	}
	
	@ConfigurationProperty
	public String getDescriptionTemplate() {
		return descriptionTemplate;
	}

	@ConfigurationProperty
	public String getDetailTemplate() {
		return detailTemplate;
	}

	public void setProxyType(String proxyType) {
		this.proxyType = proxyType;
	}
	
	@ConfigurationProperty
	public String getSslTrustManager() {
		return this.sslTrustManager;
	}

	public void setSslTrustManager(String sslTrustManager) {
		this.sslTrustManager = sslTrustManager;
	}
	
	@ConfigurationProperty
	public String getSslDisableCnCheck() {
		return sslDisableCnCheck;
	}

	public void setSslDisableCnCheck(String sslDisableCnCheck) {
		this.sslDisableCnCheck = sslDisableCnCheck;
	}
	
	// add as @ConfigurationProperty when needed
	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public void validate() throws ConfigurationException {
		assertNotEmpty(wsUrl, "wsUrl");
		assertNotEmpty(username, "username");
		assertNotEmpty(password, "password");

		validateSslTrustManager();
		
		validateLogDirectory();
	}

	private void validateSslTrustManager() throws ConfigurationException {
		if(StringUtils.isEmpty(sslTrustManager)) {
			return;
		} else {
			switch(sslTrustManager) {
				case NON_VALIDATING_TRUST_MANAGER:
					return;
				default:
					throw new ConfigurationException("sslTrustManager has invalid value '" + sslTrustManager
							+ "'. Valid values are <empty> and " + NON_VALIDATING_TRUST_MANAGER);
			}
		}
	}

	private void validateLogDirectory() throws ConfigurationException {
		if (soapLogBasedirString != null) {
			if (soapLogBasedirPath == null) {
				throw new ConfigurationException("The SOAP log basedir is not a valid path.");
			}

			if (!isDirectory(soapLogBasedirPath)) {
				if (exists(soapLogBasedirPath)) {
					throw new ConfigurationException("The path to the SOAP log basedir (" + soapLogBasedirString + ") points to an existing file.");
				}
				throw new ConfigurationException("The SOAP log basedir (" + soapLogBasedirString + ") doesn't exist.");
			}
		}
	}

	private void assertNotEmpty(String value, String name) throws ConfigurationException {
		if(value == null || value.isEmpty()) {
			throw new ConfigurationException(name + " must not be empty.");
		}
	}

	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		// taken from cxf doc
		WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

		// set the password for our message.
		pc.setPassword(getPassword());
	}

	@Override
	public String getTemplate(String id) {
		switch(id) {
		case TemplateRepository.TEMPLATE_DESCRIPTION_ID:
			return descriptionTemplate;
		case TemplateRepository.TEMPLATE_DETAIL_ID:
			return detailTemplate;
		default:
			throw  new IllegalStateException("No template with id " + id + " known in configuration.");
		}
	}

	public void setDescriptionTemplate(String descriptionTemplate) {
		this.descriptionTemplate = descriptionTemplate;
	}

	public void setDetailTemplate(String detailTemplate) {
		this.detailTemplate = detailTemplate;
	}
}
