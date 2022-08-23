package wooga.gradle.wdk.upm


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.plugins.PluginRegistry
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskProvider
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import wooga.gradle.unity.UnityPlugin
import wooga.gradle.unity.tasks.GenerateUpmPackage
import wooga.gradle.unity.tasks.Unity
import wooga.gradle.wdk.upm.internal.UnityProjects
import wooga.gradle.wdk.upm.internal.repository.DefaultUPMRepositoryHandlerConvention

import javax.inject.Inject

class UPMPlugin implements Plugin<Project> {

    static final String GROUP = "UPM"
    static final String EXTENSION_NAME = "upm"
    static final String GENERATE_UPM_PACKAGE_TASK_NAME = "upmPack"
    static final String GENERATE_META_FILES_TASK_NAME = "generateMetaFiles"
    static final String ARCHIVE_CONFIGURATION_NAME = "upm"
    static final String PUBLICATION_NAME = "upm"


    private final PluginRegistry pluginRegistry

    @Inject
    UPMPlugin(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry
    }

    @Override
    void apply(Project project) {
        project.plugins.apply(UnityPlugin.class)
        project.plugins.apply(ArtifactoryPlugin.class)
        project.plugins.apply(IvyPublishPlugin.class)
        project.plugins.apply(PublishingPlugin.class)

        def publishingExt = project.extensions.getByType(PublishingExtension)
        def extension = UPMExtension.newWithPublishingConventions(project, publishingExt, EXTENSION_NAME)
        def basePluginConvention = project.provider{ project.rootProject.convention.plugins["base"] as BasePluginConvention }

        //this generate unity meta files, sounds like it belongs to atlas-unity
        def upmGenerateMetaFiles = project.tasks.register(GENERATE_META_FILES_TASK_NAME, Unity) {
            it.group = GROUP
            onlyIf {
                return hasValidMetafiles(extension.packageDirectory, logger).getOrElse(false) || extension.generateMetaFiles.get()
            }
        }
        def upmPack = project.tasks.register(GENERATE_UPM_PACKAGE_TASK_NAME, GenerateUpmPackage) {
            it.group = GROUP
            it.packageDirectory.convention(extension.packageDirectory)
            it.archiveVersion.set(extension.version)
            //we need to set this explicitly for projects on subplugins.
            it.destinationDirectory.convention(basePluginConvention.flatMap{it.distsDirectory})
            it.dependsOn(upmGenerateMetaFiles)
        }

        def publication = configureUpmPublish(project, upmPack)
        configureArtifactory(project, extension, publication.name)
        setupPublishTasksDependencies(project)
    }


    private static IvyPublication configureUpmPublish(Project project, TaskProvider<GenerateUpmPackage> upmPack) {
        //Extends the publishing extension to be able to hold upm repositories
        project.extensions.configure(PublishingExtension, { PublishingExtension e ->
            def upmHandlerConvention = new DefaultUPMRepositoryHandlerConvention(e.repositories as DefaultRepositoryHandler)
            project.convention.plugins.put(UPMPlugin.canonicalName, upmHandlerConvention)
            new DslObject(e.repositories).convention.plugins.put(UPMPlugin.canonicalName, upmHandlerConvention)
        })

        def archiveConfig = project.configurations.register(ARCHIVE_CONFIGURATION_NAME) {
            it.transitive = false
        }
        def upmArtifact = project.artifacts.add(archiveConfig.name, upmPack)
        def publishing = project.extensions.getByType(PublishingExtension)
        def upmPublication = publishing.publications.maybeCreate(PUBLICATION_NAME, IvyPublication).with { publication ->
            project.afterEvaluate { ->
                publication.module = upmPack.flatMap { it.packageName }.getOrNull()
                publication.revision = upmPack.flatMap {it.archiveVersion }.getOrNull()
            }
            publication.artifact(upmArtifact)
            return publication
        }
        return upmPublication
    }

    private static void configureArtifactory(Project project, UPMExtension upmExtension, String publicationName) {
        project.afterEvaluate { ->
            def artifactory = project.convention.plugins.get("artifactory") as ArtifactoryPluginConvention
            artifactory.contextUrl = upmExtension.upmRepositoryBaseUrl.get()
            artifactory.publish { PublisherConfig publisherConfig ->
                 publisherConfig.repository { it ->
                    it.ivy { PublisherConfig.Repository repo ->
                        repo.artifactLayout = '[module]/-/[module]-[revision].[ext]'
                        repo.mavenCompatible = false
                        repo.repoKey = upmExtension.upmRepositoryKey.get()
                        repo.username = upmExtension.username.orNull
                        repo.password = upmExtension.password.orNull
                    }
                }
            }
        }
        project.tasks.withType(ArtifactoryTask).configureEach({ defaultTask ->
            defaultTask.publications(publicationName)
            defaultTask.publishArtifacts = false
            defaultTask.publishIvy = false
        })
    }

    private static void setupPublishTasksDependencies(Project project) {
        def artifactoryPublishTask = project.tasks.named(ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME)
        def publishTask = project.tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
        publishTask.configure {it.dependsOn(artifactoryPublishTask) }

        if (project.rootProject != project && project.rootProject.plugins.hasPlugin(PublishingPlugin)) {
            project.rootProject.tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME) {
               it.dependsOn(publishTask)
            }
        }
    }

    private static Provider<Boolean> hasValidMetafiles(Provider<Directory> packageDirectory, Logger logger = null) {
        return packageDirectory.asFile.map { File upmPackDir ->
            def filesWithoutMeta = new UnityProjects().filesWithoutMetafile(upmPackDir)
            if (filesWithoutMeta.size() > 0) {
                logger?.info("{} files found without corresponding metafile in {}", filesWithoutMeta.size(), upmPackDir.path)
            }
            return filesWithoutMeta.size() > 0
        }
    }

}
