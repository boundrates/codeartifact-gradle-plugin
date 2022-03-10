# codeartifact-gradle-plugin

A set of plugins for authenticating with and using [AWS CodeArtifact](https://aws.amazon.com/codeartifact/) repository as a source for project plugins 
and dependencies as well as target for publishing artifacts to.

## co.bound.codeartifact

A settings plugin that configures AWS CodeArtifact repository.

Usage (in `settings.gradle`):
```
plugins {
    id("co.bound.codeartifact").version("1.0.1")
}
codeartifact {
    domain = "repo-domain"
    accountId = "123456789012"
    region = "us-east-1"
    repo = "repo-name"
}
```

This will add the given repository as a source for Gradle project plugins (settings plugins are not supported) as
well as a source for project dependencies.

AWS credentials from the `default` profile will be used for authenticating with and obtaining the token from AWS CodeArtifact.

## co.bound.codeartifact-publish

A project plugin that configures AWS CodeArtifact repository for publishing artifacts to.

Usage (in `build.gradle`):
```
plugins {
    id("co.bound.codeartifact-publish").version("1.0.1")
}
```

This plugin requires `co.bound.codeartifact` plugin to also be applied in the `settings.gradle` and a CodeArtifact
repository to be configured. It will apply the `maven-publish` plugin and add the configured CodeArtifact repository
as a publishing repository.
