package wooga.gradle.wdk.upm

import org.gradle.api.file.Directory
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.api.publish.plugins.PublishingPlugin
import org.jfrog.artifactory.client.model.RepoPath
import spock.lang.Unroll

import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType

class UPMPluginIntegrationSpec extends UPMIntegrationSpec {

    @Unroll("#shouldGenerateMsg metafiles from extension property being #generateMetafiles on a project #hasMetafilesMsg metafiles")
    def "{shouldGenerateMsg} metafiles from extension property being {generateMetafiles} on a project {hasMetafilesMsg} metafiles"() {
        given: "target repository and artifact"
        def repoName = WOOGA_ARTIFACTORY_CI_REPO
        def packageName = "upm-package-name"
        and:
        def upmPackageFolder = utils.writeTestPackage(projectDir,"Assets/$packageName", packageName, "any", projectWithMetafiles)
        and:
        buildFile << """
        ${applyPlugin(UPMPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrapValueBasedOnType(artifactoryURL(repoName), String)}
                    name = "integration"
                }
            }
        }
        upm {
            repository = "integration"
            version = "any"
            packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, File)}
            generateMetaFiles = ${wrapValueBasedOnType(generateMetafiles, Boolean)}
        }
        """
        when:
        def r = runTasksSuccessfully(UPMPlugin.GENERATE_META_FILES_TASK_NAME)
        then:
        wereMetafilesGenerated ? r.wasExecuted(UPMPlugin.GENERATE_META_FILES_TASK_NAME) :
                r.wasSkipped(UPMPlugin.GENERATE_META_FILES_TASK_NAME)

        where:
        generateMetafiles | projectWithMetafiles | wereMetafilesGenerated
        true              | true                 | true
        true              | false                | true
        false             | true                 | false
        false             | false                | true
        shouldGenerateMsg = wereMetafilesGenerated ? "generates" : "doesn't generates"
        hasMetafilesMsg = projectWithMetafiles ? "with" : "without"
    }

    def "publishes folder as UPM package on subproject"() {
        given: "a gradle subproject"
        def subprojDir = new File(projectDir, "subproject")
        def subBuildFile = initializeSubproj(settingsFile, subprojDir)
        and: "root project with publishing plugin"
        buildFile << applyPlugin(PublishingPlugin)

        and: "existing UPM-ready folder in subproject"
        utils.writeTestPackage(subprojDir, upmPackageFolder, packageName, packageVersion)
        and: "artifactory credentials"
        def (username, password) = utils.credentialsFromEnv()
        and: "configured package dir and repository"
        subBuildFile << """
        ${applyPlugin(UPMPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrapValueBasedOnType(artifactoryURL(WOOGA_ARTIFACTORY_CI_REPO), String)}
                    name = ${wrapValueBasedOnType(repositoryName, String)}
                }
            }
        }
        upm {
            packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, Directory)}
            version = ${wrapValueBasedOnType(packageVersion, String)}
            repository = ${wrapValueBasedOnType(repositoryName, String)}
            username = ${wrapValueBasedOnType(username, String)}
            password = ${wrapValueBasedOnType(password, String)}
        }
        """

        when:
        def r = runTasks( "publish")

        then:
        r.success
        utils.hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, packageName, packageVersion)

        where:
        upmPackageFolder     | repositoryName
        "Assets/upm-package" | "integration"
        packageVersion = "0.0.1"
        packageName = "upm-package-name"
    }

    def "publishes folder as UPM package"() {
        given: "existing UPM-ready folder"
        utils.writeTestPackage(projectDir, upmPackageFolder, packageName, packageVersion)
        and: "artifactory credentials"
        def (username, password) = utils.credentialsFromEnv()
        and: "configured package dir and repository"
        buildFile << """
        ${applyPlugin(UPMPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrapValueBasedOnType(artifactoryURL(WOOGA_ARTIFACTORY_CI_REPO), String)}
                    name = ${wrapValueBasedOnType(repositoryName, String)}
                }
            }
        }
        upm {
            packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, Directory)}
            version = ${wrapValueBasedOnType(packageVersion, String)}
            repository = ${wrapValueBasedOnType(repositoryName, String)}
            username = ${wrapValueBasedOnType(username, String)}
            password = ${wrapValueBasedOnType(password, String)}
        }
        """

        when:
        def r = runTasksSuccessfully( "publish")

        then:
        utils.hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, packageName, packageVersion)

        where:
        upmPackageFolder     | repositoryName
        "Assets/upm-package" | "integration"
        packageVersion = "0.0.1"
        packageName = "upm-package-name"
    }

    @Unroll("publishes folder as UPM package to protected repository withn credentials set in #location")
    def "publishes folder as UPM package to protected repository withn credentials set in {location}"() {
        given: "existing UPM-ready folder"
        utils.writeTestPackage(projectDir, packageDirectory, packageName)
        and: "artifactory credentials"
        def (username, password) = utils.credentialsFromEnv()
        and: "configured package dir and repository"
        buildFile << """
        ${applyPlugin(UPMPlugin)}
        publishing {
            repositories {
                upm {
                    url = ${wrapValueBasedOnType(artifactoryURL(WOOGA_ARTIFACTORY_CI_REPO), String)}
                    name = "integration"
                    ${ location == "repository"? """
                        credentials {
                            username = ${wrapValueBasedOnType(username, String)}
                            password = ${wrapValueBasedOnType(password, String)}
                        }
                        """: ""}
                }
            }
        }
        upm {
            packageDirectory = ${wrapValueBasedOnType(packageDirectory, Directory)}
            version = ${wrapValueBasedOnType(DEFAULT_VERSION, String)}
            repository = "integration"
            ${ location == "extension"? """ 
                    username = ${wrapValueBasedOnType(username, String)}
                    password = ${wrapValueBasedOnType(password, String)}""" : ""} 
        }
        """

        when:
        def r = runTasks( "publish")

        then:
        r.success
        utils.hasPackageOnArtifactory(packageName)

        where:
        location << ["extension", "repository"]
        packageDirectory = "Assets/upm"
        packageName = "packageName"
    }
}
