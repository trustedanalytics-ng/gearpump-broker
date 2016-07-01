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

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.servicebroker.gearpump.model.GearPumpCredentials;

import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GearPumpServiceInstanceServiceTest {

    @Mock
    private ServiceInstanceService instanceService;

    @Mock
    private GearPumpSpawner gearPumpSpawner;

    @Mock
    private CredentialPersistorService credentialPersistorService;

    private GearPumpServiceInstanceService service;

    private ServiceInstance instance;
    private GearPumpCredentials gearPumpCredentials;

    private CreateServiceInstanceRequest createRequest;
    private DeleteServiceInstanceRequest deleteRequest;

    @Before
    public void before() {
        service = new GearPumpServiceInstanceService(instanceService, gearPumpSpawner, credentialPersistorService);
        instance = getServiceInstance("id");
        gearPumpCredentials = getGearPumpCredentials();
        createRequest = getCreateServiceInstanceRequest(instance);
        deleteRequest = getDeleteServiceInstanceRequest(instance);
    }

    @Test(expected = ServiceBrokerException.class)
    public void testCreateServiceInstance_failure_notInjectedGearPump() throws Exception {
        when(instanceService.createServiceInstance(createRequest)).thenReturn(instance);

        this.service.createServiceInstance(createRequest);
    }

    @Test(expected = ServiceBrokerException.class)
    public void testCreateServiceInstance_failure_failureOnProvisionInstance_nullGearPumpCredentials() throws Exception {

        when(instanceService.createServiceInstance(createRequest)).thenReturn(instance);
        gearPumpCredentials = null;
        when(gearPumpSpawner.provisionInstance(anyString(), anyString(), anyString(), anyString())).thenReturn(gearPumpCredentials);

        service.createServiceInstance(createRequest);
    }

    @Test(expected = ServiceBrokerException.class)
    public void testCreateServiceInstance_failureOnProvisionInstance_exceptionThrown() throws Exception {

        when(instanceService.createServiceInstance(createRequest)).thenReturn(instance);
        doThrow(Exception.class)
                .when(gearPumpSpawner).provisionInstance(anyString(), anyString(), anyString(), anyString());

        service.createServiceInstance(createRequest);
    }

    @Test(expected = ServiceBrokerException.class)
    public void testCreateServiceInstance_failureOnPersistCredentials_exceptionThrown() throws Exception {

        when(instanceService.createServiceInstance(createRequest)).thenReturn(instance);
        when(gearPumpSpawner.provisionInstance(anyString(), anyString(), anyString(), anyString())).thenReturn(gearPumpCredentials);

        doThrow(IOException.class).
                when(credentialPersistorService).persistCredentials(anyString(), anyMapOf(String.class, Object.class));

        service.createServiceInstance(createRequest);
    }

    protected CreateServiceInstanceRequest getCreateServiceInstanceRequest(ServiceInstance instance) {
        return new CreateServiceInstanceRequest(
                getServiceDefinition().getId(), instance.getPlanId(), instance.getOrganizationGuid(),
                instance.getSpaceGuid()).withServiceInstanceId(instance.getServiceInstanceId()).
                withServiceDefinition(getServiceDefinition());
    }

    @Test
    public void testCreateServiceInstance_success() throws Exception {
        when(instanceService.createServiceInstance(createRequest)).thenReturn(instance);
        when(gearPumpSpawner.provisionInstance(anyString(), anyString(), anyString(), anyString())).thenReturn(gearPumpCredentials);

        ServiceInstance returnedInstance = service.createServiceInstance(createRequest);

        verify(instanceService).createServiceInstance(this.createRequest);
        verify(credentialPersistorService).persistCredentials(this.createRequest.getServiceInstanceId(), this.gearPumpCredentials.toMap());

        assertThat(returnedInstance, equalTo(this.instance));

    }

    @Test
    public void testDeleteServiceInstance_success() throws Exception {
        when(credentialPersistorService.readCredentials(instance.getServiceInstanceId())).thenReturn(gearPumpCredentials);
        when(instanceService.deleteServiceInstance(deleteRequest)).thenReturn(instance);

        ServiceInstance returnedInstance = service.deleteServiceInstance(deleteRequest);

        verify(credentialPersistorService).removeCredentials(deleteRequest.getServiceInstanceId());
        verify(gearPumpSpawner).deprovisionInstance(gearPumpCredentials);
        verify(instanceService).deleteServiceInstance(deleteRequest);

        assertThat(returnedInstance, equalTo(instance));
    }

    @Test(expected = ServiceBrokerException.class)
    public void testDeleteServiceInstance_failureOnReadCredentials_exceptionThrown() throws Exception {
        doThrow(IOException.class).
                when(credentialPersistorService).readCredentials(instance.getServiceInstanceId());

        service.deleteServiceInstance(deleteRequest);
    }

    protected DeleteServiceInstanceRequest getDeleteServiceInstanceRequest(ServiceInstance instance) {
        return new DeleteServiceInstanceRequest(instance.getServiceInstanceId(), instance.getServiceDefinitionId(), instance.getPlanId());
    }

    protected GearPumpCredentials getGearPumpCredentials() {
        return new GearPumpCredentials("masters", "yarnApplicationId", "dashboardUrl", "dashboardId", "username", "password", "uaaClientName");
    }

    protected ServiceInstance getServiceInstance(String id) {
        return new ServiceInstance(
                new CreateServiceInstanceRequest(getServiceDefinition().getId(), "planId",
                        "organizationGuid", "spaceGuid").withServiceInstanceId(id));
    }

    protected ServiceDefinition getServiceDefinition() {
        return new ServiceDefinition("def", "name", "desc", true, Collections.emptyList());
    }

}
