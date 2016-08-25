/**
 * Copyright (c) 2015 Intel Corporation
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

package org.trustedanalytics.servicebroker.gearpump.service;

import com.google.common.base.Strings;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.trustedanalytics.servicebroker.gearpump.config.CatalogConfig;
import org.trustedanalytics.servicebroker.gearpump.kerberos.KerberosService;
import org.trustedanalytics.servicebroker.gearpump.model.GearPumpCredentials;
import org.trustedanalytics.servicebroker.gearpump.service.dashboard.CloudFoundryServiceException;
import org.trustedanalytics.servicebroker.gearpump.service.dashboard.DashboardDeployer;
import org.trustedanalytics.servicebroker.gearpump.service.dashboard.DashboardServiceException;
import org.trustedanalytics.servicebroker.gearpump.service.externals.GearPumpDriverExec;
import org.trustedanalytics.servicebroker.gearpump.service.externals.SpawnResult;
import org.trustedanalytics.servicebroker.gearpump.yarn.YarnAppManager;

import java.util.Map;

public class GearPumpSpawner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GearPumpSpawner.class);

    private static final String ADMIN_USERNAME = "admin";

    private final GearPumpDriverExec gearPumpDriver;
    private final DashboardDeployer cloudFoundryService;
    private final YarnAppManager yarnAppManager;
    private final CatalogConfig configuration;
    private final KerberosService kerberosService;

    @Autowired
    public GearPumpSpawner(GearPumpDriverExec gearPumpDriver,
                           DashboardDeployer cloudFoundryService,
                           YarnAppManager yarnAppManager,
                           CatalogConfig catalogConfig,
                           KerberosService kerberosService) {
        this.gearPumpDriver = gearPumpDriver;
        this.cloudFoundryService = cloudFoundryService;
        this.yarnAppManager = yarnAppManager;
        this.configuration = catalogConfig;
        this.kerberosService = kerberosService;
    }

    private GearPumpCredentials provisionOnYarn(String numberOfWorkers) throws Exception {
        SpawnResult spawnResult = gearPumpDriver.spawnGearPumpOnYarn(numberOfWorkers);
        LOGGER.debug("spawnResult: {}", spawnResult.toString());
        if (spawnResult.getStatus() == SpawnResult.STATUS_OK) {
            LOGGER.debug("SpawnResult.STATUS_OK");
            return spawnResult.getGearPumpCredentials();
        } else {
            LOGGER.warn("SpawnResult NOT OK!");
            cleanUp(spawnResult.getGearPumpCredentials());
            throw spawnResult.getException();
        }
    }

    private void provisionOnTap(GearPumpCredentials gearPumpCredentials, String spaceId, String orgId, String serviceInstanceId)
            throws DashboardServiceException, CloudFoundryServiceException {
        LOGGER.info("Provisioning on TAP");

        String uiServiceInstanceName = "gp-ui-" + serviceInstanceId;
        String uaaClientName = RandomStringUtils.randomAlphanumeric(10).toLowerCase();
        String password = RandomStringUtils.randomAlphanumeric(10).toLowerCase();

        Map<String, String> dashboardData
                = cloudFoundryService.deployUI(uiServiceInstanceName, ADMIN_USERNAME, password, gearPumpCredentials.getMasters(), spaceId, orgId, uaaClientName);

        updateCredentials(gearPumpCredentials, dashboardData);
    }

    private void updateCredentials(GearPumpCredentials gearPumpCredentials, Map<String, String> dashboardData) {
        gearPumpCredentials.setDashboardUrl(dashboardData.get("uiAppUrl"));
        gearPumpCredentials.setDashboardGuid(dashboardData.get("uiServiceInstanceGuid"));
        gearPumpCredentials.setUsername(dashboardData.get("username"));
        gearPumpCredentials.setPassword(dashboardData.get("password"));
        gearPumpCredentials.setUaaClientName(dashboardData.get("uaaClientName"));
    }

    private void cleanUp(GearPumpCredentials gearPumpCredentials) {
        LOGGER.info("cleanUp [" + gearPumpCredentials + "]");
        if ( gearPumpCredentials != null && !Strings.isNullOrEmpty(gearPumpCredentials.getYarnApplicationId())) {
            LOGGER.debug("Found yarnApplicationId {}", gearPumpCredentials.getYarnApplicationId());
            try {
                yarnAppManager.killApplication(gearPumpCredentials.getYarnApplicationId());
                LOGGER.debug("killApplication finished");
            } catch (YarnException e) {
                LOGGER.warn("YARN problem while cleaning up.", e);
            }
        } else {
            LOGGER.debug("No yarnApplicationId for cleanup.");
        }
    }

    public GearPumpCredentials provisionInstance(String serviceInstanceId, String spaceId, String orgId, String planId) throws Exception {
        LOGGER.info("Trying to provision gearPump for: " + serviceInstanceId);
        kerberosService.login();
        GearPumpCredentials credentials = null;
        try {
            credentials = provisionOnYarn(configuration.getNumberOfWorkers(planId));
            if (credentials != null) {
                provisionOnTap(credentials, spaceId, orgId, serviceInstanceId);
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred during provisioning", e);
            cleanUp(credentials);
            throw e;
        }

        return credentials;
    }

    public void deprovisionInstance(GearPumpCredentials gearPumpCredentials) throws YarnException, DashboardServiceException {
        LOGGER.info("deprovisionInstance {}", gearPumpCredentials);
        if (gearPumpCredentials != null) {
            if (gearPumpCredentials.getYarnApplicationId() != null) {
                yarnAppManager.killApplication(gearPumpCredentials.getYarnApplicationId());
                LOGGER.debug("GearPump instance on Yarn has been deleted: {}", gearPumpCredentials.getYarnApplicationId());
            } else {
                LOGGER.debug("Yarn Application ID is NULL!");
            }

            cloudFoundryService.undeployUI(gearPumpCredentials.getDashboardGuid(), gearPumpCredentials.getUaaClientName());
        }
    }
}
