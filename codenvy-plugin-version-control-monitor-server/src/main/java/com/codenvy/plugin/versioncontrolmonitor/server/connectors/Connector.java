/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.plugin.versioncontrolmonitor.server.connectors;

/**
 * Connect to a third-party service in order to add Codenvy factory related data
 *
 * @author Stephane Tournie
 */
public interface Connector {

    /**
     * Add a factory link to the third-party service
     *
     * @param factoryUrl
     *         the factory URL to add
     */
    void addFactoryLink(String factoryUrl);
}
