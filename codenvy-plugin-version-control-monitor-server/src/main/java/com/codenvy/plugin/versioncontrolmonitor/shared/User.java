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
