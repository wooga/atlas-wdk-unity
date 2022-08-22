package wooga.gradle.wdk.publish

import spock.lang.Shared
import wooga.gradle.wdk.UnityIntegrationSpec
import wooga.gradle.wdk.upm.internal.UPMTestTools

class WDKPublishIntegrationSpec extends UnityIntegrationSpec {


    @Shared
    UPMTestTools upmUtils

    @Shared
    long specCreationTs

    def setupSpec() {
        this.upmUtils = new UPMTestTools()
        this.specCreationTs = System.currentTimeMillis()
    }

    boolean notNullOrTimeout(long poolInterval = 10000, long timeoutMs = 60000, Closure operation) {
        def result = UPMTestTools.retry(timeoutMs, poolInterval, {it == null}, operation)
        return result != null
    }

    boolean timeout(long poolInterval = 10000, long timeoutMs = 60000, Closure operation) {
        return UPMTestTools.retry(timeoutMs, poolInterval, {it == null}, operation)
    }
}
