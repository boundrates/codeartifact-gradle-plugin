package co.bound.codeartifact;

import org.gradle.api.provider.Property;

public abstract class CodeArtifactPluginExtension {
    public abstract Property<String> getDomain();

    public abstract Property<String> getAccountId();

    public abstract Property<String> getRegion();

    public abstract Property<String> getRepo();
}
