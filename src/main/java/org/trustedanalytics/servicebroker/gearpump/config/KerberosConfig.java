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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.trustedanalytics.hadoop.kerberos.KrbLoginManager;
import org.trustedanalytics.hadoop.kerberos.KrbLoginManagerFactory;
import org.trustedanalytics.servicebroker.gearpump.kerberos.KerberosProperties;

import javax.validation.constraints.NotNull;
import java.io.IOException;

@Configuration
public class KerberosConfig {

    @Value("${kerberos.enabled:false}")
    @NotNull
    private boolean enabled;

    @Value("${kerberos.kdc:}")
    @NotNull
    private String kdc;

    @Value("${kerberos.realm:}")
    @NotNull
    private String realm;

    @Value("${kerberos.user:}")
    @NotNull
    private String user;

    @Value("${kerberos.password:}")
    @NotNull
    private String password;

    @Bean
    public KerberosProperties getKerberosProperties() throws IOException {
        KerberosProperties krbProps = new KerberosProperties();
        krbProps.setKdc(kdc);
        krbProps.setRealm(realm);
        krbProps.setUser(user);
        krbProps.setPassword(password);
        krbProps.setKerberosEnabled(enabled);
        return krbProps;
    }

    @Bean
    public KrbLoginManager loginManager() {
        return enabled ? KrbLoginManagerFactory.getInstance().getKrbLoginManagerInstance(kdc, realm) : null;
    }

}
