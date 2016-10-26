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
package org.trustedanalytics.servicebroker.gearpump.service.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Deprecated
@Service
public class ResourceManagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerService.class);

    @Autowired
    private ResourceLoader resourceLoader;


    public boolean checkIfExists(String filePath) {
        Resource resource = getResourceForPath(filePath);
        return resource != null && resource.exists();
    }

    private Resource getResourceForPath(String filePath) {
        return resourceLoader.getResource(String.format("classpath:%s", filePath));
    }

    public String getRealPath(String path) throws IOException {
        Resource resource = getResourceForPath(path);
        try {
            return resource.getURI().getPath();
        } catch (IOException e) {
            LOGGER.error("Cannot get resource URI. ", e);
            throw e;
        }
    }

    public InputStream getResourceInputStream(String path) throws IOException {
        Resource resource = getResourceForPath(path);
        LOGGER.info("Got the resource: {}", resource.getURI());

        return resource.getInputStream();
    }
}
