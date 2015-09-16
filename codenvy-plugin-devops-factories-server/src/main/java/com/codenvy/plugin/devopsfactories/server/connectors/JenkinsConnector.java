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
package com.codenvy.plugin.devopsfactories.server.connectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Created by stour on 09/09/15.
 */
public class JenkinsConnector implements Connector {

    private static final Logger LOG = LoggerFactory.getLogger(JenkinsConnector.class);

    private final String jobName;
    private final String jobConfigXmlUrl;

    public JenkinsConnector(final String url, final String jobName) {
        this.jobName = jobName;
        this.jobConfigXmlUrl = url + "/job/" + jobName + "/config.xml";
    }

    @Override
    public void addFactoryLink(String factoryUrl) {
        String jobConfigXml = getCurrentJenkinsJobConfiguration();
        if (jobConfigXml != null) {
            if (!jobConfigXml.contains(factoryUrl)) {
                updateJenkinsJobDescription(factoryUrl, jobConfigXml);
            } else {
                LOG.info("factory link " + factoryUrl + " already displayed on description of Jenkins job " + jobName);
            }
        }
    }

    private String getCurrentJenkinsJobConfiguration() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(jobConfigXmlUrl);
        Invocation.Builder builder = target.request(APPLICATION_XML);
        Response response = builder.get();
        if (response.getStatus() == 200) {
            String responseString = response.readEntity(String.class);
            return responseString;
        } else {
            LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
            return null;
        }
    }

    private void updateJenkinsJobDescription(String factoryUrl, String jobConfigXml) {
        Client client = ClientBuilder.newClient();

        String updatedJobConfigXml = jobConfigXml.replaceFirst(
                "(<description\\s?/>)|(<description></description>)", "<description>" + factoryUrl + "</description>");
        WebTarget target = client.target(jobConfigXmlUrl);
        Invocation.Builder builder = target.request(APPLICATION_XML).header(HttpHeaders.CONTENT_TYPE, APPLICATION_XML);
        Response response = builder.post(Entity.xml(updatedJobConfigXml));

        if (response.getStatus() == 200) {
            LOG.info("factory link " + factoryUrl + " successfully added on description of Jenkins job " + jobName);
        } else {
            LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
        }
    }
}

