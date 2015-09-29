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
 * @author stour
 */

@DTO
public interface PullRequestEvent {

    String getAction();

    void setAction(String action);

    PullRequestEvent withAction(String action);


    int getNumber();

    void setNumber(int number);

    PullRequestEvent withNumber(int number);


    PullRequest getPullRequest();

    void setPullRequest(PullRequest pullRequest);

    PullRequestEvent withPullRequest(PullRequest pullRequest);
}
