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

package org.trustedanalytics.servicebroker.gearpump.service.externals.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.net.URI;

public class CfCaller {

    public static final Logger LOGGER = LoggerFactory.getLogger(CfCaller.class);

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    private RestTemplate restTemplate;

    public CfCaller(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpEntity<String> createJsonRequest(String body, HttpHeaders headers) {
        return new HttpEntity<>(body, prepareHeaders(headers == null ? new HttpHeaders() : headers));
    }

    private HttpHeaders prepareHeaders(HttpHeaders headers) {
        headers.add("Accept", "application/json");
        headers.add("Content-type", "application/json");
        return headers;
    }

    public ResponseEntity<String> execute(String url, HttpMethod method, String body, Object... urlVariables) {
        return executeWithHeaders(url, method, body, null, urlVariables);
    }

    public ResponseEntity<String> executeWithHeaders(String url, HttpMethod method, String body, HttpHeaders headers, Object... urlVariables) {
        HttpEntity<String> request = createJsonRequest(body, headers);
        URI expanded = (new UriTemplate(url)).expand(urlVariables);
        LOGGER.info("Performing call: {}", expanded.toString());
        ResponseEntity<String> response = this.restTemplate.exchange(url, method, request, String.class, urlVariables);
        assert response != null;
        LOGGER.debug("Response status: {}", response.getStatusCode());
        LOGGER.debug("Response: {}", response);
        return response;
    }

}