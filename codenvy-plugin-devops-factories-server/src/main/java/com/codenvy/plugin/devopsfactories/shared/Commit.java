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

import javax.validation.constraints.NotNull;

/**
 * Created by stour on 09/09/15.
 */

@DTO
public interface Commit {
    String getSha();

    void setSha(@NotNull final String sha);

    Commit withSha(@NotNull final String sha);


    String getMessage();

    void setMessage(@NotNull final String message);

    Commit withMessage(@NotNull final String message);


    Author getAuthor();

    void setAuthor(@NotNull final Author author);

    Commit withAuthor(@NotNull final Author author);


    String getUrl();

    void setUrl(@NotNull final String url);

    Commit withUrl(@NotNull final String url);


    boolean getDistinct();

    void setDistinct(@NotNull final boolean distinct);

    Commit withDistinct(@NotNull final boolean distinct);
}