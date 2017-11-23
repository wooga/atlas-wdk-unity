package wooga.gradle.wdk.unity

import nebula.test.IntegrationSpec

class WdkUnityIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}
        """.stripIndent()
    }

    def "applies plugin"() {
        expect:
        runTasksSuccessfully("tasks")
    }
}
