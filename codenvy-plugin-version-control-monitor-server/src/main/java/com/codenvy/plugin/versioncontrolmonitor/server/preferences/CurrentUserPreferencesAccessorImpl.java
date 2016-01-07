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
package com.codenvy.plugin.versioncontrolmonitor.server.preferences;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;

public class CurrentUserPreferencesAccessorImpl implements PreferencesAccessor {
    private final PreferenceDao preferencesDao;

    @Inject
    public CurrentUserPreferencesAccessorImpl(final PreferenceDao preferencesDao) {
        this.preferencesDao = preferencesDao;
    }

    @Override
    public void updatePreference(final String key, final String value) throws ApiException {
        final Optional<User> currentUser = Optional.ofNullable(getCurrentUser());
        if (currentUser.isPresent()) {
            Map<String, String> content = preferencesDao.getPreferences(currentUser.get().getId());
            content.put(key, value);
            preferencesDao.setPreferences(currentUser.get().getId(), content);
        } else {
            throw new ForbiddenException("You must be authenticated to access preferences");
        }
    }

    @Override
    public String getPreference(final String key) throws ApiException {
        final Optional<User> currentUser = Optional.ofNullable(getCurrentUser());
        if (currentUser.isPresent()) {
            final String pattern = MessageFormat.format("^{0}$", key);
            Map<String, String> response = preferencesDao.getPreferences(currentUser.get().getId(), pattern);
            return response.get(key);
        } else {
            throw new ForbiddenException("You must be authenticated to access preferences");
        }
    }

    private User getCurrentUser() {
        return EnvironmentContext.getCurrent().getUser();
    }
}
