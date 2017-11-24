/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.wdk.unity

import nebula.test.IntegrationSpec
import spock.lang.Unroll

class WdkUnityCompatibilityIntegrationSpec extends IntegrationSpec {


    @Unroll
    def "will not apply when incompatible version of net.wooga.unity is applied #timing"() {
        given: "a build.gradle with net.wooga.unity < 0.16.0 applied"

        if (loadDependencies) {
            buildFile << """
            buildscript {
                repositories {
                    maven {
                        url "https://plugins.gradle.org/m2/"
                    }
                }
                dependencies {
                    classpath "gradle.plugin.net.wooga.gradle:atlas-unity:$unityPluginVersion"
                }
            }
            """.stripIndent()
        }

        and: "plugins applied"
        buildFile << """
        ${firstPlugin}
        ${secondPlugin}
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("tasks")

        then:
        result.standardOutput.contains("detected incompatible net.wooga.unity plugin") == !applies

        where:
        firstPlugin                       | secondPlugin                      | unityPluginVersion | applies
        "apply plugin: 'net.wooga.unity'" | applyPlugin(WdkUnityPlugin)       | "0.15.0"           | false
        applyPlugin(WdkUnityPlugin)       | "apply plugin: 'net.wooga.unity'" | "0.15.0"           | false
        ""                                | applyPlugin(WdkUnityPlugin)       | ""                 | true
        applyPlugin(WdkUnityPlugin)       | ""                                | ""                 | true

        loadDependencies = (unityPluginVersion != "")
    }
}
