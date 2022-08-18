package wooga.gradle.wdk.publish.internal


import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import wooga.gradle.github.base.GithubPluginExtension
import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.base.ReleaseVersion

class GithubRepository {

    final Provider<GHRepository> base
    final ProviderFactory providers
    final Logger logger

    static GithubRepository forGithubEnabledProject(Project project) {
        def extension = project.extensions.getByType(GithubPluginExtension)
        return fromExtension(extension, project.providers, project.logger)
    }

    static GithubRepository fromExtension(GithubPluginExtension ghExtension, ProviderFactory providers, Logger logger = null) {
        def repo = ghExtension.repositoryName.flatMap { repoName ->
            ghExtension.clientProvider.map {
                ghClient -> tryGetRepository(ghClient, repoName, logger).orElse(null)
            }
        }
        return new GithubRepository(repo, providers, logger)
    }

    static Optional<GHRepository> tryGetRepository(GitHub client, String repoName, Logger logger = null) {
        try {
            return Optional.of(client.getRepository(repoName))
        } catch (Exception e) {
            logger?.warn("Couldn't load Github Repository - ${e.class.simpleName}: $e.message")
            return Optional.empty()
        }
    }

    GithubRepository(Provider<GHRepository> repository, ProviderFactory providers, Logger logger = null) {
        this.base = repository
        this.providers = providers
        this.logger = logger
    }

    Provider<Boolean> releaseExists(Provider<String> currentVersion) {
        return base.map { ghRepo ->
            ghRepo.getReleaseByTagName(currentVersion.get()) != null
        }
    }

    Provider<Boolean> releaseNotExists(Provider<String> currentVersion) {
        return releaseExists(currentVersion).map{!it as Boolean }
    }

    boolean shouldGithubPublish(Git git, Provider<ReleaseStage> releaseStage,
                                Provider<ReleaseVersion> version,
                                Provider<String> branchName) {
        def currentVersion = version.map { it.version }
        def previousVersion = version.map { it.previousVersion }

        def isRelease = releaseStage
                .map { it in [ReleaseStage.Prerelease, ReleaseStage.Final] }
                .map(warnFalse{"Current releaseStage is not in [rc, final], skipping"})
                .orElse(emptyWarnProvider{"Could not establish whether the release state is a final one"})

        def noCurrentRelease = releaseNotExists(asTagName(currentVersion))
                .map(warnFalse{"There is already a github release for ${currentVersion.get()}, skipping"})

        def hasChanges = git.areDifferentCommits(asTagName(previousVersion), branchName)
                .map(warnFalse{"Current commit is the same as of last tag ${asTagName(previousVersion.get())}, skipping"})

        def currentReleaseNotExists = noCurrentRelease.orElse(hasChanges)
                .orElse(emptyWarnProvider{"Could not establish whether a github release already exists for this version"})

        return  isRelease.getOrElse(false) &&
                currentReleaseNotExists.getOrElse(true)
    }

    static Provider<String> asTagName(Provider<String> base) {
        return base.map { it -> asTagName(it) }
    }

    static String asTagName(String base) {
        return "v$base".toString()
    }

    private Closure<Boolean> warnFalse(Closure<String> message) {
        return { it -> warnFalse(it, message(it))}
    }

    private Closure<Boolean> warnFalse = { Boolean value, String message ->
        if (!value) logger?.warn(message)
        return value
    }.memoize()

    private <T> Provider<T> emptyWarnProvider(Closure<String> message) {
        return providers.provider {
            emptyWarnProvider(message()) as T
        }
    }

    private Closure<?> emptyWarnProvider = { String message ->
            logger?.warn(message)
            return null
    }.memoize()
}
