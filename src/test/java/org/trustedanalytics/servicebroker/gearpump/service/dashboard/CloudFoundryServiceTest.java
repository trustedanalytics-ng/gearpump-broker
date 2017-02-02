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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryServiceTest {

    @Mock
    private ServiceInstanceManager dashboardFactory;

    @Mock
    private UaaConnector uaaConnector;

    private DashboardDeployer dashboardDeployer;

    @Before
    public void before() throws IOException {
        dashboardDeployer = new CloudFoundryService(dashboardFactory, uaaConnector);
    }

    @Test
    public void test_deployUI() throws Exception {

        when(dashboardFactory.createInstance(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("my_guid");
        when(dashboardFactory.ensureInstanceRunning(anyString())).thenReturn(true);

        ReflectionTestUtils.setField(dashboardDeployer, "platformApiEndpoint", "http://api.domain.com");

        Map<String, String> dashboardData = dashboardDeployer.deployUI("uiInstanceName", "username", "password", "gearpumpMaster",
                "spaceId", "orgId", "uaaClientName");

        verify(uaaConnector).createUaaToken(anyString(), anyString());
        verify(uaaConnector).createUaaClient(anyString(), anyString(), anyString(), anyString());

        assertThat(dashboardData.get("uiServiceInstanceGuid"), equalTo("my_guid"));
    }

    @Test
    public void test_undeployUI() throws Exception {

        final String uiServiceInstanceId = "uiServiceInstanceId";

        when(dashboardFactory.stopInstance(eq(uiServiceInstanceId))).thenReturn(true);
        when(dashboardFactory.ensureInstanceStopped(uiServiceInstanceId)).thenReturn(true);

        dashboardDeployer.undeployUI(uiServiceInstanceId, "clientId");

        verify(dashboardFactory).deleteInstance(uiServiceInstanceId);
        verify(uaaConnector).createUaaToken(anyString(), anyString());
        verify(uaaConnector).deleteUaaClient(anyString(), anyString());
    }
}
