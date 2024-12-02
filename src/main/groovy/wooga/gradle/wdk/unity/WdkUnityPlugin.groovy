/*
 * Copyright 2021 Wooga GmbH
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

import org.apache.commons.io.FileUtils
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.sonarqube.gradle.SonarQubeExtension
import wooga.gradle.dotnetsonar.DotNetSonarqubePlugin
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.UnityPluginExtension
import wooga.gradle.unity.UnityTask
import wooga.gradle.unity.models.UnityProjectManifest
import wooga.gradle.unity.tasks.Activate
import wooga.gradle.unity.tasks.ProjectManifestTask
import wooga.gradle.unity.tasks.ReturnLicense
import wooga.gradle.unity.tasks.Unity
import wooga.gradle.wdk.unity.actions.AndroidResourceCopyAction
import wooga.gradle.wdk.unity.actions.IOSResourceCopyAction
import wooga.gradle.wdk.unity.actions.WebGLResourceCopyAction
import wooga.gradle.wdk.unity.config.SonarQubeConfiguration
import wooga.gradle.wdk.unity.tasks.DefaultResourceCopyTask
import wooga.gradle.wdk.unity.tasks.ResolveUnityPackages

import javax.inject.Inject

class WdkUnityPlugin implements Plugin<Project> {

    static Logger logger = Logging.getLogger(WdkUnityPlugin)

    static String EXTENSION_NAME = "wdk"
    static String GROUP = "wdk"

    static String ASSEMBLE_RESOURCES_TASK_NAME = "assembleResources"
    static String ASSEMBLE_IOS_RESOURCES_TASK_NAME = "assembleIOSResources"
    static String ASSEMBLE_ANDROID_RESOURCES_TASK_NAME = "assembleAndroidResources"
    static String ASSEMBLE_WEBGL_RESOURCES_TASK_NAME = "assembleWebGLResources"
    static String SETUP_TASK_NAME = "setup"
    static String RESOLVE_PACKAGES_TASK_NAME = "resolvePackages"
    static String PERFORM_TEST_BUILD_TASK_NAME = "performTestBuild"
    static String CLEAN_TEST_BUILD_TASK_NAME = "cleanTestBuild"
    static String SONARQUBE_TASK_NAME = "sonarqube"
    static String SONARQUBE_BUILD_TASK_NAME = "sonarBuildWDK"

    static String ANDROID_RESOURCES_CONFIGURATION_NAME = "android"
    static String IOS_RESOURCES_CONFIGURATION_NAME = "ios"
    static String WEBGL_RESOURCES_CONFIGURATION_NAME = "webgl"
    static String RUNTIME_CONFIGURATION_NAME = "runtime"

    static String MOVE_EDITOR_DEPENDENCIES = "moveEditorDependencies"
    static String UN_MOVE_EDITOR_DEPENDENCIES = "unMoveEditorDependencies"

    private final FileResolver fileResolver
    private final Instantiator instantiator

    @Inject
    WdkUnityPlugin(FileResolver fileResolver, Instantiator instantiator) {
        this.fileResolver = fileResolver
        this.instantiator = instantiator
    }

    @Override
    void apply(Project project) {

        project.pluginManager.apply(BasePlugin.class)
        project.pluginManager.apply(UnityPlugin.class)
        project.pluginManager.apply(DotNetSonarqubePlugin.class)

        WdkPluginExtension extension = project.extensions.create(WdkPluginExtension, EXTENSION_NAME, DefaultWdkPluginExtension, project)
        UnityPluginExtension unityPluginExtension = project.extensions.getByType(UnityPluginExtension)

        configureExtension(project, extension)
        addLifecycleTasks(project)
        createExternalResourcesConfigurations(project)
        configureCleanObjects(project, extension)
        addResourceCopyTasks(project)
        configureUnityTaskDependencies(project, unityPluginExtension)
        createTestBuildTasks(project, extension, unityPluginExtension)
        configureSonarqubeTasks(project)
    }

    private static void configureExtension(Project project, WdkPluginExtension extension) {
        UnityPluginExtension unity = project.extensions.getByType(UnityPluginExtension)
        unity.enableTestCodeCoverage.convention(true)

        extension.editorDependenciesToMoveDuringTestBuild("NSubstitute")
        extension.assetsDir.convention(unity.assetsDir.dir("Wooga"))
        extension.pluginsDir.convention(extension.assetsDir.dir("Plugins"))

        extension.paketUnity3dInstallDir.convention(unity.assetsDir.dir(WdkUnityPluginConventions.PAKET_UNITY_3D_INSTALL_DIRECTORY))
        extension.iosResourcePluginDir.convention(extension.pluginsDir.dir(WdkUnityPluginConventions.IOS_PLUGIN_DIRECTORY))
        extension.androidResourcePluginDir.convention(extension.pluginsDir.dir(WdkUnityPluginConventions.ANDROID_PLUGIN_DIRECTORY))
        extension.webGLResourcePluginDir.convention(extension.pluginsDir.dir(WdkUnityPluginConventions.WEBGL_PLUGIN_DIRECTORY))
    }

    private static void addLifecycleTasks(Project project) {
        def assembleResourcesTask = project.tasks.register(ASSEMBLE_RESOURCES_TASK_NAME, { t ->
            t.group = GROUP
            t.description = "gathers all iOS and Android resources into Plugins/ directory of the unity project"
        })

        project.tasks.register(SETUP_TASK_NAME, { t ->
            t.dependsOn(assembleResourcesTask)
        })

        project.tasks[BasePlugin.ASSEMBLE_TASK_NAME].dependsOn assembleResourcesTask
    }

    private static void createExternalResourcesConfigurations(Project project) {
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

    private static void configureCleanObjects(Project project, final WdkPluginExtension extension) {
        Delete cleanTask = (Delete) project.tasks[BasePlugin.CLEAN_TASK_NAME]

        cleanTask.delete(extension.iosResourcePluginDir)
        cleanTask.delete(extension.androidResourcePluginDir)
        cleanTask.delete(extension.webGLResourcePluginDir)
        cleanTask.delete(extension.paketUnity3dInstallDir)
    }

    private static void addResourceCopyTasks(Project project) {
        Configuration androidResources = project.configurations[ANDROID_RESOURCES_CONFIGURATION_NAME]
        Configuration iOSResources = project.configurations[IOS_RESOURCES_CONFIGURATION_NAME]
        Configuration webglResources = project.configurations[WEBGL_RESOURCES_CONFIGURATION_NAME]

        def iOSResourceCopy = project.tasks.register("assembleIOSResources", DefaultResourceCopyTask,
                { t ->
                    t.description = "gathers all additional iOS files into the Plugins/iOS directory of the unity project"
                    t.dependsOn(iOSResources)
                    t.resources = iOSResources
                    t.doLast(new IOSResourceCopyAction())
                })

        def androidResourceCopy = project.tasks.register("assembleAndroidResources", DefaultResourceCopyTask,
                { t ->
                    t.description = "gathers all *.jar and AndroidManifest.xml files into the Plugins/Android directory of the unity project"
                    t.dependsOn(androidResources)
                    t.resources androidResources
                    t.doLast(new AndroidResourceCopyAction())
                })

        def webglResourceCopy = project.tasks.register("assembleWebGLResources", DefaultResourceCopyTask,
                { t ->
                    t.description = "gathers all webgl related files into the Plugins/webGL directory of the unity project"
                    t.dependsOn(webglResources)
                    t.resources webglResources
                    t.doLast(new WebGLResourceCopyAction())
                })

        def assembleTask = project.tasks[ASSEMBLE_RESOURCES_TASK_NAME]
        assembleTask.dependsOn iOSResourceCopy, androidResourceCopy, webglResourceCopy
    }

    private static void configureUnityTaskDependencies(Project project, final UnityPluginExtension unityPluginExtension) {

        // We declare a lifecycle-like task that we can use to control how the unity project is setup;
        // this can be used in downstream projects
        def setupTask = project.tasks.named(SETUP_TASK_NAME)

        // We want to ensure that the unity project's packages are resolved
        // if the setup task is executed by itself, since if ANY other unity task is invoked, we need not do this
        def resolvePackages = project.tasks.register(RESOLVE_PACKAGES_TASK_NAME, ResolveUnityPackages) {
            it.group = GROUP
            it.batchMode.set(true)
            it.onlyIf {
                def unityTasks = project.gradle.taskGraph.allTasks.findAll({
                    Task t ->
                        t instanceof UnityTask && !(t instanceof Activate) && !(t instanceof ReturnLicense) && !(t.name == RESOLVE_PACKAGES_TASK_NAME)
                })
                def shouldExecute = unityTasks.size() <= 1
                if (!shouldExecute) {
                    logger.info("Will be skipped since there's more than one Unity task present [${unityTasks}]")
                }
                shouldExecute
            }
            it.manifest.convention(unityPluginExtension.projectManifestFile)
            it.packageLock.convention(unityPluginExtension.projectLockFile)
        }
        setupTask.configure({
            it.dependsOn resolvePackages
        })

        def resolutionStrategy = project.tasks.named(UnityPlugin.Tasks.setResolutionStrategy.toString())
        project.tasks.withType(UnityTask).configureEach{
            if(!(it instanceof Activate || it instanceof ReturnLicense || it instanceof ProjectManifestTask || it.name == UnityPlugin.Tasks.ensureProjectManifest.toString())) {
                it.dependsOn(resolutionStrategy)
            }
        }

        def cleanupUnityFiles = project.tasks.named(UnityPlugin.Tasks.cleanupDanglingUnityFiles.toString())
        project.tasks.withType(UnityTask).configureEach {
            it.dependsOn(cleanupUnityFiles)
        }

        // Make every other Unity task depend on the setup task defined by this plugin
        project.tasks.withType(UnityTask).configureEach { task ->
            if (task.name != RESOLVE_PACKAGES_TASK_NAME && task.name && !(task.name == UnityPlugin.Tasks.ensureProjectManifest.name())
                    && !(task instanceof Activate)) {
                task.dependsOn setupTask
            }
        }
    }

    static void createTestBuildTasks(final Project project, final WdkPluginExtension extension, final UnityPluginExtension unityPluginExtension) {
        File paketUnity3DReferences = project.file("paket.unity3d.references")
        File projectManifestFile = unityPluginExtension.projectManifestFile.asFile.getOrNull()
        def upmManifest = (projectManifestFile && projectManifestFile.exists()) ? UnityProjectManifest.deserialize(projectManifestFile) : null

        if ((paketUnity3DReferences.exists() && paketUnity3DReferences.text.contains("Wooga.AtlasBuildTools"))
                || (upmManifest && upmManifest.getDependencies().containsKey("com.wooga.atlas-build-tools"))) {

            List<TaskProvider<Unity>> platformTasks = ["Android", "IOS", "WebGL"].collect({ platform ->
                String taskName = "performTestBuild${platform}"
                project.tasks.register(taskName, Unity, { t ->
                    t.description = "Build test project for ${platform}"
                    t.group = GROUP
                    t.buildTarget = platform.toLowerCase()
                    t.arguments("-executeMethod", "Wooga.Atlas.BuildTools.BuildFromEditor.BuildTest${platform}")
                })
            })


            def moveEditorDependencies = project.tasks.register(MOVE_EDITOR_DEPENDENCIES, { t ->
                t.description = "moves some editor only dependencies for test builds"
                t.doLast(new Action<Task>() {
                    @Override
                    void execute(Task task) {
                        def paketUnitydir = extension.paketUnity3dInstallDir.get().asFile.path
                        extension.editorDependenciesToMoveDuringTestBuild.each { dependency ->
                            def fileToMove = new File(paketUnitydir, dependency)
                            def destination = new File(fileToMove, "Editor")
                            def tempDir = File.createTempDir(dependency, "Editor")
                            tempDir.delete()

                            logger.info("move dependency ${dependency} for test build")
                            logger.info("move ${fileToMove.path} to ${destination.path}")

                            if (fileToMove.exists()) {
                                FileUtils.moveDirectoryToDirectory(fileToMove, tempDir, true)
                                FileUtils.moveDirectory(new File(tempDir, dependency), destination)
                            } else {
                                logger.info("$fileToMove does not exist")
                            }
                        }
                    }
                })
                // @TODO: Perhaps this is needed if the task has dependencies
                // t.mustRunAfter platformTasks.dependsOn
            })

            def unMoveEditorDependecies = project.tasks.register(UN_MOVE_EDITOR_DEPENDENCIES, { t ->
                t.description = "moves some editor only dependencies back to old location"
                t.doLast(new Action<Task>() {
                    @Override
                    void execute(Task task) {
                        def paketUnitydir = extension.paketUnity3dInstallDir.get().asFile.path
                        extension.editorDependenciesToMoveDuringTestBuild.each { dependency ->

                            def destination = new File(paketUnitydir, dependency)
                            def fileToMove = new File(destination, "Editor")
                            def tempDir = File.createTempDir(dependency, "Editor")
                            tempDir.delete()

                            logger.info("move dependency ${dependency} back after test build")
                            logger.info("move ${fileToMove.path} to ${destination.path}")

                            if (fileToMove.exists()) {
                                FileUtils.moveDirectoryToDirectory(fileToMove, tempDir, true)
                                FileUtils.forceDelete(destination)
                                FileUtils.moveDirectory(new File(tempDir, "Editor"), destination)
                            } else {
                                logger.info("$fileToMove does not exist")
                            }
                        }
                    }
                })
                t.mustRunAfter platformTasks
            })

            def cleanTestBuildTask = project.tasks.register(CLEAN_TEST_BUILD_TASK_NAME, Unity, { t ->
                t.description = "Clean test build"
                t.group = GROUP
                t.arguments("-executeMethod", "Wooga.Atlas.BuildTools.BuildFromEditor.BuildTestClean")
                t.mustRunAfter platformTasks
            })

            def performTestBuildTask = project.tasks.register(PERFORM_TEST_BUILD_TASK_NAME, { t ->
                t.description = "perform all test builds"
                t.group = GROUP
                t.dependsOn(moveEditorDependencies)
                t.dependsOn(unMoveEditorDependecies)
                t.dependsOn(cleanTestBuildTask)
                t.mustRunAfter moveEditorDependencies

                t.dependsOn(platformTasks)
            })

            def checkTask = project.tasks.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
            checkTask.dependsOn performTestBuildTask
        }
    }

    static void configureSonarqubeTasks(Project project) {
        def unityExt = project.extensions.findByType(UnityPluginExtension)
        def sonarExt = project.extensions.findByType(SonarQubeExtension)

        new SonarQubeConfiguration(project).with {
            sonarTaskName = SONARQUBE_TASK_NAME
            buildTaskName = SONARQUBE_BUILD_TASK_NAME
            return it
        }.configure(unityExt, sonarExt)
    }
}
