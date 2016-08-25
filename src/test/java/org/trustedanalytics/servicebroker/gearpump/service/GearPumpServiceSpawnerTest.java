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

package org.trustedanalytics.servicebroker.gearpump.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.servicebroker.gearpump.config.CatalogConfig;
import org.trustedanalytics.servicebroker.gearpump.kerberos.KerberosService;
import org.trustedanalytics.servicebroker.gearpump.model.GearPumpCredentials;
import org.trustedanalytics.servicebroker.gearpump.service.dashboard.CloudFoundryServiceException;
import org.trustedanalytics.servicebroker.gearpump.service.dashboard.DashboardDeployer;
import org.trustedanalytics.servicebroker.gearpump.service.externals.ExternalProcessException;
import org.trustedanalytics.servicebroker.gearpump.service.externals.GearPumpDriverExec;
import org.trustedanalytics.servicebroker.gearpump.service.externals.SpawnResult;
import org.trustedanalytics.servicebroker.gearpump.yarn.YarnAppManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GearPumpServiceSpawnerTest {

    @Mock
    private GearPumpDriverExec gearPumpDriver;

    @Mock
    private DashboardDeployer dashboardDeployer;

    @Mock
    private YarnAppManager yarnAppManager;

    @Mock
    private CatalogConfig catalogConfig;

    @Mock
    private KerberosService kerberosService;

    private GearPumpSpawner gearPumpSpawner;

    @Before
    public void before() throws IOException {
        gearPumpSpawner = new GearPumpSpawner(gearPumpDriver, dashboardDeployer, yarnAppManager, catalogConfig, kerberosService);
    }

    @Test
    public void testProvisionInstance_success() throws Exception {

        final String planId = "1 worker";
        final String numberOfWorkers = "1";
        final String serviceInstanceId = "serviceInstanceId";
        final String spaceId = "spaceId";
        final String orgId = "orgId";

        when(catalogConfig.getNumberOfWorkers(planId)).thenReturn(numberOfWorkers);

        GearPumpCredentials gearPumpCredentials = getGearPumpCredentials();
        SpawnResult spawnResult = new SpawnResult(SpawnResult.STATUS_OK, gearPumpCredentials, null);
        when(gearPumpDriver.spawnGearPumpOnYarn(numberOfWorkers)).thenReturn(spawnResult);

        Map<String, String> dashboardData = getDashboardData();
        when(dashboardDeployer.deployUI(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(dashboardData);

        GearPumpCredentials returnedCredentials = gearPumpSpawner.provisionInstance(serviceInstanceId, spaceId, orgId, planId);

        verify(gearPumpDriver).spawnGearPumpOnYarn(numberOfWorkers);
        verify(dashboardDeployer).deployUI(
                eq("gp-ui-" + serviceInstanceId),
                eq("admin"),
                anyString(),
                eq(gearPumpCredentials.getMasters()),
                eq(spaceId), eq(orgId),
                anyString());

        assertThat(returnedCredentials, equalTo(gearPumpCredentials));

        // check if credentials are updated with values from dashboardData
        assertThat(returnedCredentials.getDashboardUrl(), equalTo(dashboardData.get("uiAppUrl")));
        assertThat(returnedCredentials.getDashboardGuid(), equalTo(dashboardData.get("uiServiceInstanceGuid")));
        assertThat(returnedCredentials.getUsername(), equalTo(dashboardData.get("username")));
        assertThat(returnedCredentials.getPassword(), equalTo(dashboardData.get("password")));
        assertThat(returnedCredentials.getUaaClientName(), equalTo(dashboardData.get("uaaClientName")));
    }

    @Test
    public void testProvisionInstance_failure_spawnException() throws Exception {

        final String planId = "1 worker";
        final String numberOfWorkers = "1";

        when(catalogConfig.getNumberOfWorkers(planId)).thenReturn(numberOfWorkers);

        GearPumpCredentials gearPumpCredentials = getGearPumpCredentials();

        ExternalProcessException exceptionInstance = new ExternalProcessException("onSpawnException");
        SpawnResult spawnResult = new SpawnResult(SpawnResult.STATUS_ERR, gearPumpCredentials, exceptionInstance);
        when(gearPumpDriver.spawnGearPumpOnYarn(numberOfWorkers)).thenReturn(spawnResult);

        try {
            gearPumpSpawner.provisionInstance("serviceInstanceId", "spaceId", "orgId", planId);
        } catch (Exception ex) {
            assertThat(ex.getClass(), equalTo(ExternalProcessException.class));
            verify(yarnAppManager).killApplication(gearPumpCredentials.getYarnApplicationId());
        }

    }

    @Test
    public void testProvisionInstance_failure_cfException() throws Exception {

        final String planId = "2 workers";
        final String numberOfWorkers = "2";

        when(catalogConfig.getNumberOfWorkers(planId)).thenReturn(numberOfWorkers);

        GearPumpCredentials gearPumpCredentials = getGearPumpCredentials();

        SpawnResult spawnResult = new SpawnResult(SpawnResult.STATUS_OK, gearPumpCredentials, null);
        when(gearPumpDriver.spawnGearPumpOnYarn(numberOfWorkers)).thenReturn(spawnResult);

        when(dashboardDeployer.deployUI(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new CloudFoundryServiceException(""));

        try {
            gearPumpSpawner.provisionInstance("serviceInstanceId", "spaceId", "orgId", planId);
        } catch (Exception ex) {
            assertThat(ex.getClass(), equalTo(CloudFoundryServiceException.class));
            verify(yarnAppManager).killApplication(gearPumpCredentials.getYarnApplicationId());
        }
    }

    @Test
    public void testDeprovisionInstance_success() throws Exception {
        GearPumpCredentials gearPumpCredentials = getGearPumpCredentials();

        gearPumpSpawner.deprovisionInstance(gearPumpCredentials);

        verify(yarnAppManager).killApplication(gearPumpCredentials.getYarnApplicationId());
        verify(dashboardDeployer).undeployUI(anyString(), anyString());
    }

    @Test
    public void testDeprovisionInstance_success_nullCredentials() throws Exception {
        gearPumpSpawner.deprovisionInstance(null);
        verifyZeroInteractions(yarnAppManager, dashboardDeployer);
    }

    private Map<String, String> getDashboardData() {
        return new HashMap<String, String>() {{
                put("uiAppUrl", "uiAppUrl");
                put("uiServiceInstanceGuid", "uiServiceInstanceGuid");
                put("username", "username");
                put("password", "password");
                put("uaaClientName", "uaaClientName");
            }};
    }

    protected GearPumpCredentials getGearPumpCredentials() {
        return new GearPumpCredentials("masters", "yarnApplicationId", "dashboardUrl", "dashboardId", "username", "password", "uaaClientName");
    }

}
