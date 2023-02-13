package wooga.gradle.wdk.unity.tasks

import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import wooga.gradle.unity.UnityTask

class ResolveUnityPackages extends UnityTask {

    @InputFiles
    @SkipWhenEmpty
    FileCollection getInputFiles() {
        project.files(manifest)
    }

    private final RegularFileProperty manifest = objects.fileProperty()

    @Internal
    RegularFileProperty getManifest() {
        manifest
    }

    void setManifest(Provider<RegularFile> value) {
        manifest.set(value)
    }

    void setManifest(File value) {
        manifest.set(value)
    }
    private final RegularFileProperty packageLock = objects.fileProperty()

    @OutputFile
    RegularFileProperty getPackageLock() {
        packageLock
    }

    void setPackageLock(Provider<RegularFile> value) {
        packageLock.set(value)
    }

    void setPackageLock(File value) {
        packageLock.set(value)
    }

    @TaskAction
    protected resolve() {
        packageLock.get().asFile.delete()
        exec()
    }
}
