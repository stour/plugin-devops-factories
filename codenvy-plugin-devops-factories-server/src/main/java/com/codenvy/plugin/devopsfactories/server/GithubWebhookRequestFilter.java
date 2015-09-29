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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;

/**
 * @author stour
 */

@Provider
@PreMatching
public class GithubWebhookRequestFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String eventType = requestContext.getHeaderString("X-GitHub-Event");
        UriInfo ui = requestContext.getUriInfo();
        String path = ui.getPath(false);
        if ("push".equals(eventType)) {
            // do nothing - githubPushWebhook will be called
        } else if ("pull_request".equals(eventType)) {
            // call githubPullRequestWebhook
            String sanitizedPath = (path.charAt(path.length()-1) == '/' ? path.substring(0, path.length()-2) : path);
            String newPath = sanitizedPath.substring(0, sanitizedPath.lastIndexOf('/')) + "pullrequest";
            requestContext.setRequestUri(URI.create(newPath));
        } else {
            // abort with status 501
            requestContext.abortWith(Response
                    .status(Response.Status.NOT_IMPLEMENTED)
                    .entity("Github " + eventType  + " event aren't yet implemented.")
                    .build());
        }
    }
}
