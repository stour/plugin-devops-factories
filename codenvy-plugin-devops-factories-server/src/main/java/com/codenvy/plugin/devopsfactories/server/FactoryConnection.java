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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.eclipse.che.api.auth.shared.dto.Token;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.server.FactoryService;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.UriBuilder.fromUri;

public class FactoryConnection {

    private static final Logger LOG = LoggerFactory.getLogger(FactoryConnection.class);

    private final String baseUrl;

    @Inject
    public FactoryConnection(@Named("api.endpoint") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Factory getFactory(String factoryId, Token userToken) throws ServerException {
        String url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "getFactory")
                                     .build(factoryId).toString();
        LOG.debug("getFactory: " + url);

        Factory factory = null;
        try {
            if (userToken != null) {
                Pair tokenParam = Pair.of("token", userToken.getValue());
                factory = HttpJsonHelper.get(Factory.class, url, tokenParam);
            } else {
                factory = HttpJsonHelper.get(Factory.class, url);
            }
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
        return factory;
    }

    public List<Factory> findMatchingFactories(String factoryName, Token userToken) throws ServerException {
        List<Link> factoryLinks;
        Pair factoryNameParam = Pair.of("project.name", factoryName);

        // Check if factories exist for the given attributes
        String url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "getFactoryByAttribute")
                                     .build().toString();
        Link lUrl = DtoFactory.newDto(Link.class).withHref(url).withMethod("GET");
        try {
            if (userToken != null) {
                Pair tokenParam = Pair.of("token", userToken.getValue());
                factoryLinks = HttpJsonHelper.requestArray(Link.class, lUrl, factoryNameParam, tokenParam);
            } else {
                factoryLinks = HttpJsonHelper.requestArray(Link.class, lUrl, factoryNameParam);
            }
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

        if (factoryLinks != null) {
            // Get factories by IDs
            ArrayList<Factory> factories = new ArrayList<>();

            for (Link link : factoryLinks) {
                String href = link.getHref();
                String[] hrefSplit = href.split("/");
                String factoryId = hrefSplit[hrefSplit.length - 1];

                Optional<Factory> factory = Optional.ofNullable(getFactory(factoryId, userToken));
                factory.ifPresent(f -> factories.add(f));
            }
            LOG.debug("findMatchingFactories() returned " + factories.size() + " factories");
            return factories;
        }

        return null;
    }

    public Factory updateFactory(Factory oldFactory, String repository, String commitId, Token userToken) throws ServerException {

        if (repository == null) {
            throw new ServerException("\'repository\' cannot be null. This parameter is mandatory to update factory " + oldFactory.getId() + ".");
        }

        if (commitId == null) {
            throw new ServerException("\'commitId\' cannot be null. This parameter is mandatory to update factory " + oldFactory.getId() + ".");
        }

        WorkspaceConfigDto workspace = oldFactory.getWorkspace();
        // Find project that matches factory data
        final List<ProjectConfigDto> projects = workspace.getProjects()
                                                 .stream()
                                                 .filter(projectConfig ->
                                                                 projectConfig.getSource() != null
                                                                 && !isNullOrEmpty(projectConfig.getSource().getType())
                                                                 && !isNullOrEmpty(projectConfig.getSource().getLocation())
                                                                 && projectConfig.getSource().getLocation().equals(
                                                                         repository))
                                                 .collect(toList());

        if (projects.size() == 0) {
            throw new ServerException(
                    "Factory " + oldFactory.getId() + " contains no project that matches source location " + repository + ".");
        } else if (projects.size() > 1) {
            throw new ServerException(
                    "Factory " + oldFactory.getId() + " contains several projects that match source location " + repository + ".");
        }

        // Get current factory data
        ProjectConfigDto project = projects.get(0);
        final SourceStorageDto source = project.getSource();
        Map<String, String> projectParams = source.getParameters();

        // Build new factory object with updated values
        projectParams.put("commitId", commitId);
        source.setParameters(projectParams);
        project.setSource(source);
        // Replace existing project with updated one
        projects.removeIf(p -> p.getSource().getLocation().equals(repository));
        projects.add(project);
        workspace.setProjects(projects);
        Factory updatedFactory = oldFactory.withWorkspace(workspace);

        // Update factory
        final String factoryId = updatedFactory.getId();
        String url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "updateFactory")
                                     .build(factoryId).toString();

        Factory newFactory;
        try {
            if (userToken != null) {
                Pair tokenParam = Pair.of("token", userToken.getValue());
                newFactory = HttpJsonHelper.put(Factory.class, url, updatedFactory, tokenParam);
            } else {
                newFactory = HttpJsonHelper.put(Factory.class, url, updatedFactory);
            }
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
        return newFactory;
    }

    public Factory createNewFactory(String name, String sourceLocation, String commitId, Token userToken) throws ServerException {

        // Build new factory object
        Map<String, String> sourceParams = Maps.newHashMap();
        sourceParams.put("commitId", commitId);
        SourceStorageDto source =
                DtoFactory.newDto(SourceStorageDto.class).withType("git").withLocation(sourceLocation).withParameters(sourceParams);
        ProjectConfigDto project = DtoFactory.newDto(ProjectConfigDto.class).withName(name).withType("blank").withSource(source);
        WorkspaceConfigDto workspace = DtoFactory.newDto(WorkspaceConfigDto.class).withProjects(Lists.newArrayList(project));
        Factory factory = DtoFactory.newDto(Factory.class).withV("4.0").withWorkspace(workspace);

        // Create factory
        String url;
        if (userToken != null) {
            url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "saveFactory")
                                  .queryParam("token", userToken.getValue()).build().toString();
        } else {
            url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "saveFactory").build().toString();
        }

        Factory newFactory = null;

        String postFactoryString = DtoFactory.getInstance().toJson(factory);
        FormDataMultiPart formDataMultiPart = new FormDataMultiPart().field("factoryUrl", postFactoryString);
        Client client = ClientBuilder.newClient()
                                     .register(MultiPartWriter.class).register(MultiPartReaderClientSide.class);
        WebTarget target = client.target(url);
        Invocation.Builder builder = target.request(APPLICATION_JSON).header(HttpHeaders.CONTENT_TYPE, MULTIPART_FORM_DATA);
        Response response = builder.buildPost(Entity.entity(formDataMultiPart, MULTIPART_FORM_DATA)).invoke();

        if (response.getStatus() == 200) {
            String responseString = response.readEntity(String.class);
            newFactory = DtoFactory.getInstance().createDtoFromJson(responseString, Factory.class);
        } else {
            LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
        }

        return newFactory;
    }

    public static Optional<String> getFactoryUrl(final List<Link> factoryLinks) {
        List<Link> createProjectLinks = factoryLinks.stream()
                                                    .filter(link -> "create-workspace".equals(link.getRel())).collect(Collectors.toList());
        if (!createProjectLinks.isEmpty()) {
            return Optional.of(createProjectLinks.get(0).getHref());
        } else {
            return Optional.empty();
        }
    }
}
