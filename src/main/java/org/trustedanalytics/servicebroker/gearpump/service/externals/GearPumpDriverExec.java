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

package org.trustedanalytics.servicebroker.gearpump.service.externals;

import com.google.common.base.Strings;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.trustedanalytics.servicebroker.gearpump.config.GearPumpSpawnerConfig;
import org.trustedanalytics.servicebroker.gearpump.kerberos.KerberosService;
import org.trustedanalytics.servicebroker.gearpump.model.GearPumpCredentials;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.ExternalProcessExecutor;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.ExternalProcessExecutorResult;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.HdfsUtils;
import org.trustedanalytics.servicebroker.gearpump.service.file.ResourceManagerService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GearPumpDriverExec {
    private static final Logger LOGGER = LoggerFactory.getLogger(GearPumpDriverExec.class);

    private static final String COMMAND_LINE_TEMPLATE_SPAWN = "bin/yarnclient launch -package %s -output %s";
    private static final String WORKERS_NUMBER_SWITCH = "-Dgearpump.yarn.worker.containers=";
    private static final String WORKERS_MEMORY_LIMIT  = "-Dgearpump.yarn.worker.memory=";

    @Autowired
    private GearPumpCredentialsParser gearPumpCredentialsParser;

    @Autowired
    private GearPumpOutputReportReader gearPumpOutputReportReader;

    @Autowired
    private ResourceManagerService resourceManagerService;

    @Autowired
    private GearPumpSpawnerConfig gearPumpSpawnerConfig;

    @Autowired
    private HdfsUtils hdfsUtils;

    @Autowired
    private ExternalProcessExecutor externalProcessExecutor;

    @Autowired
    private KerberosService kerberosService;

    private String destDir;

    private String getDestDir() {
        try {
            return resourceManagerService.getRealPath(gearPumpSpawnerConfig.getGearPumpDestinationFolder());
        } catch (IOException e) {
            LOGGER.debug("Swallowing exception while getting destDir.", e);
            return null;
        }
    }
    public SpawnResult spawnGearPumpOnYarn(String numberOfWorkers)  {
        LOGGER.info("spawnGearPumpOnYarn numberOfWorkers = [" + numberOfWorkers + "]");

        destDir = getDestDir();

        String outputReportFilePath = createOutputReportFilePath(destDir);

        String yarnApplicationId = null;
        String mastersUrl = null;
        Exception resultException = null;

        ExternalProcessExecutorResult processExecutorResult = deployGearPumpOnYarn(outputReportFilePath, numberOfWorkers);
        LOGGER.debug("processExecutorResult: {}", processExecutorResult);

        // try to determine appId regardless of the result (if failed, we still need appId to kill the app on yarn)
        if (!Strings.isNullOrEmpty(processExecutorResult.getOutput())) {
            yarnApplicationId = gearPumpCredentialsParser.getApplicationId(processExecutorResult.getOutput());
        }

        // obtain mastersUrl
        if (processExecutorResult.getExitCode() == 0) {
            try {
                mastersUrl = gearPumpOutputReportReader.fromOutput(outputReportFilePath).getMasterUrl();
            } catch (GearpumpOutputException e) {
                LOGGER.warn(String.format("GearpumpOutputException %s", e.getMessage()), e);
            }
        }

        // clean report file before exiting
        gearPumpOutputReportReader.fromOutput(outputReportFilePath).deleteReportFile();

        if (processExecutorResult.getExitCode() != 0) {
            resultException = new ExternalProcessException("Error executing yarnclient.", processExecutorResult.getException());
        }

        // make additional check for credentials
        if (resultException == null && (Strings.isNullOrEmpty(mastersUrl) || Strings.isNullOrEmpty(yarnApplicationId))) {
            resultException = new ExternalProcessException("Couldn't obtain yarn credentials.");
        }

        int status = resultException == null ? SpawnResult.STATUS_OK : SpawnResult.STATUS_ERR;

        return new SpawnResult(status, new GearPumpCredentials(mastersUrl, yarnApplicationId), resultException);
    }

    private ExternalProcessExecutorResult deployGearPumpOnYarn(String outputReportFilePath, String numberOfWorkers) {
        String[] command = getGearPumpYarnCommand(outputReportFilePath);
        Map<String, String> envProperties = getEnvForProcessBuilder(numberOfWorkers, gearPumpSpawnerConfig.getWorkersMemoryLimit());
        return externalProcessExecutor.run(command, destDir, envProperties);
    }

    private String[] getGearPumpYarnCommand(String outputReportFilePath) {
        String gearpumpPackUri = hdfsUtils.getHdfsUri() + gearPumpSpawnerConfig.getHdfsGearPumpPackPath();
        return String.format(COMMAND_LINE_TEMPLATE_SPAWN, gearpumpPackUri, outputReportFilePath).split(" ");
    }

    private String createOutputReportFilePath(String gearPumpDestinationFolderPath) {
        return String.format("%s/output-%d-%s.conf", gearPumpDestinationFolderPath, System.currentTimeMillis(), RandomStringUtils.randomNumeric(4));
    }

    private Map<String, String> getEnvForProcessBuilder(String numberOfWorkers, String workersMemoryLimit)  {
        Map<String, String> result = new HashMap<>();

        String envOptions = Strings.nullToEmpty(kerberosService.getKerberosJavaOpts());

        if(!Strings.isNullOrEmpty(numberOfWorkers)) {
            envOptions += " " + WORKERS_NUMBER_SWITCH + numberOfWorkers;
        }

        if(!Strings.isNullOrEmpty(workersMemoryLimit)) {
            envOptions += " " + WORKERS_MEMORY_LIMIT + workersMemoryLimit;
        }

        if (!envOptions.isEmpty()) {
            LOGGER.info("JAVA_OPTS: {}", envOptions);
            result.put("JAVA_OPTS", envOptions);
        }
        return result;
    }
}
