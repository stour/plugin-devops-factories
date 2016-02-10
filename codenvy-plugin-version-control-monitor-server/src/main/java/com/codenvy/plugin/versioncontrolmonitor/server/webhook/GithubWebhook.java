/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
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
package com.codenvy.plugin.versioncontrolmonitor.server.webhook;

/**
 * Link factories to a GitHub webhook.
 * A factory may be linked to a webhook only if source.project.location = {@link repositoryUrl}
 *
 * @author Stephane Tournie
 */
public class GithubWebhook {

    private final String   repositoryUrl;
    private final String[] factoryIDs;

    public GithubWebhook(String repositoryUrl, String[] factoryIDs) {
        this.repositoryUrl = repositoryUrl;
        // TODO Check that source.project.location = repositoryUrl for at least one project in each factory
        this.factoryIDs = factoryIDs;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String[] getFactoryIDs() {
        return factoryIDs;
    }
}
