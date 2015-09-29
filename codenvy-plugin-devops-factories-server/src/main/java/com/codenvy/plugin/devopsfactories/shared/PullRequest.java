package com.codenvy.plugin.devopsfactories.shared;

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

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface PullRequest {
    /**
     * Get pull request id.
     *
     * @return {@link String} id
     */
    String getId();

    void setId(String id);

    PullRequest withId(String id);

    /**
     * Get pull request URL.
     *
     * @return {@link String} url
     */
    String getUrl();

    void setUrl(String url);

    PullRequest withUrl(String url);

    /**
     * Get pull request html URL.
     *
     * @return {@link String} html_url
     */
    String getHtmlUrl();

    void setHtmlUrl(String htmlUrl);

    PullRequest withHtmlUrl(String htmlUrl);

    /**
     * Get pull request number.
     *
     * @return {@link String} number
     */
    String getNumber();

    void setNumber(String number);

    PullRequest withNumber(String number);

    /**
     * Get pull request state.
     *
     * @return {@link String} state
     */
    String getState();

    void setState(String state);

    PullRequest withState(String state);

    /**
     * Get pull request head.
     *
     * @return {@link PullRequestBaseOrHead} head
     */
    PullRequestBaseOrHead getHead();

    void setHead(PullRequestBaseOrHead head);

    PullRequest withHead(PullRequestBaseOrHead head);

    /**
     * Get pull request base.
     *
     * @return {@link PullRequestBaseOrHead} base
     */
    PullRequestBaseOrHead getBase();

    void setBase(PullRequestBaseOrHead base);

    PullRequest withBase(PullRequestBaseOrHead base);

    /**
     * Tells if the pull request is merged.
     *
     * @return true iff the pull request is merged
     */
    boolean getMerged();

    void setMerged(boolean merged);

    PullRequest withMerged(boolean merged);

    /**
     * Tells which user merged the pull request (if it was).
     *
     * @return the user
     */
    User getMergedBy();

    void setMergedBy(User user);

    PullRequest withMergedBy(User user);

    /**
     * Tells if the pull request is mergeable.
     *
     * @return true iff the merge can be done automatically
     */
    boolean getMergeable();

    void setMergeable(boolean mergeable);

    PullRequest withMergeable(boolean mergeable);
}
