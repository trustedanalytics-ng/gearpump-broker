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

public class DashboardServiceException extends Exception {

    public DashboardServiceException() {
        super();
    }

    public DashboardServiceException(String message) {
        super(message);
    }

    public DashboardServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    private static String getCannotObtainMessage(String param, String variable) {
        return String.format("Cannot obtain %s (check %s variable).", param, variable);
    }

    public static DashboardServiceException getCannotObtainException(String param, String variable) {
        return new DashboardServiceException(getCannotObtainMessage(param, variable));
    }

    public static DashboardServiceException getCannotObtainException(String param, String variable, Exception e) {
        return new DashboardServiceException(getCannotObtainMessage(param, variable), e);
    }
}
