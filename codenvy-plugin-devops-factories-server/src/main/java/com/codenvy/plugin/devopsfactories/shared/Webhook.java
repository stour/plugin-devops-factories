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
package com.codenvy.plugin.devopsfactories.shared;

import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * Represents a webhook on a Github repository.
 * A factory is linked to a webhook if source.project.location = {@code repositoryUrl}
 * A webhook contains at least one factory.
 */

@DTO
public interface Webhook {

    String getRepositoryUrl();

    void setRepositoryUrl(String repositoryUrl);

    Webhook withRepositoryUrl(String repositoryUrl);

    List<String> getFactoryIDs();

    void setFactoryIDs(List<String> factoryIDs);

    Webhook withFactoryIDs(List<String> factoryIDs);
}
