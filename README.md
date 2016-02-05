# plugin GitHub monitor

Handle GitHub events and update Codenvy factories accordingly.

## Concepts
* Webhook configuration: List of factories mapped to a GitHub repository. These factories each contain a project with source.location = given repository. These factories (in fact matching project in them) will be updated when events for the given GitHub repository are received.
* Connector: Represents a connection to a third-party service where Codenvy factory URL will be displayed. Currently only Jenkins connector is available.

## Behavior
Two type of GitHub events are processed in current version:
* Push event: If a webhook is configured for event.repository and if in this webhook a factory is listed that contains a project with location = event.repository and branch = event.branch then check if URL of this factory is present on configured connectors (third-party services) and if not add it.
* Pull Request event: If a webhook is configured for event.baseRepository and if in this webhook a factory is listed that contains a project with location = event.headRepository and branch = event.headBranch then update this project with location = event.baseRepository, commitId = event.headCommitId and clean branch parameter if set.

## Configure
Configuration is done using three properties files in current early version:
* /home/codenvy/webhooks.properties: List of configured webhooks.
* /home/codenvy/connectors.properties: List of configured connectors.
* /home/codenvy/credentials.properties: username and password used to authenticate against Codenvy.

Examples of properties files can be found in [test resources](https://github.com/stour/plugin-version-control-monitor/tree/master/codenvy-plugin-version-control-monitor-server/src/test/resources).
