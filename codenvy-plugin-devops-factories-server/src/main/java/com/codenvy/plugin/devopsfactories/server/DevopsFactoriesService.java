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

import com.codenvy.plugin.devopsfactories.server.connectors.JenkinsConnector;
import com.codenvy.plugin.devopsfactories.shared.PushEvent;
import com.wordnik.swagger.annotations.*;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.dto.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Api(value = "/devops",
        description = "DevOps factories manager")
@Path("/devops/{ws-id}")
public class DevopsFactoriesService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(DevopsFactoriesService.class);

    private final FactoryConnection factoryConnection;
    private final JenkinsConnector jenkinsConnector;

    @Inject
    public DevopsFactoriesService(final FactoryConnection factoryConnection) {
        this.factoryConnection = factoryConnection;

        // TODO add UI to configure + enable/disable new connectors
        jenkinsConnector = new JenkinsConnector("http://runnerp13.codenvycorp.com:61617", "desktop-console-java");
    }

    @ApiOperation(value = "Notify a new contribution on a GitHub project",
            response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Operation is forbidden")})
    @POST
    @Path("/github-webhook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response githubWebhook(@ApiParam(value = "ID of workspace to consider", required = true)
                                  @PathParam("ws-id") String workspace,
                                  @ApiParam(value = "New contribution", required = true)
                                  @Description("descriptor of contribution") PushEvent contribution)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException {

        // TODO remove temporary info logging
        LOG.info("contribution.getRef(): " + contribution.getRef()
                + ", contribution.getRepository(): " + contribution.getRepository()
                + ", contribution.getRepository().getUrl(): " + contribution.getRepository().getUrl()
                + ", contribution.getRepository().getHtmlUrl(): " + contribution.getRepository().getHtmlUrl()
                + ", contribution.getHead(): " + contribution.getAfter());

        final String[] refSplit = contribution.getRef().split("/");
        final String branch = refSplit[refSplit.length - 1];
        // TODO use repository.getHtmlUrl()
        final String repositoryUrl = contribution.getRepository().getUrl();
        final String[] repositoryUrlSplit = repositoryUrl.split("/");
        final String repositoryName = repositoryUrlSplit[repositoryUrlSplit.length - 1];
        String repositoryHtmlUrl = "https://github.com/"
                + repositoryUrlSplit[repositoryUrlSplit.length - 2] + "/" + repositoryUrlSplit[repositoryUrlSplit.length - 1];
        final String commitId = contribution.getAfter();

        final String factoryName = repositoryName + "--" + branch;
        LOG.info("factoryName: " + factoryName);

        List<Factory> factories = factoryConnection.findMatchingFactories(factoryName);

        Factory factory = null;
        if (factories == null) {
            LOG.error("factoryConnection.findMatchingFactories(" + factoryName + ") returned null");
        } else if (factories.size() == 1) {
            // Update existing factory
            Factory oldFactory = factories.get(0);
            LOG.info("factoryConnection.updateFactory(" + oldFactory + ", " + commitId + ")");
            factory = factoryConnection.updateFactory(oldFactory, commitId);

        } else if (factories.size() == 0) {
            // Generate new factory
            LOG.info("factoryConnection.createNewFactory(" + factoryName + ", " + repositoryHtmlUrl + ", " + branch + ", " + commitId + ")");
            factory = factoryConnection.createNewFactory(factoryName, repositoryHtmlUrl, branch, commitId);

        } else {
            LOG.error("findMatchingFactories(" + factoryName + ") found more than 1 factory !");
        }

        if (factory != null) {
            List<Link> factoryLinks = factory.getLinks();
            final String factoryUrl = FactoryConnection.getFactoryUrl(factoryLinks);
            if (factoryUrl != null) {
                jenkinsConnector.addFactoryLink(factoryUrl);
            }
        }
        return Response.ok().build();
    }
}
