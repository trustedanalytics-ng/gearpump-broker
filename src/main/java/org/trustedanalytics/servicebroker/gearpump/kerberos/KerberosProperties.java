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

public class KerberosProperties {

    public static final String KRB5_REALM_PROP = "java.security.krb5.realm";
    public static final String KRB5_KDC_PROP = "java.security.krb5.kdc";

    private String kdc = "";
    private String realm = "";
    private String user = "";
    private String password = "";
    private boolean enabled;

    public String getKdc() {
        return kdc;
    }

    public void setKdc(String kdc) {
        this.kdc = kdc;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setKerberosEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isKerberosEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "KerberosProperties{" +
                "enabled='" + enabled + '\'' +
                "kdc='" + kdc + '\'' +
                ", realm='" + realm + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}




