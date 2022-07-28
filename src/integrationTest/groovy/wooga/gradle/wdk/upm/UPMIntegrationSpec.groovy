package wooga.gradle.wdk.upm

import org.apache.http.client.HttpResponseException
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.jfrog.artifactory.client.model.RepoPath
import spock.lang.Shared
import wooga.gradle.wdk.UnityIntegrationSpec

class UPMIntegrationSpec extends UnityIntegrationSpec {

    @Shared
    long specStartupTime
    @Shared
    Artifactory artifactory
    @Shared
    protected static final String WOOGA_ARTIFACTORY_BASE_URL = "https://wooga.jfrog.io/wooga"
    protected static final String WOOGA_ARTIFACTORY_CI_REPO = "atlas-upm-integrationTest"

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
}
