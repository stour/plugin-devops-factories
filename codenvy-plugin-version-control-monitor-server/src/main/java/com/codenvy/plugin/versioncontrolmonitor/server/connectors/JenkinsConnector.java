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
package com.codenvy.plugin.versioncontrolmonitor.server.connectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;

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
        Optional<String> jobConfigXml = Optional.ofNullable(getCurrentJenkinsJobConfiguration());
        jobConfigXml.ifPresent(xml -> {
            Optional<Document> configDocument = Optional.ofNullable(xmlToDocument(xml));
            configDocument.ifPresent(doc -> {
                Element root = doc.getDocumentElement();
                Node descriptionNode = root.getElementsByTagName("description").item(0);

                if (!descriptionNode.getTextContent().contains(factoryUrl)) {
                    updateJenkinsJobDescription(factoryUrl, doc, descriptionNode);
                } else {
                    LOG.debug("factory link " + factoryUrl + " already displayed on description of Jenkins job " + jobName);
                }
            });
        });
    }

    protected String getCurrentJenkinsJobConfiguration() {
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

    protected void updateJenkinsJobDescription(String factoryUrl, Document configDocument, Node descriptionNode) {
        String descriptionContent = descriptionNode.getTextContent();
        descriptionNode.setTextContent(descriptionContent + "\n" + "<a href=\"" + factoryUrl + "\">" + factoryUrl + "</a>");
        String updatedJobConfigXml = documentToXml(configDocument);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(jobConfigXmlUrl);
        Invocation.Builder builder = target.request(APPLICATION_XML).header(HttpHeaders.CONTENT_TYPE, APPLICATION_XML);
        Response response = builder.post(Entity.xml(updatedJobConfigXml));

        if (response.getStatus() == 200) {
            LOG.debug("factory link " + factoryUrl + " successfully added on description of Jenkins job " + jobName);
        } else {
            LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
        }
    }

    protected String documentToXml(Document configDocument) {
        DOMSource domSource = new DOMSource(configDocument);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = tf.newTransformer();
            transformer.transform(domSource, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return writer.toString();
    }

    protected Document xmlToDocument(String jobConfigXml) {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new ByteArrayInputStream(jobConfigXml.getBytes("utf-8")));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return document;
    };
}

