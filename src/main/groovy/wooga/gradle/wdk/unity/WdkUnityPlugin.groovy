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
import org.gradle.internal.reflect.Instantiator
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.tasks.AbstractUnityTask
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

        WdkPluginExtension extension = project.extensions.create(EXTENSION_NAME, DefaultWdkPluginExtension, project, fileResolver, instantiator)
        ConventionMapping wdkExtensionMapping = ((IConventionAware) extension).getConventionMapping()

        wdkExtensionMapping.map("pluginsDir", new DefaultPluginsDirGetter(project.extensions))

        wdkExtensionMapping.map("androidResourceCopyMethod", new Callable<AndroidResourceCopyMethod>() {
            @Override
            AndroidResourceCopyMethod call() {
                return AndroidResourceCopyMethod.sync
            }
        })

        addLifecycleTasks()
        createExternalResourcesConfigurations()
        configureCleanObjects(extension)
        addResourceCopyTasks()
        configureUnityTaskDependencies()
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

    private void configureCleanObjects(final WdkPluginExtension extension) {
        Delete cleanTask = (Delete) project.tasks[BasePlugin.CLEAN_TASK_NAME]

        cleanTask.delete({ new File(extension.getPluginsDir(), "iOS") })
        cleanTask.delete({ new File(extension.getPluginsDir(), "Android") })
        cleanTask.delete({ new File(extension.getPluginsDir(), "WebGL") })
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
}
