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

/**
 * Created by stour on 09/09/15.
 */

@DTO
public interface Commit {
    String getId();

    void setId(final String id);

    Commit withId(final String id);


    boolean getDistinct();

    void setDistinct(final boolean distinct);

    Commit withDistinct(final boolean distinct);


    String getMessage();

    void setMessage(final String message);

    Commit withMessage(final String message);


    String getTimestamp();

    void setTimestamp(final String timestamp);

    Commit withTimestamp(final String timestamp);


    String getUrl();

    void setUrl(final String url);

    Commit withUrl(final String url);


    User getAuthor();

    void setAuthor(final User author);

    Commit withAuthor(final User author);


    User getCommitter();

    void setCommitter(final User committer);

    Commit withCommitter(final User committer);
}