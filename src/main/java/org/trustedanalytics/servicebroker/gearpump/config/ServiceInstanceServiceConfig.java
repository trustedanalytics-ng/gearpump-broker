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

package org.trustedanalytics.servicebroker.gearpump.config;

import org.trustedanalytics.cfbroker.store.api.BrokerStore;
import org.trustedanalytics.cfbroker.store.impl.ServiceInstanceServiceStore;
import org.trustedanalytics.servicebroker.gearpump.service.CredentialPersistorService;
import org.trustedanalytics.servicebroker.gearpump.service.GearPumpServiceInstanceService;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trustedanalytics.servicebroker.gearpump.service.GearPumpSpawner;

@Configuration
public class ServiceInstanceServiceConfig {

    @Autowired
    @Qualifier(value = Qualifiers.SERVICE_INSTANCE)
    private BrokerStore<ServiceInstance> store;

    @Bean
    public ServiceInstanceService getServiceInstanceService(GearPumpSpawner gearPumpSpawner, CredentialPersistorService credentialPersistorService) {
        return new GearPumpServiceInstanceService(new ServiceInstanceServiceStore(store), gearPumpSpawner, credentialPersistorService);
    }
}