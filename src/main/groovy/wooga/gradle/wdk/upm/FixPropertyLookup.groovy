package wooga.gradle.wdk.upm

import com.wooga.gradle.PropertyLookup
import groovy.transform.InheritConstructors
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory


/**
 * PropertyLookup in the current gradle-commons version has a bug where its uses the buildDirectory instead of the projectDirectory as root for file-related properties.
 * This class is a local fix for this issue. it can be replaced by commons' PropertyLookup when this issue is fixed there.
 */
@InheritConstructors
class FixPropertyLookup extends PropertyLookup {

    @Override
    Provider<Directory> getDirectoryValueProvider(ProviderFactory factory, ProjectLayout layout, Map<String, ?> properties, Map<String, ?> env = null, Object defaultValue = null) {
        layout.projectDirectory.dir(
                factory.provider({
                    getValueAsString(properties, env, defaultValue)
                })
        )
    }

    @Override
    Provider<RegularFile> getFileValueProvider(ProviderFactory factory, ProjectLayout layout, Map<String, ?> properties, Map<String, ?> env = null, Object defaultValue = null) {
        layout.projectDirectory.file(
                factory.provider({
                    getValueAsString(properties, env, defaultValue)
                })
        )
    }
}
