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

    def <T> T waitForNotNull(long poolInterval = 10000, long timeoutMs = 120000, Closure<T> operation) {
        def startTs = System.currentTimeMillis()
        def result = operation()
        while(result == null) {
            Thread.sleep(poolInterval)
            result = operation()
            if(System.currentTimeMillis() > startTs + timeoutMs) {
                throw new TimeoutException("timeout waiting while waiting for operation completion")
            }
        }
        return result
    }
}
