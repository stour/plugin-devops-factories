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

import com.codenvy.plugin.devopsfactories.server.connectors.Connector;
import com.codenvy.plugin.devopsfactories.server.connectors.JenkinsConnector;
import com.codenvy.plugin.devopsfactories.server.webhook.GithubWebhook;
import com.codenvy.plugin.devopsfactories.shared.PullRequestEvent;
import com.codenvy.plugin.devopsfactories.shared.PushEvent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;

@Api(value = "/devops",
     description = "DevOps factories manager")
@Path("/devops/{ws-id}")
public class DevopsFactoriesService extends Service {

    private static final Logger LOG                             = LoggerFactory.getLogger(DevopsFactoriesService.class);
    private static final String CONNECTORS_PROPERTIES_FILENAME  = "connectors.properties";
    private static final String CREDENTIALS_PROPERTIES_FILENAME = "credentials.properties";
    private static final String WEBHOOKS_PROPERTIES_FILENAME    = "webhooks.properties";

    private final AuthConnection authConnection;
    private final FactoryConnection factoryConnection;

    @Inject
    public DevopsFactoriesService(final AuthConnection authConnection, final FactoryConnection factoryConnection) {
        this.authConnection = authConnection;
        this.factoryConnection = factoryConnection;
    }

    @ApiOperation(value = "Notify a new contribution on a GitHub project",
                  response = Response.class)
    @ApiResponses({@ApiResponse(
            code = 200,
            message = "OK"
    ), @ApiResponse(
            code = 500,
            message = "Internal Server Error"
    )})
    @POST
    @Path("/github-webhook")
    @Consumes(APPLICATION_JSON)
    public Response githubWebhook(@ApiParam(value = "ID of workspace to consider", required = true)
                                  @PathParam("ws-id") String workspace,
                                  @ApiParam(value = "New contribution", required = true)
                                  @Context HttpServletRequest request)
            throws ServerException {

        LOG.info("githubWebhook");

        Response response;
        String githubHeader = request.getHeader("X-GitHub-Event");
        switch (githubHeader) {
            case "push":
                try {
                    ServletInputStream is = request.getInputStream();
                    PushEvent event = DtoFactory.getInstance().createDtoFromJson(is, PushEvent.class);
                    response = handlePushEvent(event);
                } catch (IOException e) {
                    LOG.error(e.getLocalizedMessage());
                    throw new ServerException(e.getLocalizedMessage());
                }
                break;
            case "pull_request":
                try {
                    ServletInputStream is = request.getInputStream();
                    PullRequestEvent event = DtoFactory.getInstance().createDtoFromJson(is, PullRequestEvent.class);
                    response = handlePullRequestEvent(event);
                } catch (IOException e) {
                    LOG.error(e.getLocalizedMessage());
                    throw new ServerException(e.getLocalizedMessage());
                }
                break;
            default:
                response = Response.status(NOT_IMPLEMENTED).build();
                break;
        }
        return response;
    }

    protected Response handlePushEvent(PushEvent contribution) throws ServerException {
        LOG.info("handlePushEvent");
        LOG.info("contribution.ref: " + contribution.getRef());
        LOG.info("contribution.repository.full_name: " + contribution.getRepository().getFull_name());
        LOG.info("contribution.repository.created_at: " + contribution.getRepository().getCreated_at());
        LOG.info("contribution.repository.html_url: " + contribution.getRepository().getHtml_url());
        LOG.info("contribution.after: " + contribution.getAfter());

        Response response;

        Pair<String,String> credentials = getCredentials();
        Token token = authConnection.authenticateUser(credentials.first,  credentials.second);

        // Get contribution data
        final String contribRepositoryHtmlUrl = contribution.getRepository().getHtml_url();
        final String[] contribRefSplit = contribution.getRef().split("/");
        final String contribBranch = contribRefSplit[contribRefSplit.length - 1];
        final String contribCommitId = contribution.getAfter();

        final List<String> factoryIDs = getFactoryIDsFromWebhook(contribRepositoryHtmlUrl);
        Optional<Factory> factory = Optional.ofNullable(getFactoryForBranch(factoryIDs, contribBranch, token));

        if (factory.isPresent()) {
            Factory f = factory.get();
            // Update factory with new commitId
            Optional<Factory> updatedFactory = Optional.ofNullable(factoryConnection.updateFactory(f, null, null, contribCommitId, token));

            if (updatedFactory.isPresent()) {
                Factory uf = updatedFactory.get();
                List<Link> factoryLinks = uf.getLinks();
                Optional<String> factoryUrl = FactoryConnection.getFactoryUrl(factoryLinks);

                if (factoryUrl.isPresent()) {
                    String url = factoryUrl.get();
                    // Display factory link within third-party services (using connectors configured for the factory)
                    List<Connector> connectors = getConnectors(uf.getId());
                    connectors.forEach(connector -> connector.addFactoryLink(url));
                    response = Response.ok().build();
                } else {
                    GenericEntity entity = new GenericEntity("Updated factory do not contain mandatory create-project link", String.class);
                    response = Response.accepted(entity).build();
                }
            } else {
                GenericEntity entity = new GenericEntity("Factory not updated with commit " + contribCommitId, String.class);
                response = Response.accepted(entity).build();
            }
        } else {
            GenericEntity entity = new GenericEntity("No factory found for branch " + contribBranch, String.class);
            response = Response.accepted(entity).build();
        }
        return response;
    }

    protected Response handlePullRequestEvent(PullRequestEvent prEvent) throws ServerException {

        LOG.info("handlePullRequestEvent");
        LOG.info("pull_request.head.repository.html_url: " + prEvent.getPull_request().getHead().getRepo().getHtml_url());
        LOG.info("pull_request.head.ref: " + prEvent.getPull_request().getHead().getRef());
        LOG.info("pull_request.head.sha: " + prEvent.getPull_request().getHead().getSha());
        LOG.info("pull_request.base.repository.html_url: " + prEvent.getPull_request().getBase().getRepo().getHtml_url());
        LOG.info("pull_request.base.ref: " + prEvent.getPull_request().getBase().getRef());

        Response response;

        Pair<String,String> credentials = getCredentials();
        Token token = authConnection.authenticateUser(credentials.first,  credentials.second);

        String action = prEvent.getAction();
        if ("closed".equals(action)) {
            boolean isMerged = prEvent.getPull_request().getMerged();
            if (isMerged) {
                final String prHeadBranch = prEvent.getPull_request().getHead().getRef();
                final String prHeadCommitId = prEvent.getPull_request().getHead().getSha();

                // Get base repository & branch (values after merge)
                final String prBaseRepositoryHtmlUrl = prEvent.getPull_request().getBase().getRepo().getHtml_url();
                final String prBaseBranch = prEvent.getPull_request().getBase().getRef();

                final List<String> factoryIDs = getFactoryIDsFromWebhook(prBaseRepositoryHtmlUrl);
                Optional<Factory> factory = Optional.ofNullable(getFactoryForBranch(factoryIDs, prHeadBranch, token));

                if (factory.isPresent()) {
                    Factory f = factory.get();
                    // Update factory with origin repository & branch name
                    Optional<Factory> updatedFactory =
                            Optional.ofNullable(factoryConnection.updateFactory(f, prBaseRepositoryHtmlUrl, prBaseBranch, prHeadCommitId, token));
                    if (updatedFactory.isPresent()) {
                        LOG.info("Factory successfully updated with branch " + prBaseBranch + " at commit " + prHeadCommitId);
                        // TODO Remove factory from Github webhook
                        response = Response.ok().build();
                    } else {
                        GenericEntity entity =
                                new GenericEntity("Factory not updated with branch " + prBaseBranch + " & commit " + prHeadCommitId,
                                                  String.class);
                        response = Response.accepted(entity).build();
                    }
                } else {
                    GenericEntity entity = new GenericEntity("No factory found for branch " + prHeadBranch, String.class);
                    response = Response.accepted(entity).build();
                }
            } else {
                GenericEntity entity = new GenericEntity("Pull Request was closed with unmerged commits !", String.class);
                response = Response.accepted(entity).build();
            }
        } else {
            GenericEntity entity = new GenericEntity("PullRequest Event action is " + action + ". We do not handle that.", String.class);
            response = Response.accepted(entity).build();
        }
        return response;
    }

    protected Factory getFactoryForBranch(List<String> factoryIDs, String branch, Token token) throws ServerException {
        Factory factory = null;
        for (String factoryId : factoryIDs) {
            Optional<Factory> obtainedFactory = Optional.ofNullable(factoryConnection.getFactory(factoryId, token));
            if (obtainedFactory.isPresent()) {
                final Factory f = obtainedFactory.get();
                ImportSourceDescriptor project = f.getSource().getProject();
                Optional<String> factoryBranch = Optional.ofNullable(project.getParameters().get("branch"));

                // Test if branch set for the factory corresponds to branch value in contribution event
                if (factoryBranch.isPresent() && factoryBranch.get().equals(branch)) {
                    factory = f;
                    break;
                }
            }
        }
        return factory;
    }

    protected List<String> getFactoryIDsFromWebhook(String repositoryUrl) {
        List<String> factoryIDs = Collections.emptyList();
        List<GithubWebhook> webhooks = getWebhooks();
        for (GithubWebhook webhook : webhooks) {
            // Search for a webhook configured for same repository as contribution data
            String webhookRepositoryUrl = webhook.getRepositoryUrl();
            if (repositoryUrl.equals(webhookRepositoryUrl)) {
                factoryIDs = Arrays.asList(webhook.getFactoryIDs());
            }
        }
        return factoryIDs;
    }

    /**
     * Description of webhooks in properties file is:
     * GitHub webhook: [webhook-name]=[webhook-type],[repository-url],[factory-id];[factory-id];...;[factory-id]
     *
     * @return the list of all webhooks contained in properties file {@link WEBHOOKS_PROPERTIES_FILENAME}
     */
    protected static List<GithubWebhook> getWebhooks() {
        List<GithubWebhook> webhooks = new ArrayList<>();
        Optional<Properties> webhooksProperties = Optional.ofNullable(getProperties(WEBHOOKS_PROPERTIES_FILENAME));
        webhooksProperties.ifPresent(properties -> {
            Set<String> keySet = properties.stringPropertyNames();
            keySet.stream().forEach(key -> {
                String value = properties.getProperty(key);
                String[] valueSplit = value.split(",");
                switch (valueSplit[0]) {
                    case "github":
                        String[] factoriesIDs = valueSplit[2].split(";");
                        GithubWebhook githubWebhook = new GithubWebhook(valueSplit[1], factoriesIDs);
                        webhooks.add(githubWebhook);
                        LOG.debug("new GithubWebhook(" + valueSplit[1] + ", " + Arrays.toString(factoriesIDs) + ")");
                        break;
                    default:
                        break;
                }
            });
        });
        return webhooks;
    }

    /**
     * Description of connectors in properties file is:
     * Jenkins connector: [connector-name]=[connector-type],[factory-id],[jenkins-url],[jenkins-job-name]
     *
     * @param factoryId
     * @return the list of all connectors contained in properties file {@link CONNECTORS_PROPERTIES_FILENAME}
     */
    protected static List<Connector> getConnectors(String factoryId) {
        List<Connector> connectors = new ArrayList<>();
        Optional<Properties> connectorsProperties = Optional.ofNullable(getProperties(CONNECTORS_PROPERTIES_FILENAME));
        connectorsProperties.ifPresent(properties -> {
            Set<String> keySet = properties.stringPropertyNames();
            keySet.stream()
                  .filter(key -> factoryId.equals(properties.getProperty(key).split(",")[1]))
                  .forEach(key -> {
                      String value = properties.getProperty(key);
                      String[] valueSplit = value.split(",");
                      switch (valueSplit[0]) {
                          case "jenkins":
                              JenkinsConnector jenkinsConnector = new JenkinsConnector(valueSplit[2], valueSplit[3]);
                              connectors.add(jenkinsConnector);
                              LOG.debug("new JenkinsConnector(" + valueSplit[2] + ", " + valueSplit[3] + ")");
                              break;
                          case "jira":
                              LOG.debug("Object JIRA connector not implemented !");
                              break;
                          default:
                              break;
                      }
                  });
        });
        return connectors;
    }

    protected static Pair<String, String> getCredentials() {
        String[] credentials = new String[2];
        Optional<Properties> credentialsProperties = Optional.ofNullable(getProperties(CREDENTIALS_PROPERTIES_FILENAME));
        if (credentialsProperties.isPresent()) {
            Set<String> keySet = credentialsProperties.get().stringPropertyNames();
            keySet.forEach(key -> {
                String value = credentialsProperties.get().getProperty(key);
                switch (key) {
                    case "username":
                        credentials[0] = value;
                        break;
                    case "password":
                        credentials[1] = value;
                        break;
                    default:
                        break;
                }
            });
        }
        return Pair.of(credentials[0], credentials[1]);
    }

    protected static Properties getProperties(String fileName) {
        java.nio.file.Path currentRelativePath = Paths.get("", fileName);
        String currentRelativePathString = currentRelativePath.toAbsolutePath().toString();
        Optional<URL> configPath = Optional.empty();
        try {
            configPath = Optional.ofNullable(new File(currentRelativePathString).toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (configPath.isPresent()) {
            Optional<InputStream> is = Optional.empty();
            try {
                is = Optional.of(configPath.get().openStream());
                if (is.isPresent()) {
                    Properties properties = new Properties();
                    properties.load(is.get());
                    return properties;
                }
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            } finally {
                try {
                    if (is.isPresent()) {
                        is.get().close();
                    }
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        return null;
    }
}
