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
import org.trustedanalytics.cfbroker.store.zookeeper.service.ZookeeperClient;
import org.trustedanalytics.servicebroker.gearpump.kerberos.KerberosService;
import org.trustedanalytics.servicebroker.gearpump.service.CredentialPersistorService;
import org.trustedanalytics.servicebroker.gearpump.service.GearPumpSpawner;
import org.trustedanalytics.servicebroker.gearpump.service.dashboard.DashboardDeployer;
import org.trustedanalytics.servicebroker.gearpump.service.externals.GearPumpCredentialsParser;
import org.trustedanalytics.servicebroker.gearpump.service.externals.GearPumpDriverExec;
import org.trustedanalytics.servicebroker.gearpump.yarn.YarnAppManager;

import javax.validation.constraints.NotNull;

@Configuration
public class GearPumpSpawnerConfig {

    @Value("${gearpump.pack.name}")
    @NotNull
    private String gearPumpPackName;

    @Value("${gearpump.destinationFolder}")
    @NotNull
    private String gearPumpDestinationFolder;

    @Value("${yarn.conf.dir}")
    private String yarnConfDir;

    @Value("${gearpump.hdfsDir}")
    @NotNull
    private String hdfsDir;

    @Value("${workers.memorylimit}")
    @NotNull
    private String workersMemoryLimit;

    public String getHdfsGearPumpPackPath() {
        return String.format("%s/%s", hdfsDir, gearPumpPackName);
    }

    @Bean
    public GearPumpSpawner getGearPumpSpawner(GearPumpDriverExec gearPumpDriver,
                                              DashboardDeployer dashboardDeployer,
                                              YarnAppManager yarnAppManager,
                                              CatalogConfig catalogConfig,
                                              KerberosService kerberosService) {
        return new GearPumpSpawner(gearPumpDriver, dashboardDeployer, yarnAppManager, catalogConfig, kerberosService);
    }

    @Bean
    public GearPumpDriverExec gearPumpDriverExec() {
        return new GearPumpDriverExec();
    }

    @Bean
    public GearPumpCredentialsParser gearPumpCredentialsParser() {
        return new GearPumpCredentialsParser();
    }

    @Bean
    public CredentialPersistorService credentialPersistorService(ZookeeperClient getZKClient) {
        return new CredentialPersistorService(getZKClient);
    }

    public String getGearPumpPackName() {
        return gearPumpPackName;
    }

    public String getGearPumpDestinationFolder() {
        return gearPumpDestinationFolder;
    }

    public String getHdfsDir() {
        return hdfsDir;
    }

    public String getWorkersMemoryLimit() {
        return workersMemoryLimit;
    }

    public String getYarnConfDir() {
        return yarnConfDir;
    }

    @Override
    public String toString() {
        return "GearPumpSpawnerConfig{" +
                ", gearPumpPackName='" + gearPumpPackName + '\'' +
                ", gearPumpDestinationFolder='" + gearPumpDestinationFolder + '\'' +
                ", hdfsDir='" + hdfsDir + '\'' +
                ", workersMemoryLimit='" + workersMemoryLimit + '\'' +
                ", yarnConfDir='" + yarnConfDir + '\'' +
                '}';
    }
}
