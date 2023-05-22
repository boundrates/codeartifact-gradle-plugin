package co.bound.codeartifact

import org.gradle.testkit.runner.TaskOutcome

class CodeArtifactPublishPluginTest extends PluginTest {

    def "fails with a helpful message when publishing plugin is applied without the settings plugin"() {
        given:
        buildFile << """
            plugins {
                id("java-library") 
                id("co.bound.codeartifact-publish") 
            }
            publishing {
                publications {
                    maven(MavenPublication) {
                        groupId = "com.foo"
                        artifactId = "bar"
                        version = "0.1"
                        from(components.java)
                    }
                }
            }
        """
        file("src/main/java/Foo.java") << "class Foo {}"

        when:
        def result = runTaskWithFailure(gradleVersion, "publish")

        then:
        result.output.contains("Failed to apply plugin 'co.bound.codeartifact-publish'")
        result.output.contains("Please apply the co.bound.codeartifact plugin in the settings file first and configure the codeartifact extension")

        where:
        gradleVersion << gradleVersions()
    }

    def "attempts to publish to CodeArtifact repository"() {
        given:
        givenCodeArtifactWillReturnAuthToken()
        givenCodeArtifactPluginIsConfigured()
        buildFile << """
            plugins {
                id("java-library") 
                id("co.bound.codeartifact-publish") 
            }
            publishing {
                publications {
                    maven(MavenPublication) {
                        groupId = "com.foo"
                        artifactId = "bar"
                        version = "0.1"
                        from(components.java)
                    }
                }
            }
        """
        file("src/main/java/Foo.java") << "class Foo {}"

        when:
        def result = runTaskWithFailure(gradleVersion, "publish")

        then:
        result.task(":publishMavenPublicationToCodeartifactRepository").outcome == TaskOutcome.FAILED
        result.output.contains("Failed to publish publication 'maven' to repository 'codeartifact'")
        result.output.contains("Could not PUT '${wiremock.baseUrl()}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/$repo/com/foo/bar/0.1/bar-0.1.jar'")

        where:
        gradleVersion << gradleVersions()
    }
}
