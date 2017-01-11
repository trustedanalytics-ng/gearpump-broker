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
import org.springframework.web.client.RestTemplate;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.trustedanalytics.servicebroker.gearpump.service.dashboard.UaaConnector.CREATE_UAA_CLIENT_URL;
import static org.trustedanalytics.servicebroker.gearpump.service.dashboard.UaaConnector.CREATE_UAA_TOKEN_URL;
import static org.trustedanalytics.servicebroker.gearpump.service.dashboard.UaaConnector.DELETE_UAA_CLIENT_URL;

@RunWith(MockitoJUnitRunner.class)
public class UaaConnectorTest {

    @Mock
    private RestTemplate restTemplate;

    private UaaConnector uaaConnector;

    @Before
    public void before() throws IOException {
        uaaConnector = new UaaConnector(new CfCaller(restTemplate));
    }

    @Test
    public void test_createUaaClient() throws Exception {

        final String uaaClientName = "uaaClientName";
        final String password = "uaaPassword";
        final String uiAppUrl = "http://gearpump-ui-test.api.domain.com";
        final String uaaToken = "uaaToken";

        when(restTemplate.exchange(eq(CREATE_UAA_CLIENT_URL), Mockito.any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>("created!", HttpStatus.OK));

        String responseBody = uaaConnector.createUaaClient(uaaClientName, password, uiAppUrl, uaaToken);
        assertThat(responseBody, equalTo("created!"));
    }

    @Test
    public void test_createUaaToken() throws Exception {

        final String clientId = "uaaClientName";
        final String clientSecret = "uaaPassword";

        when(restTemplate.exchange(eq(CREATE_UAA_TOKEN_URL), Mockito.any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>(
                        "{" +
                                "  \"access_token\": \"test_access_token\"," +
                                "  \"token_type\": \"test_token_type\"" +
                                "}" , HttpStatus.OK));

        String createdToken = uaaConnector.createUaaToken(clientId, clientSecret);
        assertThat(createdToken, equalTo("test_token_type test_access_token"));
    }

    @Test
    public void test_deleteUaaClient() throws Exception {

        final String clientId = "uaaClientName";
        final String uaaToken = "uaaToken";

        when(restTemplate.exchange(eq(DELETE_UAA_CLIENT_URL), Mockito.any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), Mockito.<String>any(), Mockito.<String>any()))
                .thenReturn(new ResponseEntity<>("deleted!", HttpStatus.OK));
        String responseBody = uaaConnector.deleteUaaClient(clientId, uaaToken);
        assertThat(responseBody, equalTo("deleted!"));
    }

}
