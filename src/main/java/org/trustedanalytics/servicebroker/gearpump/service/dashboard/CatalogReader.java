package org.trustedanalytics.servicebroker.gearpump.service.dashboard;

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
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.JsonUtils;

import java.io.IOException;

@Service
class CatalogReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogReader.class);

    static final String CATALOG_URL = "{apiUrl}/api/v3/offerings";

    @Value("${gearpump.uiName:}")
    private String uiServiceName;

    private String uiServiceGuid;

    private String uiServicePlanGuid;

    @Value("${tap.api.endpoint}")
    private String platformApiEndpoint;

    private final CfCaller cfCaller;

    @Autowired
    public CatalogReader(CfCaller cfCaller) {
        this.cfCaller = cfCaller;
    }

    void readGearpumpDashboardServiceOffering() throws IOException {
        ResponseEntity<String> response = readCatalog();
        JsonNode root = JsonUtils.getRoot(response.getBody());
        for (JsonNode offeringNode : root) {
            if (uiServiceName.equals(offeringNode.get("name").asText())) {
                uiServiceGuid = offeringNode.get("id").asText();
                uiServicePlanGuid = offeringNode.at("/offeringPlans/0/id").asText();
                break;
            }
        }
        LOGGER.info("Retrieved service GUIDs: serviceGuid={}, servicePlanGuid={}", uiServiceGuid, uiServicePlanGuid);
    }

    private ResponseEntity<String> readCatalog() {
        LOGGER.info("Service catalog reading");
        ResponseEntity<String> response = cfCaller.execute(CATALOG_URL, HttpMethod.GET, "", platformApiEndpoint);
        LOGGER.debug("Response body {}", response.getBody());
        return response;
    }

    String getUiServiceGuid() {
        return uiServiceGuid;
    }

    String getUiServicePlanGuid() {
        return uiServicePlanGuid;
    }
}
