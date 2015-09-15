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
import java.util.List;

/**
 * Created by stournie on 09/09/15.
 */

@DTO
public interface PushEvent {

    String getRef();

    void setRef(@NotNull final String ref);

    PushEvent withRef(@NotNull final String ref);


    String getBefore();

    void setBefore(@NotNull final String before);

    PushEvent withBefore(@NotNull final String before);


    String getAfter();

    void setAfter(@NotNull final String after);

    PushEvent withAfter(@NotNull final String after);


    boolean getCreated();

    void setCreated(@NotNull final boolean created);

    PushEvent withCreated(@NotNull final boolean created);


    boolean getDeleted();

    void setDeleted(@NotNull final boolean deleted);

    PushEvent withDeleted(@NotNull final boolean deleted);


    boolean getForced();

    void setForced(@NotNull final boolean forced);

    PushEvent withForced(@NotNull final boolean forced);


    String getBaseRef();

    void setBaseRef(@NotNull final String baseRef);

    PushEvent withBaseRef(@NotNull final String baseRef);


    String getCompare();

    void setCompare(@NotNull final String compare);

    PushEvent withCompare(@NotNull final String compare);


    List<Commit> getCommits();

    void setCommits(@NotNull final List<Commit> commits);

    PushEvent withCommits(@NotNull final List<Commit> commits);


    Commit getHeadCommit();

    void setHeadCommit(@NotNull final Commit headCommit);

    PushEvent withHeadCommit(@NotNull final Commit headCommit);


    Repository getRepository();

    void setRepository(@NotNull final Repository repository);

    PushEvent withRepository(@NotNull final Repository repository);


    User getPusher();

    void setPusher(@NotNull final User pusher);

    PushEvent withPusher(@NotNull final User pusher);


/*    Sender getSender();

    void setSender(@NotNull final Sender sender);

    PushEvent withSender(@NotNull final Sender sender); */
}