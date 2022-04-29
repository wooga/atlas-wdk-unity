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

package wooga.gradle.wdk.unity

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.internal.impldep.org.eclipse.jgit.errors.NotSupportedException
import org.gradle.util.GUtil

import javax.inject.Inject

abstract class WdkPluginExtension {

    @Inject
    ObjectFactory getObjects() {
        throw new NotSupportedException("")
    }

    private final DirectoryProperty pluginsDir = objects.directoryProperty()
    DirectoryProperty getPluginsDir(){
        pluginsDir
    }
    void setPluginsDir(Provider<Directory> value) {
        pluginsDir.set(value)
    }
    void setPluginsDir(File value) {
        pluginsDir.set(value)
    }

    // TODO: Rename to 'woogaAssetsDir' maybe?
    private final DirectoryProperty assetsDir = objects.directoryProperty()
    /**
     * @return The directory within the Unity project that contains the wdk assets. By convention within 'Assets/Wooga'
     */
    DirectoryProperty getAssetsDir() {
        assetsDir
    }
    void setAssetsDir(File value) {
        assetsDir.set(value)
    }
    void setAssetsDir(Provider<Directory> value) {
        assetsDir.set(value)
    }

    private final DirectoryProperty iosResourcePluginDir = objects.directoryProperty()
    DirectoryProperty getIosResourcePluginDir() {
        iosResourcePluginDir
    }

    private final DirectoryProperty androidResourcePluginDir = objects.directoryProperty()
    DirectoryProperty getAndroidResourcePluginDir() {
        androidResourcePluginDir
    }

    private final DirectoryProperty webGLResourcePluginDir = objects.directoryProperty()
    DirectoryProperty getWebGLResourcePluginDir() {
        webGLResourcePluginDir
    }

    private final DirectoryProperty paketUnity3dInstallDir = objects.directoryProperty()
    DirectoryProperty getPaketUnity3dInstallDir() {
        paketUnity3dInstallDir
    }

    private final List<String> editorDependeciesToMoveDuringTestBuild = new ArrayList<String>()
    List<String> getEditorDependenciesToMoveDuringTestBuild() {
        editorDependeciesToMoveDuringTestBuild
    }

    WdkPluginExtension editorDependenciesToMoveDuringTestBuild(String... dependencies) {
        if (dependencies == null) {
            throw new IllegalArgumentException("dependencies == null!")
        }
        editorDependeciesToMoveDuringTestBuild.addAll(Arrays.asList(dependencies))
        return this
    }

    WdkPluginExtension editorDependenciesToMoveDuringTestBuild(Iterable<String> dependencies) {
        GUtil.addToCollection(editorDependeciesToMoveDuringTestBuild, dependencies)
        return this
    }

    void setEditorDependenciesToMoveDuringTestBuild(Iterable<String> dependencies) {
        editorDependeciesToMoveDuringTestBuild.clear()
        editorDependeciesToMoveDuringTestBuild.addAll(dependencies)
    }

    private final DirectoryProperty packageDirectory = objects.directoryProperty()
    /**
     * @return The directory where the UPM package sources of the WDK are located.
     * At its root, it must contain a package manifest file (package.json) file.
     */
    DirectoryProperty getPackageDirectory() {
        packageDirectory
    }

    private final Property<Boolean> generateMetaFiles = objects.property(Boolean)
    @Input
    Property<Boolean> getGenerateMetaFiles() {
        generateMetaFiles
    }
    void setGenerateMetaFiles(Provider<Boolean> value) {
        generateMetaFiles.set(value)
    }
}
