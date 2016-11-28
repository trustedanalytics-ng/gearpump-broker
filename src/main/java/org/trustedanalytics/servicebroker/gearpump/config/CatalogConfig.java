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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.cloudfoundry.community.servicebroker.model.Catalog;
import org.cloudfoundry.community.servicebroker.model.Plan;
import org.cloudfoundry.community.servicebroker.model.ServiceDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class CatalogConfig {

    @Value("${cf.serviceid}")
    @NotNull
    private String cfServiceId;

    @Value("${cf.servicename}")
    @NotNull
    private String cfServiceName;

    @Value("${cf.baseId}")
    @NotNull
    private String cfBaseId;

    @Value("${metadata.imageUrl}")
    @NotNull
    private String imageUrl;

    private static final String IMAGE_URL = "imageUrl";
    private static final String DISPLAY_NAME = "displayName";
    private static final String DISPLAY_NAME_VALUE = "Apache Gearpump";
    private static final String SYSLOG_DRAIN = "syslog_drain";
    private static final String GEARPUMP_PLAN_PREFIX = "gearpump-";
    private static final String[] GEARPUMP_PLANS = {"small", "medium"};
    @Bean
    public Catalog catalog() {
        return new Catalog(Arrays.asList(new ServiceDefinition(getCfServiceId(), getCfServiceName(),
                "Modern big data real-time streaming engine", true, true, getGearPumpPlans(),
                Arrays.asList("data-science-tool"), getServiceDefinitionMetadata(), Arrays.asList(SYSLOG_DRAIN), null)));
    }

    public String getNumberOfWorkers(String planName) {
        if(planName.equalsIgnoreCase(GEARPUMP_PLAN_PREFIX.concat(GEARPUMP_PLANS[0]))) {
            return "1";
        }
        if(planName.equalsIgnoreCase(GEARPUMP_PLAN_PREFIX.concat(GEARPUMP_PLANS[1]))) {
            return "3";
        }

        return "1";
    }

    private List<Plan> getGearPumpPlans() {
        return ImmutableList.copyOf(GEARPUMP_PLANS)
                .stream()
                .map(x -> "" + x)
                .map(this::createPlan)
                .collect(Collectors.toList());
    }

    private Plan createPlan(String planName) {
        return new Plan(
                getCfBaseId() + "-" + planName,
                planName,
                "Run Gearpump with " + getNumberOfWorkers(planName),
                ImmutableMap.of("planName", (Object) planName),
                true
        );
    }

    private Map<String, Object> getServiceDefinitionMetadata() {
        Map<String,Object> serviceMetadata = new HashMap<>();

        serviceMetadata.put(IMAGE_URL, getImageUrl());
        serviceMetadata.put(DISPLAY_NAME, DISPLAY_NAME_VALUE);

        return serviceMetadata;
    }


    public String getCfServiceName() {
        return cfServiceName;
    }

    public void setCfServiceName(String cfServiceName) {
        this.cfServiceName = cfServiceName;
    }

    public String getCfServiceId() {
        return cfServiceId;
    }

    public void setCfServiceId(String cfServiceId) {
        this.cfServiceId = cfServiceId;
    }

    public String getCfBaseId() {
        return cfBaseId;
    }

    public void setCfBaseId(String cfBaseId) {
        this.cfBaseId = cfBaseId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "CatalogConfig{" +
                "cfServiceId='" + cfServiceId + '\'' +
                ", cfServiceName='" + cfServiceName + '\'' +
                ", cfBaseId='" + cfBaseId + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
