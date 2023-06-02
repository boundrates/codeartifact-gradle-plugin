# codeartifact-gradle-plugin

[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/co/bound/plugins/maven-metadata.xml.svg?colorB=007ec6&label=Plugin%20Portal)](https://plugins.gradle.org/plugin/co.bound.codeartifact)

A set of plugins for authenticating with and using [AWS CodeArtifact](https://aws.amazon.com/codeartifact/) repository as a source for project plugins
and dependencies as well as a target for publishing artifacts to.

## Why another CodeArtifact plugin?

The existing solutions for Gradle with CodeArtifact cater for the most common use cases - consuming and publishing
project dependencies from/to CodeArtifact repository.

Another use case exists though - organizations that work with multiple projects spread across many repositories may want
to [encapsulate aspects of common project conventions and build logic using Gradle plugins](https://docs.gradle.org/current/samples/sample_publishing_convention_plugins.html).
Those plugins are published to a repository like any other library.
However, to consume them, Gradle needs to discover them from a plugin repository. 
Gradle configures the plugin repositories early in its lifecycle, before it starts to configure any of the projects.
Thus, the need to configure CodeArtifact as source of other plugins in a `settings` plugin.

This plugin solves this latter use case - consuming Gradle plugins that are published to CodeArtifact.
In other words, with this plugin you can publish a Gradle plugin to a CodeArtifact repository, and you can apply that
published plugin in a Gradle build.

## co.bound.codeartifact

A settings plugin that configures AWS CodeArtifact repository.

Usage (in `settings.gradle`):
```groovy
plugins {
    id("co.bound.codeartifact").version("1.4.0")
}
codeartifact {
    domain = "repo-domain"
    accountId = "123456789012"
    region = "us-east-1"
    repo = "repo-name"
}
```
or `settings.gradle.kts`
```kotlin
plugins {
    id("co.bound.codeartifact").version("1.4.0")
}
codeartifact {
    domain.set("repo-domain")
    accountId.set("123456789012")
    region.set("us-east-1")
    repo.set("repo-name")
}
```

This will add the given repository as a source for Gradle project plugins (settings plugins are not supported) as
well as a source for project dependencies.

AWS credentials from the `default` (configurable with environment variable `AWS_PROFILE`) profile will be used for authenticating with and obtaining the token from AWS CodeArtifact.

## co.bound.codeartifact-publish

A project plugin that configures AWS CodeArtifact repository for publishing artifacts to.

Usage (in `build.gradle`):
```
plugins {
    id("co.bound.codeartifact-publish")
}
```

This plugin requires `co.bound.codeartifact` plugin to also be applied in the `settings.gradle` and a CodeArtifact
repository to be configured. It will apply the `maven-publish` plugin and add the configured CodeArtifact repository
as a publishing repository.


# License

The project is [licensed](LICENSE) under Apache License 2.0.
