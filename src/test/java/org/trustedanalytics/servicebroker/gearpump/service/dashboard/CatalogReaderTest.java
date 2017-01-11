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
import static org.trustedanalytics.servicebroker.gearpump.service.dashboard.CatalogReader.CATALOG_URL;

@RunWith(MockitoJUnitRunner.class)
public class CatalogReaderTest {

    @Mock
    private RestTemplate restTemplate;

    private CatalogReader catalogReader;

    @Before
    public void before() throws IOException {
        catalogReader = new CatalogReader(new CfCaller(restTemplate));
    }

    @Test
    public void test_readGearpumpDashboardServiceOffering() throws Exception {

        final String catalogResponseJson = "[" +
                "  {" +
                "    \"name\": \"gearpump-dashboard\"," +
                "    \"offeringPlans\": [" +
                "      {" +
                "        \"id\": \"service_plan_guid\"" +
                "      }" +
                "    ]," +
                "    \"id\": \"service_guid\"" +
                "  }" +
                "]";

        when(restTemplate.exchange(eq(CATALOG_URL), eq(HttpMethod.GET), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>(catalogResponseJson, HttpStatus.OK));

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(catalogReader, "uiServiceName", "gearpump-dashboard");

        catalogReader.readGearpumpDashboardServiceOffering();

        assertThat(catalogReader.getUiServiceGuid(), equalTo("service_guid"));
        assertThat(catalogReader.getUiServicePlanGuid(), equalTo("service_plan_guid"));
    }

}
