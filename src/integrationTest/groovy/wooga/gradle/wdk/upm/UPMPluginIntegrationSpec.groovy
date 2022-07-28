package wooga.gradle.wdk.upm

import org.gradle.api.file.Directory
import org.jfrog.artifactory.client.model.RepoPath
import spock.lang.Unroll
import wooga.gradle.unity.utils.PackageManifestBuilder
import wooga.gradle.wdk.unity.WdkUnityPlugin

import java.util.stream.IntStream

import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType

class UPMPluginIntegrationSpec extends UPMIntegrationSpec {

    private final String DEFAULT_VERSION = "0.0.1"

    def setup() {
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}
            ${applyPlugin(UPMPlugin)}
        
        """.stripIndent()
    }

    @Unroll("#shouldGenerateMsg metafiles from extension property being #generateMetafiles on a project #hasMetafilesMsg metafiles")
    def "{shouldGenerateMsg} metafiles from extension property being {generateMetafiles} on a project {hasMetafilesMsg} metafiles"() {
        given: "target repository and artifact"
        def repoName = WOOGA_ARTIFACTORY_CI_REPO
        def packageName = "upm-package-name"
        and:
        def upmPackageFolder = writeTestPackage("Assets/$packageName", packageName, "any", projectWithMetafiles)
        and:
        buildFile << """
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

    def "publishes folder as UPM package with extension credentials"() {
        given: "existing UPM-ready folder"
        def packageVersion = "0.0.1"
        def packageName = "upm-package-name"
        writeTestPackage(upmPackageFolder, packageName, packageVersion)
        and: "artifactory credentials"
        def (username, password) = credentialsFromEnv()
        and: "configured package dir and repository"
        buildFile << """
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
        hasPackageOnArtifactory(WOOGA_ARTIFACTORY_CI_REPO, packageName, packageVersion)

        where:
        upmPackageFolder     | repositoryName
        "Assets/upm-package" | "integration"
    }

    def "publishes folder as UPM package to protected repository"() {
        given:
        when:
        then:
        where:
        username | password
        ""       | ""
    }

    File writeTestPackage(String packageDirectory, String packageName, String packageVersion = DEFAULT_VERSION, boolean hasMetafiles = true, int fileCount = 1) {
        def packageDir = directory(packageDirectory)
        def packageManifestFile = file("package.json", packageDir)
        if (hasMetafiles) {
            file("package.json.meta", packageDir).write("META")
        }
        packageManifestFile.write(new PackageManifestBuilder(packageName, packageVersion).build())
        IntStream.range(0, fileCount).forEach {
            def baseName = "Sample$fileCount".toString()
            file("${baseName}.cs", packageDir).write("class ${baseName} {}")
            if (hasMetafiles) {
                file("${baseName}.cs.meta", packageDir).write("META")
            }
        }
        return packageDir
    }

    boolean hasPackageOnArtifactory(String repoName, String artifactName, String artifactVersion = DEFAULT_VERSION) {
        List<RepoPath> packages = artifactory.searches()
                .repositories(repoName)
                .artifactsByName(artifactName)
                .version(artifactVersion)
                .doSearch()
        packages = packages.findAll {it.itemPath.endsWith(".json")}
        assert packages.size() == 1: "Could not find artifact `${artifactName}` on repository ${repoName}"
        return true
    }
}
