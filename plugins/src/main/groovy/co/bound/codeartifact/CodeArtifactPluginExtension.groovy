package co.bound.codeartifact

import org.gradle.api.provider.Property

abstract class CodeArtifactPluginExtension {
    abstract Property<String> getDomain()
    abstract Property<String> getAccountId()
    abstract Property<String> getRegion()
    abstract Property<String> getRepo()
}
