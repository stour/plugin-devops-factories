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
     * Get repository's name.
     *
     * @return {@link String} name
     */
    String getName();

    void setName(String name);

    /**
     * Get repository's location.
     *
     * @return {@link String} url
     */
    String getUrl();

    void setUrl(String url);

    /**
     * Get repository's home page.
     *
     * @return {@link String} home page
     */
    String getHomepage();

    void setHomepage(String homePage);

    /**
     * Get the number of repository's forks.
     *
     * @return forks
     */
    int getForks();

    void setForks(int forks);

    /**
     * Get repository's language.
     *
     * @return {@link String} language
     */
    String getLanguage();

    void setLanguage(String language);

    /**
     * Get fork state.
     *
     * @return {@link Boolean} <code>true</code> id forked
     */
    boolean isFork();

    void setFork(boolean isFork);

    /**
     * Get the number of repository's watchers.
     *
     * @return {@link Integer} the number of watchers
     */
    int getWatchers();

    void setWatchers(int watchers);

    /**
     * Get private state of the repository.
     *
     * @return {@link Boolean} private state of the repository
     */
    boolean isPrivate_repo();

    void setPrivate_repo(boolean isPrivate_repo);

    /**
     * Get repository's size.
     *
     * @return {@link Integer} size
     */
    int getSize();

    void setSize(int size);

    /**
     * Get repository's description.
     *
     * @return {@link String} description
     */
    String getDescription();

    void setDescription(String description);

    /**
     * Get SSH URL.
     *
     * @return {@link String} SSH URL
     */
    String getSsh_url();

    void setSsh_url(String ssh_url);

    /**
     * Get HTML URL.
     *
     * @return {@link String} HTML URL
     */
    String getHtml_url();

    void setHtml_url(String html_url);

    /**
     * Get updated date.
     *
     * @return {@link String}
     */
    String getUpdated_at();

    void setUpdated_at(String updated_at);

    /**
     * Get Git URL.
     *
     * @return {@link String} Git URL
     */
    String getGit_url();

    void setGit_url(String git_url);

    /**
     * Get whether repository has wiki.
     *
     * @return {@link Boolean} <code> true</code> - has wiki
     */
    boolean isHas_wiki();

    void setHas_wiki(boolean isHas_wiki);

    /**
     * Get clone URL.
     *
     * @return {@link String} clone URL
     */
    String getClone_url();

    void setClone_url(String clone_url);

    /**
     * Get SVN URL.
     *
     * @return {@link String} SVN URL
     */
    String getSvn_url();

    void setSvn_url(String svn_url);

    /**
     * Get the number of opened issues.
     *
     * @return {@link Integer} number of opened issues
     */
    int getOpened_issues();

    void setOpened_issues(int opened_issues);

    /**
     * Get repository's created date.
     *
     * @return {@link String} created date
     */
    String getCreated_at();

    void setCreated_at(String created_at);

    /**
     * Get repository's pushed date.
     *
     * @return {@link String} pushed date
     */
    String getPushed_at();

    void setPushed_at(String pushed_at);

    /**
     * Get whether repository has downloads.
     *
     * @return {@link Boolean} <code> true</code> - has downloads
     */
    boolean isHas_downloads();

    void setHas_downloads(boolean isHas_downloads);

    /**
     * Get whether repository has issues.
     *
     * @return {@link Boolean} <code> true</code> - has issues
     */
    boolean isHas_issues();

    void setHas_issues(boolean isHas_issues);


    String getFull_name();

    void setFull_name(String full_name);
}