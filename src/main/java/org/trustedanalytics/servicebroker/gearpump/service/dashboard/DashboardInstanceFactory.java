/**
 * Copyright (c) 2017 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trustedanalytics.servicebroker.gearpump.service.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;

import static org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller.CONTENT_TYPE_HEADER;

@Service
class DashboardInstanceFactory implements ServiceInstanceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardInstanceFactory.class);

    private static final String CREATE_SERVICE_BODY_TEMPLATE = "{\"name\":\"%s\",\"type\":\"service\",\"offeringId\":\"%s\",\"bindings\":[]," +
            "\"metadata\":[" +
                "{\"key\": \"PLAN_ID\", \"value\": \"%s\"}," +
                "{\"key\": \"USERNAME\", \"value\": \"%s\"}," +
                "{\"key\": \"PASSWORD\", \"value\": \"%s\"}," +
                "{\"key\": \"GEARPUMP_MASTER\", \"value\": \"%s\"}," +
                "{\"key\": \"UAA_CLIENT_ID\", \"value\": \"%s\"}," +
                "{\"key\": \"UAA_CLIENT_SECRET\", \"value\": \"%s\"}" +
            "]}";

    static final String CREATE_SERVICE_INSTANCE_URL = "{apiUrl}/api/v3/services";
    static final String DELETE_SERVICE_INSTANCE_URL = "{apiUrl}/api/v3/services/{instanceId}";
    static final String GET_SERVICE_INSTANCE_URL = "{apiUrl}/api/v3/services/{instanceId}";
    static final String STOP_SERVICE_INSTANCE_URL = "{apiUrl}/api/v3/services/{instanceId}/stop";

    static final String METADATA_ID = "/id";
    static final String METADATA_STATE = "/state";
    static final String INSTANCE_STATE_STOPPED = "STOPPED";

    private final CfCaller cfCaller;
    private final CatalogReader catalogReader;
    private final DashboardStateValidator stateValidator;

    @Value("${tap.api.endpoint}")
    private String platformApiEndpoint;

    @Autowired
    DashboardInstanceFactory(CfCaller cfCaller, CatalogReader catalogReader, DashboardStateValidator stateValidator) {
        this.cfCaller = cfCaller;
        this.catalogReader = catalogReader;
        this.stateValidator = stateValidator;
    }

    @PostConstruct
    protected void init() throws IOException {
        catalogReader.readGearpumpDashboardServiceOffering();
    }

    @Override
    public String createInstance(String uiInstanceName, String spaceId, String orgId, String username,
                                 String password, String gearpumpMaster, String uaaClientName) throws IOException {
        LOGGER.info("Creating Dashboard service instance");

        String body = createCreateRequestBody(uiInstanceName, spaceId, orgId, username, password, gearpumpMaster, uaaClientName);
        ResponseEntity<String> response = cfCaller.executeWithHeaders(CREATE_SERVICE_INSTANCE_URL, HttpMethod.POST, body, getHttpHeaders(), platformApiEndpoint);

        String instanceId = cfCaller.getValueFromJson(response.getBody(), METADATA_ID);
        LOGGER.debug("UI Service Instance Guid '{}'", instanceId);

        return instanceId;
    }

    private String createCreateRequestBody(String uiInstanceName, String spaceId, String orgId, String username,
                                           String password, String gearpumpMaster, String uaaClientName) {

        LOGGER.debug("Creating request body. uiInstanceName: {}, spaceId: {}, orgId: {}, username: {}, gearpumpMaster: {}, uaaClientName: {}",
                uiInstanceName, spaceId, orgId, username, gearpumpMaster, password);
        String body = String.format(CREATE_SERVICE_BODY_TEMPLATE, uiInstanceName,
                catalogReader.getUiServiceGuid(),
                catalogReader.getUiServicePlanGuid(),
                username, password, gearpumpMaster, uaaClientName, password);
        LOGGER.debug("Create req body: {}", body);
        return body;
    }

    private static HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_TYPE_HEADER, "application/json");
        return headers;
    }

    @Override
    public void deleteInstance(String instanceId) throws DashboardServiceException {
        LOGGER.info("Deleting Dashboard service instance");
        try {
            cfCaller.executeWithHeaders(DELETE_SERVICE_INSTANCE_URL, HttpMethod.DELETE, "", getHttpHeaders(), platformApiEndpoint, instanceId);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                LOGGER.warn("Cannot delete Gearpump UI instance. Gearpump UI instance with GUID {} doesn't exist. Skipping.", instanceId);
            } else {
                LOGGER.debug("Cannot delete Gearpump UI instance with GUID {} - rethrowing exception.", instanceId);
                throw e;
            }
        }
    }

    @Override
    public boolean stopInstance(String instanceId) throws DashboardServiceException {
        LOGGER.info("Stopping Dashboard service instance");
        try {
            cfCaller.executeWithHeaders(STOP_SERVICE_INSTANCE_URL, HttpMethod.PUT, "", getHttpHeaders(), platformApiEndpoint, instanceId);
            return true;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                LOGGER.warn("Gearpump UI instance with GUID {} doesn't exist. Skipping.", instanceId);
                return false;
            } else {
                LOGGER.debug("Cannot stop Gearpump UI instance with GUID {} - rethrowing exception.", instanceId);
                throw e;
            }
        }
    }

    @Override
    public String getInstanceState(String instanceData) throws DashboardServiceException {
        LOGGER.info("Getting Dashboard service instance state");
        String instanceState = getInstanceProperty(instanceData, METADATA_STATE);
        if (instanceState == null) {
            throw new DashboardServiceException("Instance state not found.");
        }
        LOGGER.info("Dashboard service instance state = {}", instanceState);
        return instanceState;
    }

    @Override
    public String getInstanceProperty(String instanceData, String propertyName) throws DashboardServiceException {
        try {
            return cfCaller.getValueFromJson(instanceData, propertyName);
        } catch (IOException e) {
            LOGGER.error("No such instance property: {}", propertyName);
            throw new DashboardServiceException(String.format("No such instance property: %s", propertyName), e);
        }
    }

    @Override
    public Optional<String> getInstance(String instanceId) throws DashboardServiceException {
        LOGGER.info("Getting dashboard service instance");
        try {
            ResponseEntity<String> response = cfCaller.executeWithHeaders(GET_SERVICE_INSTANCE_URL, HttpMethod.GET, "", getHttpHeaders(), platformApiEndpoint, instanceId);
            LOGGER.debug("Response: {}", response);
            return Optional.of(response.getBody());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                LOGGER.warn("Cannot load Gearpump UI instance data. Gearpump UI instance with GUID {} doesn't exist. Skipping.", instanceId);
                return Optional.empty();
            } else {
                LOGGER.debug("Cannot get Gearpump UI instance with GUID {} - rethrowing exception.", instanceId);
                throw e;
            }
        }
    }

    @Override
    public Optional<Boolean> isInstanceStopped(String instanceId) throws DashboardServiceException {
        LOGGER.info("Checking if the Gearpump dashboard has been stopped");

        Optional<String> instanceData = getInstance(instanceId);
        if (!instanceData.isPresent()) {
            return Optional.empty();
        }

        String instanceState = getInstanceState(instanceData.get());
        boolean stopped = INSTANCE_STATE_STOPPED.equals(instanceState);

        return Optional.of(stopped);
    }

    @Override
    public boolean ensureInstanceIsStopped(String instanceId) throws DashboardServiceException {
        LOGGER.info("Ensuring if the Gearpump dashboard has been stopped");
        return stateValidator.validate(() -> isInstanceStopped(instanceId));
    }

}
