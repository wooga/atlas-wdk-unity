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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.IConventionAware
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.plugins.LifecycleBasePlugin
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.UnityPluginExtension
import wooga.gradle.unity.tasks.AbstractUnityTask
import wooga.gradle.unity.tasks.Unity
import wooga.gradle.wdk.unity.tasks.AndroidResourceCopyAction
import wooga.gradle.wdk.unity.tasks.DefaultResourceCopyTask
import wooga.gradle.wdk.unity.tasks.IOSResourceCopyAction
import wooga.gradle.wdk.unity.tasks.ResourceCopyTask
import wooga.gradle.wdk.unity.tasks.WebGLResourceCopyAction

import javax.inject.Inject
import java.util.concurrent.Callable

class WdkUnityPlugin implements Plugin<Project> {

    static Logger logger = Logging.getLogger(WdkUnityPlugin)

    static String ASSEMBLE_RESOURCES_TASK_NAME = "assembleResources"
    static String ASSEMBLE_IOS_RESOURCES_TASK_NAME = "assembleIOSResources"
    static String ASSEMBLE_ANDROID_RESOURCES_TASK_NAME = "assembleAndroidResources"
    static String ASSEMBLE_WEBGL_RESOURCES_TASK_NAME = "assembleWebGLResources"
    static String SETUP_TASK_NAME = "setup"
    static String GROUP = "wdk"
    static String EXTENSION_NAME = "wdk"
    static String ANDROID_RESOURCES_CONFIGURATION_NAME = "android"
    static String IOS_RESOURCES_CONFIGURATION_NAME = "ios"
    static String WEBGL_RESOURCES_CONFIGURATION_NAME = "webgl"
    static String RUNTIME_CONFIGURATION_NAME = "runtime"

    static String PERFORM_TEST_BUILD_TASK_NAME = "performTestBuild"
    static String CLEAN_TEST_BUILD_TASK_NAME = "cleanTestBuild"

    private Project project
    private final FileResolver fileResolver
    private final Instantiator instantiator

    @Inject
    WdkUnityPlugin(FileResolver fileResolver, Instantiator instantiator) {
        this.fileResolver = fileResolver
        this.instantiator = instantiator
    }

    @Override
    void apply(Project project) {
        this.project = project

        project.pluginManager.apply(BasePlugin.class)
        project.pluginManager.apply(UnityPlugin.class)

        WdkPluginExtension wdk = project.extensions.create(EXTENSION_NAME, DefaultWdkPluginExtension, project, fileResolver)

        UnityPluginExtension unity = project.extensions.getByType(UnityPluginExtension)
        ConventionMapping wdkExtensionMapping = ((IConventionAware) wdk).getConventionMapping()

        wdkExtensionMapping.map("pluginsDir", new Callable<File>() {
            @Override
            File call() throws Exception {
                return new File(wdk.getAssetsDir(), "Plugins")
            }
        })

        wdkExtensionMapping.map("assetsDir", new Callable<File>() {
            @Override
            File call() throws Exception {
                return new File(unity.getAssetsDir(), "Wooga")
            }
        })

        addLifecycleTasks()
        createExternalResourcesConfigurations()
        configureCleanObjects(wdk)
        addResourceCopyTasks()
        configureUnityTaskDependencies()

        createTestBuildTasks(project, project.tasks)
    }

    private void addLifecycleTasks() {
        def assembleResourcesTask = project.tasks.create(name: ASSEMBLE_RESOURCES_TASK_NAME, group: GROUP)
        assembleResourcesTask.description = "gathers all iOS and Android resources into Plugins/ directory of the unity project"
        project.tasks.create(name: SETUP_TASK_NAME, group: GROUP, dependsOn: assembleResourcesTask)
        project.tasks[BasePlugin.ASSEMBLE_TASK_NAME].dependsOn assembleResourcesTask
    }

    private void createExternalResourcesConfigurations() {
        Configuration androidConfiguration = project.configurations.maybeCreate(ANDROID_RESOURCES_CONFIGURATION_NAME)
        androidConfiguration.description = "android application resources"
        androidConfiguration.transitive = false

        Configuration iosConfiguration = project.configurations.maybeCreate(IOS_RESOURCES_CONFIGURATION_NAME)
        iosConfiguration.description = "ios application resources"
        iosConfiguration.transitive = false

        Configuration webglConfiguration = project.configurations.maybeCreate(WEBGL_RESOURCES_CONFIGURATION_NAME)
        iosConfiguration.description = "webgl application resources"
        iosConfiguration.transitive = false

        Configuration runtimeConfiguration = project.configurations.maybeCreate(RUNTIME_CONFIGURATION_NAME)
        runtimeConfiguration.transitive = true
        runtimeConfiguration.extendsFrom(androidConfiguration, iosConfiguration, webglConfiguration)
    }

    private void configureCleanObjects(final WdkPluginExtension wdk) {
        Delete cleanTask = (Delete) project.tasks[BasePlugin.CLEAN_TASK_NAME]

        cleanTask.delete({ wdk.getIOSResourcePluginDir() })
        cleanTask.delete({ wdk.getAndroidResourcePluginDir() })
        cleanTask.delete({ wdk.getWebGLResourcePluginDir() })
        cleanTask.delete({ wdk.getPaketUnity3dInstallDir() })
    }

    private void addResourceCopyTasks() {
        Configuration androidResources = project.configurations[ANDROID_RESOURCES_CONFIGURATION_NAME]
        Configuration iOSResources = project.configurations[IOS_RESOURCES_CONFIGURATION_NAME]
        Configuration webglResources = project.configurations[WEBGL_RESOURCES_CONFIGURATION_NAME]

        def assembleTask = project.tasks[ASSEMBLE_RESOURCES_TASK_NAME]

        ResourceCopyTask iOSResourceCopy = project.tasks.create(name: "assembleIOSResources", type: DefaultResourceCopyTask) as ResourceCopyTask
        iOSResourceCopy.description = "gathers all additional iOS files into the Plugins/iOS directory of the unity project"
        iOSResourceCopy.dependsOn(iOSResources)
        iOSResourceCopy.resources = iOSResources
        iOSResourceCopy.doLast(new IOSResourceCopyAction())

        ResourceCopyTask androidResourceCopy = project.tasks.create(name: "assembleAndroidResources", type: DefaultResourceCopyTask) as ResourceCopyTask
        androidResourceCopy.description = "gathers all *.jar and AndroidManifest.xml files into the Plugins/Android directory of the unity project"
        androidResourceCopy.dependsOn(androidResources)
        androidResourceCopy.resources androidResources
        androidResourceCopy.doLast(new AndroidResourceCopyAction())

        ResourceCopyTask webglResourceCopy = project.tasks.create(name: "assembleWebGLResources", type: DefaultResourceCopyTask) as ResourceCopyTask
        webglResourceCopy.description = "gathers all webgl related files into the Plugins/webGL directory of the unity project"
        webglResourceCopy.dependsOn(webglResources)
        webglResourceCopy.resources webglResources
        webglResourceCopy.doLast(new WebGLResourceCopyAction())

        assembleTask.dependsOn iOSResourceCopy, androidResourceCopy, webglResourceCopy
    }

    private void configureUnityTaskDependencies() {
        if (project.pluginManager.hasPlugin("net.wooga.unity")) {
            project.tasks.withType(AbstractUnityTask, new Action<AbstractUnityTask>() {
                @Override
                void execute(AbstractUnityTask task) {
                    task.dependsOn project.tasks[SETUP_TASK_NAME]
                }
            })
        }
    }

    static void createTestBuildTasks(Project project, TaskContainer tasks) {
        File paketUnity3DReferences = project.file("paket.unity3d.references")

        if (paketUnity3DReferences.exists() && paketUnity3DReferences.text.contains("Wooga.AtlasBuildTools")) {
            def checkTask = tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
            def performTestBuildTask = tasks.create(PERFORM_TEST_BUILD_TASK_NAME)
            performTestBuildTask.with {
                description = "perform all test builds"
                group = GROUP
            }

            checkTask.dependsOn performTestBuildTask

            def cleanTestBuildTask = tasks.create(name: CLEAN_TEST_BUILD_TASK_NAME, type: Unity) as Unity
            cleanTestBuildTask.with {
                description = "Clean test build"
                group = GROUP
                args "-executeMethod", "Wooga.Atlas.BuildTools.BuildFromEditor.BuildTestClean"
            }

            performTestBuildTask.dependsOn cleanTestBuildTask

            ["Android", "IOS", "WebGL"].each { platform ->
                String taskName = "performTestBuild${platform}"
                def performTestBuildPlatform = tasks.create(name: taskName, type: Unity) as Unity
                performTestBuildPlatform.with {
                    args "-executeMethod", "Wooga.Atlas.BuildTools.BuildFromEditor.BuildTest${platform}"
                    description = "Build test project for ${platform}"

                }

                performTestBuildTask.dependsOn performTestBuildPlatform
                cleanTestBuildTask.mustRunAfter performTestBuildPlatform
            }


        }
    }
}
