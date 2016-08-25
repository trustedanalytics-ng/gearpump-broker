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
package org.trustedanalytics.servicebroker.gearpump.kerberos;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustedanalytics.hadoop.kerberos.KrbLoginManager;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.ExternalProcessEnvBuilder;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;

@Service
public class KerberosService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosService.class);

    private final KerberosProperties kerberosProperties;
    private final KrbLoginManager loginManager;
    private final Configuration hadoopConfiguration;

    @Autowired
    public KerberosService(KrbLoginManager loginManager, KerberosProperties kerberosProperties, Configuration hadoopConfiguration) {
        this.kerberosProperties = kerberosProperties;
        this.hadoopConfiguration = hadoopConfiguration;
        this.loginManager = loginManager;
    }

    public String getKerberosJavaOpts() {
        return kerberosProperties.isKerberosEnabled() ? String.format("%s %s", buildKdcOption(), buildRealmOption()) : null;
    }

    private String buildRealmOption() {
        return ExternalProcessEnvBuilder.buildJavaParam(KerberosProperties.KRB5_REALM_PROP, kerberosProperties.getRealm());
    }

    private String buildKdcOption() {
        return ExternalProcessEnvBuilder.buildJavaParam(KerberosProperties.KRB5_KDC_PROP, kerberosProperties.getKdc());
    }

    public KerberosProperties getKerberosProperties() {
        return kerberosProperties;
    }

    public void login() throws LoginException, IOException {
        LOGGER.info("Logging in to Hadoop, kerberosEnabled ? {}", kerberosProperties.isKerberosEnabled());
        if (kerberosProperties.isKerberosEnabled()) {
            Subject subject = loginManager.loginWithCredentials(kerberosProperties.getUser(), kerberosProperties.getPassword().toCharArray());
            loginManager.loginInHadoop(subject, hadoopConfiguration);
            LOGGER.debug("Logged in to hadoop");
        }
    }
}
