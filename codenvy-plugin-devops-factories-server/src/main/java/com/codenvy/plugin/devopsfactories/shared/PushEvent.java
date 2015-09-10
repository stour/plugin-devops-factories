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
import org.eclipse.che.ide.ext.github.shared.GitHubRepository;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Created by stournie on 09/09/15.
 */

@DTO
public interface PushEvent {

    String getRef();

    void setRef(@NotNull final String ref);

    PushEvent withRef(@NotNull final String ref);


    String getHead();

    void setHead(@NotNull final String head);

    PushEvent withHead(@NotNull final String head);


    String getBefore();

    void setBefore(@NotNull final String before);

    PushEvent withBefore(@NotNull final String before);


    int getSize();

    void setSize(@NotNull final int size);

    PushEvent withSize(@NotNull final int size);


    int getDistinctSize();

    void setDistinctSize(@NotNull final int DistinctSize);

    PushEvent withDistinctSize(@NotNull final int DistinctSize);


    List<Commit> getCommitsArray();

    void setCommitsArray(@NotNull final List<Commit> commitsArray);

    PushEvent withCommitsArray(@NotNull final List<Commit> commitsArray);


    GitHubRepository getRepository();

    void setRepository(@NotNull final GitHubRepository repository);

    PushEvent withRepository(@NotNull final GitHubRepository repository);
}