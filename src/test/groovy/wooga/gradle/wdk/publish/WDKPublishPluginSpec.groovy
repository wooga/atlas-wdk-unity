package wooga.gradle.wdk.publish

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit
import spock.lang.Shared
import wooga.gradle.github.publish.GithubPublishPlugin
import wooga.gradle.github.publish.tasks.GithubPublish
import wooga.gradle.githubReleaseNotes.tasks.GenerateReleaseNotes
import wooga.gradle.version.VersionCodeScheme
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.VersionScheme
import wooga.gradle.wdk.publish.internal.releasenotes.ReleaseNotesBodyStrategy
import wooga.gradle.wdk.upm.UPMExtension

class WDKPublishPluginSpec extends ProjectSpec {

    @Shared
    Grgit grgit

    def setup() {
        grgit = Grgit.init()
    }

    def "configures version into extensions"() {
        when:
        project.plugins.apply(WDKPublishPlugin)

        then:
        def (versionExt, upmExt) = loadExtensions(VersionPluginExtension, UPMExtension)
        versionExt.versionScheme.get() == VersionScheme.semver2
        versionExt.versionCodeScheme.get() == VersionCodeScheme.semver
        upmExt.version.get() == versionExt.version.map { it.version }.get()
    }

    def "doesn't publishes github release if release with same tag already exists"() {
        //integration, needs actual github for creating tag and stuff
    }


    def "configure github release notes with version #version"() {
        when:
        project.plugins.apply(WDKPublishPlugin)
        and:
        def versionExt = loadExtension(VersionPluginExtension)


        then:
        def releaseNotesTask = project.tasks.named(WDKPublishPlugin.GITHUB_RELEASE_NOTES_TASK_NAME, GenerateReleaseNotes).get()
        releaseNotesTask.from.get() == versionExt.version.map { "v$it.previousVersion" }.orNull
        !releaseNotesTask.to.present
        releaseNotesTask.branch.get() == grgit.branch.current().name
        releaseNotesTask.strategy.orNull instanceof ReleaseNotesBodyStrategy
        releaseNotesTask.output.asFile.get() == new File("${project.buildDir}/outputs/release-notes.md")

        where:
        previousVersion << [null, "0.0.1"]
    }

    def "configure github publish task"() {

    }

    def "configure github publishing"() {
        when:
        project.plugins.apply(WDKPublishPlugin)
        and:
        def versionExt = loadExtension(VersionPluginExtension)

        then:
        def releaseNotesTask = project.tasks.named(WDKPublishPlugin.GITHUB_RELEASE_NOTES_TASK_NAME, GenerateReleaseNotes).get()
        def githubPublishTask = project.tasks.named(GithubPublishPlugin.PUBLISH_TASK_NAME, GithubPublish).get()
        //TODO test this on integration
//        def archiveCfg = project.configurations.named(WDKPublishPlugin.WDK_PUBLISH_ARCHIVE_NAME)
//        githubPublishTask.from(archiveCfg)
//        githubPublishTask.dependsOn(archiveCfg)
        githubPublishTask.tagName.get() == versionExt.version.map { "v$it.version" }.orNull
        githubPublishTask.releaseName.get() == versionExt.version.map { it.version }.orNull
        githubPublishTask.targetCommitish.get() == grgit.branch.current().name
        githubPublishTask.prerelease.get() == versionExt.isFinal.map { !it }.get()
        githubPublishTask.body == releaseNotesTask.output.map { it.asFile.text }
    }

    private <T, U> Tuple2<T, U> loadExtensions(Class<T> tClass, Class<U> uClass) {
        return [project.extensions.findByType(tClass), project.extensions.findByType(uClass)]
    }

    private <T> T loadExtension(Class<T> tClass) {
        return project.extensions.findByType(tClass)
    }


}
