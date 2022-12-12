package wooga.gradle.wdk.unity


import org.gradle.api.file.Directory
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.jfrog.artifactory.client.model.RepoPath
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.unity.utils.PackageManifestBuilder

class PublishWdkUnityIntegrationSpec extends com.wooga.gradle.test.IntegrationSpec {

    @Shared
    def packageTitle = "UpmTestPackage"

    @Shared
    def packageVersion = "0.0.1"

    @Shared
    def packageName = "com.wooga.${packageTitle.toLowerCase()}"

    @Shared
    def packageArtifactName = "${packageName}-${packageVersion}"

    @Shared
    def packageFileName = "${packageArtifactName}.tgz"

    @Shared
    def artifactoryUrl = "https://wooga.jfrog.io/wooga"

    @Shared
    Artifactory artifactory

    @Shared
    def artifactoryRepoName = "atlas-upm-integrationTest"

    def setup() {
        buildFile << """
            ${applyPlugin(WdkUnityPlugin)}
        """.stripIndent()
    }

    def getCredentials() {
        System.getenv("repositoryCredentials") ?: System.getenv("atlas_upm_integration_user")
    }

    def setupSpec() {
        String artifactoryCredentials = getCredentials()
        assert artifactoryCredentials
        def credentials = artifactoryCredentials.split(':')
        artifactory = ArtifactoryClientBuilder.create()
            .setUrl(artifactoryUrl)
            .setUsername(credentials[0])
            .setPassword(credentials[1])
            .build()
    }

    def cleanup() {
        cleanupArtifactory(artifactoryRepoName, packageFileName)
    }

    def cleanupArtifactory(String repoName, String artifactName) {
        List<RepoPath> searchItems = artifactory.searches()
            .repositories(repoName)
            .artifactsByName(artifactName)
            .doSearch()

        for (RepoPath searchItem : searchItems) {
            String repoKey = searchItem.getRepoKey()
            String itemPath = searchItem.getItemPath()
            artifactory.repository(repoName).delete(itemPath)
        }
    }

    def hasPackageOnArtifactory(String repoName, String artifactName) {
        List<RepoPath> packages = artifactory.searches()
            .repositories(repoName)
            .artifactsByName(artifactName)
            .doSearch()

        assert packages.size() == 1 : "Could not find artifact `${artifactName}` on repository ${repoName}"
        true
    }

    /**
     * The main test, which publishes directly to the JFrog remote artifactory
     */
    def "publishes to artifactory"() {

        given: "a sample project directory with a package manifest"
        def packageDir = writeTestPackage(packageTitle, packageName)
        assert packageDir.exists()

        and: "configuration of the main plugin and the artifactory plugin"
        String repositoryCredentials = getCredentials()
        assert repositoryCredentials
        def userName = repositoryCredentials.split(":").first()
        def password = repositoryCredentials.split(":").last()
        buildFile << """
wdk {
generateMetaFiles.set(false)
}

artifactory {
    contextUrl = "https://wooga.jfrog.io/artifactory/"
    publish {
        repository {
            repoKey = ${wrapValueBasedOnType(artifactoryRepoName, String)}
            username = ${wrapValueBasedOnType(userName, String)}
            password = ${wrapValueBasedOnType(password, String)}
        }
    }
}
"""

        and: "configuration of the task to generate the package"
        buildFile << """
upmPack {
packageDirectory.set(${wrapValueBasedOnType(packageTitle, Directory)})
packageName = ${wrapValueBasedOnType(packageName, String)}
archiveVersion.set(${wrapValueBasedOnType(packageVersion, String)})
}
"""

        when:
        def result = runTasksSuccessfully(WdkUnityPlugin.GENERATE_UPM_PACKAGE_TASK_NAME, "publish")

        then:
        result.success

        def buildDir = new File(projectDir, "build")
        buildDir.exists()
        def distDir = new File(buildDir, "distributions")
        distDir.exists()
        def packageFile = new File(distDir, packageFileName)
        packageFile.exists()

        hasPackageOnArtifactory(artifactoryRepoName, packageArtifactName)
    }

    @Unroll
    def "does not add generate meta files task if not set by extension"() {
        given: "configuration of the main plugin"
        directory("Foobar")
        buildFile << """
wdk {
packageDirectory.set(${wrapValueBasedOnType("Foobar", Directory)})
generateMetaFiles.set(${wrapValueBasedOnType(generateMetaFiles, Boolean)})
}
"""
        when:
        def result = runTasksSuccessfully(WdkUnityPlugin.GENERATE_UPM_PACKAGE_TASK_NAME, "--dry-run")

        then:
        present == outputContains(result, WdkUnityPlugin.GENERATE_META_FILES_TASK_NAME)

        where:
        generateMetaFiles | present
        true              | true
        false             | false
    }

    File writeTestPackage(String packageDirectory, String packageName) {
        def packageDir = directory(packageDirectory)
        def packageManifestFile = file("package.json", packageDir)
        packageManifestFile.write(new PackageManifestBuilder(packageName, packageVersion).build())
        file("Sample.cs", packageDir).write("class Sample {}")
        file("Sample.cs.meta", packageDir).write("META")
        packageDir
    }
}
