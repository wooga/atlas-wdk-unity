package wooga.gradle.wdk.publish

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.gradle.GrgitPlugin
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskProvider
import wooga.gradle.github.publish.GithubPublishPlugin
import wooga.gradle.github.publish.tasks.GithubPublish
import wooga.gradle.githubReleaseNotes.GithubReleaseNotesPlugin
import wooga.gradle.githubReleaseNotes.tasks.GenerateReleaseNotes
import wooga.gradle.upm.artifactory.UPMArtifactoryExtension
import wooga.gradle.upm.artifactory.UPMArtifactoryPlugin
import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.VersionCodeScheme
import wooga.gradle.version.VersionPlugin
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.VersionScheme
import wooga.gradle.version.internal.release.base.ReleaseVersion
import wooga.gradle.wdk.publish.internal.Git
import wooga.gradle.wdk.publish.internal.GithubRepository
import wooga.gradle.wdk.publish.internal.releasenotes.ReleaseNotesBodyStrategy


class WDKPublishPlugin implements Plugin<Project> {
    static final String EXTENSION_NAME = "wdkPublish"
    static final String ARCHIVE_CONFIGURATION_NAME = "wdkPublishArchive"
    static final String GITHUB_RELEASE_NOTES_TASK_NAME = "wdkGithubReleaseNotes"
    static final String GITHUB_PUBLISH_TASK_NAME = GithubPublishPlugin.PUBLISH_TASK_NAME

    @Deprecated
    /**
     * Snapshot task name. Equivalent to publish, doesn't set a release stage at all.
     * DEPRECATED, only here to provide support to older pipelines that uses NebulaRelease task names
     */
    static final String SNAPSHOT_TASK_NAME = "snapshot"
    @Deprecated
    /**
     * preflight task name. Equivalent to publish, doesn't set a release stage at all.
     * DEPRECATED, only here to provide support to older pipelines that uses NebulaRelease task names
     */
    static final String PREFLIGHT_TASK_NAME = "preflight"
    @Deprecated
    /**
     * RC task name. Equivalent to publish, doesn't set a release stage at all.
     * DEPRECATED, only here to provide support to older pipelines that uses NebulaRelease task names
     */
    static final String RC_TASK_NAME = "rc"
    @Deprecated
    /**
     * final task name. Equivalent to publish, doesn't set a release stage at all.
     * DEPRECATED, only here to provide support to older pipelines that uses NebulaRelease task names
     */
    static final String FINAL_TASK_NAME = "final"

    protected Project project
    protected Git git
    protected GithubRepository ghRepo

    @Override
    void apply(Project project) {
        //project.apply only runs the actual plugin apply() in the first time its called, so don't worry about that.
        project.plugins.apply(GrgitPlugin)
        project.plugins.apply(PublishingPlugin)
        project.plugins.apply(UPMArtifactoryPlugin)
        project.rootProject.plugins.apply(VersionPlugin)
        project.plugins.apply(GithubPublishPlugin)
        project.plugins.apply(GithubReleaseNotesPlugin)

        this.project = project
        this.git = Git.withGrgit(project.extensions.getByType(Grgit))
        this.ghRepo = GithubRepository.forGithubEnabledProject(project)

        def extension = WDKPublishExtension.newWithConventions(project, EXTENSION_NAME)
        def versionExt = configureVersion()

        def archiveCfg = createArchiveConfiguration(ARCHIVE_CONFIGURATION_NAME)
        def ghReleaseNotes = configureReleaseNotes(versionExt.version, versionExt.releaseStage, extension.releaseNotesFile)
        def ghPublish = configureGithubPublish(versionExt, ghReleaseNotes, archiveCfg)

        def upmVersion = project == project.rootProject ?
                versionExt.version :
                versionExt.inferVersion(VersionScheme.upm)
        configureUPM(versionExt.stage, upmVersion)
        configurePublish(ghPublish)
    }

    UPMArtifactoryExtension configureUPM(Provider<String> releaseStage, Provider<ReleaseVersion> version) {

        def upmExt = project.extensions.findByType(UPMArtifactoryExtension).with { upm ->
            upm.projects.configureEach { upmProject ->
                upmProject.version.convention(version.map {it.version }
                                                     .map(logInfo{ String versionStr -> "setting UPM project ${upmProject.name} version as ${versionStr}" })
                )
            }
            upm.repository.convention(releaseStage.map(logWarn{"Using release stage $it as UPM repository"}))
            return upm
        }
        return upmExt
    }


    void configurePublish(Provider<GithubPublish> ghPublish) {
        def publishTask = project.tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) {
            it.dependsOn(ghPublish)
        }
        //This only exists to provide support for pipelines that still use the old Nebula Release task names
        [SNAPSHOT_TASK_NAME, PREFLIGHT_TASK_NAME, RC_TASK_NAME, FINAL_TASK_NAME].collect { String stageTaskName ->
            project.tasks.register(stageTaskName) {
                it.dependsOn(publishTask)
            }
        }

    }

    NamedDomainObjectProvider<Configuration> createArchiveConfiguration(String configName) {
        def archive = project.configurations.register(configName) {
            def upmArchiveConfig = project.configurations.getByName(UPMArtifactoryPlugin.ROOT_ARCHIVE_CONFIGURATION_NAME)
            it.extendsFrom(upmArchiveConfig)
        }
        return archive
    }

    VersionPluginExtension configureVersion() {
        //version plugin can only be applied in the root project.
        def versionExt = project.rootProject.extensions.findByType(VersionPluginExtension)
        if (project.rootProject == project) {
            versionExt.versionScheme.set(VersionScheme.semver2)
            versionExt.versionCodeScheme.set(VersionCodeScheme.semver)
        } else {
            project.logger.warn("Using root project version configuration")
        }
        return versionExt
    }

    TaskProvider<GenerateReleaseNotes> configureReleaseNotes(Provider<ReleaseVersion> version,
                                                             Provider<ReleaseStage> releaseStage,
                                                             Provider<RegularFile> releaseNotesFile) {
        def currentVersion = version.map { it.version }
        def previousVersion = version.map { it.previousVersion }
        def branchName = git.currentBranchName(project)

        def releaseNotesTask = project.tasks.register(GITHUB_RELEASE_NOTES_TASK_NAME, GenerateReleaseNotes) { t ->
            t.from.set(ghRepo.asTagName(previousVersion))
            t.branch.set(branchName)
            t.strategy.set(new ReleaseNotesBodyStrategy())
            t.output.set(releaseNotesFile)
            t.releaseName.set(currentVersion)
            onlyIf { ghRepo.shouldGithubPublish(git, releaseStage, version, branchName) }
        }
        return releaseNotesTask
    }

    TaskProvider<GithubPublish> configureGithubPublish(VersionPluginExtension versionExt,
                                                       TaskProvider<GenerateReleaseNotes> releaseNotesTask,
                                                       Provider<Configuration> upmArchive) {
        def currentVersion = versionExt.version.map { it.version }
        def branchName = git.currentBranchName(project)

        def releaseNotesText = releaseNotesTask
                .flatMap { it.output }
                .map { it.asFile.text }
        def ghPublishTask = project.tasks.named(GithubPublishPlugin.PUBLISH_TASK_NAME, GithubPublish) {
            it.dependsOn(releaseNotesTask)
            it.from(upmArchive.map { it.allArtifacts.collect {it.file } })
            it.tagName.set(ghRepo.asTagName(currentVersion))
            it.releaseName.set(currentVersion)
            it.targetCommitish.set(branchName)
            it.prerelease.set(versionExt.isFinal.map{!it as boolean}.orElse(true))
            it.body.set(releaseNotesText)

            it.onlyIf { ghRepo.shouldGithubPublish(git, versionExt.releaseStage, versionExt.version, branchName) }
        }
        return ghPublishTask
    }

    private <T> Closure<T> logWarn(Closure<String> logString) {
        return { T it -> logWarn(it, logString(it)) }
    }

    //this whole memoization pattern is to assure that for a given input, this log only runs once.
    Closure<?> logWarn = { it, String message ->
        project.logger.warn(message)
        return it
    }.memoize()


    private <T> Closure<T> logInfo(Closure<String> logString) {
        return { T it -> logInfo(it, logString(it)) }
    }

    //this whole memoization pattern is to assure that for a given input, this log only runs once.
    Closure<?> logInfo = { it, String message ->
        project.logger.info(message)
        return it
    }.memoize()
}
