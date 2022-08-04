package wooga.gradle.wdk.publish

import com.wooga.gradle.BaseSpec
import org.gradle.api.file.RegularFileProperty

trait WDKPublishSpec extends BaseSpec {

    final RegularFileProperty releaseNotesFile //= new File("${project.buildDir}/outputs/release-notes.md")

}
