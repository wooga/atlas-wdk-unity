package wooga.gradle.wdk.publish.internal


import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import wooga.gradle.github.base.GithubPluginExtension

class GithubRepository {

    Provider<GHRepository> base

    static GithubRepository forGithubEnabledProject(Project project) {
        def extension = project.extensions.getByType(GithubPluginExtension)
        return fromExtension(extension, project.logger)
    }

    static GithubRepository fromExtension(GithubPluginExtension ghExtension, Logger logger = null) {
        def repo = ghExtension.repositoryName.flatMap { repoName ->
            ghExtension.clientProvider.map { ghClient -> ghClient.getRepository(repoName) }
        }

        return new GithubRepository(repo)
    }

    static Optional<GHRepository> tryGetRepository(GitHub client, String repoName, Logger logger = null) {
        try {
            return Optional.of(client.getRepository(repoName))
        } catch (Exception e) {
            logger?.warn("Can't run github API - ${e.class.simpleName}: $e.message")
            return Optional.empty()
        }
    }

    GithubRepository(Provider<GHRepository> repository) {
        this.base = repository
    }

    Provider<Boolean> releaseExists(Provider<String> currentVersion) {
        return base.map { ghRepo ->
            ghRepo.getReleaseByTagName(currentVersion.get()) != null
        }
    }

    Provider<Boolean> releaseNotExists(Provider<String> currentVersion) {
        return releaseExists(currentVersion).map{!it as Boolean }
    }
}
