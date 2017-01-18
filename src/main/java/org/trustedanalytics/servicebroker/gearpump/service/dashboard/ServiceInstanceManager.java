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

import java.io.IOException;
import java.util.Optional;

interface ServiceInstanceManager {

    String createInstance(String uiInstanceName, String spaceId, String orgId, String username,
                          String password, String gearpumpMaster, String uaaClientName) throws IOException;

    void deleteInstance(String instanceId) throws DashboardServiceException;

    boolean stopInstance(String instanceId) throws DashboardServiceException;

    String getInstanceProperty(String instanceData, String propertyName) throws DashboardServiceException;

    String getInstanceState(String instanceData) throws DashboardServiceException;

    Optional<String> getInstance(String instanceId) throws DashboardServiceException;

    Optional<Boolean> isInstanceStopped(String instanceId) throws DashboardServiceException;

    boolean ensureInstanceIsStopped(String instanceId) throws DashboardServiceException;
}
