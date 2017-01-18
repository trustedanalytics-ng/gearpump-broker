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

package org.trustedanalytics.servicebroker.gearpump.config;

import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.trustedanalytics.cfbroker.store.api.BrokerStore;
import org.trustedanalytics.cfbroker.store.serialization.RepositoryDeserializer;
import org.trustedanalytics.cfbroker.store.serialization.RepositorySerializer;
import org.trustedanalytics.cfbroker.store.zookeeper.service.ZookeeperClient;
import org.trustedanalytics.cfbroker.store.zookeeper.service.ZookeeperClientBuilder;
import org.trustedanalytics.cfbroker.store.zookeeper.service.ZookeeperStore;
import org.trustedanalytics.hadoop.kerberos.KrbLoginManagerFactory;
import org.trustedanalytics.servicebroker.gearpump.kerberos.KerberosProperties;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

@Configuration
public class BrokerStoreConfig {

    @Autowired
    private KerberosProperties kerberosProperties;

    private FactoryHelper helper;

    @Autowired
    @Qualifier(Qualifiers.SERVICE_INSTANCE)
    private RepositorySerializer<ServiceInstance> instanceSerializer;

    @Autowired
    @Qualifier(Qualifiers.SERVICE_INSTANCE)
    private RepositoryDeserializer<ServiceInstance> instanceDeserializer;

    @Autowired
    @Qualifier(Qualifiers.SERVICE_INSTANCE_BINDING)
    private RepositorySerializer<CreateServiceInstanceBindingRequest> bindingSerializer;

    @Autowired
    @Qualifier(Qualifiers.SERVICE_INSTANCE_BINDING)
    private RepositoryDeserializer<CreateServiceInstanceBindingRequest> bindingDeserializer;

    @Value("${zookeeper.cluster}")
    private String zookeeperCluster;

    @Value("${zookeeper.node}")
    private String zookeeperNode;

    public BrokerStoreConfig() {
        this.helper = new FactoryHelper();
    }

    BrokerStoreConfig(FactoryHelper helper) {
        this.helper = helper;
    }

    @Bean
    @Qualifier(Qualifiers.SERVICE_INSTANCE)
    public BrokerStore<ServiceInstance> getServiceInstanceStore(ZookeeperClient zkClient) throws IOException {
        return new ZookeeperStore<ServiceInstance>(zkClient, instanceSerializer, instanceDeserializer);
    }

    @Bean
    @Qualifier(Qualifiers.SERVICE_INSTANCE_BINDING)
    public BrokerStore<CreateServiceInstanceBindingRequest> getServiceInstanceBindingStore(ZookeeperClient zkClient) throws IOException {
        return new ZookeeperStore<CreateServiceInstanceBindingRequest>(zkClient, bindingSerializer, bindingDeserializer);
    }

    @Bean
    @Profile("cloud")
    public ZookeeperClient getZKClient() throws IOException, NoSuchAlgorithmException {
        ZookeeperClient zkClient;

        if(kerberosProperties.isKerberosEnabled()) {
            zkClient = helper.getSecureZkClientInstance(
                    zookeeperCluster,
                    kerberosProperties.getUser(),
                    kerberosProperties.getPassword(),
                    kerberosProperties.getKdc(),
                    kerberosProperties.getRealm(),
                    zookeeperNode
            );
        } else {
            zkClient = helper.getInsecureZkClientInstance(
                    zookeeperCluster,
                    kerberosProperties.getUser(),
                    kerberosProperties.getPassword(),
                    zookeeperNode
            );
        }
        zkClient.init();
        return zkClient;
    }

    static final class FactoryHelper {
        ZookeeperClient getSecureZkClientInstance(String zkCluster, String user, String pass, String kdc, String realm, String zkNode) throws IOException, NoSuchAlgorithmException {
            KrbLoginManagerFactory.getInstance().getKrbLoginManagerInstance(kdc, realm);
            return new ZookeeperClientBuilder(zkCluster, user, pass, zkNode).withRootCreation(getAcl(user, pass)).build();
        }

        ZookeeperClient getInsecureZkClientInstance(String zkCluster, String user, String pass, String zkNode) throws IOException, NoSuchAlgorithmException {
            return new ZookeeperClientBuilder(zkCluster, user, pass, zkNode).withRootCreation(getAcl(user, pass)).build();
        }

        private List<ACL> getAcl(String user, String pass) throws NoSuchAlgorithmException {
            String digest = DigestAuthenticationProvider.generateDigest(String.format("%s:%s", user, pass));
            return Arrays.asList(new ACL(ZooDefs.Perms.ALL, new Id("digest", digest)));
        }
    }
}
