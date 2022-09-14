package wooga.gradle.wdk.upm

import org.gradle.api.file.Directory
import org.gradle.api.publish.plugins.PublishingPlugin
import spock.lang.Unroll
import wooga.gradle.wdk.upm.internal.UPMTestTools

import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType

class UPMPluginIntegrationSpec extends UPMIntegrationSpec {

    @Unroll("#shouldGenerateMsg metafiles from extension property being #generateMetafiles on a project #hasMetafilesMsg metafiles")
    def "{shouldGenerateMsg} metafiles from extension property being {generateMetafiles} on a project {hasMetafilesMsg} metafiles"() {
        given: "target repository and artifact"
        def repoName = WOOGA_ARTIFACTORY_CI_REPO
        def packageName = "upm-package-name"
        and:
        def upmPackageFolder = utils.writeTestPackage(projectDir, "Assets/$packageName", packageName, "any", projectWithMetafiles)
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
            projects {
                defaultProject {
                    version = "any"
                    packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, File)}
                    generateMetaFiles = ${wrapValueBasedOnType(generateMetafiles, Boolean)}
                }
            }
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

    @Unroll("#shouldGenerateMsg metafiles if #needMsg upm project(s) needs metafiles")
    def "generates metafiles once if any upm project needs metafiles"() {
        given: "target repository and artifact"
        def repoName = WOOGA_ARTIFACTORY_CI_REPO
        def packageName = "upm-package-name"
        and:
        def upmPackageFolder = utils.writeTestPackage(projectDir,
                "Assets/$packageName", packageName, "any", true)
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
            projects {
                proj1 {
                    packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, File)}
                    generateMetaFiles = ${wrapValueBasedOnType(projectGeneratesMetafiles[0], Boolean)}
                }
                proj2 {
                    packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, File)}
                    generateMetaFiles = ${wrapValueBasedOnType(projectGeneratesMetafiles[1], Boolean)}
                }
                proj3 {
                    packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, File)}
                    generateMetaFiles = ${wrapValueBasedOnType(projectGeneratesMetafiles[2], Boolean)}
                }
                proj4 {
                    packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, File)}
                    generateMetaFiles = ${wrapValueBasedOnType(projectGeneratesMetafiles[3], Boolean)}
                }
            }
        }
        """
        when:
        def r = runTasksSuccessfully(UPMPlugin.GENERATE_META_FILES_TASK_NAME)
        then:
        wereMetafilesGenerated ? r.wasExecuted(UPMPlugin.GENERATE_META_FILES_TASK_NAME) :
                r.wasSkipped(UPMPlugin.GENERATE_META_FILES_TASK_NAME)

        where:
        projectGeneratesMetafiles    | wereMetafilesGenerated
        [true, false, false, false]  | true
        [true, true, true, false]    | true
        [false, false, false, false] | false
        shouldGenerateMsg = wereMetafilesGenerated ? "generates" : "doesn't generates"
        needMsg = wereMetafilesGenerated ? "${projectGeneratesMetafiles.count { it }}" : "no"
    }

    def "publishes upm project as UPM package on gradle subproject"() {
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
            repository = ${wrapValueBasedOnType(repositoryName, String)}
            username = ${wrapValueBasedOnType(username, String)}
            password = ${wrapValueBasedOnType(password, String)}
            projects {
                defaultProject {
                    packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, Directory)}
                    version = ${wrapValueBasedOnType(packageVersion, String)}
                }
            }
        }
        """

        when:
        def r = runTasks("publish")

        then:
        r.success
        utils.hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, packageName, packageVersion)

        where:
        upmPackageFolder     | repositoryName
        "Assets/upm-package" | "integration"
        packageVersion = "0.0.1"
        packageName = "upm-package-name"
    }

    def "publishes project as UPM package"() {
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
            repository = ${wrapValueBasedOnType(repositoryName, String)}
            username = ${wrapValueBasedOnType(username, String)}
            password = ${wrapValueBasedOnType(password, String)}
            projects {
                defaultProject {
                    packageDirectory = ${wrapValueBasedOnType(upmPackageFolder, Directory)}
                    version = ${wrapValueBasedOnType(packageVersion, String)}
                }
            }
        }
        """

        when:
        def r = runTasksSuccessfully("publish")

        then:
        utils.hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, packageName, packageVersion)

        where:
        upmPackageFolder     | repositoryName
        "Assets/upm-package" | "integration"
        packageVersion = "0.0.1"
        packageName = "upm-package-name"
    }

    def "publishes many projects as UPM packages"() {
        given: "existing UPM-ready folders"
        [upmPackageFolders, packageNames].transpose().each { tuple -> def (String folder, String pkgName) = tuple
                utils.writeTestPackage(projectDir, folder, pkgName)
        }
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
            repository = ${wrapValueBasedOnType(repositoryName, String)}
            username = ${wrapValueBasedOnType(username, String)}
            password = ${wrapValueBasedOnType(password, String)}
            projects {
                ${ (0..packageNames.size()-1).collect {i ->
                """${packageNames[i]} {
                    packageDirectory = ${wrapValueBasedOnType(upmPackageFolders[i], Directory)}
                    version = ${wrapValueBasedOnType(versions[i], String)}
                }"""
                }.join("\n")}                
            }
        }
        """

        when:
        def r = runTasksSuccessfully("publish")

        then:
        [packageNames, versions].transpose().every {def tuple -> def (String packageName, String version) = tuple
            utils.hasPackageOnArtifactory(UPMTestTools.WOOGA_ARTIFACTORY_CI_REPO, packageName, version)
        }

        where:
        packageNames                         | versions
        ["package1", "package2", "package3"] | ["0.0.1", "0.0.2", "0.0.3"]
        upmPackageFolders = packageNames.collect { "Assets/upm-$it".toString() }
        repositoryName = "integration"
    }

    @Unroll("publishes project as UPM package to protected repository with credentials set in #location")
    def "publishes project as UPM package to protected repository with credentials set in {location}"() {
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
                    ${location == "repository" ? """
                        credentials {
                            username = ${wrapValueBasedOnType(username, String)}
                            password = ${wrapValueBasedOnType(password, String)}
                        }
                        """ : ""}
                }
            }
        }
        upm {
            repository = "integration"
            ${location == "extension" ? """ 
            username = ${wrapValueBasedOnType(username, String)}
            password = ${wrapValueBasedOnType(password, String)}""" : ""}
            projects {
                defaultProject { 
                    packageDirectory = ${wrapValueBasedOnType(packageDirectory, Directory)}
                    version = ${wrapValueBasedOnType(DEFAULT_VERSION, String)}
                }
            }
        }
        """

        when:
        def r = runTasks("publish")

        then:
        r.success
        utils.hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, packageName)

        where:
        location << ["extension", "repository"]
        packageDirectory = "Assets/upm"
        packageName = "packageName"
    }
}
