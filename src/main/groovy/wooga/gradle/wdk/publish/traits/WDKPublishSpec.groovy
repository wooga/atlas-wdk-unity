package wooga.gradle.wdk.publish.traits

import com.wooga.gradle.BaseSpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option

trait WDKPublishSpec extends BaseSpec {

    @Option(option = "release-notes-file", description = """
    Target markdown file that will contain the generated release notes which will be present as well on github.
    """)
    private final RegularFileProperty releaseNotesFile = objects.fileProperty()

    @Input
    RegularFileProperty getReleaseNotesFile() {
        return releaseNotesFile
    }

    void setReleaseNotesFile(Provider<? extends RegularFile> file) {
        releaseNotesFile.set(file)
    }

    void setReleaseNotesFile(RegularFile file) {
        releaseNotesFile.set(file)
    }
}
