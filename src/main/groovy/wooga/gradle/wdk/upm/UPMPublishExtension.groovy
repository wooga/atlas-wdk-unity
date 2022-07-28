package wooga.gradle.wdk.upm

import org.gradle.api.Project

class UPMPublishExtension implements UPMPublishSpec {


    static UPMPublishExtension newWithConventions(Project project, String extensionName) {
        def extension = project.extensions.create(extensionName, UPMPublishExtension)
        extension.repository.convention(UPMPublishConventions.repository.getStringValueProvider(project))
        extension.version.convention(UPMPublishConventions.version.getStringValueProvider(project).orElse(project.provider{ project.version.toString() }))
        extension.username.convention(UPMPublishConventions.username.getStringValueProvider(project))
        extension.password.convention(UPMPublishConventions.password.getStringValueProvider(project))
        extension.packageDirectory.convention(UPMPublishConventions.packageDirectory.getDirectoryValueProvider(project))
        extension.generateMetaFiles.convention(UPMPublishConventions.generateMetaFiles.getBooleanValueProvider(project))
        return extension
    }

}
