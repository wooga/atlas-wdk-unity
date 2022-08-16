package wooga.gradle.wdk.upm


import spock.lang.Shared
import wooga.gradle.wdk.UnityIntegrationSpec
import wooga.gradle.wdk.upm.internal.UPMTestTools

class UPMIntegrationSpec extends UnityIntegrationSpec {

    static final String DEFAULT_VERSION = UPMTestTools.DEFAULT_VERSION
    static final String WOOGA_ARTIFACTORY_CI_REPO = UPMTestTools.WOOGA_ARTIFACTORY_CI_REPO

    @Shared
    long specStartupTime
    @Shared
    UPMTestTools utils


    def setupSpec() {
        this.specStartupTime = System.currentTimeMillis()
        this.utils = new UPMTestTools()
    }

    def cleanup() {
        utils.cleanupArtifactoryRepoInRange(specStartupTime, System.currentTimeMillis())
    }

    String artifactoryURL(String repoName) {
        return utils.artifactoryURL(repoName)
    }
}
