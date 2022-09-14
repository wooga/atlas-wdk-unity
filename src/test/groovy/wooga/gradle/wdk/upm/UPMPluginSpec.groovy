package wooga.gradle.wdk.upm

import nebula.test.ProjectSpec
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import spock.lang.Unroll
import wooga.gradle.unity.tasks.GenerateUpmPackage
import wooga.gradle.unity.tasks.Unity
import wooga.gradle.wdk.tools.GradleTestUtils
import wooga.gradle.wdk.upm.tools.UPMFixtures

class UPMPluginSpec extends ProjectSpec {

    private GradleTestUtils utils
    private UPMFixtures fixtures

    def setup() {
        utils = new GradleTestUtils(project)
        fixtures = new UPMFixtures(utils)
    }

    def "creates UPM extension"() {
        given:
        assert !project.plugins.hasPlugin(UPMPlugin)
        assert !project.extensions.findByName(UPMPlugin.EXTENSION_NAME)

        when:
        project.plugins.apply(UPMPlugin)

        then:
        project.extensions.getByName(UPMPlugin.EXTENSION_NAME)

    }

    @Unroll("applies #pluginToAdd plugin")
    def "applies plugins"() {
        given:
        assert !project.plugins.hasPlugin(UPMPlugin)
        assert !project.plugins.hasPlugin(pluginToAdd)

        when:
        project.plugins.apply(UPMPlugin)

        then:
        project.plugins.hasPlugin(pluginToAdd)

        where:
        pluginToAdd << [ArtifactoryPlugin, IvyPublishPlugin]
    }

    @Unroll("create #taskName task")
    def 'Creates needed tasks'() {
        given:
        assert !project.plugins.hasPlugin(UPMPlugin)
        assert !project.tasks.findByName(taskName)

        when:
        project.plugins.apply(UPMPlugin)

        then:
        def task = project.tasks.getByName(taskName)
        taskType.isInstance(task)

        where:
        taskName                                | taskType
        UPMPlugin.GENERATE_META_FILES_TASK_NAME | Unity
    }


    @Unroll("create packaging tasks for #projectNames projects")
    def 'Creates UPM packaging tasks for every project'() {
        given:
        assert !project.plugins.hasPlugin(UPMPlugin)
        projectNames.every { projectName ->
            assert !project.tasks.findByName("$projectName$UPMPlugin.GENERATE_UPM_PACKAGE_TASK_SUFFIX")
        }

        when:
        project.plugins.apply(UPMPlugin)
        and:
        utils.evaluate {
            fixtures.configurePublish(repositoryName)
            utils.requireExtension(UPMExtension).with {
                repository = repositoryName
                projects { container ->
                    projectNames.each { container.create(it) }
                }
            }
        }

        then:
        def tasks = projectNames.collect { project.tasks.getByName("$it$UPMPlugin.GENERATE_UPM_PACKAGE_TASK_SUFFIX") }
        tasks.size() == projectNames.size()
        tasks.each { it instanceof GenerateUpmPackage }

        where:
        projectNames << [["project1", "project2"]]
        repositoryName = "repository"
    }


    def "Root archive configuration 'upm' stores all UPM artifacts"() {
        given:
        assert !project.plugins.hasPlugin(UPMPlugin)
        assert !project.configurations.findByName(rootConfigName)

        when:
        project.plugins.apply(BasePlugin) //should UPMPlugin just apply base?
        project.plugins.apply(UPMPlugin)
        and:
        utils.evaluate {
            fixtures.configurePublish(repoName)
            fixtures.configureUpmProjects(repoName) { container ->
                projectNames.each { container.create(it) }
            }
        }

        then:
        def rootConfig = project.configurations.findByName(rootConfigName)
        def artifactFiles = rootConfig.allArtifacts.collect { it.file }
        artifactFiles.size() == projectNames.size()
        artifactFiles.every { upmArtifact ->
            upmArtifact != null
            upmArtifact.name.endsWith(".tgz")
        }

        where:
        rootConfigName = UPMPlugin.ROOT_ARCHIVE_CONFIGURATION_NAME
        projectNames = ["project1", "project2"]
        repoName = "repository"
    }

    @Unroll("Archive configuration for project #projectName store UPM artifact for that project")
    def "Archive configuration for project #projectName store UPM artifact for that project"() {
        given:
        def configName = "$projectName${UPMPlugin.ARCHIVE_CONFIGURATION_SUFFIX}"
        assert !project.plugins.hasPlugin(UPMPlugin)
        assert !project.configurations.findByName(configName)

        when:
        project.plugins.apply(BasePlugin) //should UPMPlugin just apply base?
        project.plugins.apply(UPMPlugin)
        and:
        utils.evaluate {
            fixtures.configurePublish(repoName)
            fixtures.configureUpmProjects(repoName) {
                it.create(projectName)
            }
        }

        then:
        def config = project.configurations.getByName(configName)
        def artifactFile = config.allArtifacts.collect { it.file }.first()
        artifactFile != null
        artifactFile.name.endsWith(".tgz")

        where:
        projectName << ["project1", "project2"]
        repoName = "repository"
    }

    @Unroll("Publication for project #projectName is configured")
    def "Publication for project #projectName is configured"() {
        given:
        assert !project.plugins.hasPlugin(UPMPlugin)

        when:
        project.plugins.apply(UPMPlugin)
        and:
        utils.evaluate {
            fixtures.configurePublish(repoName)
            fixtures.configureUpmProjects(repoName) {
                it.create(projectName) { UPMProjectDeclaration upm ->
                    upm.version = version
                }
            }
            project.tasks.named("${projectName}${UPMPlugin.GENERATE_UPM_PACKAGE_TASK_SUFFIX}") { GenerateUpmPackage generatePackTask ->
                generatePackTask.packageName = packageName
            }
        }

        then:
        def publications = utils.requireExtension(PublishingExtension).publications
        def publication = publications.getByName(publicationName) as IvyPublication
        def baseArtifact = publication.artifacts.first()
        publication != null
        publication.module == packageName
        publication.revision == version
        baseArtifact != null
        baseArtifact.type == "tgz"

        where:
        projectName | packageName | version
        "project1"  | "pkg"       | "0.0.1"
        "project2"  | "package"   | "0.0.2"
        repoName = "repository"
        publicationName = "$projectName${UPMPlugin.PUBLICATION_SUFFIX}"

    }
}
