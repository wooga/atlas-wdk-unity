package wooga.gradle.wdk.publish

import org.gradle.api.Project
import wooga.gradle.upm.artifactory.internal.Extensions
import wooga.gradle.wdk.publish.traits.WDKPublishSpec


class WDKPublishExtension implements WDKPublishSpec {

    static WDKPublishExtension newWithConventions(Project project, String extName) {
        def extension = project.extensions.create(extName, WDKPublishExtension)
        Extensions.setPropertiesOwner(WDKPublishExtension, extension, extName)
        def defaultReleaseNotesFile = project.layout.buildDirectory.file("outputs/release-notes.md")

        extension.releaseNotesFile.convention(
                WDKPublishConvention.releaseNotesFile
                .getFileValueProvider(project,null, project.provider { project.layout.projectDirectory })
                .orElse(defaultReleaseNotesFile))
        return extension
    }

}
