package wooga.gradle.wdk.unity.tasks

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import wooga.gradle.unity.UnityTask

class ResolveUnityPackages extends UnityTask {
    private final RegularFileProperty manifest = objects.fileProperty()

    @InputFile
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
