/*
 * Copyright 2018 Wooga GmbH
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
import wooga.gradle.unity.UnityPlugin

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
        result.wasExecuted(WdkUnityPlugin.MOVE_EDITOR_DEPENDENCIES)
        result.wasExecuted(WdkUnityPlugin.UN_MOVE_EDITOR_DEPENDENCIES)
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
    def "task :#taskName calls unity with correct build target"() {
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
        WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "Android" | "-buildTarget android"
        WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "IOS"     | "-buildTarget ios"
        WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "WebGL"   | "-buildTarget webgl"
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
        taskA                                                   | taskB                                                   | execute
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME               | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "Android" | ["check"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME               | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "IOS"     | ["check"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME               | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "WebGL"   | ["check"]
        WdkUnityPlugin.UN_MOVE_EDITOR_DEPENDENCIES              | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "Android" | ["check"]
        WdkUnityPlugin.UN_MOVE_EDITOR_DEPENDENCIES              | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "IOS"     | ["check"]
        WdkUnityPlugin.UN_MOVE_EDITOR_DEPENDENCIES              | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "WebGL"   | ["check"]
        WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "WebGL"   | WdkUnityPlugin.MOVE_EDITOR_DEPENDENCIES                 | ["check"]
        WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "Android" | WdkUnityPlugin.MOVE_EDITOR_DEPENDENCIES                 | ["check"]
        WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "IOS"     | WdkUnityPlugin.MOVE_EDITOR_DEPENDENCIES                 | ["check"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME               | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "Android" | ["cleanTestBuild", "performTestBuildAndroid", "performTestBuildIOS", "performTestBuildWebGL"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME               | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "IOS"     | ["cleanTestBuild", "performTestBuildAndroid", "performTestBuildIOS", "performTestBuildWebGL"]
        WdkUnityPlugin.CLEAN_TEST_BUILD_TASK_NAME               | WdkUnityPlugin.PERFORM_TEST_BUILD_TASK_NAME + "WebGL"   | ["cleanTestBuild", "performTestBuildAndroid", "performTestBuildIOS", "performTestBuildWebGL"]
    }

    @Unroll
    def "can set custom dependencies to move with #setInstruction"() {
        given: "a paket.unity3d.references file with 'Wooga.AtlasBuildTools'"
        def reference = createFile("paket.unity3d.references")
        reference << """
        Wooga.AtlasBuildTools
        """.stripIndent()

        and: "some assets in paket install directory"
        def paketInstallDirectory = new File(projectDir, "Assets/Paket.Unity3D")
        paketInstallDirectory.mkdirs()

        def sourceDir = new File(paketInstallDirectory, name)
        sourceDir.mkdirs()
        def testFile = createFile("Test.cs", sourceDir)
        def testFile2 = createFile("Test.cs.meta", sourceDir)

        and: "a future directory where files are moved to"
        def destinationDir = new File(sourceDir, "Editor")
        assert !destinationDir.exists()

        and: "dependencies to be moved configured"
        buildFile << """
        
        $setInstruction 
        
        """.stripIndent()

        when:
        def result = runTasksSuccessfully(WdkUnityPlugin.MOVE_EDITOR_DEPENDENCIES)

        then:
        result.success
        destinationDir.exists()
        !sourceDir.list().contains(testFile.name)
        destinationDir.list().contains(testFile.name)
        destinationDir.list().contains(testFile2.name)

        when:
        def meta = createFile("Editor.meta", sourceDir)
        runTasksSuccessfully(WdkUnityPlugin.UN_MOVE_EDITOR_DEPENDENCIES)

        then:
        !meta.exists()
        !destinationDir.exists()
        sourceDir.list().contains(testFile.name)
        sourceDir.list().contains(testFile2.name)

        where:
        name          | setInstruction
        "Dependency1" | "wdk.editorDependenciesToMoveDuringTestBuild(['Dependency1'])"
        "Dependency1" | "wdk.editorDependenciesToMoveDuringTestBuild('Dependency1')"
        "Dependency1" | "wdk.editorDependenciesToMoveDuringTestBuild = ['Dependency1']"
    }

    def "fails set of custom dependencies when value is null"() {
        given: "a paket.unity3d.references file with 'Wooga.AtlasBuildTools'"
        def reference = createFile("paket.unity3d.references")
        reference << """
        Wooga.AtlasBuildTools
        """.stripIndent()

        and: "some assets in paket install directory"
        def paketInstallDirectory = new File(projectDir, "Assets/Paket.Unity3D")
        paketInstallDirectory.mkdirs()

        def sourceDir = new File(paketInstallDirectory, "Dependency1")
        sourceDir.mkdirs()
        createFile("Test.cs", sourceDir)

        and: "a future directory where files are moved to"
        def destinatonDir = new File(sourceDir, "Editor")
        assert !destinatonDir.exists()

        and: "dependencies to be moved configured"
        buildFile << """
        
        wdk.editorDependenciesToMoveDuringTestBuild(null)
        
        """.stripIndent()

        expect:
        runTasksWithFailure(WdkUnityPlugin.MOVE_EDITOR_DEPENDENCIES)
    }
}
