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

import nebula.test.ProjectSpec
import org.gradle.api.DefaultTask
import spock.lang.Unroll
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
    def 'adds pluginToAdd #pluginToAdd'(String pluginToAdd) {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.plugins.hasPlugin(pluginToAdd)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.plugins.hasPlugin(pluginToAdd)

        where:
        pluginToAdd << ['base']
    }
}
