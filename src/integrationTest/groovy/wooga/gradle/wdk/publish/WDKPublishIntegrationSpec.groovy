package wooga.gradle.wdk.publish

import spock.lang.Shared
import wooga.gradle.wdk.UnityIntegrationSpec
import wooga.gradle.wdk.upm.internal.UPMTestTools

import java.util.concurrent.TimeoutException


class WDKPublishIntegrationSpec extends UnityIntegrationSpec {


    @Shared
    UPMTestTools upmUtils

    @Shared
    long specCreationTs

    def setupSpec() {
        this.upmUtils = new UPMTestTools()
        this.specCreationTs = System.currentTimeMillis()
    }

    boolean waitForTimeout(long poolInterval = 10000, long timeoutMs = 60000, Closure operation) {
        def startTs = System.currentTimeMillis()
        def result = operation()
        while(result == null) {
            Thread.sleep(poolInterval)
            result = operation()
            if(System.currentTimeMillis() > startTs + timeoutMs) {
                return false
            }
        }
        return true
    }
}
