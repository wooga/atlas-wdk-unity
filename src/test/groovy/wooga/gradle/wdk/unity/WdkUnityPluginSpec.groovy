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

import nebula.test.ProjectSpec
import org.gradle.api.DefaultTask
import spock.lang.Unroll
import wooga.gradle.dotnetsonar.DotNetSonarqubePlugin
import wooga.gradle.dotnetsonar.SonarScannerExtension
import wooga.gradle.dotnetsonar.tasks.BuildSolution
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.UnityPluginExtension
import wooga.gradle.wdk.unity.tasks.ResourceCopyTask

class WdkUnityPluginSpec extends ProjectSpec {

    public static final String PLUGIN_NAME = 'net.wooga.wdk-unity'

    def 'Creates the [wdk] extension'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.extensions.findByName(WdkUnityPlugin.EXTENSION_NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def extension = project.extensions.findByName(WdkUnityPlugin.EXTENSION_NAME)
        extension instanceof DefaultWdkPluginExtension
    }

    @Unroll("creates the task #taskName")
    def 'Creates needed tasks'(String taskName, Class taskType) {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.tasks.findByName(taskName)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def task = project.tasks.findByName(taskName)
        taskType.isInstance(task)

        where:
        taskName                                            | taskType
        WdkUnityPlugin.ASSEMBLE_RESOURCES_TASK_NAME         | DefaultTask
        WdkUnityPlugin.ASSEMBLE_IOS_RESOURCES_TASK_NAME     | ResourceCopyTask
        WdkUnityPlugin.ASSEMBLE_ANDROID_RESOURCES_TASK_NAME | ResourceCopyTask
        WdkUnityPlugin.ASSEMBLE_WEBGL_RESOURCES_TASK_NAME   | ResourceCopyTask
        WdkUnityPlugin.SETUP_TASK_NAME                      | DefaultTask
        WdkUnityPlugin.SONARQUBE_BUILD_TASK_NAME            | BuildSolution
        WdkUnityPlugin.SONARQUBE_TASK_NAME                  | DefaultTask
    }

    @Unroll
    def "creates needed configuration #configurationName"() {
        given:
        assert !project.configurations.findByName(configurationName)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.configurations.getByName(configurationName)

        where:
        configurationName                                   | _
        WdkUnityPlugin.IOS_RESOURCES_CONFIGURATION_NAME     | _
        WdkUnityPlugin.ANDROID_RESOURCES_CONFIGURATION_NAME | _
        WdkUnityPlugin.WEBGL_RESOURCES_CONFIGURATION_NAME   | _
    }

    @Unroll
    def 'adds pluginToAdd #pluginToAdd'(Object pluginToAdd) {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.plugins.hasPlugin(pluginToAdd)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.plugins.hasPlugin(pluginToAdd)

        where:
        pluginToAdd << ['base', UnityPlugin, DotNetSonarqubePlugin]
    }

    def "configures sonarqube extension"() {
        given: "project without plugin applied"
        assert !project.plugins.hasPlugin(PLUGIN_NAME)

        when: "applying plugin"
        project.plugins.apply(PLUGIN_NAME)
        project.evaluate()

        then:
        def sonarExt = project.extensions.getByType(SonarScannerExtension)
        def unityExt = project.extensions.getByType(UnityPluginExtension)
        and: "sonarqube extension is configured with defaults"
        def properties = sonarExt.computeSonarProperties(project)
        def assetsDir = unityExt.assetsDir.get().asFile.path
        def reportsDir = unityExt.reportsDir.get().asFile.path
        properties["sonar.exclusions"] == "${assetsDir}/Paket.Unity3D/**"
        properties["sonar.cs.nunit.reportsPaths"] == "${reportsDir}/**/*.xml"
        properties["sonar.cs.opencover.reportsPaths"] == "${reportsDir}/**/*.xml"
    }

    def "configures sonarBuildUnity task"() {
        given: "project without plugin applied"
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        and: "props file with fixes to run unity project on msbuild properly"


        when: "applying plugin"
        project.plugins.apply(PLUGIN_NAME)

        then:
        def unityExt = project.extensions.getByType(UnityPluginExtension)
        def buildTask = project.tasks.getByName("sonarBuildWDK") as BuildSolution
        buildTask.solution.get().asFile == new File(projectDir, "${project.name}.sln")
        buildTask.dotnetExecutable.getOrElse(null) == unityExt.dotnetExecutable.getOrElse(null)
        buildTask.environment.getting("FrameworkPathOverride").getOrElse(null) ==
                unityExt.monoFrameworkDir.map { it.asFile.absolutePath }.getOrElse(null)
        buildTask.extraArgs.get().any {
            it.startsWith("/p:CustomBeforeMicrosoftCommonProps=") &&
                    it.endsWith(".project-fixes.props")
        }
    }
}
