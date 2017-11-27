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

package wooga.gradle.wdk.unity.tasks

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import wooga.gradle.wdk.unity.AndroidResourceCopyMethod
import wooga.gradle.wdk.unity.WdkPluginExtension

class AndroidResourceCopyAction extends ResourceCopyAction {

    @Override
    void copyResources(Project project, WdkPluginExtension extension, Configuration resources) {
        File collectDir = extension.getAndroidResourcePluginDir()
        collectDir.mkdirs()

        if (extension.androidResourceCopyMethod == AndroidResourceCopyMethod.sync) {
            project.sync(new Action<CopySpec>() {
                @Override
                void execute(CopySpec copySpec) {
                    copySpec.from(resources)
                    copySpec.include '**/*.jar'
                    copySpec.include '**/*.aar'
                    copySpec.into collectDir
                }
            })
        } else if (extension.androidResourceCopyMethod == AndroidResourceCopyMethod.arrUnpack) {
            def artifacts = resources.resolve()
            def aarArtifacts = artifacts.findAll { it.path =~ /\.aar$/ }

            aarArtifacts.each { artifact ->
                def artifactName = artifact.name.replace(".aar", "")
                project.sync(new Action<CopySpec>() {
                    @Override
                    void execute(CopySpec copySpec) {
                        copySpec.from project.zipTree(artifact)
                        copySpec.into "$collectDir/$artifactName"
                        copySpec.include 'AndroidManifest.xml'
                        copySpec.include '**/*.jar'
                        copySpec.rename(/classes\.jar/, "${artifactName}.jar")
                    }
                })
            }

            project.sync(new Action<CopySpec>() {
                @Override
                void execute(CopySpec copySpec) {
                    copySpec.from resources
                    copySpec.into "$collectDir/libs"
                    copySpec.include '*.jar'
                }
            })
        }
    }
}
