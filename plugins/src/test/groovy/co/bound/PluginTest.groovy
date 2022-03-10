package co.bound

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

abstract class PluginTest extends Specification {

    final String accountId = "123456789012"
    final String region = "eu-west-1"
    final String domain = UUID.randomUUID().toString()
    final String repo = UUID.randomUUID().toString()

    @TempDir
    File testProjectDir
    File settingsFile
    File buildFile

    private GradleRunner gradleRunner

    final WireMockServer wiremock = new WireMockServer(options().dynamicPort())

    File file(String path) {
        return new File(testProjectDir, path)
    }

    def setup() {
        file('src/main/java').mkdirs()
        file('src/test/java').mkdirs()
        settingsFile = file('settings.gradle') << """
            rootProject.name = 'test'
            dependencyResolutionManagement {
                repositories {
                    mavenCentral() 
                } 
            }
        """
        buildFile = file('build.gradle')
        wiremock.start()
        gradleRunner = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withEnvironment(Map.of(
                        "CODEARTIFACT_URL_OVERRIDE", wiremock.baseUrl(),
                        "AWS_ACCESS_KEY_ID", UUID.randomUUID().toString(),
                        "AWS_SECRET_ACCESS_KEY", UUID.randomUUID().toString()
                ))
    }

    def cleanup() {
        println(wiremock.findAllUnmatchedRequests())
        wiremock.stop()
    }

    void givenCodeArtifactWillReturnAuthToken() {
        wiremock.stubFor(
                WireMock.post(WireMock.urlMatching("/v1/authorization-token.*"))
                            .willReturn(WireMock.jsonResponse("""{"authorizationToken":"foobar","expiration":${System.currentTimeSeconds()+10}}""", 200))
        )
    }

    void givenCodeArtifactPluginIsConfigured() {
        settingsFile.setText("""
            plugins {
                id("co.bound.codeartifact")
            }
            codeartifact {
                domain = "$domain"
                accountId = "$accountId"
                region = "$region"
                repo = "$repo"
            }
            ${settingsFile.text}
        """)
    }

    BuildResult runTask(String task) {
        def result = gradleRunner
                .withArguments(task, '--stacktrace')
                .build()
        println(result.output)
        return result
    }

    BuildResult runTaskWithFailure(String task) {
        def result = gradleRunner
                .withArguments(task, '--stacktrace')
                .buildAndFail()
        println(result.output)
        return result
    }
}
