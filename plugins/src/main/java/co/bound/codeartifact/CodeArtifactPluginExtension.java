package co.bound.codeartifact;

import co.bound.codeartifact.internal.SerializableAction;
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor;
import org.gradle.api.provider.Property;

public abstract class CodeArtifactPluginExtension {
    public abstract Property<String> getDomain();

    public abstract Property<String> getAccountId();

    public abstract Property<String> getRegion();

    public abstract Property<String> getRepo();

    public abstract Property<SerializableAction<MavenRepositoryContentDescriptor>> getMavenContent();
}
