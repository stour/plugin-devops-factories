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

import com.codenvy.plugin.versioncontrolmonitor.shared.Webhook;
import com.google.common.collect.Maps;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.dto.server.JsonStringMapImpl;
import org.eclipse.che.dto.shared.JsonStringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public class WebhooksProvider {

    private static final Logger LOG = LoggerFactory.getLogger(WebhooksProvider.class);

    private static final String WEBHOOK_PREFERENCES = "vcmonitor.webhooks";

    private final PreferencesAccessor preferencesAccessor;

    @Inject
    public WebhooksProvider(final PreferencesAccessor preferencesAccessor) {
        this.preferencesAccessor = preferencesAccessor;
    }

    public Webhook getWebhook(final String webhookId) throws ApiException {
        Optional<String> webhooksPrefsString;
        webhooksPrefsString = Optional.ofNullable(preferencesAccessor.getPreference(WEBHOOK_PREFERENCES));
        if (webhooksPrefsString.isPresent()) {
            Map<String, Webhook> webhooks = DtoFactory.getInstance().createMapDtoFromJson(webhooksPrefsString.get(), Webhook.class);
            Optional<Webhook> webhook = Optional.ofNullable(webhooks.get(webhookId));
            if (webhook.isPresent()) {
                return webhook.get();
            } else {
                throw new NotFoundException("Webhook " + webhookId + " not found.");
            }
        } else {
            throw new ServerException("No Webhook found.");
        }
    }

    public void saveWebhook(final Webhook webhook) throws ApiException {
        if (webhook.getFactoryIDs().size() == 0) {
            throw new ServerException(
                    "Webhook for repository " + webhook.getRepositoryUrl() +
                    " contains no factoryID. A webhook must contain at least one factoryID.");
        }
        final String webhookId = constructWebhookId(webhook.getRepositoryUrl());
        final Optional<String> webhooksPrefsString;
        webhooksPrefsString = Optional.ofNullable(preferencesAccessor.getPreference(WEBHOOK_PREFERENCES));
        if (webhooksPrefsString.isPresent()) {
            JsonStringMap<Webhook> webhooks = DtoFactory.getInstance().createMapDtoFromJson(webhooksPrefsString.get(), Webhook.class);
            Optional<Webhook> existingWebhook = Optional.ofNullable(webhooks.get(webhookId));
            if (existingWebhook.isPresent()) {
                throw new ServerException("Webhook for repository " + webhookId + " already exists.");
            } else {
                webhooks.put(webhookId, webhook);
                String updatedWebhooksPrefsString = DtoFactory.getInstance().toJson(webhooks);
                preferencesAccessor.updatePreference(WEBHOOK_PREFERENCES, updatedWebhooksPrefsString);
            }
        } else {
            JsonStringMap<Webhook> webhooks = new JsonStringMapImpl<>(Maps.newHashMapWithExpectedSize(1));
            webhooks.put(webhookId, webhook);
            String newWebhooksPrefsString = DtoFactory.getInstance().toJson(webhooks);
            LOG.info("newWebhooksPrefsString: " + newWebhooksPrefsString);
            preferencesAccessor.updatePreference(WEBHOOK_PREFERENCES, newWebhooksPrefsString);
        }
    }

    public void updateWebhook(final String webhookId, final Webhook webhook) throws ApiException {
        String updatedWebhookId = constructWebhookId(webhook.getRepositoryUrl());
        if (!webhookId.equals(updatedWebhookId)) {
            throw new ServerException(
                    "Update webhook repository URL isn't permitted. Please consider to delete webhook " + webhookId +
                    "and save a new webhook for repository " + webhook.getRepositoryUrl());
        }
        if (webhook.getFactoryIDs().size() == 0) {
            throw new ServerException(
                    "Webhook for repository " + webhook.getRepositoryUrl() +
                    " contains no factoryID. A webhook must contain at least one factoryID.");
        }
        final Optional<String> webhooksPrefsString;
        webhooksPrefsString = Optional.ofNullable(preferencesAccessor.getPreference(WEBHOOK_PREFERENCES));
        if (webhooksPrefsString.isPresent()) {
            JsonStringMap<Webhook> webhooks = DtoFactory.getInstance().createMapDtoFromJson(webhooksPrefsString.get(), Webhook.class);
            Optional<Webhook> existingWebhook = Optional.ofNullable(webhooks.get(webhookId));
            if (existingWebhook.isPresent()) {
                webhooks.put(webhookId, webhook);
                String updatedWebhooksPrefsString = DtoFactory.getInstance().toJson(webhooks);
                preferencesAccessor.updatePreference(WEBHOOK_PREFERENCES, updatedWebhooksPrefsString);
            } else {
                throw new NotFoundException("Webhook " + webhookId + " not found.");
            }
        } else {
            throw new ServerException("No Webhook found.");
        }
    }

    public void removeWebhook(final String webhookId) throws ApiException {
        final Optional<String> webhooksPrefsString;
        webhooksPrefsString = Optional.ofNullable(preferencesAccessor.getPreference(WEBHOOK_PREFERENCES));
        if (webhooksPrefsString.isPresent()) {
            JsonStringMap<Webhook> webhooks = DtoFactory.getInstance().createMapDtoFromJson(webhooksPrefsString.get(), Webhook.class);
            Optional<Webhook> webhook = Optional.ofNullable(webhooks.remove(webhookId));
            if (webhook.isPresent()) {
                String updatedWebhooksPrefsString = DtoFactory.getInstance().toJson(webhooks);
                preferencesAccessor.updatePreference(WEBHOOK_PREFERENCES, updatedWebhooksPrefsString);
                return;
            } else {
                throw new NotFoundException("Webhook " + webhookId + " not found.");
            }
        } else {
            throw new ServerException("No Webhook found.");
        }
    }

    public static String constructWebhookId(String repositoryUrl) throws ServerException {
        URL url;
        try {
            url = new URL(repositoryUrl);
        } catch (MalformedURLException e) {
            LOG.error(e.getLocalizedMessage());
            throw new ServerException(e.getLocalizedMessage());
        }
        String host = url.getHost();
        String user = url.getPath().split("/")[1];
        String repository = url.getPath().split("/")[2];

        return host + ":" + user + ":" + repository;
    }
}
