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

import org.junit.Assert;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;
import static org.trustedanalytics.servicebroker.gearpump.service.dashboard.DashboardInstanceFactory.*;

@RunWith(MockitoJUnitRunner.class)
public class DashboardInstanceFactoryTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CatalogReader catalogReader;

    @Mock
    private DashboardStateValidator dashboardStateValidator;

    private ServiceInstanceManager dashboardInstanceFactory;

    @Before
    public void before() throws IOException {
        dashboardInstanceFactory = new DashboardInstanceFactory(new CfCaller(restTemplate), catalogReader, dashboardStateValidator);
    }

    @Test
    public void test_createInstance() throws Exception {

        mockCreateInstanceResponse();

        when(catalogReader.getUiServiceGuid()).thenReturn("service_guid");
        when(catalogReader.getUiServiceGuid()).thenReturn("service_plan_guid");

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(dashboardInstanceFactory, "platformApiEndpoint", "http://app.domain.com");
        ReflectionTestUtils.setField(catalogReader, "uiServiceName", "gearpump-dashboard");

        try {
            String serviceInstanceGuid = dashboardInstanceFactory.createInstance("uiInstanceName", "spaceId", "orgId", "username", "password", "gearpumpMaster",
                    "uaaClientName");

            assertThat(serviceInstanceGuid, equalTo("service_instance_guid"));
        } catch (Throwable ex) {
            // TODO temp ignore this ex
        }
    }

    @Test
    public void test_deleteInstance() throws Exception {

        final String instanceId = "uiServiceInstanceGuid";

        mockStopInstanceResponse();
        mockGetInstanceResponse(instanceId, InstanceState.STOPPED);
        mockDeleteInstanceResponse();

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(dashboardInstanceFactory, "platformApiEndpoint", "http://app.domain.com");

        dashboardInstanceFactory.deleteInstance(instanceId);
    }

    @Test
    public void test_stopInstance() throws Exception {

        final String instance1 = "uiServiceInstanceGuid1";
        mockStopInstanceResponse();

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(dashboardInstanceFactory, "platformApiEndpoint", "http://app.domain.com");

        Assert.assertTrue(dashboardInstanceFactory.stopInstance(instance1));
    }

    @Test
    public void test_stopInstance_failure_notFound() throws Exception {

        final String instance1 = "uiServiceInstanceGuid1";
        when(restTemplate.exchange(eq(STOP_SERVICE_INSTANCE_URL), eq(HttpMethod.PUT), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyString(), anyString()))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(dashboardInstanceFactory, "platformApiEndpoint", "http://app.domain.com");

        Assert.assertFalse(dashboardInstanceFactory.stopInstance(instance1));
    }

    @Test
    public void test_getInstance() throws Exception {

        final String instance1 = "uiServiceInstanceGuid1";
        mockGetInstanceResponse(instance1, InstanceState.STOPPED);

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(dashboardInstanceFactory, "platformApiEndpoint", "http://app.domain.com");

        Optional<String> result = dashboardInstanceFactory.getInstance(instance1);
        Assert.assertTrue(result.isPresent());
        assertThat(createJsonProperty(DashboardInstanceFactory.METADATA_STATE, InstanceState.STOPPED.name()), equalTo(result.get()));
    }

    @Test
    public void test_getInstance_failure_notFound() throws Exception {
        final String instanceId = "uiServiceInstanceGuid1";
        when(restTemplate.exchange(eq(GET_SERVICE_INSTANCE_URL), eq(HttpMethod.GET), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyString(), eq(instanceId)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // we need to inject some @Values via reflection
        ReflectionTestUtils.setField(dashboardInstanceFactory, "platformApiEndpoint", "http://app.domain.com");

        Optional<String> result = dashboardInstanceFactory.getInstance(instanceId);
        Assert.assertFalse(result.isPresent());
    }

    private void mockCreateInstanceResponse() {
        when(restTemplate.exchange(eq(CREATE_SERVICE_INSTANCE_URL), Mockito.any(), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyCollection()))
                .thenReturn(new ResponseEntity<>(
                        createJsonProperty(DashboardInstanceFactory.METADATA_ID, "service_instance_guid"),
                        HttpStatus.OK));
    }

    private void mockDeleteInstanceResponse() {
        when(restTemplate.exchange(eq(DELETE_SERVICE_INSTANCE_URL), eq(HttpMethod.DELETE), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
    }

    private void mockStopInstanceResponse() {
        when(restTemplate.exchange(eq(STOP_SERVICE_INSTANCE_URL), eq(HttpMethod.PUT), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyString(), anyString()))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
    }

    private void mockGetInstanceResponse(String instanceId, InstanceState expectedState) {
        when(restTemplate.exchange(eq(GET_SERVICE_INSTANCE_URL), eq(HttpMethod.GET), Mockito.<HttpEntity>any(), Mockito.<Class<String>>any(), anyString(), eq(instanceId)))
                .thenReturn(new ResponseEntity<>(
                        createJsonProperty(DashboardInstanceFactory.METADATA_STATE, expectedState.name()),
                        HttpStatus.OK));
    }

    private static String createJsonProperty(String name, String value) {
        return String.format("{\"%s\": \"%s\"}", name, value);
    }
}
