/**
 * Copyright (c) 2016 Intel Corporation
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
package org.trustedanalytics.servicebroker.gearpump.service.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller;

import java.io.IOException;
import java.util.Base64;

import static org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller.AUTHORIZATION_HEADER;
import static org.trustedanalytics.servicebroker.gearpump.service.externals.helpers.CfCaller.CONTENT_TYPE_HEADER;

@Service
class UaaConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(UaaConnector.class);

    private static final String UAA_ACCESS_TOKEN = "/access_token";
    private static final String UAA_TOKEN_TYPE = "/token_type";

    private static final String CREATE_UAA_TOKEN_BODY_TEMPLATE = "grant_type=client_credentials&response_type=token";
    private static final String CREATE_UAA_CLIENT_BODY_TEMPLATE = "{\"client_id\":\"%s\",\"name\":\"%s\",\"client_secret\":\"%s\",\"scope\":[\"openid\",\"tap.user\"],\"resource_ids\":[\"none\"],\"authorities\":[\"uaa.resource\"],\"authorized_grant_types\":[\"client_credentials\",\"authorization_code\",\"refresh_token\"],\"autoapprove\":true,\"access_token_validity\":43200,\"redirect_uri\":[\"%s\"]}";

    static final String CREATE_UAA_TOKEN_URL = "{uaaTokenUrl}";
    static final String CREATE_UAA_CLIENT_URL = "{uaaUrl}/oauth/clients";
    static final String DELETE_UAA_CLIENT_URL = "{uaaUrl}/oauth/clients/{client_id}";

    private static final String REDIRECT_URI_SUFFIX = "/login/oauth2/cloudfoundryuaa/callback";

    private final CfCaller cfCaller;

    @Value("${uaa.endpoint}")
    private String uaaApiEndpoint;

    @Value("${uaa.token_uri}")
    private String uaaTokenApiEndpoint;

    @Autowired
    UaaConnector(CfCaller cfCaller) {
        this.cfCaller = cfCaller;
    }

    /**
     * Creates a UAA token.
     * @param clientId The client ID
     * @param clientSecret The client secret
     * @return The generated UAA token
     * @throws DashboardServiceException If token extracting goes wrong
     */
    public String createUaaToken(String clientId, String clientSecret) throws DashboardServiceException {
        LOGGER.info("Creating new UAA token. clientId: {}", clientId);

        String authorizationString = new String(Base64.getEncoder().encode((clientId + ":" + clientSecret).getBytes()));

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION_HEADER, "Basic " + authorizationString);
        headers.add(CONTENT_TYPE_HEADER, "application/x-www-form-urlencoded");

        ResponseEntity<String> response = cfCaller.executeWithHeaders(CREATE_UAA_TOKEN_URL, HttpMethod.POST, CREATE_UAA_TOKEN_BODY_TEMPLATE, headers, uaaTokenApiEndpoint);
        return extractToken(cfCaller, response);
    }

    /**
     * Creates a UAA client.
     * @param clientId The client ID
     * @param clientSecret The client secret
     * @param redirectUri The callback URI
     * @param token The UAA token
     * @return The response body
     */
    public String createUaaClient(String clientId, String clientSecret, String redirectUri, String token) {
        LOGGER.info("Creating new UAA client");
        String body = String.format(CREATE_UAA_CLIENT_BODY_TEMPLATE, clientId, clientId, clientSecret, "http://" + redirectUri + REDIRECT_URI_SUFFIX);
        LOGGER.debug("body: {}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION_HEADER, token);
        headers.add(CONTENT_TYPE_HEADER, "application/json");

        ResponseEntity<String> response = cfCaller.executeWithHeaders(CREATE_UAA_CLIENT_URL, HttpMethod.POST, body, headers, uaaApiEndpoint);
        LOGGER.debug("Created UAA client. Response body: {}", response.getBody());
        return response.getBody();
    }

    /**
     * Deletes a UAA client.
     * @param clientId The UAA client ID
     * @param token The UAA token
     * @return The response or null if client not found
     * @throws HttpClientErrorException If there was an error during HTTP connection
     */
    public String deleteUaaClient(String clientId, String token) {
        LOGGER.info("Deleting UAA client: {}", clientId);

        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION_HEADER, token);
        headers.add(CONTENT_TYPE_HEADER, "application/json");

        try {
            return cfCaller.executeWithHeaders(DELETE_UAA_CLIENT_URL, HttpMethod.DELETE, "", headers, uaaApiEndpoint, clientId).getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.debug("Cannot delete UAA client: {}. It is not exists.", clientId);
            } else {
                LOGGER.debug("Cannot delete UAA client: {} Error: {}", clientId, e.getStatusText());
                throw e;
            }
        }
        return null;
    }

    private String extractToken(CfCaller cfCaller, ResponseEntity<String> response) throws DashboardServiceException {
        try {
            return cfCaller.getValueFromJson(response.getBody(), UAA_TOKEN_TYPE)
                    + " " + cfCaller.getValueFromJson(response.getBody(), UAA_ACCESS_TOKEN);
        } catch (IOException e) {
            throw new DashboardServiceException("Cannot obtain UAA token.", e);
        }
    }
}
