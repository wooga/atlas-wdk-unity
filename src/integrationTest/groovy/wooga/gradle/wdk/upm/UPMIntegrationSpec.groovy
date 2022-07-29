package wooga.gradle.wdk.upm

import org.apache.http.client.HttpResponseException
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.jfrog.artifactory.client.model.RepoPath
import spock.lang.Shared
import wooga.gradle.unity.utils.PackageManifestBuilder
import wooga.gradle.wdk.UnityIntegrationSpec

import java.util.stream.IntStream

class UPMIntegrationSpec extends UnityIntegrationSpec {

    protected static final String WOOGA_ARTIFACTORY_BASE_URL = "https://wooga.jfrog.io/wooga"
    protected static final String WOOGA_ARTIFACTORY_CI_REPO = "atlas-upm-integrationTest"

    protected final String DEFAULT_VERSION = "0.0.1"

    @Shared
    long specStartupTime
    @Shared
    Artifactory artifactory



    Tuple2<String, String> credentialsFromEnv(String userEnv="ATLAS_ARTIFACTORY_INTEGRATION_USER",
                                              String pwdEnv="ATLAS_ARTIFACTORY_INTEGRATION_PASSWORD") {
        def user = mustGetEnv(userEnv)
        def password = mustGetEnv(pwdEnv)
        return [user, password]
    }
    //TODO: test this task and see why it gets stuck
    //https://jenkins.atlas.wooga.com/blue/organizations/jenkins/wdk_unity%2Fwdk-unity-AsyncAwait/detail/PR-48/1/pipeline
    def mustGetEnv(String envKey) {
        return Optional.ofNullable(System.getenv(envKey)).
                orElseThrow{-> new IllegalArgumentException("$envKey environment not present")}
    }

    def setupSpec() {
        this.specStartupTime = System.currentTimeMillis()
        def (username, password) = credentialsFromEnv()
        this.artifactory = ArtifactoryClientBuilder.create()
                .setUrl(WOOGA_ARTIFACTORY_BASE_URL)
                .setUsername(username)
                .setPassword(password)
                .build()
    }

    def cleanup() {
        cleanupArtifactoryRepo(WOOGA_ARTIFACTORY_CI_REPO)
    }

    static String artifactoryURL(String repoName) {
        return "$WOOGA_ARTIFACTORY_BASE_URL/$repoName"
    }

    /**
     * Delete everything in given repo created within the execution time of this Spec
     * @param repoName
     */
    def cleanupArtifactoryRepo(String repoName) {
        List<RepoPath> searchItems = artifactory.searches()
                .repositories(repoName)
                .artifactsCreatedInDateRange(specStartupTime, System.currentTimeMillis())
                .doSearch()

        for (RepoPath searchItem : searchItems) {
            deleteFromArtifactoryIfExists(repoName, searchItem.getItemPath())
        }
    }

    def deleteFromArtifactoryIfExists(String repoName, String itemPath) {
        try {
            artifactory.repository(repoName).delete(itemPath)
        } catch(HttpResponseException e) {
            if(e.statusCode == 404) {
                println("${itemPath} was already deleted by artifactory, skipping")
            } else {
                throw e
            }
        }
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
}
