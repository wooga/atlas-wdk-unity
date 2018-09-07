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

import nebula.test.IntegrationSpec
import spock.lang.Unroll

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AssembleResourcesIntegrationSpec extends IntegrationSpec {

    File iOSResourcebase
    File androidResourcebase
    File webGlResourcebase

    File androidPlugins
    File iOSPlugins
    File webGlPlugins

    def setup() {
        androidPlugins = new File(projectDir, "Assets/Wooga/Plugins/Android")
        iOSPlugins = new File(projectDir, "Assets/Wooga/Plugins/iOS")
        webGlPlugins = new File(projectDir, "Assets/Wooga/Plugins/WebGL")

        def resourcesBase = new File(projectDir, "test/resources")
        iOSResourcebase = new File(resourcesBase, "iOS")
        androidResourcebase = new File(resourcesBase, "android")
        webGlResourcebase = new File(resourcesBase, "webGl")

        iOSResourcebase.mkdirs()
        androidResourcebase.mkdirs()
        webGlResourcebase.mkdirs()
    }

    def createAARPackage(File path) {
        createAARPackage(path, false)
    }

    def createAARPackage(File path, Boolean internalLibraries) {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(path))
        out.putNextEntry(new ZipEntry("classes.jar"))
        out.putNextEntry(new ZipEntry("AndroidManifest.xml"))
        if (internalLibraries) {
            out.putNextEntry(new ZipEntry("libs/"))
            out.putNextEntry(new ZipEntry("libs/test1.jar"))
            out.putNextEntry(new ZipEntry("libs/test2.jar"))
        }
        out.close()
    }

    def createFrameworkZip(File path) {
        String frameworkName = path.name.replace('.zip', '')
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(path))

        out.putNextEntry(new ZipEntry("$frameworkName/"))
        out.putNextEntry(new ZipEntry("$frameworkName/binary"))
        out.putNextEntry(new ZipEntry("$frameworkName/Headers/"))
        out.putNextEntry(new ZipEntry("$frameworkName/Versions/"))
        out.close()
    }

    def "skips copy tasks when no dependencies are set"() {
        given: "a build file without external dependencies"

        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}
        """.stripIndent()

        and: "the plugins directories don't exist yet"
        assert !androidPlugins.exists()
        assert !iOSPlugins.exists()
        assert !webGlPlugins.exists()

        when: "running the setup task"
        def result = runTasksSuccessfully(WdkUnityPlugin.SETUP_TASK_NAME)

        then:
        result.wasExecuted(WdkUnityPlugin.ASSEMBLE_RESOURCES_TASK_NAME)
        result.wasExecuted("assembleIOSResources")
        result.wasExecuted("assembleAndroidResources")
        result.wasExecuted("assembleWebGLResources")
        !androidPlugins.exists()
        !iOSPlugins.exists()
        !webGlPlugins.exists()
    }

    def "syncs iOS resources when configured"() {
        given: "a test class to copy"
        createFile("WGTestClass.mm", iOSResourcebase)

        and: "a .framework zip mock"
        createFrameworkZip(createFile("Test.framework.zip", iOSResourcebase))

        and: "an empty output directory"
        assert !iOSPlugins.list()

        and: "a build file with artifact dependency to that file"
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}

            dependencies {
                ios fileTree(dir: "${iOSResourcebase.path.replace('\\', '/')}")
            }

        """.stripIndent()

        when: "running the setup task"
        runTasksSuccessfully(WdkUnityPlugin.SETUP_TASK_NAME)

        then:
        !androidPlugins.list()
        iOSPlugins.list()
        iOSPlugins.list().contains('WGTestClass.mm')
        iOSPlugins.list().contains('Test.framework')
    }

    def "syncs webGL resources when configured"() {
        given: "a test class to copy"
        createFile("index.js", webGlResourcebase)
        createFile("index.css", webGlResourcebase)

        and: "an empty output directory"
        assert !webGlPlugins.list()

        and: "a build file with artifact dependency to that file"
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}

            dependencies {
                webgl fileTree(dir: "${webGlResourcebase.path.replace('\\', '/')}")
            }

        """.stripIndent()

        when: "running the setup task"
        runTasksSuccessfully(WdkUnityPlugin.SETUP_TASK_NAME)

        then:
        !androidPlugins.list()
        webGlPlugins.list()
        webGlPlugins.list().contains('index.js')
        webGlPlugins.list().contains('index.css')
    }

    def "syncs android resources when configured"() {
        given: "a jar file mock to copy"
        createFile("WGDeviceInfo.jar", androidResourcebase)

        and: "an aar file mock"
        createAARPackage(createFile("WGDeviceInfo.aar", androidResourcebase))

        and: "an empty output directory"
        assert !androidPlugins.list()

        and: "a build file with artifact dependency to that file"
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}

            dependencies {
                android fileTree(dir: "${androidResourcebase.path.replace('\\', '/')}")
            }

        """.stripIndent()

        when: "running the setup task"
        runTasksSuccessfully(WdkUnityPlugin.SETUP_TASK_NAME)

        then:
        !iOSPlugins.list()
        androidPlugins.list()
        androidPlugins.list().contains('WGDeviceInfo.jar')
        androidPlugins.list().contains('WGDeviceInfo.aar')
    }

    @Unroll
    def "can set plugins folder in extension as #type"() {
        given: "a jar file mock to copy"
        createFile("WGDeviceInfo.jar", androidResourcebase)

        and: "a custom plugins directory"
        androidPlugins = new File(projectDir, "Assets/Plugins/Custom/Android")

        and: "a build file with artifact dependency to that file"
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}

            wdk.pluginsDir = ${value}"${androidPlugins.path.replace('\\', '/')}"
            
            dependencies {
                android fileTree(dir: "${androidResourcebase.parentFile.path.replace('\\', '/')}")
            }

        """.stripIndent()

        when: "running the setup task"
        runTasksSuccessfully(WdkUnityPlugin.SETUP_TASK_NAME)

        then:
        androidPlugins.list()

        where:
        type     | value
        "file"   | "file "
        "object" | ""
    }

    @Unroll
    def "can set assets folder in extension as #type"() {
        given: "a jar file mock to copy"
        createFile("WGDeviceInfo.jar", androidResourcebase)

        and: "a custom assets directory"
        def customAssets = new File(projectDir, "Assets/Test")
        customAssets.mkdirs()

        androidPlugins = new File(customAssets, "Plugins/Android")
        androidPlugins.mkdirs()

        and: "a build file with artifact dependency to that file"
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}

            wdk.assetsDir = ${value}"${customAssets.path.replace('\\', '/')}"
            
            dependencies {
                android fileTree(dir: "${androidResourcebase.parentFile.path.replace('\\', '/')}")
            }

        """.stripIndent()

        when: "running the setup task"
        runTasksSuccessfully(WdkUnityPlugin.SETUP_TASK_NAME)

        then:
        androidPlugins.list()

        where:
        type     | value
        "file"   | "file "
        "object" | ""
    }

    @Unroll()
    def "clean deletes iOS and Android plugins dir with pluginsDir set to #pluginsDir"() {
        given: "a jar file mock to copy"
        createFile("WGDeviceInfo.jar", androidResourcebase)

        and: "a .framework mock"
        createFrameworkZip(createFile("Test.framework.zip", iOSResourcebase))

        and: "custom set pluginsDir"
        androidPlugins = new File(projectDir, pluginsDir + '/Android')
        iOSPlugins = new File(projectDir, pluginsDir + '/iOS')

        and: "a build file with artifact dependency to that file"
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}

            wdk.pluginsDir = "${pluginsDir}"
            dependencies {
                ios fileTree(dir: "${iOSResourcebase.path.replace('\\', '/')}")
                android fileTree(dir: "${androidResourcebase.path.replace('\\', '/')}")
            }

        """.stripIndent()

        and: "and setup run"
        runTasksSuccessfully(WdkUnityPlugin.SETUP_TASK_NAME)
        assert androidPlugins.list()
        assert iOSPlugins.list()

        when:
        runTasksSuccessfully('clean')

        then:
        !androidPlugins.exists()
        !iOSPlugins.exists()

        where:
        pluginsDir << ["Assets/Plugins", "Assets/Plugins/Custom"]
    }

    def "clean deletes Paket.Unity3d directory"() {
        given: "a Paket.Unity3D in the assets directory"
        def unity3dDir = new File(projectDir, "Assets/Paket.Unity3D")
        unity3dDir.mkdirs()

        and: "a build file with artifact dependency to that file"
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}
        """.stripIndent()

        when:
        runTasksSuccessfully('clean')

        then:
        !unity3dDir.exists()
    }
}
