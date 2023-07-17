package co.bound.codeartifact;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.services.BuildServiceRegistration;
import org.gradle.api.services.BuildServiceRegistry;

import javax.inject.Inject;

public class PublishPlugin implements Plugin<Project> {
    private final BuildServiceRegistry sharedServices;

    @Inject
    public PublishPlugin(BuildServiceRegistry sharedServices) {
        this.sharedServices = sharedServices;
    }

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(MavenPublishPlugin.class);

        PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
        publishing.repositories(handler -> {
            BuildServiceRegistration<CodeArtifactRepoProvider, ?> codeArtifactRepoProviderService = providerRegistration();
            if (codeArtifactRepoProviderService == null) {
                throw new IllegalStateException(
                        "Please apply the co.bound.codeartifact plugin in the settings file first and configure the codeartifact extension");
            }
            CodeArtifactRepoProvider provider = codeArtifactRepoProviderService.getService().get();
            provider.configureRepo(handler);
        });
    }

    @SuppressWarnings("unchecked")
    private BuildServiceRegistration<CodeArtifactRepoProvider, ?> providerRegistration() {
        return (BuildServiceRegistration<CodeArtifactRepoProvider, ?>) sharedServices.getRegistrations()
                .findByName("codeartifactRepoProvider");
    }
}
