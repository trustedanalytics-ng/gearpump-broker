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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.JsonUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;

@Service
class DashboardInstanceFactory implements ServiceInstanceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardInstanceFactory.class);

    private static final String CREATE_SERVICE_BODY_TEMPLATE = "{" +
            "\"name\":\"%s\"," +
            "\"type\":\"SERVICE\"," +
            "\"offeringId\":\"%s\"," +
            "\"bindings\":[]," +
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
                                 String password, String gearpumpMaster, String uaaClientName) throws DashboardServiceException {
        LOGGER.info("Creating Dashboard service instance");

        String body = createCreateRequestBody(uiInstanceName, spaceId, orgId, username, password, gearpumpMaster, uaaClientName);
        ResponseEntity<String> response = cfCaller.execute(CREATE_SERVICE_INSTANCE_URL, HttpMethod.POST, body, platformApiEndpoint);

        String instanceId = getInstanceProperty(response.getBody(), METADATA_ID);
        LOGGER.info("UI service instanceId: {}", instanceId);

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

    @Override
    public void deleteInstance(String instanceId) throws DashboardServiceException {
        LOGGER.info("Deleting Dashboard service instance");
        try {
            cfCaller.execute(DELETE_SERVICE_INSTANCE_URL, HttpMethod.DELETE, "", platformApiEndpoint, instanceId);

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
            cfCaller.execute(STOP_SERVICE_INSTANCE_URL, HttpMethod.PUT, "", platformApiEndpoint, instanceId);
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
    public Optional<String> getInstance(String instanceId) throws DashboardServiceException {
        LOGGER.info("Getting dashboard service instance");
        try {
            ResponseEntity<String> response = cfCaller.execute(GET_SERVICE_INSTANCE_URL, HttpMethod.GET, "", platformApiEndpoint, instanceId);
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

    private static String getInstanceProperty(String instanceData, String propertyName) throws DashboardServiceException {
        try {
            return JsonUtils.getValueFromJson(instanceData, propertyName);
        } catch (IOException e) {
            LOGGER.error("No such instance property: {}", propertyName);
            throw new DashboardServiceException(String.format("No such instance property: %s", propertyName), e);
        }
    }

    @Override
    public Optional<Boolean> hasInstanceState(String instanceId, InstanceState expectedState) throws DashboardServiceException {
        LOGGER.info("Checking if the Gearpump dashboard is in state: {}", expectedState);

        Optional<String> instanceData = getInstance(instanceId);
        if (!instanceData.isPresent()) {
            return Optional.empty();
        }

        String instanceState = getInstanceProperty(instanceData.get(), METADATA_STATE);
        LOGGER.info("Current Gearpump dashboard state: {}", instanceState);
        boolean hasState = expectedState.name().equals(instanceState);
        return Optional.of(hasState);
    }

    @Override
    public boolean ensureInstanceStopped(String instanceId) throws DashboardServiceException {
        return stateValidator.validate(this::hasInstanceState, instanceId, InstanceState.STOPPED);
    }

    @Override
    public boolean ensureInstanceRunning(String instanceId) throws DashboardServiceException {
        return stateValidator.validate(this::hasInstanceState, instanceId, InstanceState.RUNNING);
    }
}
