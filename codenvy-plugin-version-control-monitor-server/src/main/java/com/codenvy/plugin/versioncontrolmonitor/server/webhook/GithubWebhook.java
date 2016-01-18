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
package com.codenvy.plugin.versioncontrolmonitor.server.webhook;

/**
 * Represents a webhook on a Github repository.
 * A factory can be linked to a webhook only if source.project.location = {@link repositoryUrl}
 * A webhook is linked to at least one factory.
 *
 * @author stour
 */
public class GithubWebhook {

    private final String repositoryUrl;
    private final String[] factoryIDs;

    public GithubWebhook(String repositoryUrl, String[] factoryIDs) {
        this.repositoryUrl = repositoryUrl;
        // TODO Check that source.project.location = repositoryUrl for each factory
        this.factoryIDs = factoryIDs;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String[] getFactoryIDs() {
        return factoryIDs;
    }

    /**
     *
     * Configure a webhook on a Github repository
     */
    public void configure() {

    }
}