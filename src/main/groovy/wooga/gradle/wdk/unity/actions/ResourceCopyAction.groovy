/*
 * Copyright 2021 Wooga GmbH
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

package wooga.gradle.wdk.unity.actions

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import wooga.gradle.wdk.unity.WdkPluginExtension
import wooga.gradle.wdk.unity.tasks.ResourceCopyTask

abstract class ResourceCopyAction implements Action<ResourceCopyTask> {

    @Override
    final void execute(ResourceCopyTask task) {
        Project project = task.project
        WdkPluginExtension extension = project.extensions.getByType(WdkPluginExtension)
        Configuration resources = task.resources

        copyResources(project, extension, resources)
    }

    abstract void copyResources(Project project, WdkPluginExtension extension, Configuration resources)
}
