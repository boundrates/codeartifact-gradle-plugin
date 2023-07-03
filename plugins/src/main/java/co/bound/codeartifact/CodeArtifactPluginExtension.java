package co.bound.codeartifact;

import org.gradle.api.provider.Property;

public abstract class CodeArtifactPluginExtension {
    public static final String REPOSITORY_NAME = "codeartifact";

    public abstract Property<String> getDomain();

    public abstract Property<String> getAccountId();

    public abstract Property<String> getRegion();

    public abstract Property<String> getRepo();
}
