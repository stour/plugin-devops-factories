/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.plugin.versioncontrolmonitor.server;

import org.eclipse.che.api.auth.AuthenticationService;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

import static javax.ws.rs.core.UriBuilder.fromUri;

/**
 * Wrapper class for calls to Codenvy auth REST API
 *
 * @author Stephane Tournie
 */
public class AuthConnection {

    private static final Logger LOG = LoggerFactory.getLogger(AuthConnection.class);

    private final String baseUrl;

    @Inject
    public AuthConnection(@Named("api.endpoint") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Authenticate against Codenvy
     *
     * @param username the username of the user to authenticate
     * @param password the password of the user to authenticate
     * @return an auth token if authentication is successful, null otherwise
     * @throws ServerException
     */
    public Token authenticateUser(String username, String password) throws ServerException {
        Token userToken;
        String url = fromUri(baseUrl).path(AuthenticationService.class).path(AuthenticationService.class, "authenticate")
                                     .build().toString();
        try {
            String myCredentials = "{ \"username\": \"" + username + "\", \"password\": \"" + password + "\" }";
            userToken = HttpJsonHelper.post(Token.class, url, DtoFactory.getInstance().createDtoFromJson(myCredentials, Credentials.class));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getLocalizedMessage());
        } catch (ServerException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getLocalizedMessage());
        } catch (UnauthorizedException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getLocalizedMessage());
        } catch (ForbiddenException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getLocalizedMessage());
        } catch (NotFoundException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getLocalizedMessage());
        } catch (ConflictException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getLocalizedMessage());
        }
        if (userToken != null) {
            LOG.debug("successfully authenticated with token " + userToken);
        }
        return userToken;
    }
}
