package wooga.gradle.wdk.publish

import com.wooga.gradle.PropertyLookup

class WDKPublishConvention {

    static final PropertyLookup releaseNotesFile = new PropertyLookup(
            ["WDK_PUBLISH_RELEASE_NOTES_FILE"],
            ["wdk.publish.releaseNotes", "publish.releaseNotes"],
            null)
}
