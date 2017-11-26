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

import org.gradle.api.plugins.ExtensionContainer
import wooga.gradle.unity.UnityPluginExtension

import java.util.concurrent.Callable

class DefaultPluginsDirGetter implements Callable<File> {

    private ExtensionContainer extensions

    DefaultPluginsDirGetter(ExtensionContainer extensions) {
        this.extensions = extensions
    }

    @Override
    File call() throws Exception {
        UnityPluginExtension unity = extensions.getByType(UnityPluginExtension)
        return unity.getPluginsDir()
    }
}
