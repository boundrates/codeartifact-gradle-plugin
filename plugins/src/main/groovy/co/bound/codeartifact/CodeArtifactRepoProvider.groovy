package co.bound.codeartifact

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
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
        Property<File> getGradleUserHome()
        Property<Boolean> getOffline()
    }

    void configureRepo(MavenArtifactRepository spec) {
        var params = getParameters()
        var domain = getRequiredProperty(params.domain)
        var accountId = getRequiredProperty(params.accountId)
        var region = getRequiredProperty(params.region)
        var repo = getRequiredProperty(params.repo)

        spec.name = "codeartifact"
        configureCodeArtifactUrl(spec, domain, accountId, region, repo)
        spec.credentials { pwd ->
            pwd.username = "aws"
            if (!params.offline.get()) {
                pwd.password = getToken(domain, accountId, region, params.gradleUserHome.get())
            }
        }
    }

    String getToken(String domain, String accountId, String region, File gradleUserHome) {
        if (isValidToken(token)) {
            return token.authorizationToken()
        }
        var cacheFile = getCacheFile(domain, accountId, region, gradleUserHome)
        if (cacheFile.exists()) {
            token = tokenFromMap(new JsonSlurper().parse(cacheFile) as Map<String, Object>)
            if (isValidToken(token)) {
                return token.authorizationToken()
            }
        }
        token = makeCodeArtifactClient(region)
                .getAuthorizationToken(req -> req.domain(domain).domainOwner(accountId))
        cacheFile.text = JsonOutput.toJson([token: token.authorizationToken(), expiration: token.expiration().epochSecond])
        return token.authorizationToken()
    }

    private GetAuthorizationTokenResponse tokenFromMap(Map<String, Object> info) {
        return GetAuthorizationTokenResponse.builder()
            .authorizationToken(info.token as String)
            .expiration(Instant.ofEpochSecond(info.expiration as long)).build() as GetAuthorizationTokenResponse
    }

    private File getCacheFile(String domain, String accountId, String region, File gradleUserHome) {
        def file = new File(gradleUserHome, "caches/codeartifact/$domain-$accountId-${region}.json")
        file.parentFile.mkdirs()
        return file
    }

    private boolean isValidToken(GetAuthorizationTokenResponse token) {
        return token != null && token.expiration() > Instant.now()
    }

    private void configureCodeArtifactUrl(MavenArtifactRepository spec, String domain, String accountId, String region, String repo) {
        def overriddenCodeArtifactUrl = getOverriddenCodeArtifactUrl()
        spec.setAllowInsecureProtocol(overriddenCodeArtifactUrl != null)
        def urlPrefix = overriddenCodeArtifactUrl == null ? "https:/" : overriddenCodeArtifactUrl
        spec.setUrl(URI.create("${urlPrefix}/${domain}-${accountId}.d.codeartifact.${region}.amazonaws.com/maven/${repo}/"))
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
