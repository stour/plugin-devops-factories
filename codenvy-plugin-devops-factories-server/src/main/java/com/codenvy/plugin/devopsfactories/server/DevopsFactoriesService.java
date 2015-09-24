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
import com.codenvy.plugin.devopsfactories.shared.PushEvent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.commons.lang.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Api(value = "/devops",
        description = "DevOps factories manager")
@Path("/devops/{ws-id}")
public class DevopsFactoriesService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(DevopsFactoriesService.class);
    private static final String CONNECTORS_PROPERTIES_FILENAME = "connectors.properties";
    private static final String CREDENTIALS_PROPERTIES_FILENAME = "credentials.properties";

    private final FactoryConnection factoryConnection;

    @Inject
    public DevopsFactoriesService(final FactoryConnection factoryConnection) {
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
                                  @Description("descriptor of contribution") PushEvent contribution)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException {

        LOG.debug("contribution.getRef(): " + contribution.getRef()
                + ", contribution.getRepository().getHtmlUrl(): " + contribution.getRepository().getHtmlUrl()
                + ", contribution.getAfter(): " + contribution.getAfter());

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

        List<Factory> factories = factoryConnection.findMatchingFactories(factoryName);

        Optional<Factory> factory = Optional.empty();
        if (factories != null) {
            if (factories.size() == 1) {
                // Update existing factory
                Factory oldFactory = factories.get(0);
                LOG.debug("factoryConnection.updateFactory(" + oldFactory + ", " + commitId + ")");
                factory = Optional.ofNullable(factoryConnection.updateFactory(oldFactory, commitId));

            } else if (factories.size() == 0) {
                // Generate new factory
                LOG.debug("factoryConnection.createNewFactory(" + factoryName + ", " + repositoryHtmlUrl + ", " + branch + ", " + commitId + ")");
                factory = Optional.ofNullable(factoryConnection.createNewFactory(factoryName, repositoryHtmlUrl, branch, commitId));

            } else {
                LOG.error("findMatchingFactories(" + factoryName + ") found more than 1 factory !");
            }
        }

        factory.ifPresent(f -> {
            List<Link> factoryLinks = f.getLinks();
            Optional<String> factoryUrl = FactoryConnection.getFactoryUrl(factoryLinks);

            // Get connectors & provide them factory link to display
            List<Connector> connectors = getConnectors(factoryName);
            factoryUrl.ifPresent(
                    url -> connectors.forEach(connector -> connector.addFactoryLink(url)));
        });
        return Response.ok().build();
    }

    /**
     * description of connectors in properties file is:
     *      Jenkins connector: [connector-name]=[factory-name],[connector-type],[jenkins-url],[jenkins-job-name]
     *      JIRA connector: [connector-name]=[factory-name],[connector-type],[jira-url],[issue-id]
     *
     * @param factoryName
     * @return the list of all connectors contained in properties file {@link CONNECTORS_PROPERTIES_FILENAME}
     */
    protected static List<Connector> getConnectors(String factoryName) {
        List<Connector> connectors = new ArrayList<>();
        Optional<Properties> connectorsProperties = Optional.ofNullable(getProperties(CONNECTORS_PROPERTIES_FILENAME));
        connectorsProperties.ifPresent(properties -> {
            Set<String> keySet = properties.stringPropertyNames();
            keySet.stream()
                    .filter(key -> factoryName.equals(properties.getProperty(key).split(",")[0]))
                    .forEach(key -> {
                        String value = properties.getProperty(key);
                        String[] valueSplit = value.split(",");
                        switch (valueSplit[1]) {
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
