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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trustedanalytics.hadoop.config.client.*;
import org.trustedanalytics.servicebroker.gearpump.kerberos.KerberosProperties;
import org.trustedanalytics.servicebroker.gearpump.kerberos.KerberosService;

import javax.security.auth.login.LoginException;
import java.io.IOException;

@Configuration
public class KerberosConfig {

    @Bean
    public KerberosProperties getKerberosProperties() throws IOException {
        AppConfiguration helper = Configurations.newInstanceFromEnv();
        ServiceInstanceConfiguration krbConf = helper.getServiceConfig(ServiceType.KERBEROS_TYPE);

        KerberosProperties krbProps = new KerberosProperties();
        krbProps.setKdc(krbConf.getProperty(Property.KRB_KDC).get());
        krbProps.setRealm(krbConf.getProperty(Property.KRB_REALM).get());
        krbProps.setUser(krbConf.getProperty(Property.USER).get());
        krbProps.setPassword(krbConf.getProperty(Property.PASSWORD).get());

        return krbProps;
    }

    @Bean
    public KerberosService kerberosService() throws IOException, LoginException {
        return new KerberosService(getKerberosProperties());
    }
}
