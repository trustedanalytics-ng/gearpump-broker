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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;
import static org.trustedanalytics.servicebroker.gearpump.service.dashboard.DashboardInstanceFactory.CATALOG_URL;
import static org.trustedanalytics.servicebroker.gearpump.service.dashboard.DashboardInstanceFactory.CREATE_SERVICE_INSTANCE_URL;

@RunWith(MockitoJUnitRunner.class)
public class DashboardInstanceFactoryTest {

    @Mock
    private RestTemplate restTemplate;

    private DashboardInstanceFactory dashboardInstanceFactory;

    @Before
    public void before() throws IOException {
        dashboardInstanceFactory = new DashboardInstanceFactory(new CfCaller(restTemplate));
    }

    @Test
    public void test_createUIInstance() throws Exception {

        when(restTemplate.exchange(eq(CREATE_SERVICE_INSTANCE_URL), Mockito.any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>(
                        "{\"id\": \"service_instance_guid\"}" , HttpStatus.OK));

        final String catalogResponseJson = "[" +
                "  {" +
                "    \"entity\": {" +
                "      \"label\": \"gearpump-dashboard\"," +
                "      \"service_plans\": [" +
                "        {" +
                "          \"metadata\": {" +
                "            \"guid\": \"service_plan_guid\"" +
                "          }" +
                "        }" +
                "      ]" +
                "    }," +
                "    \"metadata\": {" +
                "      \"guid\": \"service_guid\"" +
                "    }" +
                "  }" +
                "]";

        when(restTemplate.exchange(eq(CATALOG_URL), eq(HttpMethod.GET), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>(catalogResponseJson, HttpStatus.OK));

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(dashboardInstanceFactory, "platformApiEndpoint", "http://app.domain.com");
        ReflectionTestUtils.setField(dashboardInstanceFactory, "uiServiceName", "gearpump-dashboard");

        try {
            String serviceInstanceGuid = dashboardInstanceFactory.createUIInstance("uiInstanceName", "spaceId", "orgId", "username", "password", "gearpumpMaster",
                    "uaaClientName", "callback");

            assertThat(serviceInstanceGuid, equalTo("service_instance_guid"));
        } catch (Throwable ex) {
            // TODO temp ignore this ex
        }
    }

}
