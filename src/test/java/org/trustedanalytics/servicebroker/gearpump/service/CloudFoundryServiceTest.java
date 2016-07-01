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
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.trustedanalytics.servicebroker.gearpump.config.CfCallerConfiguration;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CloudFoundryServiceTest {

    private static final String CREATE_SERVICE_INSTANCE_URL = "{apiUrl}/v2/service_instances";
    private static final String CREATE_UAA_TOKEN_URL = "{uaaTokenUrl}";
    private static final String CREATE_UAA_CLIENT_URL = "{uaaUrl}/oauth/clients";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CfCallerConfiguration cfCallerConfiguration;

    private CloudFoundryService cloudFoundryService;

    @Before
    public void before() throws IOException {
        cloudFoundryService = new CloudFoundryService(cfCallerConfiguration, new CfCaller(restTemplate));
    }

    @Test
    public void test_deployUI() throws Exception {

        when(restTemplate.exchange(eq(CREATE_UAA_TOKEN_URL), Mockito.<HttpMethod>any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>(
                        "{" +
                        "  \"access_token\": \"test_access_token\"," +
                        "  \"token_type\": \"test_token_type\"" +
                        "}" , HttpStatus.OK));

        when(restTemplate.exchange(eq(CREATE_UAA_CLIENT_URL), Mockito.<HttpMethod>any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        when(restTemplate.exchange(eq(CREATE_SERVICE_INSTANCE_URL), Mockito.<HttpMethod>any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>(
                        "{" +
                        "  \"metadata\": {" +
                        "    \"guid\": \"my_guid\"" +
                        "  }" +
                        "}", HttpStatus.OK));

        // we need to inject some @Values via reflection

        ReflectionTestUtils.setField(cloudFoundryService, "cfApiEndpoint", "http://app.domain.com");
        ReflectionTestUtils.setField(cloudFoundryService, "loginApiEndpoint", "http://app.domain.com/oauth/authorize");

        Map<String, String> dashboardData = cloudFoundryService.deployUI("uiInstanceName", "username", "password", "gearpumpMaster",
                "spaceId", "orgId", "uaaClientName");

        assertThat(dashboardData.get("uiServiceInstanceGuid"), equalTo("my_guid"));
    }
}
