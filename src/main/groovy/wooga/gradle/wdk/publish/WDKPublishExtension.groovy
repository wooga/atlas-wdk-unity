package wooga.gradle.wdk.publish

import org.gradle.api.Project
import wooga.gradle.wdk.publish.traits.WDKPublishSpec
import wooga.gradle.wdk.upm.internal.Extensions


class WDKPublishExtension implements WDKPublishSpec {

    static WDKPublishExtension newWithConventions(Project project, String extName) {
        def extension = project.extensions.create(extName, WDKPublishExtension)
        Extensions.setPropertiesOwner(WDKPublishExtension, extension, extName)
        def defaultReleaseNotesFile = project.layout.buildDirectory.file("outputs/release-notes.md")
        extension.with {
            releaseNotesFile.convention(
                    WDKPublishConvention.releaseNotesFile.getFileValueProvider(project).orElse(defaultReleaseNotesFile))
        }
        return extension
    }

}
