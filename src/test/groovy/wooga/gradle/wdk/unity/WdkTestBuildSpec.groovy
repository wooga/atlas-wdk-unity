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

import nebula.test.ProjectSpec
import org.gradle.api.DefaultTask
import spock.lang.Unroll
import wooga.gradle.unity.tasks.Unity

class WdkTestBuildSpec extends ProjectSpec {

    public static final String PLUGIN_NAME = 'net.wooga.wdk-unity'

    def setup() {

    }

    @Unroll
    def "generates task :#taskName when 'AtlasBuildTools' is available"() {
        given: "paket.unity.references file with AtlasBuildTools"
        File reference = new File(projectDir, "paket.unity3d.references")
        reference << """
        Wooga.AtlasBuildTools
        """

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def task = project.tasks.findByName(taskName)
        taskType.isInstance(task)

        where:
        taskName                  | taskType
        "performTestBuild"        | DefaultTask
        "cleanTestBuild"          | Unity
        "performTestBuildIOS"     | Unity
        "performTestBuildAndroid" | Unity
        "performTestBuildWebGL"   | Unity
    }

    @Unroll
    def "skips generation of task :#taskName when 'AtlasBuildTools' is not available paket.unity.references"() {
        given: "paket.unity.references file"
        File reference = new File(projectDir, "paket.unity3d.references")
        reference << """
        """

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        !project.tasks.findByName(taskName)

        where:
        taskName                  | _
        "performTestBuild"        | _
        "cleanTestBuild"          | _
        "performTestBuildIOS"     | _
        "performTestBuildAndroid" | _
        "performTestBuildWebGL"   | _
    }

    @Unroll
    def "skips generation of task :#taskName when paket.unity.references is missing"() {
        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        !project.tasks.findByName(taskName)

        where:
        taskName                  | _
        "performTestBuild"        | _
        "cleanTestBuild"          | _
        "performTestBuildIOS"     | _
        "performTestBuildAndroid" | _
        "performTestBuildWebGL"   | _
    }

}
