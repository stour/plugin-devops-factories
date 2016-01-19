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

import com.codenvy.plugin.versioncontrolmonitor.server.connectors.Connector;
import com.codenvy.plugin.versioncontrolmonitor.server.connectors.JenkinsConnector;
import com.codenvy.plugin.versioncontrolmonitor.server.webhook.GithubWebhook;
import com.codenvy.plugin.versioncontrolmonitor.shared.PullRequestEvent;
import com.codenvy.plugin.versioncontrolmonitor.shared.PushEvent;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_IMPLEMENTED;

@Api(value = "/vcmonitor",
     description = "Version Control Monitor")
@Path("/vcmonitor")
public class VersionControlMonitorService extends Service {

    private static final Logger LOG                             = LoggerFactory.getLogger(VersionControlMonitorService.class);
    private static final String CONNECTORS_PROPERTIES_FILENAME  = "connectors.properties";
    private static final String CREDENTIALS_PROPERTIES_FILENAME = "credentials.properties";
    private static final String WEBHOOKS_PROPERTIES_FILENAME    = "webhooks.properties";

    private final AuthConnection    authConnection;
    private final FactoryConnection factoryConnection;

    @Inject
    public VersionControlMonitorService(final AuthConnection authConnection, final FactoryConnection factoryConnection) {
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
    public Response githubWebhook(@ApiParam(value = "New contribution", required = true)
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

        // Authenticate on Codenvy
        final Pair<String, String> credentials = getCredentials();
        final Token token = authConnection.authenticateUser(credentials.first, credentials.second);

        // Get contribution data
        final String contribRepositoryHtmlUrl = contribution.getRepository().getHtml_url();
        final String[] contribRefSplit = contribution.getRef().split("/");
        final String contribBranch = contribRefSplit[contribRefSplit.length - 1];

        // Get webhook configured for given repository
        final Optional<GithubWebhook> webhook = Optional.ofNullable(getWebhook(contribRepositoryHtmlUrl));
        if (!webhook.isPresent()) {
            return Response.accepted(new GenericEntity<>("No webhook configured for repository " + contribRepositoryHtmlUrl, String.class))
                           .build();
        }
        final GithubWebhook w = webhook.get();

        // Get factory id's listed into the webhook
        final List<String> factoryIDs = Arrays.asList(w.getFactoryIDs());

        // Get factory that contains a project for given repository and branch
        final Predicate<ProjectConfigDto> matchingProjectPredicate = (p ->
                                                                              p.getSource() != null
                                                                              && !isNullOrEmpty(p.getSource().getType())
                                                                              && !isNullOrEmpty(p.getSource().getLocation())
                                                                              &&
                                                                              contribRepositoryHtmlUrl.equals(p.getSource().getLocation())
                                                                              || (contribRepositoryHtmlUrl + ".git")
                                                                                         .equals(p.getSource().getLocation())
                                                                                 && "master".equals(contribBranch)
                                                                              || !isNullOrEmpty(p.getSource().getParameters().get("branch"))
                                                                                 && contribBranch.equals(
                                                                                      p.getSource().getParameters().get("branch")));
        final Optional<Factory> factory = Optional.ofNullable(getFactoryThatContainsProject(factoryIDs, matchingProjectPredicate, token));
        if (!factory.isPresent()) {
            return Response.accepted(
                    new GenericEntity<>("No factory found for repository " + contribRepositoryHtmlUrl + " and branch " + contribBranch,
                                        String.class)).build();
        }

        // Get 'open factory' URL
        final Factory f = factory.get();
        final List<Link> factoryLinks = f.getLinks();
        final Optional<String> factoryUrl = FactoryConnection.getFactoryUrl(factoryLinks);
        if (!factoryUrl.isPresent()) {
            return Response.accepted(new GenericEntity<>("Factory do not contain mandatory \'create-workspace\' link", String.class))
                           .build();
        }
        final String url = factoryUrl.get();

        // Get connectors configured for the factory
        final List<Connector> connectors = getConnectors(f.getId());

        // Display factory link within third-party services
        connectors.forEach(connector -> connector.addFactoryLink(url));
        return Response.ok().build();

    }

    protected Response handlePullRequestEvent(PullRequestEvent prEvent) throws ServerException {
        LOG.info("handlePullRequestEvent");
        LOG.info("pull_request.head.repository.html_url: " + prEvent.getPull_request().getHead().getRepo().getHtml_url());
        LOG.info("pull_request.head.ref: " + prEvent.getPull_request().getHead().getRef());
        LOG.info("pull_request.head.sha: " + prEvent.getPull_request().getHead().getSha());
        LOG.info("pull_request.base.repository.html_url: " + prEvent.getPull_request().getBase().getRepo().getHtml_url());
        LOG.info("pull_request.base.ref: " + prEvent.getPull_request().getBase().getRef());

        // Authenticate on Codenvy
        final Pair<String, String> credentials = getCredentials();
        final Token token = authConnection.authenticateUser(credentials.first, credentials.second);

        // Check that event indicates a successful merging
        final String action = prEvent.getAction();
        if (!"closed".equals(action)) {
            return Response
                    .accepted(new GenericEntity<>(
                            "PullRequest Event action is " + action + ". " + this.getClass().getSimpleName() + " do not handle this one.",
                            String.class))
                    .build();
        }
        final boolean isMerged = prEvent.getPull_request().getMerged();
        if (!isMerged) {
            return Response.accepted(new GenericEntity<>("Pull Request was closed with unmerged commits !", String.class)).build();
        }

        // Get head repository data
        final String prHeadRepositoryHtmlUrl = prEvent.getPull_request().getHead().getRepo().getHtml_url();
        final String prHeadBranch = prEvent.getPull_request().getHead().getRef();
        final String prHeadCommitId = prEvent.getPull_request().getHead().getSha();

        // Get base repository data
        final String prBaseRepositoryHtmlUrl = prEvent.getPull_request().getBase().getRepo().getHtml_url();

        // Get webhook configured for given repository
        final Optional<GithubWebhook> webhook = Optional.ofNullable(getWebhook(prBaseRepositoryHtmlUrl));
        if (!webhook.isPresent()) {
            return Response.accepted(new GenericEntity<>("No webhook configured for repository " + prBaseRepositoryHtmlUrl, String.class))
                           .build();
        }
        final GithubWebhook w = webhook.get();

        // Get factory id's listed into the webhook
        final List<String> factoryIDs = Arrays.asList(w.getFactoryIDs());

        // Get factory that contains a project for given repository and branch
        final Predicate<ProjectConfigDto> matchingProjectPredicate = (p ->
                                                                              p.getSource() != null
                                                                              && !isNullOrEmpty(p.getSource().getType())
                                                                              && !isNullOrEmpty(p.getSource().getLocation())
                                                                              && prHeadRepositoryHtmlUrl.equals(p.getSource().getLocation())
                                                                              || (prHeadRepositoryHtmlUrl + ".git")
                                                                                         .equals(p.getSource().getLocation())
                                                                                 && "master".equals(prHeadBranch)
                                                                              || !isNullOrEmpty(p.getSource().getParameters().get("branch"))
                                                                                 && prHeadBranch.equals(
                                                                                      p.getSource().getParameters().get("branch")));
        final Optional<Factory> factory = Optional.ofNullable(getFactoryThatContainsProject(factoryIDs, matchingProjectPredicate, token));
        if (!factory.isPresent()) {
            return Response.accepted(new GenericEntity<>("No factory found for branch " + prHeadBranch, String.class)).build();
        }
        final Factory f = factory.get();

        // Update project into the factory with given repository and branch
        final Factory updatedfactory = updateProjectInFactory(f, matchingProjectPredicate, prBaseRepositoryHtmlUrl, prHeadCommitId);

        // Update factory with new project data
        final Optional<Factory> persistedFactory = Optional.ofNullable(factoryConnection.updateFactory(updatedfactory, token));
        if (!persistedFactory.isPresent()) {
            return Response.accepted(
                    new GenericEntity<>(
                            "Error during update of factory with source location " + prBaseRepositoryHtmlUrl + " & commitId " +
                            prHeadCommitId,
                            String.class)).build();
        }
        LOG.info("Factory successfully updated with source location " + prBaseRepositoryHtmlUrl + " & commitId " + prHeadCommitId);

        // TODO Remove factory id from webhook

        return Response.ok().build();
    }

    /**
     * Get the factory that contains a project matching a predicate
     *
     * @param factoryIDs
     *         Id's of factories to search into
     * @param matchingPredicate
     *         the matching predicate projects must fulfill
     * @param token
     *         the authentication token to use against Codenvy API
     * @return the first factory that contains a project that matches the predicate
     * @throws ServerException
     */
    protected Factory getFactoryThatContainsProject(List<String> factoryIDs, Predicate<ProjectConfigDto> matchingPredicate, Token token)
            throws ServerException {
        Factory factory = null;
        for (String factoryId : factoryIDs) {
            Optional<Factory> obtainedFactory = Optional.ofNullable(factoryConnection.getFactory(factoryId, token));
            if (obtainedFactory.isPresent()) {
                final Factory f = obtainedFactory.get();
                final List<ProjectConfigDto> projects = f.getWorkspace().getProjects()
                                                         .stream()
                                                         .filter(matchingPredicate)
                                                         .collect(toList());
                if (!projects.isEmpty()) {
                    factory = f;
                    break;
                }
            }
        }
        return factory;
    }

    /**
     * Update project matching given predicate in given factory
     *
     * @param factory
     *         the factory to search for projects
     * @param matchingProjectPredicate
     *         the matching predicate project must fulfill
     * @param baseRepository
     *         the repository to set as source location for matching project in factory
     * @param headCommitId
     *         the commitId to set as 'commitId' parameter for matching project in factory
     * @return the project that matches the predicate given in argument
     * @throws ServerException
     */
    protected Factory updateProjectInFactory(Factory factory, Predicate<ProjectConfigDto> matchingProjectPredicate, String baseRepository,
                                             String headCommitId) throws ServerException {
        // Get matching project in factory
        WorkspaceConfigDto workspace = factory.getWorkspace();
        final List<ProjectConfigDto> matchingProjects = workspace.getProjects()
                                                                 .stream()
                                                                 .filter(matchingProjectPredicate)
                                                                 .collect(toList());

        if (matchingProjects.size() == 0) {
            throw new ServerException(
                    "Factory " + factory.getId() + " contains no project for given repository and branch.");
        } else if (matchingProjects.size() > 1) {
            throw new ServerException(
                    "Factory " + factory.getId() + " contains several projects for given repository and branch");
        }
        ProjectConfigDto matchingProject = matchingProjects.get(0);

        // Update repository and commitId
        final SourceStorageDto source = matchingProject.getSource();
        final Map<String, String> projectParams = source.getParameters();
        source.setLocation(baseRepository);
        projectParams.put("commitId", headCommitId);

        // Clean branch parameter if exist
        projectParams.remove("branch");

        // Replace existing project with updated one
        source.setParameters(projectParams);
        matchingProject.setSource(source);

        final List<ProjectConfigDto> factoryProjects = workspace.getProjects();
        factoryProjects.removeIf(p -> matchingProject.getName().equals(p.getName()));
        factoryProjects.add(matchingProject);
        workspace.setProjects(factoryProjects);

        return factory.withWorkspace(workspace);
    }

    protected GithubWebhook getWebhook(String repositoryUrl) throws ServerException {
        List<GithubWebhook> webhooks = getWebhooks();
        GithubWebhook webhook = null;
        for (GithubWebhook w : webhooks) {
            String webhookRepositoryUrl = w.getRepositoryUrl();
            if (repositoryUrl.equals(webhookRepositoryUrl)) {
                webhook = w;
            }
        }
        return webhook;
    }

    /**
     * Description of webhooks in properties file is:
     * GitHub webhook: [webhook-name]=[webhook-type],[repository-url],[factory-id];[factory-id];...;[factory-id]
     *
     * @return the list of all webhooks contained in properties file {@link WEBHOOKS_PROPERTIES_FILENAME}
     */
    protected static List<GithubWebhook> getWebhooks() throws ServerException {
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
    protected static List<Connector> getConnectors(String factoryId) throws ServerException {
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

    protected static Pair<String, String> getCredentials() throws ServerException {
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

    protected static Properties getProperties(String fileName) throws ServerException {
        java.nio.file.Path currentRelativePath = Paths.get("", fileName);
        String currentRelativePathString = currentRelativePath.toAbsolutePath().toString();
        Optional<URL> configPath = Optional.empty();
        try {
            configPath = Optional.ofNullable(new File(currentRelativePathString).toURI().toURL());
        } catch (MalformedURLException e) {
            LOG.error(e.getLocalizedMessage());
            throw new ServerException(e.getLocalizedMessage());
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
