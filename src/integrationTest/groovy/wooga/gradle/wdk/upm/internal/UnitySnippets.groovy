package wooga.gradle.wdk.upm.internal

import com.wooga.gradle.test.executable.FakeExecutables
import org.gradle.api.file.RegularFile

class UnitySnippets implements UnitySnippetsTrait {}

trait UnitySnippetsTrait {

    static String configureMockUnity(File baseDir) {
        return configureMockUnity(new File(baseDir, "fakeUnity").absolutePath)
    }

    static String configureMockUnity(String fakeUnityLocation) {
        def unityTestLocation = FakeExecutables.argsReflector(fakeUnityLocation, 0).executable
        return BasicSnippets.extension("unity") {
            it.unityPath = [unityTestLocation, RegularFile]
        }
    }
}
