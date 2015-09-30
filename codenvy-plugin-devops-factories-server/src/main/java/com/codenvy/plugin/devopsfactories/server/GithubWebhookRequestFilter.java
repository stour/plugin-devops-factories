/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.plugin.devopsfactories.server;

import org.everrest.core.Filter;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.RequestFilter;

import javax.ws.rs.Path;
import java.net.URI;
import java.util.List;

/**
 * @author stour
 */
@Path("devops/*")
@Filter
public class GithubWebhookRequestFilter implements RequestFilter {

    /**
     * Can modify original request.
     *
     * @param request the request
     */
    @Override
    public void doFilter(GenericContainerRequest request) {
        List<String> githubEventHeader = request.getRequestHeader("X-GitHub-Event");
        if (githubEventHeader.size() > 0) {
            String eventType = githubEventHeader.get(0);
            if ("push".equals(eventType)) {
                // do nothing - githubPushWebhook will be called
            } else if ("pull_request".equals(eventType)) {
                // call githubPullRequestWebhook
                URI uri = request.getRequestUri();
                String path = uri.getPath();
                String sanitizedPath = (path.charAt(path.length()-1) == '/' ? path.substring(0, path.length()-2) : path);
                String newPath = sanitizedPath.substring(0, sanitizedPath.lastIndexOf('/')) + "pullrequest";
                request.setUris(URI.create(newPath), request.getBaseUri());
            }
        }
    }
}
