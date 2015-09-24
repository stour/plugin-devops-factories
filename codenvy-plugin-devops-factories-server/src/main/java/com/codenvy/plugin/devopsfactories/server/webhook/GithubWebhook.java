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
package com.codenvy.plugin.devopsfactories.server.webhook;

/**
 * A Github webhook associates an existing Codenvy factoryId with a Github repository & branch.
 *
 * @author stour
 */
public class GithubWebhook {

    private final String factoryId;

    public GithubWebhook(String factoryId) {
        this.factoryId = factoryId;
    }

    public String getFactoryId() {
        return factoryId;
    }

    /**
     *
     * Configure a webhook on a Github repository based on
     * value of source.project.location & source.project.parameters.branch
     *
     * --> source.project.location & source.project.parameters.branch MUST be set in factory.json
     */
    public void configure() {

    }
}
