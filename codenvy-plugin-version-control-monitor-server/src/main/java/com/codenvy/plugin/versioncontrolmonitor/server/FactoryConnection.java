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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.UriBuilder.fromUri;

/**
 * Wrapper class for calls to Codenvy factory REST API
 *
 * @author Stephane Tournie
 */
public class FactoryConnection {

    private static final Logger LOG = LoggerFactory.getLogger(FactoryConnection.class);

    private final String baseUrl;

    @Inject
    public FactoryConnection(@Named("api.endpoint") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Get a given factory
     *
     * @param factoryId
     *         the id of the factory
     * @param userToken
     *         the authentication token to use against the Codenvy API
     * @return the expected factory or null if an error occurred during the call to 'getFactory'
     * @throws ServerException
     */
    public Factory getFactory(String factoryId, Token userToken) throws ServerException {
        String url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "getFactory")
                                     .build(factoryId).toString();
        LOG.debug("getFactory: " + url);

        Factory factory;
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

    /**
     * Find factories with given name
     *
     * @param factoryName
     *         the factory name to match
     * @param userToken
     *         the authentication token to use against the Codenvy API
     * @return the list of factories that match or null if an error occurred during the REST calls
     * @throws ServerException
     */
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

    /**
     * Update a given factory
     *
     * @param factory
     *         the factory to update
     * @param userToken
     *         the authentication token to use against the Codenvy API
     * @return the updated factory or null if an error occurred during the call to 'updateFactory'
     * @throws ServerException
     */
    public Factory updateFactory(Factory factory, Token userToken) throws ServerException {
        final String factoryId = factory.getId();
        final String url = fromUri(baseUrl).path(FactoryService.class).path(FactoryService.class, "updateFactory")
                                           .build(factoryId).toString();

        Factory newFactory;
        try {
            if (userToken != null) {
                final Pair tokenParam = Pair.of("token", userToken.getValue());
                newFactory = HttpJsonHelper.put(Factory.class, url, factory, tokenParam);
            } else {
                newFactory = HttpJsonHelper.put(Factory.class, url, factory);
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

    /**
     * Create a new factory with given project data
     *
     * @param name
     *         the name of the project
     * @param sourceLocation
     *         the repository location to set in project
     * @param commitId
     *         the commitId to set in project source storage
     * @param userToken
     *         the authentication token to use against the Codenvy API
     * @return the freshly created factory or null if an error occurred during the call to 'saveFactory'
     * @throws ServerException
     */
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

    /**
     * Get value of a given factory link
     *
     * @param factoryLinks
     *         the factory links
     * @param rel
     *         the name of the link to get
     * @return the value of given factory link if exist, an empty {@link Optional} otherwise
     */
    public static Optional<String> getFactoryUrl(final List<Link> factoryLinks, String rel) {
        List<Link> createProjectLinks = factoryLinks.stream()
                                                    .filter(link -> rel.equals(link.getRel())).collect(Collectors.toList());
        if (!createProjectLinks.isEmpty()) {
            return Optional.of(createProjectLinks.get(0).getHref());
        }
        return Optional.empty();
    }
}
