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
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.ide.rest.RestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by stour on 10/09/15.
 */
public class FactoryConnection {

    private static final Logger LOG = LoggerFactory.getLogger(FactoryConnection.class);

    private final String baseUrl;
    private final Client client;

    @Inject
    public FactoryConnection(@RestContext String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = ClientBuilder.newClient();
    }

    public List<Factory> findMatchingFactories(String factoryName) {
        Pair attribute = Pair.of("project.name", factoryName);

        // Check if factories exist for the given attributes
        WebTarget target = client.target(baseUrl + "/factory/find?" + attribute.first + "=" + attribute.second);
        Response response = target.request(MediaType.APPLICATION_JSON).get();

        List<Link> factoryLinks = null;
        if (response.getStatus() == 200) {
            String responseString = response.readEntity(String.class);
            factoryLinks = DtoFactory.getInstance().createListDtoFromJson(responseString, Link.class);
        } else {
            LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
        }

        // Get factories by IDs
        ArrayList<Factory> factories = new ArrayList<>();

        if (factoryLinks != null) {
            for (Link link : factoryLinks) {
                String href = link.getHref();
                String[] hrefSplit = href.split("/");
                String factoryId = hrefSplit[hrefSplit.length-1];

                WebTarget targetGet = client.target(baseUrl + "/factory/" + factoryId);
                Response responseGet = targetGet.request(MediaType.APPLICATION_JSON).get();

                if (responseGet.getStatus() == 200) {
                    String responseString = responseGet.readEntity(String.class);
                    Factory factory = DtoFactory.getInstance().createDtoFromJson(responseString, Factory.class);
                    factories.add(factory);
                } else {
                    LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
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
        WebTarget target = client.target(baseUrl + "/factory/" + factoryId);
        Response response = target.request(MediaType.APPLICATION_JSON)
                .put(Entity.json(DtoFactory.getInstance().toJson(updatedFactory)));

        Factory newFactory = null;
        if (response.getStatus() == 200) {
            String responseString = response.readEntity(String.class);
            newFactory = DtoFactory.getInstance().createDtoFromJson(responseString, Factory.class);
        } else {
            LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
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
        WebTarget target = client.target(baseUrl + "/factory");
        Response response = target.request(MediaType.APPLICATION_JSON)
                .post(Entity.json(DtoFactory.getInstance().toJson(postFactory)));

        Factory newFactory = null;
        if (response.getStatus() == 200) {
            String responseString = response.readEntity(String.class);
            newFactory = DtoFactory.getInstance().createDtoFromJson(responseString, Factory.class);
        }  else {
            LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
        }
        return newFactory;
    }
}
