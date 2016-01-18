/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.plugin.versioncontrolmonitor.server;

import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.dto.server.DtoFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static javax.ws.rs.core.Response.Status.OK;

@RunWith(MockitoJUnitRunner.class)
public class TestVersionControlMonitorService {

    private final static String REQUEST_HEADER_GITHUB_EVENT = "X-GitHub-Event";

    private VersionControlMonitorService fakeVersionControlMonitorService;

    @Before
    public void setUp() throws Exception {
        // Prepare authConnection
        Token fakeToken = DtoFactory.newDto(Token.class).withValue("fakeToken");
        AuthConnection mockAuthConnection = mock(AuthConnection.class);
        when(mockAuthConnection.authenticateUser("somebody@somemail.com", "somepwd")).thenReturn(fakeToken);

        // Prepare factoryConnection
        URL factoryResource = getClass().getResource("/factory-MKTG-341.json");
        Factory fakeFactory = DtoFactory.getInstance().createDtoFromJson(readFile(factoryResource.getFile(), StandardCharsets.UTF_8),
                                                                         Factory.class);
        FactoryConnection mockFactoryConnection = mock(FactoryConnection.class);
        when(mockFactoryConnection.getFactory("fakeFactoryId", fakeToken)).thenReturn(fakeFactory);
        when(mockFactoryConnection.updateFactory(fakeFactory, "https://github.com/bmicklea-ce/dashboard", "MKTG-341",
                                                 "https://github.com/codenvy-demos/dashboard", "d35d80c275514c226f4785a93ba34c46abb309e6",
                                                 fakeToken)).thenReturn(fakeFactory);

        // Prepare VersionControlMonitorService
        fakeVersionControlMonitorService =
                new VersionControlMonitorService(mockAuthConnection, mockFactoryConnection);
    }

    @Test
    public void testGithubWebhookPushEventNoConnector() throws Exception {
        HttpServletRequest mockRequest = prepareRequest("push");
        Response response = fakeVersionControlMonitorService.githubWebhook("my-workspace", mockRequest);
        Assert.assertTrue(response.getStatus() == OK.getStatusCode());
    }

    @Test
    public void testGithubWebhookPullRequestEventNoConnector() throws Exception {
        HttpServletRequest mockRequest = prepareRequest("pull_request");
        Response response = fakeVersionControlMonitorService.githubWebhook("my-workspace", mockRequest);
        Assert.assertTrue(response.getStatus() == OK.getStatusCode());
    }

    protected HttpServletRequest prepareRequest(String eventType) throws Exception {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        String githubEventString = null;
        switch (eventType) {
            case "pull_request":
                URL urlPR = getClass().getResource("/pull_request_event.json");
                githubEventString = readFile(urlPR.getFile(), StandardCharsets.UTF_8);
                break;
            case "push":
                URL urlP = getClass().getResource("/push_event.json");
                githubEventString = readFile(urlP.getFile(), StandardCharsets.UTF_8);
                break;
            default:
                break;
        }
        ServletInputStream fakeInputStream = null;
        if (githubEventString != null) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(githubEventString.getBytes(StandardCharsets.UTF_8));
            fakeInputStream = new ServletInputStream() {
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }
            };
        }
        when(mockRequest.getHeader(REQUEST_HEADER_GITHUB_EVENT)).thenReturn(eventType);
        when(mockRequest.getInputStream()).thenReturn(fakeInputStream);

        return mockRequest;
    }

    protected String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
