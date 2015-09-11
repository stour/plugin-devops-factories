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
import org.eclipse.che.api.core.*;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.LinkParameter;
import org.eclipse.che.api.factory.FactoryService;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.UriBuilder.fromUri;

/**
 * Created by stour on 10/09/15.
 */
public class FactoryConnection {

    private static final Logger LOG = LoggerFactory.getLogger(FactoryConnection.class);

    @Inject
    @Named("api.endpoint")
    protected String apiEndPoint;

    public FactoryConnection() {

    }

    public List<Factory> findMatchingFactories(String factoryName) {
        Pair factoryNameParam = Pair.of("project.name", factoryName);

        // Check if factories exist for the given attributes
        String url = fromUri(apiEndPoint).path(FactoryService.class).path(FactoryService.class, "getFactoryByAttribute")
                .build().toString();
        LOG.info("getFactoryByAttribute: " + url);
        List<Link> factoryLinks = null;
        Link lUrl = DtoFactory.newDto(Link.class).withHref(url).withMethod("GET");
        try {
            factoryLinks = HttpJsonHelper.requestArray(Link.class, lUrl, factoryNameParam);
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

        // Get factories by IDs
        ArrayList<Factory> factories = new ArrayList<>();

        if (factoryLinks != null) {
            for (Link link : factoryLinks) {
                String href = link.getHref();
                String[] hrefSplit = href.split("/");
                String factoryId = hrefSplit[hrefSplit.length-1];

                String url1 = fromUri(apiEndPoint).path(FactoryService.class).path(FactoryService.class, "getFactory")
                        .build(factoryId).toString();
                LOG.info("getFactory: " + url1);

                try {
                    Factory factory = HttpJsonHelper.get(Factory.class, url1);
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
        }
        return factories;
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
        String url = fromUri(apiEndPoint).path(FactoryService.class).path(FactoryService.class, "updateFactory")
                .build(factoryId).toString();

        Factory newFactory = null;
        try {
            newFactory = HttpJsonHelper.put(Factory.class, url, updatedFactory);
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
        String url = fromUri(apiEndPoint).path(FactoryService.class).path(FactoryService.class, "saveFactory")
                .build().toString();

        Factory newFactory = null;
        try {
            newFactory = HttpJsonHelper.post(Factory.class, url, postFactory);
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
}
