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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
class DashboardStateValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardStateValidator.class);

    @FunctionalInterface
    interface StateValiditySupplier {
        Optional<Boolean> validate(String instanceId, InstanceState expectedState) throws DashboardServiceException;
    }

    @Value("${gearpump.dashboard.stateValidator.maxRetryCount}")
    private int maxRetryCount;

    @Value("${gearpump.dashboard.stateValidator.retryInterval}")
    private long retryInterval;

    @Value("${gearpump.dashboard.stateValidator.retryEnabled:false}")
    private boolean retryEnabled;

    boolean validate(StateValiditySupplier validator, String instanceId, InstanceState expectedState) throws DashboardServiceException {
        LOGGER.info("Ensuring if a Gearpump dashboard is in state: {}", expectedState);

        Optional<Boolean> result;
        int retryCount = 0;

        while (true) {
            result = validator.validate(instanceId, expectedState);
            LOGGER.info("The Gearpump dashboard has state {}? {}", expectedState, result);

            if (!result.isPresent() || result.get().equals(true) || !retryEnabled || retryCount >= maxRetryCount) {
                break;
            }

            LOGGER.info("Value not yet available. Let's retry validation. retryCount/maxRetryCount/retryInterval: {}/{}/{}", retryCount, maxRetryCount, retryInterval);
            retryCount++;

            if (retryInterval > 0) {
                sleepMoment();
            }

        }

        return result.isPresent() ? result.get() : false;
    }

    private void sleepMoment() {
        try {
            Thread.sleep(retryInterval * 1000);
        } catch (InterruptedException e) {
            LOGGER.warn("Thread.sleep() interrupted.", e);
            Thread.currentThread().interrupt();
        }
    }

    void disableRetrials() {
        this.retryEnabled = false;
    }

}
