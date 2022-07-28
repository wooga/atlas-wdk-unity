package wooga.gradle.wdk.upm

import com.wooga.gradle.BaseSpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option

trait UPMPublishSpec implements BaseSpec {

    @Option(option = "repository", description = """
    The repository where the UPM package will be published. Must be one of the UPM repositories declared previously on the publishing extension.
    """)
    private final Property<String> repository = objects.property(String)

    @Input
    Property<String> getRepository() {
        return repository
    }

    void setRepository(Provider<String> repository) {
        this.repository.set(repository)
    }

    void setRepository(String repository) {
        this.repository.set(repository)
    }

    @Option(option = "version", description = """
    The version of the UPM package being published. 
    """)
    private final Property<String> version = objects.property(String)

    @Input
    Property<String> getVersion() {
        return version
    }

    void setVersion(Provider<String> version) {
        this.version.set(version)
    }

    void setVersion(String version) {
        this.version.set(version)
    }

    @Option(option = "publish-username", description = """
    The username credential of the target publish repository
    """)
    private final Property<String> username = objects.property(String)

    @Input
    Property<String> getUsername() {
        return username
    }

    void setUsername(Provider<String> username) {
        this.username.set(username)
    }

    void setUsername(String username) {
        this.username.set(username)
    }

    @Option(option = "publish-password", description = """
    The password credential of the target publish repository
    """)
    private final Property<String> password = objects.property(String)

    @Input
    Property<String> getPassword() {
        return password
    }

    void setPassword(Provider<String> password) {
        this.password.set(password)
    }

    void setPassword(String password) {
        this.password.set(password)
    }

    @Option(option = "package-directory", description = """
    The directory where the UPM package sources of the WDK are located.
     At its root, it must contain a package manifest file (package.json) file.
    """)
    private final DirectoryProperty packageDirectory = objects.directoryProperty()
    
    @Input
    DirectoryProperty getPackageDirectory() {
        return packageDirectory
    }

    void setPackageDirectory(Provider<? extends Directory> directory) {
        packageDirectory.set(directory)
    }

    void setPackageDirectory(Directory directory) {
        packageDirectory.set(directory)
    }

    @Option(option = "generate-meta-files", description = """
    If the creation of unity metafiles should be forced.
    """)
    private final Property<Boolean> generateMetaFiles = objects.property(Boolean)
    @Input
    Property<Boolean> getGenerateMetaFiles() {
        return generateMetaFiles
    }

    void setGenerateMetaFiles(Provider<Boolean> value) {
        generateMetaFiles.set(value)
    }

    void setGenerateMetaFiles(Boolean value) {
        generateMetaFiles.set(value)
    }

}