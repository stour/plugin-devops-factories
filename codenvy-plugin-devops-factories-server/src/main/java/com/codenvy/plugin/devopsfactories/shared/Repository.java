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

@DTO
public interface Repository {
    /**
     * Get repository's ID.
     *
     * @return {@link String} ID
     */
    String getId();

    void setId(String id);

    Repository withId(String id);

    /**
     * Get repository's name.
     *
     * @return {@link String} name
     */
    String getName();

    void setName(String name);

    Repository withName(String name);

    /**
     * Get repository's location.
     *
     * @return {@link String} htmlUrl
     */
    String getHtmlUrl();

    void setHtmlUrl(String htmlUrl);

    Repository withHtmlUrl(String htmlUrl);
}