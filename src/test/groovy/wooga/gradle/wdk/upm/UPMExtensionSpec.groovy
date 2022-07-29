package wooga.gradle.wdk.upm

import nebula.test.ProjectSpec
import org.gradle.api.InvalidUserDataException
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.publish.PublishingExtension
import spock.lang.Unroll

class UPMExtensionSpec extends ProjectSpec {

    def "sets extension properties dependent on the publishing extension"() {
        given:
        project.plugins.apply(UPMPlugin)
        and:
        project.extensions.getByType(UPMExtension).with {
            it.repository = "test"
        }
        and:
        def publishExt = project.extensions.getByType(PublishingExtension)
        when:
        publishExt.with {
            repositories {
                upm {
                    url = "$baseURL/$repositoryKey"
                    name = "test"
                }
            }
        }
        (project as DefaultProject).evaluate()

        then:
        def ext = project.extensions.getByType(UPMExtension)
        ext.upmRepositoryBaseUrl.present
        ext.upmRepositoryBaseUrl.get() == baseURL
        ext.upmRepositoryKey.present
        ext.upmRepositoryKey.get() == repositoryKey

        where:
        baseURL = "https://jfrogrepo/artifactory"
        repositoryKey = "repository"
    }

    def "throws on project evaluation if repository property is not set"() {
        given:
        project.plugins.apply(UPMPlugin)
        and:
        def publishExt = project.extensions.getByType(PublishingExtension)
        publishExt.with {
            repositories {
                upm {
                    url = "https://artifactoryhost/artifactory/repository"
                    name = "test"
                }
            }
        }
        when:
        (project as DefaultProject).evaluate()
        then:
        def e = thrown(ProjectConfigurationException)
        e.cause instanceof MissingValueException
    }

    def "throws on project evaluation if selected repository does not exists"() {
        given:
        project.plugins.apply(UPMPlugin)
        and:
        def publishExt = project.extensions.getByType(PublishingExtension)
        def upmExt = project.extensions.getByType(UPMExtension)
        publishExt.with {
            repositories {
                upm {
                    url = "https://artifactoryhost/artifactory/repository"
                    name = "test"
                }
            }
        }
        upmExt.repository = "otherRepo"
        when:
        (project as DefaultProject).evaluate()
        then:
        def e = thrown(ProjectConfigurationException)
        e.cause instanceof MissingValueException
    }


    @Unroll("throws on project evaluation if selected repository is not valid #invalidURL")
    def "throws on project evaluation if selected repository is not valid"() {
        given:
        project.plugins.apply(UPMPlugin)
        and:
        def publishExt = project.extensions.getByType(PublishingExtension)
        def upmExt = project.extensions.getByType(UPMExtension)
        publishExt.with {
            repositories {
                upm {
                    url = invalidURL
                    name = "test"
                }
            }
        }
        upmExt.repository = "otherRepo"

        when:
        (project as DefaultProject).evaluate()
        then:
        def e = thrown(ProjectConfigurationException)
        e.cause instanceof InvalidUserDataException

        where:
        invalidURL << ["whithoutprotocol/artifactory/repository", "https://withoutrepopath"]
    }

    def "throws on project evaluation if no publish repository is set"() {
        given:
        project.plugins.apply(UPMPlugin)
        and:
        def upmExt = project.extensions.getByType(UPMExtension)
        upmExt.repository = "repo"

        when:
        (project as DefaultProject).evaluate()

        then:
        def e = thrown(ProjectConfigurationException)
        e.cause instanceof MissingValueException
    }
}
