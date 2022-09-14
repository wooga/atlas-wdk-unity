package wooga.gradle.wdk.upm

import org.gradle.api.Named
import org.gradle.api.Project
import wooga.gradle.wdk.upm.internal.Extensions
import wooga.gradle.wdk.upm.internal.UPMProjectConfigurator
import wooga.gradle.wdk.upm.traits.UPMPackSpec


class UPMProjectDeclaration implements UPMPackSpec, Named {

    protected String name

    static UPMProjectDeclaration withStaticConventions(Project project, String name) {
        def extension = project.objects.newInstance(UPMProjectDeclaration).with {
            it.name = name
            return it
        }
        Extensions.setPropertiesOwner(UPMProjectDeclaration, extension, name)
        extension.with {
            version.convention(UPMConventions.version.resolve(name).getStringValueProvider(project).orElse(project.provider { project.version.toString() }))
            packageDirectory.convention(UPMConventions.packageDirectory.resolve(name).getDirectoryValueProvider(project))
            generateMetaFiles.convention(UPMConventions.generateMetaFiles.resolve(name).getBooleanValueProvider(project))
        }
        return extension
    }

    UPMProjectDeclaration() {}

    @Override
    String getName() {
        return name
    }

    def createConfigurator(Project project) {
        return new UPMProjectConfigurator(project, this)
    }

}
