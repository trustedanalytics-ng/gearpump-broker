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
package io.gearpump.services.security.oauth2.impl;

import com.typesafe.config.Config;
import scala.concurrent.ExecutionContext;

public class CustomCloudFoundryUAAOAuth2Authenticator extends CloudFoundryUAAOAuth2Authenticator {

    private static final String GEARPUMP_UI_OAUTH2_AUTHENTICATOR_SCOPE = "scope";

    private String scope;

    @Override
    public void init(Config config, ExecutionContext executionContext) {
        if (config.hasPath(GEARPUMP_UI_OAUTH2_AUTHENTICATOR_SCOPE)) {
            this.scope = config.getString(GEARPUMP_UI_OAUTH2_AUTHENTICATOR_SCOPE);
        }
        super.init(config, executionContext);
    }

    @Override
    public String scope() {
        return this.scope == null ? super.scope() : this.scope;
    }
}