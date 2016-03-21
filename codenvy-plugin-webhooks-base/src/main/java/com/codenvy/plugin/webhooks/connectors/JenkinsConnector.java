/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.plugin.webhooks.connectors;

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
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Jenkins implementation of {@link Connector}
 * One {@link JenkinsConnector} is configured for one Jenkins job
 *
 * @author Stephane Tournie
 */
public class JenkinsConnector implements Connector {

    private static final Logger LOG = LoggerFactory.getLogger(JenkinsConnector.class);

    // The name of the Jenkins job
    private final String jobName;
    // The URL of the XML configuration of the Jenkins job
    private final String jobConfigXmlUrl;

    /**
     * Constructor
     *
     * @param url
     *         the URL of the Jenkins instance to connect to
     * @param jobName
     *         the name of the Jenkins job
     */
    public JenkinsConnector(final String url, final String jobName) {
        this.jobName = jobName;
        this.jobConfigXmlUrl = url + "/job/" + jobName + "/config.xml";
    }

    /**
     * Add a factory link to configured Jenkins job
     *
     * @param factoryUrl
     *         the factory URL to add
     */
    @Override
    public void addFactoryLink(String factoryUrl) {
        Optional<String> jobConfigXml = getCurrentJenkinsJobConfiguration();
        jobConfigXml.ifPresent(xml -> {
            Optional<Document> configDocument = xmlToDocument(xml);
            configDocument.ifPresent(doc -> {
                Element root = doc.getDocumentElement();
                Node descriptionNode = root.getElementsByTagName("description").item(0);

                if (!descriptionNode.getTextContent().contains(factoryUrl)) {
                    updateJenkinsJobDescription(factoryUrl, doc, descriptionNode);
                } else {
                    LOG.debug("factory link {} already displayed on description of Jenkins job {}", factoryUrl, jobName);
                }
            });
        });
    }

    protected Optional<String> getCurrentJenkinsJobConfiguration() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(jobConfigXmlUrl);
        Invocation.Builder builder = target.request(APPLICATION_XML);
        Response response = builder.get();
        if (response.getStatus() == 200) {
            String responseString = response.readEntity(String.class);
            return Optional.of(responseString);
        } else {
            LOG.error(response.getStatus() + " - " + response.readEntity(String.class));
            return Optional.empty();
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
            LOG.debug("factory link {} successfully added on description of Jenkins job ", factoryUrl, jobName);
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
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return writer.toString();
    }

    protected Optional<Document> xmlToDocument(String jobConfigXml) {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new ByteArrayInputStream(jobConfigXml.getBytes("utf-8")));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(document);
    }
}

