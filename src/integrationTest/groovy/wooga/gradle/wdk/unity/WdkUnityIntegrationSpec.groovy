/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.wdk.unity

import spock.lang.Unroll

class WdkUnityIntegrationSpec extends UnityIntegrationSpec {

    def setup() {
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}
        """.stripIndent()
    }

    def "applies plugin"() {
        expect:
        runTasksSuccessfully("tasks")
    }

    def "runs test build tasks"() {
        given: "a paket.unity3d.references file"
        def reference = createFile("paket.unity3d.references")

        and: "Wooga.AtlasBuildTools referenced"
        reference << """
        Wooga.AtlasBuildTools
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("check")

        then:
        result.wasExecuted(WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME)
        result.wasExecuted(WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME)
        result.wasExecuted(WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "Android")
        result.wasExecuted(WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "IOS")
        result.wasExecuted(WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "WebGL")
    }


    @Unroll
    def "task :#taskName calls unity with correct commandline args"() {
        given: "a paket.unity3d.references file with 'Wooga.AtlasBuildTools'"
        def reference = createFile("paket.unity3d.references")
        reference << """
        Wooga.AtlasBuildTools
        """.stripIndent()

        when:
        def result = runTasksSuccessfully(taskName)

        then:
        result.standardOutput.contains(args)

        where:
        taskName                                                | args
        WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "Android" | "-executeMethod Wooga.Atlas.BuildTools.BuildFromEditor.BuildTestAndroid"
        WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "IOS"     | "-executeMethod Wooga.Atlas.BuildTools.BuildFromEditor.BuildTestIOS"
        WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "WebGL"   | "-executeMethod Wooga.Atlas.BuildTools.BuildFromEditor.BuildTestWebGL"
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME               | "-executeMethod Wooga.Atlas.BuildTools.BuildFromEditor.BuildTestClean"
    }

    @Unroll
    def "verify :#taskA runs after :#taskB when execute '#execute'"() {
        given: "a paket.unity3d.references file with 'Wooga.AtlasBuildTools'"
        def reference = createFile("paket.unity3d.references")
        reference << """
        Wooga.AtlasBuildTools
        """.stripIndent()

        when:
        def result = runTasksSuccessfully(*(execute << "--dry-run"))

        then:
        result.standardOutput.indexOf(":$taskA ") > result.standardOutput.indexOf(":$taskB ")

        where:
        taskA                                     | taskB                                                   | execute
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "Android" | ["check"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "IOS"     | ["check"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "WebGL"   | ["check"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "Android" | ["cleanTestBuild", "performTestBuildAndroid", "performTestBuildIOS", "performTestBuildWebGL"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "IOS"     | ["cleanTestBuild", "performTestBuildAndroid", "performTestBuildIOS", "performTestBuildWebGL"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "WebGL"   | ["cleanTestBuild", "performTestBuildAndroid", "performTestBuildIOS", "performTestBuildWebGL"]
    }

    def "reconfigures exportUnityPackage"() {

    }
}
