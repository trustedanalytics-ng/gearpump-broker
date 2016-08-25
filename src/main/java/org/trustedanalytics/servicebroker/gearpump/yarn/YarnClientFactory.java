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
package org.trustedanalytics.servicebroker.gearpump.yarn;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trustedanalytics.hadoop.config.client.helper.DelegatingYarnClient;
import org.trustedanalytics.hadoop.config.client.helper.UgiWrapper;
import org.trustedanalytics.servicebroker.gearpump.kerberos.KerberosService;

import javax.security.auth.login.LoginException;
import java.io.IOException;

@Configuration
public class YarnClientFactory {

    @Autowired
    private KerberosService kerberosService;

    @Autowired
    private org.apache.hadoop.conf.Configuration yarnConfiguration;

    @Bean
    public YarnClientFactory yarnClientFactory() {
        return new YarnClientFactory();
    }

    public YarnClient getYarnClient() throws IOException, LoginException {
        kerberosService.login();
        String ticketCachePath = yarnConfiguration.get(CommonConfigurationKeys.KERBEROS_TICKET_CACHE_PATH);
        UserGroupInformation ugi = UserGroupInformation.getBestUGI(ticketCachePath, kerberosService.getKerberosProperties().getUser());
        YarnClient yarnClient = new DelegatingYarnClient(YarnClient.createYarnClient(), new UgiWrapper(ugi));
        yarnClient.init(yarnConfiguration);
        yarnClient.start();
        return yarnClient;
    }
}
