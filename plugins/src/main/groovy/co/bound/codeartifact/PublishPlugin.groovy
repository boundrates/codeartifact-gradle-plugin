package co.bound.codeartifact

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.services.BuildServiceRegistry

import javax.inject.Inject

class PublishPlugin implements Plugin<Project> {

  private final BuildServiceRegistry sharedServices

  @Inject
  PublishPlugin(BuildServiceRegistry sharedServices) {
    this.sharedServices = sharedServices
  }

  @Override
  void apply(Project project) {
    project.plugins.apply(MavenPublishPlugin)

    PublishingExtension publishing = project.extensions.getByType(PublishingExtension)
    publishing.repositories {
      var codeArtifactRepoProviderService = sharedServices.registrations.findByName("codeartifactRepoProvider")
      if (codeArtifactRepoProviderService == null) {
        throw new IllegalStateException("Please apply the co.bound.codeartifact plugin in the settings file first and configure the codeartifact extension")
      }
      maven(codeArtifactRepoProviderService.service.get()::configureRepo)
    }
  }
}
