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

import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.internal.Factory
import wooga.gradle.unity.UnityPluginExtension

class DefaultWdkPluginExtension implements WdkPluginExtension {

    private final FileResolver fileResolver
    private final Project project

    private Factory<File> pluginsDir
    private Factory<File> assetsDir

    static String IOS_PLUGIN_DIRECTORY = "iOS"
    static String ANDROID_PLUGIN_DIRECTORY = "Android"
    static String WEBGL_PLUGIN_DIRECTORY = "WebGL"
    static String PAKET_UNITY_3D_INSTALL_DIRECTORY = "Paket.Unity3D"

    DefaultWdkPluginExtension(Project project, FileResolver fileResolver) {
        this.project = project
        this.fileResolver = fileResolver
    }

    @Override
    File getAssetsDir() {
        if (assetsDir) {
            return assetsDir.create()
        }

        return null
    }

    @Override
    void setAssetsDir(File path) {
        assetsDir = fileResolver.resolveLater(path)
    }

    @Override
    void setAssetsDir(Object path) {
        assetsDir = fileResolver.resolveLater(path)
    }

    @Override
    File getPluginsDir() {
        if (pluginsDir) {
            return pluginsDir.create()
        }

        return null
    }

    @Override
    void setPluginsDir(File reportsDir) {
        pluginsDir = fileResolver.resolveLater(reportsDir)
    }

    @Override
    void setPluginsDir(Object reportsDir) {
        pluginsDir = fileResolver.resolveLater(reportsDir)
    }

    @Override
    File getIOSResourcePluginDir() {
        return new File(getPluginsDir(), IOS_PLUGIN_DIRECTORY)
    }

    @Override
    File getAndroidResourcePluginDir() {
        return new File(getPluginsDir(), ANDROID_PLUGIN_DIRECTORY)
    }

    @Override
    File getWebGLResourcePluginDir() {
        return new File(getPluginsDir(), WEBGL_PLUGIN_DIRECTORY)
    }

    @Override
    File getPaketUnity3dInstallDir() {
        UnityPluginExtension unity = project.extensions.getByType(UnityPluginExtension)
        return new File(unity.getAssetsDir(), PAKET_UNITY_3D_INSTALL_DIRECTORY)
    }
}
