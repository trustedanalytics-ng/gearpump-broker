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

import java.io.File;
import java.io.IOException;


@Service
public class PrerequisitesChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrerequisitesChecker.class);

    @Autowired
    private HdfsUtils hdfsUtils;

    @Autowired
    private GearPumpSpawnerConfig gearPumpSpawnerConfig;

    /**
     * Ensure that requirements are met in order to start provisioning:
     * <li>gearpump binary pack (archive) is present</li>
     * <li>the archive is unpacked</li>
     * <li>the archive is stored on hdfs</li>
     */
    public void ensurePrerequisities() {
        LOGGER.info("ensure prerequisities invoked");

        String gearPumpPackName = gearPumpSpawnerConfig.getGearPumpPackName();
        String archiveLocalPath = gearPumpPackName;
        // 1. check if the archive exists
        checkIfTheArchiveExists(archiveLocalPath, gearPumpPackName);

        // 2. check if unpacked version exists
        checkIfUnpackedVersionExists(archiveLocalPath);

        // 3. check if hdfs directory exists
        checkIfHdfsDirExists();

        //4. check if the pack is in hdfs
        checkPresenceOfPackOnHdfs(archiveLocalPath);
    }



    private void checkIfTheArchiveExists(String archiveLocalPath, String gearPumpPackName) {
        LOGGER.info("Checking if archive {} exists. ", archiveLocalPath);
        File gearpumpArchive = new File(archiveLocalPath);
        LOGGER.debug("gearpumpArchive.path: {}", gearpumpArchive.getAbsolutePath());
        boolean archivePresent = gearpumpArchive.exists();
        if (!archivePresent) {
            LOGGER.info("GearPump archive not present. Downloading it...");
            // for now it's always there - it's included in the build
            LOGGER.error("Not yet implemented");
            throw new PrerequisitesException("Downloading archive not yet implemented.");
        }
        LOGGER.info("GearPump archive {} is present. ", gearPumpPackName);
    }

    private void checkIfUnpackedVersionExists(String archiveLocalPath) {
        LOGGER.info("Checking if the archive was unpacked in location: {}", gearPumpSpawnerConfig.getGearPumpDestinationFolder());
        File gearpumpDestinationFolder = new File(gearPumpSpawnerConfig.getGearPumpDestinationFolder());
        LOGGER.debug("gearpumpDestinationFolder.path: {}", gearpumpDestinationFolder.getAbsolutePath());
    }

    private void checkIfHdfsDirExists() {
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

    private void checkPresenceOfPackOnHdfs(String archiveLocalPath) {
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
                hdfsUtils.upload(archiveLocalPath, hdfsFilePath);
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
