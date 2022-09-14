package wooga.gradle.wdk.upm

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import wooga.gradle.wdk.upm.internal.Extensions
import wooga.gradle.wdk.upm.internal.repository.UPMArtifactRepository
import wooga.gradle.wdk.upm.traits.UPMPublishSpec

import java.util.stream.Collectors

class UPMExtension implements UPMPublishSpec {

    final NamedDomainObjectContainer<UPMProjectDeclaration> projects
    protected Provider<UPMArtifactRepository> selectedUPMRepository

    static UPMExtension withProjectsContainer(Project project, String extensionName) {
        NamedDomainObjectFactory<UPMProjectDeclaration> projectsFactory = {
            String name -> UPMProjectDeclaration.withStaticConventions(project, name)
        }
        def projects = project.objects.domainObjectContainer(UPMProjectDeclaration, projectsFactory)

        return project.extensions.create(extensionName, UPMExtension, projects)
    }

    static UPMExtension withStaticConventions(Project project, String name) {
        def extension = withProjectsContainer(project, name)
        Extensions.setPropertiesOwner(UPMExtension, extension, name)
        extension.with {
            repository.convention(UPMConventions.repository.getStringValueProvider(project))
            username.convention(UPMConventions.username.getStringValueProvider(project))
            password.convention(UPMConventions.password.getStringValueProvider(project))
        }
        return extension
    }

    static UPMExtension withPublishingConventions(Project project, PublishingExtension publishingExt, String name) {
        def extension = withStaticConventions(project, name)
        extension.with {
            def upmRepositories = project.provider({ upmReposFromPublishing(publishingExt) })
            it.selectedUPMRepository = upmRepositories.flatMap {
                upmRepos -> extension.repository.map{ upmRepos[it]}
            }
            username.convention(UPMConventions.username.getStringValueProvider(project)
                    .orElse(selectedUPMRepository.map {it.credentials?.username}))
            password.convention(UPMConventions.password.getStringValueProvider(project)
                    .orElse(selectedUPMRepository.map {it.credentials?.password}))
        }
        return extension
    }


    UPMExtension(NamedDomainObjectContainer<UPMProjectDeclaration> projects) {
        this.projects = projects
    }

    Provider<List<UPMProjectDeclaration>> getProjectsProvider() {
        def projectList = objects.listProperty(UPMProjectDeclaration)

        projects.configureEach { UPMProjectDeclaration upmProject ->
            projectList.add(upmProject)
        }
        return projectList
    }

    public void projects(Action<? super NamedDomainObjectContainer<UPMProjectDeclaration>> action) {
        action.execute(projects);
    }

    private static Map<String, UPMArtifactRepository> upmReposFromPublishing(PublishingExtension publishExt) {
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
