/**
 * Copyright (c) 2016 Intel Corporation
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

import static org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller.CONTENT_TYPE_HEADER;

@Service
class DashboardInstanceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardInstanceFactory.class);

    private static final String METADATA_GUID = "/id";

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

    private final CfCaller cfCaller;
    private final CatalogReader catalogReader;

    @Value("${tap.api.endpoint}")
    private String platformApiEndpoint;

    @Autowired
    DashboardInstanceFactory(CfCaller cfCaller, CatalogReader catalogReader) {
        this.cfCaller = cfCaller;
        this.catalogReader = catalogReader;
    }

    @PostConstruct
    protected void init() throws IOException {
        catalogReader.readGearpumpDashboardServiceOffering();
    }

    String createUIInstance(String uiInstanceName, String spaceId, String orgId, String username,
                            String password, String gearpumpMaster, String uaaClientName) throws IOException {
        LOGGER.info("Creating Dashboard service instance");

        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_TYPE_HEADER, "application/json");

        String body = createCreateRequestBody(uiInstanceName, spaceId, orgId, username, password, gearpumpMaster, uaaClientName);

        ResponseEntity<String> response = cfCaller.executeWithHeaders(CREATE_SERVICE_INSTANCE_URL, HttpMethod.POST, body, headers, platformApiEndpoint);

        String uiServiceInstanceGuid = cfCaller.getValueFromJson(response.getBody(), METADATA_GUID);
        LOGGER.debug("UI Service Instance Guid '{}'", uiServiceInstanceGuid);

        return uiServiceInstanceGuid;
    }

    private String createCreateRequestBody(String uiInstanceName, String spaceId, String orgId, String username, String password,
                                           String gearpumpMaster, String uaaClientName)
    {
        LOGGER.debug("Creating request body. uiInstanceName: {}, spaceId: {}, orgId: {}, username: {}, gearpumpMaster: {}, uaaClientName: {}",
                uiInstanceName, spaceId, orgId, username, gearpumpMaster, password);
        String body = String.format(CREATE_SERVICE_BODY_TEMPLATE, uiInstanceName,
                catalogReader.getUiServiceGuid(),
                catalogReader.getUiServicePlanGuid(),
                username, password, gearpumpMaster, uaaClientName, password);
        LOGGER.debug("Create req body: {}", body);
        return body;
    }

    void deleteUIServiceInstance(String uiServiceInstanceGuid) throws DashboardServiceException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(CONTENT_TYPE_HEADER, "application/json");

            cfCaller.executeWithHeaders(DELETE_SERVICE_INSTANCE_URL, HttpMethod.DELETE, "", headers, platformApiEndpoint, uiServiceInstanceGuid);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                LOGGER.warn("GearPump UI instance with GUID {} doesn't exist. Skipping.", uiServiceInstanceGuid);
            } else {
                LOGGER.debug("Cannot delete GearPump UI instance with GUID {} - rethrowing excepiton.", uiServiceInstanceGuid);
                throw e;
            }
        }
    }

}
