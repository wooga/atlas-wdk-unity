package wooga.gradle.wdk.upm


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.plugins.PluginRegistry
import org.gradle.api.logging.Logger
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskProvider
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import wooga.gradle.unity.tasks.GenerateUpmPackage
import wooga.gradle.unity.tasks.Unity
import wooga.gradle.wdk.unity.WdkUnityPluginConventions
import wooga.gradle.wdk.upm.internal.UnityMetafileDetector
import wooga.gradle.wdk.upm.internal.repository.internal.DefaultUPMRepositoryHandlerConvention
import wooga.gradle.wdk.upm.internal.repository.internal.UPMArtifactRepository

import javax.inject.Inject
import java.util.stream.Collectors

class UPMPlugin implements Plugin<Project> {

    static final String GROUP = "UPM"
    static final String EXTENSION_NAME = "upm"
    static final String GENERATE_UPM_PACKAGE_TASK_NAME = "upmPack"
    static final String GENERATE_META_FILES_TASK_NAME = "generateMetaFiles"

    private final PluginRegistry pluginRegistry

    @Inject
    UPMPlugin(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry
    }

    @Override
    void apply(Project project) {
        def extension = UPMPublishExtension.newWithConventions(project, EXTENSION_NAME)

        //this generate unity meta files, sounds like it belongs to atlas-unity
        def upmGenerateMetaFiles = project.tasks.register(GENERATE_META_FILES_TASK_NAME, Unity) {
            it.group = GROUP
            onlyIf {
                return hasValidMetafiles(extension, logger) || extension.generateMetaFiles.get()
            }
        }
        def upmPack = project.tasks.register(GENERATE_UPM_PACKAGE_TASK_NAME, GenerateUpmPackage) {
            it.group = GROUP
            it.packageDirectory.convention(extension.packageDirectory)
            it.archiveVersion.convention(extension.version)
            it.dependsOn(upmGenerateMetaFiles)
        }

        def publication = configureUpmPublish(project, upmPack)
        configureArtifactory(project, extension, publication.name)
        setupPublishTasksDependencies(project)
    }



    private static IvyPublication configureUpmPublish(Project project, TaskProvider<GenerateUpmPackage> upmPack) {
        //Extends the publishing extension to be able to hold upm repositories
        Project p = project
        project.extensions.configure(PublishingExtension, { PublishingExtension e ->
            def upmHandlerConvention = new DefaultUPMRepositoryHandlerConvention(e.repositories as DefaultRepositoryHandler)
//            def upmPluginId = pluginRegistry.inspect(UPMPlugin).pluginId.id
            p.convention.plugins.put(UPMPlugin.canonicalName, upmHandlerConvention)
            new DslObject(e.repositories).convention.plugins.put(UPMPlugin.canonicalName, upmHandlerConvention)
        })

        project.configurations.maybeCreate(WdkUnityPluginConventions.upmConfigurationName).with {
            it.transitive = false
        }
        def upmArtifact = project.artifacts.add(WdkUnityPluginConventions.upmConfigurationName, upmPack)
        def publishing = project.extensions.getByType(PublishingExtension)
        def upmPublication = publishing.publications.maybeCreate(WdkUnityPluginConventions.upmPublicationName, IvyPublication).with { publication ->
            project.afterEvaluate {
                publication.module = upmPack.flatMap { it.packageName }.get()
                publication.revision = upmPack.flatMap {it.archiveVersion }.get()
            }
            publication.artifact(upmArtifact)
            return publication
        }
        return upmPublication
    }

    private static void configureArtifactory(Project project, UPMPublishExtension upmExtension, String publicationName) {
        project.afterEvaluate { ->
            def artifactory = project.convention.plugins.get("artifactory") as ArtifactoryPluginConvention
            def repoName = upmExtension.repository.get()
            def upmRepo = upmRepoFromPublishing(project)[repoName]
            artifactory.contextUrl = upmRepo.baseUrl
            artifactory.publish { PublisherConfig publisherConfig ->
                publisherConfig.repository { it ->
                    it.ivy { PublisherConfig.Repository repo ->
                        repo.artifactLayout = '[module]/-/[module]-[revision].[ext]'
                        repo.mavenCompatible = false
                        repo.repoKey = upmRepo.repositoryKey
                        repo.username = upmExtension.username.orElse(project.provider{upmRepo?.credentials?.username}).orNull
                        repo.password = upmExtension.password.orElse(project.provider{upmRepo?.credentials?.password}).orNull
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

        if (project.rootProject != project) {
            def rootPublishTask = project.rootProject.tasks.getByName(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
            rootPublishTask.dependsOn(publishTask)
        }
    }

    private static boolean hasValidMetafiles(UPMPublishExtension extension, Logger logger) {
        def upmPackDir = extension.packageDirectory.asFile.get()
        def filesWithoutMeta = new UnityMetafileDetector().filesWithoutMetafile(upmPackDir)
        if (filesWithoutMeta.size() > 0) {
            logger.info("{} files found without corresponding metafile in {}", filesWithoutMeta.size(), upmPackDir.path)
        }
        return filesWithoutMeta.size() > 0
    }

    private static Map<String, UPMArtifactRepository> upmRepoFromPublishing(Project project) {
        def publishExt = project.extensions.findByType(PublishingExtension)
        publishExt.repositories.withType(UPMArtifactRepository).stream().map {
            repo -> new Tuple2<>(repo.name, repo)
        }.collect(Collectors.toMap({ it.first as String }, { it.second as UPMArtifactRepository }))
    }

}
