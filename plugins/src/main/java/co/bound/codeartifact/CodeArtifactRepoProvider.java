package co.bound.codeartifact;

import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codeartifact.CodeartifactClient;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenRequest;
import software.amazon.awssdk.services.codeartifact.model.GetAuthorizationTokenResponse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

public abstract class CodeArtifactRepoProvider implements BuildService<CodeArtifactRepoProvider.Params> {
    private static final String CODEARTIFACT_REPOSITORY_NAME = "codeartifact";

    public void configureRepo(RepositoryHandler repositories) {
        if (repositories.findByName(CODEARTIFACT_REPOSITORY_NAME) == null) {
            repositories.maven(spec -> spec.setName(CODEARTIFACT_REPOSITORY_NAME));
        }
        repositories.named(CODEARTIFACT_REPOSITORY_NAME, spec -> configureRepo((MavenArtifactRepository) spec));
    }

    private void configureRepo(MavenArtifactRepository spec) {
        Params params = getParameters();
        String domain = getRequiredProperty(params.getDomain());
        String accountId = getRequiredProperty(params.getAccountId());
        String region = getRequiredProperty(params.getRegion());
        String repo = getRequiredProperty(params.getRepo());

        configureCodeArtifactUrl(spec, domain, accountId, region, repo);
        spec.credentials(pwd -> {
            pwd.setUsername("aws");
            if (!params.getOffline().get()) {
                try {
                    pwd.setPassword(getToken(domain, accountId, region, params.getGradleUserHome().get().toPath()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private GetAuthorizationTokenResponse token = null;

    public String getToken(String domain, String accountId, String region, Path gradleUserHome) throws IOException {
        if (isValidToken(token)) {
            return token.authorizationToken();
        }

        Path cacheFile = getCacheFile(domain, accountId, region, gradleUserHome);
        if (Files.exists(cacheFile)) {
            token = readTokenFromCache(cacheFile);
            if (isValidToken(token)) {
                return token.authorizationToken();
            }
        }

        try (CodeartifactClient client = makeCodeArtifactClient(region)) {
            token = client.getAuthorizationToken(GetAuthorizationTokenRequest.builder()
                    .domain(domain).domainOwner(accountId)
                    .build());
        }
        saveTokenToCache(cacheFile, token);
        return token.authorizationToken();
    }

    private void saveTokenToCache(Path cacheFile, GetAuthorizationTokenResponse token) throws IOException {
        var properties = new Properties();
        properties.setProperty("token", token.authorizationToken());
        properties.setProperty("expiration", String.valueOf(token.expiration().getEpochSecond()));
        try (var out = Files.newOutputStream(cacheFile)) {
            properties.store(out, null);
        }
    }

    private GetAuthorizationTokenResponse readTokenFromCache(Path cacheFile) throws IOException {
        var  info = new Properties();
        try (var is = Files.newInputStream(cacheFile)) {
            info.load(is);
        }
        long expiration = Long.parseLong(info.getProperty("expiration"));
        return DefaultGroovyMethods.asType(GetAuthorizationTokenResponse.builder()
                        .authorizationToken(info.getProperty("token"))
                        .expiration(Instant.ofEpochSecond(expiration)).build(),
                GetAuthorizationTokenResponse.class);
    }

    private Path getCacheFile(String domain, String accountId, final String region, Path gradleUserHome) throws IOException {
        var file = gradleUserHome.resolve("caches/codeartifact/" + domain + "-" + accountId + "-" + region + ".properties");
        Files.createDirectories(file.getParent());
        return file;
    }

    private boolean isValidToken(GetAuthorizationTokenResponse token) {
        return token != null && token.expiration().compareTo(Instant.now()) > 0;
    }

    private void configureCodeArtifactUrl(
            MavenArtifactRepository spec, String domain, String accountId, String region, String repo) {
        String overriddenCodeArtifactUrl = getOverriddenCodeArtifactUrl();
        spec.setAllowInsecureProtocol(overriddenCodeArtifactUrl != null);
        String urlPrefix = overriddenCodeArtifactUrl == null ? "https:/" : overriddenCodeArtifactUrl;
        spec.setUrl(URI.create(urlPrefix + "/" + domain + "-" + accountId + ".d.codeartifact." + region + ".amazonaws.com/maven/" + repo + "/"));
    }

    private static CodeartifactClient makeCodeArtifactClient(String region) {
        var client = CodeartifactClient.builder();
        String awsUrlOverride = getOverriddenCodeArtifactUrl();
        if (awsUrlOverride != null) {
            client = client.endpointOverride(URI.create(awsUrlOverride));
        }

        return client.region(Region.of(region)).build();
    }

    private static String getOverriddenCodeArtifactUrl() {
        return System.getenv("CODEARTIFACT_URL_OVERRIDE");
    }

    private static String getRequiredProperty(Property<String> property) {
        if (!property.isPresent()) {
            throw new IllegalStateException(
                    "Please configure the AWS CodeArtifactRepository using the codeartifact block in the settings file:\n    codeartifact {\n        domain = \"repo-domain\"\n        accountId = \"123456789012\"\n        region = \"us-east-1\"\n        repo = \"repo-name\"\n    }");
        }

        return property.get();
    }

    public interface Params extends BuildServiceParameters {
        Property<String> getDomain();

        Property<String> getAccountId();

        Property<String> getRegion();

        Property<String> getRepo();

        Property<File> getGradleUserHome();

        Property<Boolean> getOffline();
    }
}
