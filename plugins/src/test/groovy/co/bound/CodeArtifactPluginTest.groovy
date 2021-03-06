package co.bound

class CodeArtifactPluginTest extends PluginTest {

    def "not configuring the codeartifact repository emits a helpful error message"() {
        settingsFile.setText("""
            plugins {
                id("co.bound.codeartifact")
            }
            $configuration
            ${settingsFile.text}
        """)
        def result = runTaskWithFailure("tasks")

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

    def "searches for plugins in configured CodeArtifact repository"() {
        given:
        givenCodeArtifactWillReturnAuthToken()
        givenCodeArtifactPluginIsConfigured()
        buildFile << """
            plugins {
                id("foo.bar").version("42")
            }
        """

        when:
        def result = runTaskWithFailure("tasks")

        then:
        result.output.contains("Plugin [id: 'foo.bar', version: '42'] was not found in any of the following sources:")
        result.output.contains("Searched in the following repositories:")
        result.output.contains("codeartifact(${wiremock.baseUrl()}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/$repo/)")
    }

    def "searches for dependencies in configured CodeArtifact repository"() {
        given:
        givenCodeArtifactWillReturnAuthToken()
        givenCodeArtifactPluginIsConfigured()
        buildFile << """
            plugins {
                id("java") 
            }
            dependencies {
                implementation("foo:bar:42")
            }
        """
        file("src/main/java/Foo.java") << "class Foo {}"

        when:
        def result = runTaskWithFailure("build")

        then:
        result.output.contains("Could not find foo:bar:42")
        result.output.contains("Searched in the following locations")
        result.output.contains("- ${wiremock.baseUrl()}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/$repo/foo/bar/42/bar-42.pom")
    }
}
