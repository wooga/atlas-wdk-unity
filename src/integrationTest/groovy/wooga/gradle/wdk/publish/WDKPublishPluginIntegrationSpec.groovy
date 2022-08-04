package wooga.gradle.wdk.publish

import wooga.gradle.version.ReleaseStage
import wooga.gradle.wdk.unity.IntegrationSpec

class WDKPublishPluginIntegrationSpec extends IntegrationSpec {

    //ps: github release == github publish + github release notes
    def "#message github release if release.stage is #stage"() {
        given: "applied WDKPublish plugin"
        applyPlugin(WDKPublishPlugin)

        when: "running github release task"
        def result = runTasks(WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME, "--dry-run")

        then: "release task runs for release stages"
        result.wasExecuted(WDKPublishPlugin.GITHUB_PUBLISH_TASK_NAME)
        where:
        stage                    | shouldRelease
        ReleaseStage.Prerelease  | true
        ReleaseStage.Final       | true
        ReleaseStage.Preflight   | false
        ReleaseStage.Snapshot    | false
        ReleaseStage.Development | false
        ReleaseStage.Unknown     | false
        message = shouldRelease ? "runs" : "doesn't run"
    }

    def "github publish task only runs if are there changes"() {

    }
}
