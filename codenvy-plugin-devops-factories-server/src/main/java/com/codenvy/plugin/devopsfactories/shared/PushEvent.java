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
 * Created by stournie on 09/09/15.
 */

@DTO
public interface PushEvent {

    String getRef();

    void setRef(final String ref);

    PushEvent withRef(final String ref);


    String getBefore();

    void setBefore(final String before);

    PushEvent withBefore(final String before);


    String getAfter();

    void setAfter(final String after);

    PushEvent withAfter(final String after);


    boolean getCreated();

    void setCreated(final boolean created);

    PushEvent withCreated(final boolean created);


    boolean getDeleted();

    void setDeleted(final boolean deleted);

    PushEvent withDeleted(final boolean deleted);


    boolean getForced();

    void setForced(final boolean forced);

    PushEvent withForced(final boolean forced);


    String getBaseRef();

    void setBaseRef(final String baseRef);

    PushEvent withBaseRef(final String baseRef);


    String getCompare();

    void setCompare(final String compare);

    PushEvent withCompare(final String compare);


    List<Commit> getCommits();

    void setCommits(final List<Commit> commits);

    PushEvent withCommits(final List<Commit> commits);


    Commit getHeadCommit();

    void setHeadCommit(final Commit headCommit);

    PushEvent withHeadCommit(final Commit headCommit);


    Repository getRepository();

    void setRepository(final Repository repository);

    PushEvent withRepository(final Repository repository);


    User getPusher();

    void setPusher(final User pusher);

    PushEvent withPusher(final User pusher);


/*    Sender getSender();

    void setSender(final Sender sender);

    PushEvent withSender(final Sender sender); */
}