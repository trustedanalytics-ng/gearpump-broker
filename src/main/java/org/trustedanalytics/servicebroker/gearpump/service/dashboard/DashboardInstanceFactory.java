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

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;

import java.io.IOException;

import static org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller.CONTENT_TYPE_HEADER;

@Service
class DashboardInstanceFactory {
    private final Logger LOGGER = LoggerFactory.getLogger(DashboardInstanceFactory.class);

    private static final String METADATA_GUID = "/id";

    private static final String CREATE_SERVICE_BODY_TEMPLATE = "{\"name\":\"%s\",\"type\":\"service\",\"bindings\":[]," +
            "\"metadata\":[" +
                "{\"key\": \"PLAN_ID\", \"value\": \"%s\"}," +
                "{\"key\": \"USERNAME\", \"value\": \"%s\"}," +
                "{\"key\": \"PASSWORD\", \"value\": \"%s\"}," +
                "{\"key\": \"GEARPUMP_MASTER\", \"value\": \"%s\"}," +
                "{\"key\": \"UAA_CLIENT_ID\", \"value\": \"%s\"}," +
                "{\"key\": \"UAA_CLIENT_SECRET\", \"value\": \"%s\"}" +
            "]}";

    static final String CREATE_SERVICE_INSTANCE_URL = "{apiUrl}/api/v1/services/{serviceId}";
    static final String DELETE_SERVICE_INSTANCE_URL = "{apiUrl}/api/v1/services/{instanceId}";
    static final String CATALOG_URL = "{apiUrl}/api/v1/catalog";

    private final CfCaller cfCaller;

    @Value("${gearpump.uiName:}")
    private String uiServiceName;

    @Value("${gearpump.uiServiceId:}")
    private String uiServiceGuid;

    @Value("${gearpump.uiServicePlanId:}")
    private String uiServicePlanGuid;

    @Value("${tap.api.endpoint}")
    private String platformApiEndpoint;

    @Autowired
    DashboardInstanceFactory(CfCaller cfCaller) {
        this.cfCaller = cfCaller;
    }

    public String createUIInstance(String uiInstanceName, String spaceId, String orgId, String username,
                            String password, String gearpumpMaster, String uaaClientName, String callback) throws IOException, DashboardServiceException {
        LOGGER.info("Creating Dashboard service instance");

        if (StringUtils.isEmpty(uiServicePlanGuid)) {
            processServiceCatalog();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_TYPE_HEADER, "application/json");

        String body = createCreateRequestBody(uiInstanceName, spaceId, orgId, username, password, gearpumpMaster, uaaClientName, callback);

        ResponseEntity<String> response = cfCaller.executeWithHeaders(CREATE_SERVICE_INSTANCE_URL, HttpMethod.POST, body, headers, platformApiEndpoint, uiServiceGuid);

        String uiServiceInstanceGuid = cfCaller.getValueFromJson(response.getBody(), METADATA_GUID);
        LOGGER.debug("UI Service Instance Guid '{}'", uiServiceInstanceGuid);

        return uiServiceInstanceGuid;
    }

    private void processServiceCatalog() throws IOException {
        LOGGER.info("Service catalog processing");

        HttpHeaders headers = new HttpHeaders();
        headers.add(CONTENT_TYPE_HEADER, "application/json");

        ResponseEntity<String> response = cfCaller.executeWithHeaders(CATALOG_URL, HttpMethod.GET, "", headers, platformApiEndpoint);

        JsonNode root = cfCaller.getRoot(response.getBody());
        for (JsonNode entityNode : root) {
            JsonNode entity = entityNode.get("entity");
            if (uiServiceName.equals(entity.get("label").asText())) {
                this.uiServiceGuid = entityNode.at("/metadata/guid").asText();
                this.uiServicePlanGuid = entityNode.at("/entity/service_plans/0/metadata/guid").asText();
                break;
            }
        }

        LOGGER.info("Retrieved service GUIDs: serviceGuid={}, servicePlanGuid={}", uiServiceGuid, uiServicePlanGuid);
    }

    private String createCreateRequestBody(String uiInstanceName, String spaceId, String orgId, String username, String password, String gearpumpMaster,
                                           String uaaClientName, String callback) {
        String body = String.format(CREATE_SERVICE_BODY_TEMPLATE, uiInstanceName, uiServicePlanGuid, username, password, gearpumpMaster, uaaClientName, password);
        LOGGER.debug("Create req body: {}", body);
        return body;
    }

    public void deleteUIServiceInstance(String uiServiceInstanceGuid) throws DashboardServiceException {
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
