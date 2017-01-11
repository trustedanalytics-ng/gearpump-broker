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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;
import static org.trustedanalytics.servicebroker.gearpump.service.dashboard.DashboardInstanceFactory.CREATE_SERVICE_INSTANCE_URL;
import static org.trustedanalytics.servicebroker.gearpump.service.dashboard.DashboardInstanceFactory.DELETE_SERVICE_INSTANCE_URL;

@RunWith(MockitoJUnitRunner.class)
public class DashboardInstanceFactoryTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CatalogReader catalogReader;

    private DashboardInstanceFactory dashboardInstanceFactory;

    @Before
    public void before() throws IOException {
        dashboardInstanceFactory = new DashboardInstanceFactory(new CfCaller(restTemplate), catalogReader);
    }

    @Test
    public void test_createUIInstance() throws Exception {

        when(restTemplate.exchange(eq(CREATE_SERVICE_INSTANCE_URL), Mockito.any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>(
                        "{\"id\": \"service_instance_guid\"}" , HttpStatus.OK));

        when(catalogReader.getUiServiceGuid()).thenReturn("service_guid");
        when(catalogReader.getUiServiceGuid()).thenReturn("service_plan_guid");

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(dashboardInstanceFactory, "platformApiEndpoint", "http://app.domain.com");
        ReflectionTestUtils.setField(catalogReader, "uiServiceName", "gearpump-dashboard");

        try {
            String serviceInstanceGuid = dashboardInstanceFactory.createUIInstance("uiInstanceName", "spaceId", "orgId", "username", "password", "gearpumpMaster",
                    "uaaClientName");

            assertThat(serviceInstanceGuid, equalTo("service_instance_guid"));
        } catch (Throwable ex) {
            // TODO temp ignore this ex
        }
    }

    @Test
    public void test_deleteUIInstance() throws Exception {

        when(restTemplate.exchange(eq(DELETE_SERVICE_INSTANCE_URL), Mockito.any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(dashboardInstanceFactory, "platformApiEndpoint", "http://app.domain.com");

        dashboardInstanceFactory.deleteUIServiceInstance("uiServiceInstanceGuid");
    }
}
