package wooga.gradle.wdk.upm

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import wooga.gradle.wdk.upm.internal.Extensions
import wooga.gradle.wdk.upm.internal.UnityProjects
import wooga.gradle.wdk.upm.internal.repository.UPMArtifactRepository
import wooga.gradle.wdk.upm.traits.UPMPackSpec
import wooga.gradle.wdk.upm.traits.UPMPublishSpec

import java.util.stream.Collectors

class UPMExtension implements UPMPublishSpec, UPMPackSpec {

    protected Provider<UPMArtifactRepository> selectedUPMRepository

    static UPMExtension newWithPublishingConventions(Project project, PublishingExtension publishingExt, String extensionName) {
        def extension = newWithConventions(project, extensionName)
        extension.with {
            def upmRepositories = project.provider({ upmRepoFromPublishing(publishingExt) })
            //better this than zip, bc zip have awful error messages, and like this we can pinpoint the eventual error better.
            it.selectedUPMRepository = upmRepositories.map {
                upmRepos -> upmRepos[extension.repository.get()]
            }

            username.convention(UPMConventions.username.getStringValueProvider(project)
                                .orElse(selectedUPMRepository.map {it.credentials?.username}))
            password.convention(UPMConventions.password.getStringValueProvider(project)
                                .orElse(selectedUPMRepository.map {it.credentials?.password}))
        }
        return extension
    }

    static UPMExtension newWithConventions(Project project, String extensionName) {
        def extension = project.extensions.create(extensionName, UPMExtension)
        Extensions.setPropertiesOwner(UPMExtension, extension, extensionName)

        extension.with {
            repository.convention(UPMConventions.repository.getStringValueProvider(project))

            version.convention(UPMConventions.version.getStringValueProvider(project)
                    .orElse(project.provider { project.version.toString() }))
            packageDirectory.convention(UPMConventions.packageDirectory.getDirectoryValueProvider(project))
            generateMetaFiles.convention(UPMConventions.generateMetaFiles.getBooleanValueProvider(project))

            username.convention(UPMConventions.username.getStringValueProvider(project))
            password.convention(UPMConventions.password.getStringValueProvider(project))
        }

        return extension
    }

    private static Map<String, UPMArtifactRepository> upmRepoFromPublishing(PublishingExtension publishExt) {
        return publishExt.repositories.withType(UPMArtifactRepository).stream().map {
            repo -> new Tuple2<>(repo.name, repo)
        }.collect(Collectors.toMap({ it.first as String }, { it.second as UPMArtifactRepository }))
    }

    Provider<String> getUpmRepositoryBaseUrl() {
        return selectedUPMRepository.map {it.baseUrl}
    }

    Provider<String> getUpmRepositoryKey() {
        return selectedUPMRepository.map{it.repositoryKey}
    }
}
