package wooga.gradle.wdk.upm

import nebula.test.ProjectSpec
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import spock.lang.Unroll
import wooga.gradle.unity.tasks.GenerateUpmPackage
import wooga.gradle.unity.tasks.Unity

class UPMPluginSpec extends ProjectSpec {

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
        taskName                                 | taskType
        UPMPlugin.GENERATE_UPM_PACKAGE_TASK_NAME | GenerateUpmPackage
        UPMPlugin.GENERATE_META_FILES_TASK_NAME  | Unity
    }

    def "Archive configuration 'upm' stores UPM artifact"() {
        given:
        assert !project.plugins.hasPlugin(UPMPlugin)
        assert !project.configurations.findByName(configName)

        when:
        project.plugins.apply(BasePlugin) //should UPMPlugin just apply base?
        project.plugins.apply(UPMPlugin)

        then:
        def config = project.configurations.findByName(configName)
        def upmArtifact = config.allArtifacts.files.first()
        upmArtifact != null
        upmArtifact.name.endsWith(".tgz")


        where:
        configName = UPMPlugin.ARCHIVE_CONFIGURATION_NAME
    }
}
