package co.bound.codeartifact;

import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceRegistry;

import javax.inject.Inject;

public class SettingsPlugin implements Plugin<Settings> {
    private final Gradle gradle;
    private final BuildServiceRegistry sharedServices;

    @Inject
    public SettingsPlugin(Gradle gradle) {
        this.gradle = gradle;
        this.sharedServices = gradle.getSharedServices();
    }

    @Override
    public void apply(Settings settings) {
        CodeArtifactPluginExtension codeartifact = settings.getExtensions().create("codeartifact", CodeArtifactPluginExtension.class);
        Provider<CodeArtifactRepoProvider> serviceProvider = sharedServices.registerIfAbsent(
                "codeartifactRepoProvider",
                CodeArtifactRepoProvider.class,
                spec -> spec.parameters(params -> {
                    params.getDomain().set(codeartifact.getDomain());
                    params.getAccountId().set(codeartifact.getAccountId());
                    params.getRegion().set(codeartifact.getRegion());
                    params.getRepo().set(codeartifact.getRepo());
                    params.getMavenContent().set(codeartifact.getMavenContent());
                    params.getGradleUserHome().set(settings.getStartParameter().getGradleUserHomeDir());
                    params.getOffline().set(settings.getStartParameter().isOffline());
                }));

        gradle.settingsEvaluated(ignore -> {
            settings.pluginManagement(management ->
                    management.repositories(repositories ->
                            repositories.maven(serviceProvider.get()::configureRepo)));
            //noinspection UnstableApiUsage
            settings.dependencyResolutionManagement(management ->
                    management.repositories(handler ->
                            handler.maven(serviceProvider.get()::configureRepo)));
        });
    }
}
