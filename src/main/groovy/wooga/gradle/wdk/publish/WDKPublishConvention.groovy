package wooga.gradle.wdk.publish

import com.wooga.gradle.PropertyLookup
import wooga.gradle.wdk.upm.FixPropertyLookup

class WDKPublishConvention {

    static final PropertyLookup releaseNotesFile = new FixPropertyLookup(
            ["WDK_PUBLISH_RELEASE_NOTES_FILE"],
            ["wdk.publish.releaseNotes", "publish.releaseNotes"],
            null)
}
