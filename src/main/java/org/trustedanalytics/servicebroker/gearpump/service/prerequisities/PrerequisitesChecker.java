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

package org.trustedanalytics.servicebroker.gearpump.service.prerequisities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.trustedanalytics.servicebroker.gearpump.config.GearPumpSpawnerConfig;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.HdfsUtils;

import java.io.IOException;

@Service
class PrerequisitesChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrerequisitesChecker.class);

    private final HdfsUtils hdfsUtils;
    private final GearPumpSpawnerConfig gearPumpSpawnerConfig;

    @Autowired
    public PrerequisitesChecker(HdfsUtils hdfsUtils, GearPumpSpawnerConfig gearPumpSpawnerConfig) {
        this.hdfsUtils = hdfsUtils;
        this.gearPumpSpawnerConfig = gearPumpSpawnerConfig;
    }

    void ensureHdfsDirectoryExists() {
        String hdfsDirectory = gearPumpSpawnerConfig.getHdfsDir();
        LOGGER.info("Check if HDFS directory for GearPump archive exists. hdfsDirectory: {}", hdfsDirectory);
        boolean hdfsDirExists;
        try {
            hdfsDirExists = hdfsUtils.directoryExists(hdfsDirectory);
        } catch (IOException e) {
            LOGGER.error("Error checking if HDFS directory for GearPump exists.", e);
            throw new PrerequisitesException("Error checking if HDFS directory for GearPump exists.", e);
        }

        if (!hdfsDirExists) {
            LOGGER.info("HDFS directory doesn't exist. Creating it now ...");
            try {
                hdfsUtils.createDir(hdfsDirectory);
                // make sure VCAP user can use this directory
                hdfsUtils.elevatePermissions(hdfsDirectory);
            } catch (IOException e) {
                LOGGER.error("Error creating HDFS directory.", e);
                throw new PrerequisitesException("Error creating HDFS directory.", e);
            }
        }
        LOGGER.info("HDFS directory for GearPump archive exists.");
    }

    void ensureGearpumpArchiveExistsOnHdfs() {
        String hdfsFilePath = gearPumpSpawnerConfig.getHdfsGearPumpPackPath();
        LOGGER.info("Checking if the archive ({}) is stored in hdfs", hdfsFilePath);
        boolean hdfsFileExists;
        try {
            hdfsFileExists = hdfsUtils.exists(hdfsFilePath);
        } catch (IOException e) {
            LOGGER.error("Error checking the archive presence.", e);
            throw new PrerequisitesException("Error checking the archive presence.", e);
        }

        if (!hdfsFileExists) {
            try {
                LOGGER.info("The archive is not on HDFS. Uploading it now...", hdfsFilePath);
                hdfsUtils.upload(gearPumpSpawnerConfig.getGearPumpPackName(), hdfsFilePath);
                // make sure VCAP user can use this file
                hdfsUtils.elevatePermissions(hdfsFilePath);
            } catch (IOException e) {
                LOGGER.error("Error uploading archive.", e);
                throw new PrerequisitesException("Error uploading archive.", e);
            }
        }
        LOGGER.info("Archive IS stored in hdfs");
    }
}
