package co.bound.codeartifact

import org.gradle.util.GradleVersion

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.head
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.okXml
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern

class CodeArtifactPluginTest extends PluginTest {

    def "not configuring the codeartifact repository emits a helpful error message"() {
        settingsFile.setText("""
            plugins {
                id("co.bound.codeartifact")
            }
            $configuration
            ${settingsFile.text}
        """)
        def result = runTaskWithFailure(GradleVersion.current().getVersion(), "tasks")

        expect:
        result.output.contains("Please configure the AWS CodeArtifactRepository using the codeartifact block in the settings file:")

        where:
        configuration << [
                "",
                "codeartifact { domain = 'foo' }",
                "codeartifact { domain = 'foo'; accountId = 'foo' }",
                "codeartifact { domain = 'foo'; accountId = 'foo'; region = 'foo' }"
        ]
    }

    def "searches for plugins in configured CodeArtifact repository - Groovy DSL"() {
        given:
        givenCodeArtifactWillReturnAuthToken()
        givenCodeArtifactPluginIsConfigured()
        buildFile << """
            plugins {
                id("foo.bar").version("42")
            }
        """

        when:
        def result = runTaskWithFailure(gradleVersion, "tasks")

        then:
        result.output.contains("Plugin [id: 'foo.bar', version: '42'] was not found in any of the following sources:")
        result.output.contains("Searched in the following repositories:")
        result.output.contains("codeartifact(${wiremock.baseUrl()}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/$repo/)")
        wiremock.verify(1, newRequestPattern(POST, urlMatching("/v1/authorization-token.*")))
        wiremock.verify(1, newRequestPattern(GET, anyUrl()))

        where:
        gradleVersion << gradleVersions()
    }

    def "searches for plugins in configured CodeArtifact repository for - Kotlin DSL"() {
        given:
        givenCodeArtifactWillReturnAuthToken()
        useKotlinBuildScript()
        settingsFile.setText("""
            plugins {
                id("co.bound.codeartifact")
            }
            codeartifact {
                domain.set("$domain")
                accountId.set("$accountId")
                region.set("$region")
                repo.set("$repo")
            }
            ${settingsFile.text}
        """)
        buildFile << """
            plugins {
                id("foo.bar").version("42")
            }
        """

        when:
        def result = runTaskWithFailure(gradleVersion, "tasks")

        then:
        result.output.contains("Plugin [id: 'foo.bar', version: '42'] was not found in any of the following sources:")
        result.output.contains("Searched in the following repositories:")
        result.output.contains("codeartifact(${wiremock.baseUrl()}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/$repo/)")
        wiremock.verify(1, newRequestPattern(POST, urlMatching("/v1/authorization-token.*")))
        wiremock.verify(1, newRequestPattern(GET, anyUrl()))

        where:
        gradleVersion << gradleVersions()
    }

    def "can configure repository filters - Kotlin DSL"() {
        given:
        givenCodeArtifactWillReturnAuthToken()
        file("gradle.properties") << "systemProp.org.gradle.unsafe.kotlin.assignment=true"
        useKotlinBuildScript()
        settingsFile.setText("""
            pluginManagement {
                repositories {
                    maven {
                        name = "codeartifact"
                        // Kotlin's version requires some URL to be specified
                        // Whatever we specify here will be overriden and correctly configured by the codeartifact plugin
                        url = uri("https://www.foo.bar") 
                        content {
                            includeGroup("foo.other") 
                        }
                    }
                }
            }
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
        buildFile << """
            plugins {
                id("foo.bar").version("42")
            }
        """

        when:
        def result = runTaskWithFailure(GradleVersion.current().getVersion(), "tasks", "-i")

        then:
        result.output.contains("Plugin [id: 'foo.bar', version: '42'] was not found in any of the following sources:")
        result.output.contains("Searched in the following repositories:")
        result.output.contains("codeartifact(${wiremock.baseUrl()}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/$repo/)")
        wiremock.verify(1, newRequestPattern(POST, urlMatching("/v1/authorization-token.*")))
        wiremock.verify(0, newRequestPattern(GET, anyUrl()))
    }

    def "can configure repository filters - Groovy DSL"() {
        given:
        givenCodeArtifactWillReturnAuthToken()
        settingsFile.setText("""
            pluginManagement {
                repositories {
                    maven {
                        name("codeartifact")
                        content {
                            includeGroup("foo.other") 
                        }
                    }
                }
            }
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
        buildFile << """
            plugins {
                id("foo.bar").version("42")
            }
        """

        when:
        def result = runTaskWithFailure(GradleVersion.current().getVersion(), "tasks", "-i")

        then:
        result.output.contains("Plugin [id: 'foo.bar', version: '42'] was not found in any of the following sources:")
        result.output.contains("Searched in the following repositories:")
        result.output.contains("codeartifact(${wiremock.baseUrl()}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/$repo/)")
        wiremock.verify(1, newRequestPattern(POST, urlMatching("/v1/authorization-token.*")))
        wiremock.verify(0, newRequestPattern(GET, anyUrl()))
    }

    def "does not fetch the token for offline mode"() {
        given:
        givenCodeArtifactWillReturnAuthToken()
        givenCodeArtifactPluginIsConfigured()
        buildFile << """
            plugins {
                id("foo.bar").version("42")
            }
        """

        when:
        def result = runTaskWithFailure(gradleVersion, "tasks", "--offline")

        then:
        result.output.contains("Plugin [id: 'foo.bar', version: '42'] was not found in any of the following sources:")
        result.output.contains("Searched in the following repositories:")
        result.output.contains("codeartifact(${wiremock.baseUrl()}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/$repo/)")
        wiremock.verify(0, allRequests())

        where:
        gradleVersion << gradleVersions()
    }

    def "searches for dependencies in configured CodeArtifact repository"() {
        given:
        givenCodeArtifactWillReturnAuthToken()
        givenCodeArtifactPluginIsConfigured()
        wiremock.stubFor(head(anyUrl()).willReturn(ok()))
        wiremock.stubFor(get(urlMatching(".*/42-SNAPSHOT/maven-metadata.xml"))
                .willReturn(okXml("""
            <metadata>
              <groupId>foo</groupId>
              <artifactId>bar</artifactId>
              <versioning>
                <versions>
                  <version>42-SNAPSHOT</version>
                </versions>
              </versioning>
            </metadata>""")))
        wiremock.stubFor(get(urlMatching(".*/bar-42-SNAPSHOT.pom"))
                .willReturn(okXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>foo</groupId>
              <artifactId>bar</artifactId>
              <version>42-SNAPSHOT</version>
            </project>""")))
        wiremock.stubFor(get(urlMatching(".*/bar-42-SNAPSHOT.pom.sha1")).willReturn(ok("1")))
        wiremock.stubFor(get(urlMatching(".*/bar-42-SNAPSHOT.jar"))
        // minimal empty zip file
                .willReturn(ok().withBody(new byte[]{0x50, 0x4B, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00})))
        wiremock.stubFor(get(urlMatching(".*/bar-42-SNAPSHOT.jar.sha1")).willReturn(ok("2")))
        buildFile << """
            plugins {
                id("java") 
            }
            dependencies {
                implementation("foo:bar:42-SNAPSHOT")
            }
        """
        file("src/main/java/bar").mkdirs()
        file("src/main/java/bar/Foo.java") << """
            package bar;
            class Foo {}
        """

        when:
        runTask(gradleVersion, "build", "-i")
        runTask(gradleVersion, "build", "--refresh-dependencies")

        then:
        // credentials are cached
        wiremock.verify(1, newRequestPattern(POST, urlMatching("/v1/authorization-token.*")))
        wiremock.verify(2, newRequestPattern(GET, urlMatching(".*/bar-42-SNAPSHOT.jar")))
        wiremock.findAllUnmatchedRequests().isEmpty()

        where:
        gradleVersion << gradleVersions()
    }
}
