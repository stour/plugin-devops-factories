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
public interface User {
    String getName();

    void setName(final String name);

    User withName(final String name);


    String getEmail();

    void setEmail(final String email);

    User withEmail(final String email);


    String getUsername();

    void setUsername(final String username);

    User withUsername(final String username);
}
