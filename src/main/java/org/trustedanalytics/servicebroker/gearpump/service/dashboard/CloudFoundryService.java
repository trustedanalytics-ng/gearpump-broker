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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudFoundryService implements DashboardDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFoundryService.class);

    private final UaaConnector uaaConnector;
    private final DashboardInstanceFactory dashboardFactory;

    @Value("${api.endpoint}")
    private String cfApiEndpoint;

    @Value("${uaa.admin_client.id}")
    private String ssoAdminClientId;

    @Value("${uaa.admin_client.secret}")
    private String ssoAdminClientSecret;

    @Autowired
    public CloudFoundryService(DashboardInstanceFactory dashboardFactory, UaaConnector uaaConnector) {
        this.dashboardFactory = dashboardFactory;
        this.uaaConnector = uaaConnector;
    }

    @Override
    public Map<String, String> deployUI(String uiInstanceName, String username, String password, String gearpumpMaster,
                                        String spaceId, String orgId, String uaaClientName)
            throws DashboardServiceException, CloudFoundryServiceException {

        LOGGER.info("Deploying GearPump dashboard: uiInstanceName={}", uiInstanceName);

        String uiServiceInstanceGuid;
        String uiAppUrl = uiInstanceName + "." + getTapEndpointDomain();

        String uaaToken = uaaConnector.createUaaToken(ssoAdminClientId, ssoAdminClientSecret);
        uaaConnector.createUaaClient(uaaClientName, password, uiAppUrl, uaaToken);

        try {
            uiServiceInstanceGuid = dashboardFactory.createUIInstance(uiInstanceName, spaceId, orgId, username, password, gearpumpMaster, uaaClientName);
        } catch (IOException e) {
            throw new CloudFoundryServiceException("Cannot create UI instance.", e);
        }

        Map<String, String> dashboardData = new HashMap<>();
        dashboardData.put("uiServiceInstanceGuid", uiServiceInstanceGuid);
        dashboardData.put("uiAppUrl", uiAppUrl);
        dashboardData.put("username", username);
        dashboardData.put("password", password);
        dashboardData.put("uaaClientName", uaaClientName);

        return dashboardData;
    }

    private String getTapEndpointDomain() throws DashboardServiceException {
        String domain;
        try {
            URL url = new URL(cfApiEndpoint);
            domain = url.getHost();
            LOGGER.debug("Extracted domain: {}", domain);
            domain = domain.replaceFirst("([a-zA-Z0-9-]*\\.)", "");
            LOGGER.debug("Trimmed domain: {}", domain);
        } catch (MalformedURLException e) {
            domain = "";
        }
        if (domain.isEmpty()) {
            throw new DashboardServiceException("Cannot extract domain.");
        }
        return domain;
    }

    @Override
    public void undeployUI(String uiServiceInstanceId, String clientId) throws DashboardServiceException {
        LOGGER.info("Undeploying GearPump dashboard: uiServiceInstanceId={}", uiServiceInstanceId);

        dashboardFactory.deleteUIServiceInstance(uiServiceInstanceId);

        String uaaToken = uaaConnector.createUaaToken(ssoAdminClientId, ssoAdminClientSecret);
        uaaConnector.deleteUaaClient(clientId, uaaToken);
    }
}
