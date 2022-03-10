package co.bound.codeartifact

import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.codeartifact.CodeartifactClient
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse

import java.time.Instant

abstract class CodeArtifactRepoProvider implements BuildService<Params> {
    private GetAuthorizationTokenResponse token = null

    interface Params extends BuildServiceParameters {
        Property<String> getDomain()
        Property<String> getAccountId()
        Property<String> getRegion()
        Property<String> getRepo()
    }

    void configureRepo(MavenArtifactRepository spec) {
        def params = getParameters()
        String domain = getRequiredProperty(params.getDomain())
        String accountId = getRequiredProperty(params.getAccountId())
        String region = getRequiredProperty(params.getRegion())
        String repo = getRequiredProperty(params.getRepo())

        spec.name = "codeartifact"
        configureCodeArtifactUrl(spec, domain, accountId, region, repo)
        spec.credentials {
            it.username("aws")
            it.password(this.getToken(domain, accountId, region))
        }
    }

    String getToken(String domain, String accountId, String region) {
        if (token == null || token.expiration() <= Instant.now()) {
            token = makeCodeArtifactClient(region)
                    .getAuthorizationToken(req -> req.domain(domain).domainOwner(accountId))
        }
        return token.authorizationToken()
    }

    private void configureCodeArtifactUrl(MavenArtifactRepository spec, String domain, String accountId, String region, String repo) {
        def overridenCodeArtifactUrl = getOverriddenCodeArtifactUrl()
        spec.setAllowInsecureProtocol(overridenCodeArtifactUrl != null)
        def urlPrefix = overridenCodeArtifactUrl == null ? "https:/" : overridenCodeArtifactUrl
        spec.url(URI.create("${urlPrefix}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/${repo}/"))
    }

    private static CodeartifactClient makeCodeArtifactClient(String region) {
        def client = CodeartifactClient.builder()
        def awsUrlOverride = getOverriddenCodeArtifactUrl()
        if (awsUrlOverride != null) {
            client = client.endpointOverride(URI.create(awsUrlOverride))
        }
        return client.region(Region.of(region)).build()
    }

    private static String getOverriddenCodeArtifactUrl() {
        return System.getenv("CODEARTIFACT_URL_OVERRIDE")
    }

    private static String getRequiredProperty(Property<String> property) {
        if (!property.isPresent()) {
            throw new IllegalStateException("""Please configure the AWS CodeArtifactRepository using the codeartifact block in the settings file:
    codeartifact {
        domain = "repo-domain"
        accountId = "123456789012"
        region = "us-east-1"
        repo = "repo-name"
    }""")
        }
        return property.get()
    }
}
