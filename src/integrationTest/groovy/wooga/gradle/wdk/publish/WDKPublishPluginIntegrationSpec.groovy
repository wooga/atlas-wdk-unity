package wooga.gradle.wdk.publish

import com.wooga.gradle.test.GradleSpecUtils
import com.wooga.spock.extensions.github.GithubRepository
import com.wooga.spock.extensions.github.Repository
import com.wooga.spock.extensions.github.api.RateLimitHandlerWait
import com.wooga.spock.extensions.github.api.TravisBuildNumberPostFix
import org.gradle.api.publish.plugins.PublishingPlugin
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import org.junit.Assume
import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.github.publish.GithubPublishPlugin
import wooga.gradle.release.ReleasePlugin
import wooga.gradle.wdk.tools.GrGitExtended
import wooga.gradle.wdk.upm.UPMPlugin
import wooga.gradle.wdk.upm.internal.BasicSnippetsTrait
import wooga.gradle.wdk.upm.internal.GithubSnippetsTrait
import wooga.gradle.wdk.upm.internal.UPMSnippets
import wooga.gradle.wdk.upm.internal.UPMSnippetsTrait
import wooga.gradle.wdk.upm.internal.UPMTestTools
import wooga.gradle.wdk.upm.internal.UnitySnippetsTrait

class WDKPublishPluginIntegrationSpec extends WDKPublishIntegrationSpec
        implements GithubSnippetsTrait, UnitySnippetsTrait, BasicSnippetsTrait, UPMSnippetsTrait {


    @GithubRepository(
            usernameEnv = "ATLAS_GITHUB_INTEGRATION_USER",
            tokenEnv = "ATLAS_GITHUB_INTEGRATION_PASSWORD",
            repositoryPostFixProvider = [TravisBuildNumberPostFix.class],
            rateLimitHandler = RateLimitHandlerWait.class,
            resetAfterTestCase = true
    )
    @Shared
    Repository testRepo

    def setup() {
        buildFile << """
            github {
                repositoryName = "reponame" //please keep invalid repository names for tests that don't require github
                username = "fake"
                password = "fake"
            }
        """
    }

    def cleanup() {
        upmUtils.cleanupArtifactoryRepoInRange(specCreationTs, System.currentTimeMillis())
    }

    //ps: github release == github publish + github release notes
    @Unroll("#runMsg github release if release.stage is #stage")
    def "only runs github release on some stages"() {
        given:
        buildFile << applyPlugin(WDKPublishPlugin) << "\n"
        buildFile << minimalUPMConfiguration(projectDir, "any")
        and:
        buildFile << mockTask(WDKPublishPlugin.GITHUB_RELEASE_NOTES_TASK_NAME, """
            def file = it.output.get().asFile
            file.text = "fake release note"
        """)
        buildFile << mockTask(WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME)
        and:
        buildFile << """
        versionBuilder {
            stage = ${wrapValueBasedOnType(stage, String)}
        }
        """
        when: "running github release task"
        def result = runTasks(WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME)

        then: "release task runs for release stages"
        result.wasSkipped("$WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME") == !shouldRelease

        where:
        stage      | shouldRelease
        "rc"       | true
        "final"    | true
        "snapshot" | false
        "dev"      | false
        runMsg = shouldRelease ? "runs" : "doesn't run"
    }

    @Unroll("#runMsg github release if #lastVersionTag and current HEAD on local git #changesMsg")
    def "github publish task runs if there are changes between last and current versions on local git"() {
        given:
        buildFile << applyPlugin(WDKPublishPlugin) << "\n"
        buildFile << minimalUPMConfiguration(projectDir)
        and:
        def git = GrGitExtended.initWithIgnoredFiles(projectDir, "Assets/")
        if (lastVersionTag != null) {
            new File(projectDir, '.gitignore') << "\nuserHome/".stripIndent()
            git.add(patterns: ['.gitignore'])
            git.commit(message: 'tag commit')
            git.tag.add(name: lastVersionTag)
        }
        if (hasChanges) {
            new File(projectDir, '.gitignore') << "\notherUserHome/".stripIndent()
            git.add(patterns: ['.gitignore'])
            git.commit(message: 'other commit')
        }
        and:
        buildFile << mockTask(WDKPublishPlugin.GITHUB_RELEASE_NOTES_TASK_NAME, """
            def file = it.output.get().asFile
            file.text = "fake release note"
        """)
        buildFile << mockTask(WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME)
        and:

        when: "running github release task"
        def result = runTasks(WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME, "-Prelease.stage=$releaseStage", "-Prelease.scope=$releaseScope")

        then: "release task runs for release stages"
        result.success
        result.wasSkipped("$WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME") == !publishes

        where:
        lastVersionTag | hasChanges | publishes
        null           | true       | true
        "v0.0.1"       | true       | true
        "v0.1.0"       | false      | false
        "v1.0.0"       | false      | false
        releaseStage = "final"
        releaseScope = "major"
        runMsg = publishes ? "runs" : "skips"
        changesMsg = hasChanges ? "aren't equals" : "are equals"
    }

    @IgnoreIf({ os.windows }) //for some reason local git doesn't initializes properly for this test on windows.
    @Unroll("#runMsg github release if #currentVersion #changesMsg already published on github")
    def "github publish task only runs if there are changes between last and current versions on github"() {
        given:
        if (lastVersion != null) {
            testRepo.commit('release commit')
            testRepo.createRelease(lastVersion, lastVersion)
            Assume.assumeTrue(notNullOrTimeout { testRepo.getReleaseByTagName(lastVersion) })
        }
        GrGitExtended.initWithRemote(projectDir, testRepo, "Assets/")
        if (alreadyPublished) {
            testRepo.createRelease(currentVersion, currentVersion)
            Assume.assumeTrue(notNullOrTimeout { testRepo.getReleaseByTagName(lastVersion) })
        }
        and:
        buildFile << applyPlugin(WDKPublishPlugin)
        buildFile << minimalUPMConfiguration(projectDir)
        buildFile << configureGithubPlugin(testRepo)
        buildFile << mockTask(WDKPublishPlugin.GITHUB_RELEASE_NOTES_TASK_NAME, """
            def file = it.output.get().asFile
            file.text = "fake release note"
        """)
        buildFile << mockTask(WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME)

        when: "running github release task"
        def result = runTasks(WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME, "-Prelease.scope=$releaseScope", "-Prelease.stage=$releaseStage")

        then: "release task runs for release stages"
        result.success
        result.wasSkipped("$WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME") == !publishes

        cleanup:
        testRepo.cleanupReleases()

        where:
        lastVersion | currentVersion | alreadyPublished | publishes
        null        | "v1.0.0"       | false            | true
        null        | "v1.0.0"       | true             | false
        "v0.0.1"    | "v1.0.0"       | true             | false
        "v0.1.0"    | "v1.0.0"       | false            | true
        releaseStage = "final"
        releaseScope = "major"
        runMsg = publishes ? "runs" : "skips"
        changesMsg = alreadyPublished ? "is" : "is not"
    }


    def "publish task runs github publishing together with UPM package publish"() {
        given:
        buildFile << applyPlugin(WDKPublishPlugin) << "\n"
        buildFile << minimalUPMConfiguration(projectDir, "packageName", "repository", true)
        buildFile << configureGithubPlugin(testRepo)

        and:
        GrGitExtended.initWithRemote(projectDir, testRepo, "Assets/")

        when: "running publish task"
        def result = runTasks(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME, "-Prelease.stage=final", "-Prelease.scope=major")

        then:
        result.success
        result.wasExecuted(WDKPublishPlugin.GITHUB_RELEASE_NOTES_TASK_NAME)
        result.wasExecuted(WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME)
        result.wasExecuted(UPMSnippets.UPM_PROJECT_NAME+UPMPlugin.GENERATE_UPM_PACKAGE_TASK_SUFFIX)
        result.wasExecuted(ArtifactoryTask.DEPLOY_TASK_NAME)
        and:
        def gitRelease = testRepo.getReleaseByTagName(expectedGHTag)
        gitRelease != null

        cleanup:
        testRepo.cleanupReleases()

        where:
        expectedVersion = "1.0.0"
        expectedGHTag = "v${expectedVersion}"
    }

    @Unroll("wdk version #semver2Version in subproject should be the same as the project version #semver1Version in the root project")
    def "wdk version should be the same as the project version in the root project, if its present"() {
        given: "a gradle subproject"
        def subprojDir = new File(projectDir, "subproject")
        def subBuildFile = initializeSubproj(settingsFile, subprojDir)

        and: "a published previous version tag"
        def git = GrGitExtended.initWithRemote(projectDir, testRepo, "subproject/")
        git.commitChange()
        git.tag.add(name: "v${previousVersion}")
        git.commitChange()
        git.push(tags: true)
        git.push()

        and: "a base project with the old release plugin applied on semver v1"
        buildFile << applyPlugin(ReleasePlugin)
        buildFile << configureGithubPlugin(testRepo)
        buildFile << """ versionBuilder { versionScheme = "semver" } """
        buildFile << mockTask(ReleasePlugin.RELEASE_NOTES_BODY_TASK_NAME, """
            def file = it.output.get().asFile
            file.text = "fake release note"
        """)
        buildFile << mockTask(GithubPublishPlugin.PUBLISH_TASK_NAME)
        testRepo.createRelease(expectedGHVersion, expectedGHVersion)
        buildFile << mockTask(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME, """
            logger.warn("project ver:|&|&\${project.version.orNull}|&|&")
            logger.warn("project vercode:|&|&\${project.versionCode}|&|&")
        """)

        and: "a subproject with the WDKPublish plugin applied"
        subBuildFile << """
            ${applyPlugin(WDKPublishPlugin)}
            ${minimalUPMConfiguration(subprojDir, DEFAULT_PACKAGE_NAME, DEFAULT_REPOSITORY, true)}
            ${configureGithubPlugin(testRepo)}
            ${configureMockUnity(subprojDir)}
            ${mockTask(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME, """
                logger.warn("upm package ver:|&|&\${upm.projects.collect{it.version.orNull}}|&|&")
                logger.warn("subproject vercode:|&|&\${project.versionCode}|&|&")
            """)}
        """

        when: "running publish task from root plugin"
        def result = runTasks("publish", "-Prelease.stage=$releaseStage", "-Prelease.scope=$releaseScope")

        then:
        result.success
        result.standardOutput.contains("project ver:|&|&${semver1Version}|&|&")
        result.standardOutput.contains("upm package ver:|&|&[${semver2Version}]|&|&")
        result.standardOutput.contains("project vercode:|&|&${versionCode}|&|&")
        result.standardOutput.contains("subproject vercode:|&|&${versionCode}|&|&")
        UPMTestTools.retry(60000, 10000, {it == 0}) {
            testRepo.listReleases().toList().count { it.tagName == expectedGHVersion }
        } == 1

        upmUtils.hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, DEFAULT_PACKAGE_NAME, semver2Version)

        cleanup:
        testRepo.cleanupReleases()

        where:
        semver1Version          | semver2Version        | versionCode | releaseStage
        "1.0.0-branchMain00001" | "1.0.0-branch.main.1" | 10000       | "snapshot"
        "1.0.0-rc00001"         | "1.0.0-rc.1"          | 10000       | "rc"
        "1.0.0"                 | "1.0.0"               | 10000       | "final"
        previousVersion = "0.1.0"
        expectedGHVersion = "v$semver1Version"
        releaseScope = "major"
    }
}
