package wooga.gradle.wdk.publish

import org.gradle.api.plugins.BasePlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import spock.lang.Unroll
import wooga.gradle.github.publish.GithubPublishPlugin
import wooga.gradle.github.publish.tasks.GithubPublish
import wooga.gradle.githubReleaseNotes.tasks.GenerateReleaseNotes
import wooga.gradle.upm.artifactory.UPMArtifactoryExtension
import wooga.gradle.upm.artifactory.UPMArtifactoryPlugin
import wooga.gradle.version.VersionCodeScheme
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.VersionScheme
import wooga.gradle.wdk.publish.internal.releasenotes.ReleaseNotesBodyStrategy
import wooga.gradle.wdk.publish.tools.BaseGradleSpec
import wooga.gradle.wdk.tools.GrGitExtended
import wooga.gradle.wdk.tools.GradleTestUtils

class WDKPublishPluginSpec extends BaseGradleSpec {

    def "configures version extensions for project == rootProject"() {
        given:
        assert !project.plugins.hasPlugin(WDKPublishPlugin)
        assert project.rootProject == project
        when:
        project.plugins.apply(WDKPublishPlugin)

        then:
        def versionExt = utils.requireExtension(VersionPluginExtension)
        versionExt.versionScheme.get() == VersionScheme.semver2
        versionExt.versionCodeScheme.get() == VersionCodeScheme.semver
    }

    @Unroll("configures upm extension version as #expectedVersion when version plugin version scheme is #versionScheme")
    def "configures upm extension version as the same as version plugin when wdk publish plugin is in root project"() {
        given:
        project.ext["release.scope"] = releaseScope
        project.ext["release.stage"] = releaseStage

        and: "git repository needed for test to track its own versions through tags"
        GrGitExtended.initWithIgnoredFiles(projectDir, "*")

        when:
        project.plugins.apply(WDKPublishPlugin)
        and: "version plugin version forced to..."
        utils.requireExtension(VersionPluginExtension).with {
            it.versionScheme = versionScheme
        }
        and: "upm plugin configured with some project"
        utils.requireExtension(UPMArtifactoryExtension).projects {
            it.create("sample")
            it.create("otherSample")
        }

        then:
        def upmExt = utils.requireExtension(UPMArtifactoryExtension)
        project.version.toString() == expectedVersion
        upmExt.projects.each {it.version.get() == expectedVersion }
        upmExt.repository.get() == releaseStage

        where:
        versionScheme         | expectedVersion     | releaseStage
        VersionScheme.semver  | "1.0.0-master00001" | "snapshot"
        VersionScheme.semver2 | "1.0.0-master.1"    | "snapshot"
        VersionScheme.semver  | "1.0.0-rc00001"     | "rc"
        VersionScheme.semver2 | "1.0.0-rc.1"        | "rc"
        VersionScheme.semver  | "1.0.0"             | "final"
        VersionScheme.semver2 | "1.0.0"             | "final"
        releaseScope = "major"
    }

    @Unroll("configures upm extension version as #semver2Version when version plugin version scheme is #versionScheme")
    def "configures upm extension version always as semver2 when not in root project"() {
        given:
        def subproject = addSubproject("subproject")
        def subProjUtils = new GradleTestUtils(subproject)
        project.ext["release.scope"] = releaseScope
        project.ext["release.stage"] = releaseStage

        and: "git repository needed for test to track its own versions through tags"
        GrGitExtended.initWithIgnoredFiles(projectDir, "*")

        when:
        subproject.plugins.apply(WDKPublishPlugin)
        and: "version plugin version forced to..."
        utils.requireExtension(VersionPluginExtension).with {
            it.versionScheme = versionScheme
        }
        and: "upm plugin configured with some project"
        subProjUtils.requireExtension(UPMArtifactoryExtension).projects {
            it.create("sample")
            it.create("otherSample")
        }

        then:
        def upmExt = subProjUtils.requireExtension(UPMArtifactoryExtension)
        upmExt.projects.each {
            it.version.get() == semver2Version
        }

        where:
        versionScheme         | semver2Version   | releaseStage
        VersionScheme.semver  | "1.0.0-master.1" | "snapshot"
        VersionScheme.semver2 | "1.0.0-master.1" | "snapshot"
        VersionScheme.semver  | "1.0.0-rc.1"     | "rc"
        VersionScheme.semver2 | "1.0.0-rc.1"     | "rc"
        VersionScheme.semver  | "1.0.0"          | "final"
        VersionScheme.semver2 | "1.0.0"          | "final"
        releaseScope = "major"
    }

    @Unroll("configures upm extension version as #semver2Version when version plugin version is set as #versionScheme-#releaseStage-#releaseScope")
    def "configures upm extension version as #semver2Version when version plugin version is set as #versionScheme-#releaseStage-#releaseScope"() {
        given:
        def subproject = addSubprojectWithDirectory("subproject")
        def subProjUtils = new GradleTestUtils(subproject)
        project.ext["release.scope"] = releaseScope
        project.ext["release.stage"] = releaseStage

        and: "git repository needed for test to track its own versions through tags"
        GrGitExtended.initWithIgnoredFiles(projectDir, "*")

        when:
        subproject.plugins.apply(WDKPublishPlugin)
        and: "version plugin version forced to..."
        utils.requireExtension(VersionPluginExtension).with {
            it.versionScheme = versionScheme
        }
        and: "upm plugin configured with some project"
        subProjUtils.requireExtension(UPMArtifactoryExtension).projects {
            it.create("sample")
            it.create("otherSample")
        }

        then:
        def upmExt = subProjUtils.requireExtension(UPMArtifactoryExtension)
        project.version.toString() == baseVersion
        subproject.version.toString() == baseVersion
        upmExt.projects.each {
            it.version.get() == semver2Version
        }


        where:
        versionScheme         | baseVersion         | semver2Version   | releaseStage | releaseScope
        VersionScheme.semver  | "1.0.0-master00001" | "1.0.0-master.1" | "snapshot"   | "major"
        VersionScheme.semver2 | "1.0.0-master.1"    | "1.0.0-master.1" | "snapshot"   | "major"
        VersionScheme.semver  | "1.0.0-rc00001"     | "1.0.0-rc.1"     | "rc"         | "major"
        VersionScheme.semver2 | "1.0.0-rc.1"        | "1.0.0-rc.1"     | "rc"         | "major"
        VersionScheme.semver  | "1.0.0"             | "1.0.0"          | "final"      | "major"
        VersionScheme.semver2 | "1.0.0"             | "1.0.0"          | "final"      | "major"

        VersionScheme.semver  | "0.1.0-master00001" | "0.1.0-master.1" | "snapshot"   | "minor"
        VersionScheme.semver2 | "0.1.0-master.1"    | "0.1.0-master.1" | "snapshot"   | "minor"
        VersionScheme.semver  | "0.1.0-rc00001"     | "0.1.0-rc.1"     | "rc"         | "minor"
        VersionScheme.semver2 | "0.1.0-rc.1"        | "0.1.0-rc.1"     | "rc"         | "minor"
        VersionScheme.semver  | "0.1.0"             | "0.1.0"          | "final"      | "minor"
        VersionScheme.semver2 | "0.1.0"             | "0.1.0"          | "final"      | "minor"

        VersionScheme.semver  | "0.0.1-master00001" | "0.0.1-master.1" | "snapshot"   | "patch"
        VersionScheme.semver2 | "0.0.1-master.1"    | "0.0.1-master.1" | "snapshot"   | "patch"
        VersionScheme.semver  | "0.0.1-rc00001"     | "0.0.1-rc.1"     | "rc"         | "patch"
        VersionScheme.semver2 | "0.0.1-rc.1"        | "0.0.1-rc.1"     | "rc"         | "patch"
        VersionScheme.semver  | "0.0.1"             | "0.0.1"          | "final"      | "patch"
        VersionScheme.semver2 | "0.0.1"             | "0.0.1"          | "final"      | "patch"

    }


    @Unroll("configure github release notes from #previousVersion to HEAD")
    def "configure github release notes"() {
        given:
        project.ext["release.scope"] = releaseScope
        project.ext["release.stage"] = releaseStage
        and:
        def git = GrGitExtended.initWithIgnoredFiles(projectDir, "*.*")
        if (previousVersion) {
            git.commitChange()
            git.tag.add(name: previousGHTag)
        }

        when:
        project.plugins.apply(WDKPublishPlugin)

        then:
        def releaseNotesTask = project.tasks.named(WDKPublishPlugin.GITHUB_RELEASE_NOTES_TASK_NAME, GenerateReleaseNotes).get()
        releaseNotesTask.from.orNull == previousGHTag
        !releaseNotesTask.to.present
        releaseNotesTask.branch.get() == grgit.branch.current().name
        releaseNotesTask.strategy.orNull instanceof ReleaseNotesBodyStrategy
        releaseNotesTask.output.asFile.get() == new File("${project.buildDir}/outputs/release-notes.md")

        where:
        previousVersion << [null, "0.1.0"]
        previousGHTag = previousVersion ? "v$previousVersion".toString() : null
        releaseScope = "major"
        releaseStage = "final"
        expectedVersion = "1.0.0"
    }

    @Unroll
    def "configure github publishing"() {
        given:
        project.ext["wdk.publish.releaseNotes"] = fixtures.fakeReleaseNotes.absolutePath
        project.ext["release.stage"] = releaseStage
        project.ext["release.scope"] = releaseScope
        and: "git repository needed for test to track its own versions"
        GrGitExtended.initWithIgnoredFiles(projectDir, "*")

        when:
        project.plugins.apply(WDKPublishPlugin)

        then:
        def releaseNotesTask = project.tasks.named(WDKPublishPlugin.GITHUB_RELEASE_NOTES_TASK_NAME, GenerateReleaseNotes).get()
        def githubPublishTask = project.tasks.named(GithubPublishPlugin.PUBLISH_TASK_NAME, GithubPublish).get()
        def ghPublishDependencies = githubPublishTask.taskDependencies.getDependencies(githubPublishTask)
        ghPublishDependencies.contains(releaseNotesTask)
        githubPublishTask.tagName.get() == expectedGHTag
        githubPublishTask.releaseName.get() == expectedVersion
        githubPublishTask.targetCommitish.get() == grgit.branch.current().name
        githubPublishTask.prerelease.get() == (releaseStage != "final")
        githubPublishTask.body.get() == fixtures.fakeReleaseNotes.text

        where:
        expectedVersion  | releaseStage
        "1.0.0-master.1" | "snapshot"
        "1.0.0-rc.1"     | "rc"
        "1.0.0"          | "final"
        releaseScope = "major"
        expectedGHTag = "v${expectedVersion}"
    }

    @Unroll("maps NebulaRelease task #nebulaTaskName to publish task")
    def "maps NebulaRelease tasks to publish task"() {
        when:
        project.plugins.apply(WDKPublishPlugin)

        then:
        def publishTask = project.tasks.getByName(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
        def task = project.tasks.findByName(nebulaTaskName)
        task != null
        task.taskDependencies.getDependencies(task).contains(publishTask)

        where:
        nebulaTaskName << ["snapshot", "preflight", "rc", "final"]
    }

    def "Archive configuration 'wdkPublishArchive' stores UPM artifact"() {
        given:
        assert !project.plugins.hasPlugin(WDKPublishPlugin)
        assert !project.configurations.findByName(configName)

        when:
        project.plugins.apply(BasePlugin)
        project.plugins.apply(WDKPublishPlugin)

        then:
        def upmArchiveConfig = project.configurations.getByName(UPMArtifactoryPlugin.ROOT_ARCHIVE_CONFIGURATION_NAME)
        def archiveCfg = project.configurations.getByName(configName)
        archiveCfg.extendsFrom.contains(upmArchiveConfig)

        archiveCfg.allArtifacts.collect{it.file} == upmArchiveConfig.allArtifacts.collect{it.file}

        where:
        configName = WDKPublishPlugin.ARCHIVE_CONFIGURATION_NAME
    }

}
