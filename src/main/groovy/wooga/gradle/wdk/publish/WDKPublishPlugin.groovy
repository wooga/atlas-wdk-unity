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
import org.kohsuke.github.GHTag
import wooga.gradle.github.base.GithubPluginExtension
import wooga.gradle.github.publish.GithubPublishPlugin
import wooga.gradle.github.publish.tasks.GithubPublish
import wooga.gradle.githubReleaseNotes.tasks.GenerateReleaseNotes
import wooga.gradle.version.VersionCodeScheme
import wooga.gradle.version.VersionPlugin
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.VersionScheme
import wooga.gradle.wdk.publish.internal.releasenotes.ReleaseNotesBodyStrategy
import wooga.gradle.wdk.upm.UPMExtension
import wooga.gradle.wdk.upm.UPMPlugin

import java.util.stream.StreamSupport


class WDKPublishPlugin implements Plugin<Project> {
    static final String WDK_PUBLISH_EXTENSION_NAME = "wdkPublish"
    static final String WDK_PUBLISH_ARCHIVE_NAME = "wdkPublishArchive"
    static final String GITHUB_PUBLISH_TASK_NAME = "wdkGithubPublish"
    static final String GITHUB_RELEASE_NOTES_TASK_NAME = "wdkGithubReleaseNotes"

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

    Project project
    Grgit grgit

    @Override
    void apply(Project project) {
        project.plugins.apply(GrgitPlugin)
        project.plugins.apply(PublishingPlugin)
        project.plugins.apply(UPMPlugin)
        project.plugins.apply(VersionPlugin)

        this.project = project
        this.grgit = project.extensions.findByType(Grgit)

        def extension = project.extensions.create(WDK_PUBLISH_EXTENSION_NAME, WDKPublishExtension)
        def upmExt = project.extensions.findByType(UPMExtension)
        def versionExt = configureVersion()
        upmExt.version = versionExt.version.map { it.version }

        def archiveCfg = createArchiveConfiguration(WDKPublishPlugin.WDK_PUBLISH_ARCHIVE_NAME)
        def ghPublish = configureGithubPublish(versionExt, archiveCfg, extension.releaseNotesFile)

        configurePublish(ghPublish)
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
        def upmArchiveConfig = project.configurations.named(UPMPlugin.ARCHIVE_CONFIGURATION_NAME)
        def archive = project.configurations.register(configName) {
            it.extendsFrom(upmArchiveConfig.get())
        }
        return archive
    }
    //TODO: If paket task runs first, the version generated here wont be the same as the one there.
    // Check if the version plugin was already applied on a parent project, and if so, just fetch that instance and regenerate the same version in semver2.
    // Maybe changes on version plugin are needed for the re-generation to work)
    VersionPluginExtension configureVersion() {
        //
        def versionExt = project.extensions.findByType(VersionPluginExtension)
        versionExt.with {
            versionScheme.set(VersionScheme.semver2)
            versionCodeScheme.set(VersionCodeScheme.semver)
        }
        return versionExt
    }

    TaskProvider<GithubPublish> configureGithubPublish(VersionPluginExtension versionExt, Provider<Configuration> archiveCfg, Provider<RegularFile> releaseNotesFile) {
        def previousVersion = versionExt.version.map { it.previousVersion }
        def currentVersion = versionExt.version.map { it.version }
        def branchName = project.provider { grgit.branch.current().name }
        def releaseNotesTask = project.tasks.register(GITHUB_RELEASE_NOTES_TASK_NAME, GenerateReleaseNotes) { t ->
            t.from.set(asGHTagName(previousVersion).orNull)
            t.branch.set(branchName)
            t.strategy.set(new ReleaseNotesBodyStrategy())
            t.output.set(releaseNotesFile)
        }
        def ghPublishTask = project.tasks.named(GithubPublishPlugin.PUBLISH_TASK_NAME, GithubPublish) {
            it.from(archiveCfg)
            it.dependsOn(archiveCfg)
            it.dependsOn(releaseNotesTask)
            it.tagName.set(asGHTagName(currentVersion))
            it.releaseName.set(versionExt.version.map { it.version })
            it.targetCommitish.set(branchName)
            it.prerelease.set(versionExt.isFinal.map { !it as boolean })
            it.body.set(releaseNotesFile.map { it.asFile.text })

            it.onlyIf { GithubPublish task ->
                def previousTagName = asGHTagName(previousVersion)
                return previousTagName.present?
                        areSameCommit(task.targetCommitish, previousTagName).orElse(true) : true
            }
        }
        return ghPublishTask
    }
    //TODO: test this
    private Provider<Boolean> areSameCommit(Provider<String> commitishProvider, Provider<String> tagNameProvider) {
        def previousVersionSHA = findRemoteTag(tagNameProvider)
                .map { tag -> tag.commit.SHA1}
                .orElse(project.provider { previousTag -> grgit.show(commit: previousTag)?.commit?.id })

        def currentVersionSHA =
                commitishProvider.map { commitish -> grgit.show(commit: commitish)?.commit?.id }

        if(previousVersionSHA.present && currentVersionSHA.present) {
            return previousVersionSHA.zip(currentVersionSHA) { first, second -> first == second }
        } else {
            return project.provider{ null as Boolean }
        }

    }

    private static Provider<String> asGHTagName(Provider<String> base) {
        return base.map { it -> "v$it".toString() }
    }

    private Provider<GHTag> findRemoteTag(Provider<String> tag) {
        def ghExtension = project.extensions.findByType(GithubPluginExtension)
        if (ghExtension) {
            ghExtension.repositoryName.map {repoName ->
                ghExtension.clientProvider.get().getRepository(repoName)
            }.map { ghRepo ->
                def tagName = tag.get()
                def tagsIterator = ghRepo.listTags().iterator()
                def tagsStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(tagsIterator, Spliterator.ORDERED), false)
                return tagsStream.filter { it.name == tagName }.findFirst().orElse(null)
            }
        }
        return project.provider{null as GHTag} //empty provider
    }

}
