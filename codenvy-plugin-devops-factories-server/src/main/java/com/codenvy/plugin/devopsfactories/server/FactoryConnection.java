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

import com.google.common.collect.Maps;
import org.eclipse.che.api.auth.AuthenticationService;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.core.*;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.FactoryService;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.dto.server.DtoFactory;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.internal.MultiPartReaderClientSide;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.UriBuilder.fromUri;

/**
 * Created by stour on 10/09/15.
 */
public class FactoryConnection {

    private static final Logger LOG = LoggerFactory.getLogger(FactoryConnection.class);

    private final String baseUrl;
    private Optional<Token> userToken;

    @Inject
    public FactoryConnection(@Named("api.endpoint") String baseUrl) {
        this.baseUrl = baseUrl;

        Pair<String, String> credentials = DevopsFactoriesService.getCredentials();
        userToken = Optional.ofNullable(authenticateUser(credentials.first, credentials.second));
    }

    protected Token authenticateUser(String username, String password) {
        Token userToken = null;
        // Authenticate on Codenvy
        String url = fromUri(baseUrl).path(AuthenticationService.class).path(AuthenticationService.class, "authenticate")
                .build().toString();
        try {
            String myCredentials = "{ \"username\": \"" + username + "\", \"password\": \"" + password + "\" }";
            userToken = HttpJsonHelper.post(Token.class, url, DtoFactory.getInstance().createDtoFromJson(myCredentials, Credentials.class));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (ServerException e) {
            LOG.error(e.getMessage(), e);
        } catch (UnauthorizedException e) {
            LOG.error(e.getMessage(), e);
        } catch (ForbiddenException e) {
            LOG.error(e.getMessage(), e);
        } catch (NotFoundException e) {
            LOG.error(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            if (userToken != null) {
                LOG.debug("successfully authenticated with token " + userToken);
            }
            return userToken;
        }
    }

    public List<Factory> findMatchingFactories(String factoryName) {
        List<Link> factoryLinks = null;
        Pair factoryNameParam = Pair.of("project.name", factoryName);

        // Check if factories exist for the given attributes
        String url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "getFactoryByAttribute")
                .build().toString();
        Link lUrl = DtoFactory.newDto(Link.class).withHref(url).withMethod("GET");
        try {
            if (userToken.isPresent()) {
                Token token = userToken.get();
                Pair tokenParam = Pair.of("token", token.getValue());
                factoryLinks = HttpJsonHelper.requestArray(Link.class, lUrl, factoryNameParam, tokenParam);
            } else {
                factoryLinks = HttpJsonHelper.requestArray(Link.class, lUrl, factoryNameParam);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (ServerException e) {
            LOG.error(e.getMessage(), e);
        } catch (UnauthorizedException e) {
            LOG.error(e.getMessage(), e);
        } catch (ForbiddenException e) {
            LOG.error(e.getMessage(), e);
        } catch (NotFoundException e) {
            LOG.error(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.error(e.getMessage(), e);
        }

        if (factoryLinks != null) {
            // Get factories by IDs
            ArrayList<Factory> factories = new ArrayList<>();

            LOG.debug("findMatchingFactories() found " + factoryLinks.size() + " factories");
            for (Link link : factoryLinks) {
                String href = link.getHref();
                String[] hrefSplit = href.split("/");
                String factoryId = hrefSplit[hrefSplit.length - 1];

                String url1 = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "getFactory")
                        .build(factoryId).toString();
                LOG.debug("getFactory: " + url1);

                try {
                    Factory factory;
                    if (userToken.isPresent()) {
                        Token token = userToken.get();
                        Pair tokenParam = Pair.of("token", token.getValue());
                        factory = HttpJsonHelper.get(Factory.class, url1, tokenParam);
                    } else {
                        factory = HttpJsonHelper.get(Factory.class, url1);
                    }
                    factories.add(factory);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                } catch (ServerException e) {
                    LOG.error(e.getMessage(), e);
                } catch (UnauthorizedException e) {
                    LOG.error(e.getMessage(), e);
                } catch (ForbiddenException e) {
                    LOG.error(e.getMessage(), e);
                } catch (NotFoundException e) {
                    LOG.error(e.getMessage(), e);
                } catch (ConflictException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            LOG.debug("findMatchingFactories() returned " + factories.size() + " factories");
            return factories;
        }

        return null;
    }

    public Factory updateFactory(Factory oldFactory, String commidId) {
        // Get current factory data
        final String factoryId = oldFactory.getId();
        final String factoryV = oldFactory.getV();
        final NewProject project = oldFactory.getProject();
        final String sourceType = oldFactory.getSource().getProject().getType();
        final String sourceLocation = oldFactory.getSource().getProject().getLocation();

        // Build new factory object with updated commitId
        Map<String, String> projectParams = oldFactory.getSource().getProject().getParameters();
        projectParams.put("commitId", commidId);
        ImportSourceDescriptor updatedSourceProject = DtoFactory.newDto(ImportSourceDescriptor.class).withType(sourceType)
                .withLocation(sourceLocation).withParameters(projectParams);
        Source updatedSource = DtoFactory.newDto(Source.class).withProject(updatedSourceProject);
        Factory updatedFactory = DtoFactory.newDto(Factory.class).withV(factoryV).withSource(updatedSource).withProject(project);

        // Update factory
        String url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "updateFactory")
                .build(factoryId).toString();

        Factory newFactory = null;
        try {
            if (userToken.isPresent()) {
                Token token = userToken.get();
                Pair tokenParam = Pair.of("token", token.getValue());
                newFactory = HttpJsonHelper.put(Factory.class, url, updatedFactory, tokenParam);
            } else {
                newFactory = HttpJsonHelper.put(Factory.class, url, updatedFactory);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (ServerException e) {
            LOG.error(e.getMessage(), e);
        } catch (UnauthorizedException e) {
            LOG.error(e.getMessage(), e);
        } catch (ForbiddenException e) {
            LOG.error(e.getMessage(), e);
        } catch (NotFoundException e) {
            LOG.error(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.error(e.getMessage(), e);
        }
        return newFactory;
    }

    public Factory createNewFactory(String name, String sourceLocation, String branch, String commitId) {
        // Build new factory object
        Map<String, String> projectParams = Maps.newHashMap();
        projectParams.put("branch", branch);
        projectParams.put("commitId", commitId);
        ImportSourceDescriptor sourceProject = DtoFactory.newDto(ImportSourceDescriptor.class).withType("git")
                .withLocation(sourceLocation).withParameters(projectParams);
        Source source = DtoFactory.newDto(Source.class).withProject(sourceProject);
        BuildersDescriptor builders = DtoFactory.newDto(BuildersDescriptor.class).withDefault("maven");
        NewProject project = DtoFactory.newDto(NewProject.class).withName(name).withVisibility("public")
                .withType("Maven").withBuilders(builders);
        Factory postFactory = DtoFactory.newDto(Factory.class).withV("2.1").withSource(source).withProject(project);

        // Create factory
        String url;
        if (userToken.isPresent()) {
            Token token = userToken.get();
            url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "saveFactory")
                    .queryParam("token", token.getValue()).build().toString();
        } else {
            url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "saveFactory").build().toString();
        }

        Factory newFactory = null;

        String postFactoryString = DtoFactory.getInstance().toJson(postFactory);
        FormDataMultiPart formDataMultiPart = new FormDataMultiPart().field("factoryUrl", postFactoryString);
        Client client = ClientBuilder.newClient()
                .register(MultiPartWriter.class).register(MultiPartReaderClientSide.class);
        WebTarget target = client.target(url);
        Invocation.Builder builder = target.request(APPLICATION_JSON).header(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA);
        Response response = builder.buildPost(Entity.entity(formDataMultiPart, MULTIPART_FORM_DATA)).invoke();

        if (response.getStatus() == 200) {
            String responseString = response.readEntity(String.class);
            newFactory = DtoFactory.getInstance().createDtoFromJson(responseString, Factory.class);
        }  else {
            LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
        }

        return newFactory;
    }

    public static Optional<String> getFactoryUrl(final List<Link> factoryLinks) {
        List<Link> createProjectLinks = factoryLinks.stream()
                .filter(link -> "create-project".equals(link.getRel())).collect(Collectors.toList());
        return Optional.of(createProjectLinks.get(0).getHref());
    }
}
