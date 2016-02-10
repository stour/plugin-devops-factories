/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.plugin.versioncontrolmonitor.shared;

import org.eclipse.che.dto.shared.DTO;

@DTO
public interface PullRequestEvent {

    String getAction();

    void setAction(String action);

    PullRequestEvent withAction(String action);


    int getNumber();

    void setNumber(int number);

    PullRequestEvent withNumber(int number);


    PullRequest getPull_request();

    void setPull_request(PullRequest pull_request);

    PullRequestEvent withPull_request(PullRequest pull_request);
}
