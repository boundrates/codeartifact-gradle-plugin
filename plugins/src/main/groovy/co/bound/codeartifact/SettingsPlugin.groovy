package co.bound.codeartifact

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.services.BuildServiceRegistry

import javax.inject.Inject

class SettingsPlugin implements Plugin<Settings> {

  private final Gradle gradle
  private final BuildServiceRegistry sharedServices

  @Inject
  SettingsPlugin(Gradle gradle, BuildServiceRegistry sharedServices) {
    this.gradle = gradle
    this.sharedServices = sharedServices

  }

  @Override
  void apply(Settings settings) {
    var codeartifact = settings.extensions.create("codeartifact", CodeArtifactPluginExtension)
    var serviceProvider = sharedServices.registerIfAbsent("codeartifactRepoProvider", CodeArtifactRepoProvider) {
      parameters {
        domain.set(codeartifact.domain)
        accountId.set(codeartifact.accountId)
        region.set(codeartifact.region)
        repo.set(codeartifact.repo)
        gradleUserHome.set(settings.startParameter.gradleUserHomeDir)
        offline.set(settings.startParameter.offline)
      }
    }

    gradle.settingsEvaluated {
      settings.pluginManagement {
        repositories {
          maven(serviceProvider.get()::configureRepo)
        }
      }
      settings.dependencyResolutionManagement {
        repositories {
          maven(serviceProvider.get()::configureRepo)
        }
      }
    }
  }
}
