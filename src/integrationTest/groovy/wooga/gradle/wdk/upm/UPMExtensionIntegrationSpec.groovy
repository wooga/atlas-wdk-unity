package wooga.gradle.wdk.upm

import com.wooga.gradle.test.PropertyLocation
import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import com.wooga.gradle.test.writers.PropertySetInvocation
import com.wooga.gradle.test.writers.PropertySetterWriter
import org.gradle.api.file.Directory
import spock.lang.Unroll

import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType
import static com.wooga.gradle.test.writers.PropertySetInvocation.*

class UPMExtensionIntegrationSpec extends UPMIntegrationSpec {

    static final String existingUPMPackage = "upmPackage"

    def setup() {
        buildFile << """
            ${applyPlugin(UPMPlugin)}
        """.stripIndent()
    }

    @Unroll("can set property #property with #invocation and type #type with build.gradle")
    def "can set property on upm extension with build.gradle"() {
        given: "existing UPM-ready folder"
        writeTestPackage(existingUPMPackage, existingUPMPackage)
        and:
        buildFile << """
        publishing {
            repositories {
                upm {
                    url = "https://artifactoryhost/artifactory/repository"
                    name = ${property == "repository" ? wrapValueBasedOnType(rawValue, String) : wrapValueBasedOnType("any", String)}
                }
            }
        }
        """
        if (property != "repository") {
            buildFile << "upm {repository = ${wrapValueBasedOnType("any", String)}}\n"
        }

        when:
//        set.location = invocation == PropertySetInvocation.none ? PropertyLocation.none : set.location
        def propertyQuery = runPropertyQuery(get, set)
                                            .withDirectoryPathsRelativeToProject()
                                            .withDirectoryProviderPathsRelativeToProject()


                withSerializer(Directory, {
            String dir -> new File(projectDir, dir).absolutePath
        }).withSerializer("Provider<Directory>", {
            String dir -> new File(projectDir, dir).absolutePath
        })

        then:
        propertyQuery.matches(rawValue)

        where:

        property            | invocation  | rawValue           | type
        "repository"        | providerSet | "repoName"         | "Provider<String>"
        "repository"        | assignment  | "repoName"         | "Provider<String>"
        "repository"        | setter      | "repoName"         | "String"
        "repository"        | assignment  | "repoName"         | "String"

        "version"           | providerSet | "0.0.1"            | "Provider<String>"
        "version"           | assignment  | "0.0.1"            | "Provider<String>"
        "version"           | setter      | "0.0.1"            | "String"
        "version"           | assignment  | "0.0.1"            | "String"
        "version"           | none        | "unspecified"      | "String" //unspecified is the default for project.version

        "username"          | providerSet | "user"             | "Provider<String>"
        "username"          | assignment  | "user"             | "Provider<String>"
        "username"          | setter      | "user"             | "String"
        "username"          | assignment  | "user"             | "String"
        "username"          | none        | null               | "String"
        //once set, should we be able to get credentials in build.gradle?
        "password"          | providerSet | "pwd"              | "Provider<String>"
        "password"          | assignment  | "pwd"              | "Provider<String>"
        "password"          | setter      | "pwd"              | "String"
        "password"          | assignment  | "pwd"              | "String"
        "password"          | none        | null               | "String"

        "packageDirectory"  | providerSet | "upmpkg"           | "Provider<Directory>"
        "packageDirectory"  | assignment  | "upmpkg"           | "Provider<Directory>"
        "packageDirectory"  | setter      | "upmpkg"           | "Directory" //not working
        "packageDirectory"  | assignment  | "upmpkg"           | "Directory"
        "packageDirectory"  | none        | existingUPMPackage | "Directory"

        "generateMetaFiles" | providerSet | true               | "Provider<Boolean>"
        "generateMetaFiles" | assignment  | false              | "Provider<Boolean>"
        "generateMetaFiles" | setter      | true               | "Boolean"
        "generateMetaFiles" | assignment  | false              | "Boolean"
        "generateMetaFiles" | none        | false              | "Boolean"

        set = new PropertySetterWriter("upm", property)
                .set(rawValue, type)
                .toScript(invocation)
//                .to(invocation == PropertySetInvocation.none ? PropertyLocation.none : set.location)
        get = new PropertyGetterTaskWriter(set)
    }

    @Unroll("can set property #property with env var #envVar")
    def "can set property from environment"() {
        given: "existing UPM-ready folder"
        writeTestPackage(existingUPMPackage, existingUPMPackage)
        and: "configured build.gradle file"
        buildFile << """
        publishing {
            repositories {
                upm {
                    url = "https://artifactoryhost/artifactory/repository"
                    name = ${property == "repository" ? wrapValueBasedOnType(rawValue, String) : wrapValueBasedOnType("any", String)}
                }
            }
        }
        """
        if (property != "repository") {
            buildFile << "upm {repository = ${wrapValueBasedOnType("any", String)}}\n"
        }

        when:
        def propertyQuery = runPropertyQuery(get, set).withSerializer(Directory) {
            String dir -> new File(projectDir, dir).absolutePath
        }

        then:
        propertyQuery.matches(rawValue)

        where:
        property            | envVar                    | type      | rawValue
        "repository"        | "UPM_PUBLISH_REPOSITORY"  | String    | "repoName"
        "version"           | "UPM_PUBLISH_VERSION"     | String    | "0.0.1"
        "username"          | "UPM_USR"                 | String    | "username"
        "username"          | "UPM_USERNAME"            | String    | "username"
        "password"          | "UPM_PWD"                 | String    | "passw0rd"
        "password"          | "UPM_PASSWORD"            | String    | "passw0rd"
        "packageDirectory"  | "UPM_PACKAGE_DIR"         | Directory | "wdk-name"
        "packageDirectory"  | "UPM_PACKAGE_DIRECTORY"   | Directory | "wdk-name"
        "generateMetaFiles" | "UPM_GENERATE_METAFILES"  | Boolean   | "true"
        "generateMetaFiles" | "UPM_GENERATE_META_FILES" | Boolean   | "false"

        set = new PropertySetterWriter("upm", property)
                .set(rawValue, type)
                .withEnvironmentKey(envVar)
                .to(PropertyLocation.environment)

        get = new PropertyGetterTaskWriter(set)
    }


    @Unroll("can set property #property with gradle property #gradlePropName")
    def "can set property with gradle property"() {
        given: "existing UPM-ready folder"
        writeTestPackage(existingUPMPackage, existingUPMPackage)
        and: "configured build.gradle file"
        buildFile << """
        publishing {
            repositories {
                upm {
                    url = "https://artifactoryhost/artifactory/repository"
                    name = ${property == "repository" ? wrapValueBasedOnType(rawValue, String) : wrapValueBasedOnType("any", String)}
                }
            }
        }
        """
        if (property != "repository") {
            buildFile << "upm {repository = ${wrapValueBasedOnType("any", String)}}\n"
        }
        when:
        def propertyQuery = runPropertyQuery(get, set).withSerializer(Directory) {
            String dir -> new File(projectDir, dir).absolutePath
        }

        then:
        propertyQuery.matches(rawValue)

        where:
        property            | gradlePropName           | type      | rawValue
        "repository"        | "upm.publish.repository" | String    | "repoName"
        "repository"        | "publish.repository"     | String    | "repoName"
        "version"           | "upm.publish.version"    | String    | "0.0.1"
        "version"           | "publish.version"        | String    | "0.0.1"
        "username"          | "upm.publish.username"   | String    | "username"
        "username"          | "publish.username"       | String    | "username"
        "password"          | "upm.publish.password"   | String    | "passw0rd"
        "password"          | "publish.password"       | String    | "passw0rd"
        "packageDirectory"  | "upm.package.directory"  | Directory | "wdk-name"
        "generateMetaFiles" | "upm.generate.metafiles" | Boolean   | "true"

        set = new PropertySetterWriter("upm", property)
                .set(rawValue, type)
                .withPropertyKey(gradlePropName)
                .to(PropertyLocation.propertyCommandLine)
        get = new PropertyGetterTaskWriter(set)
    }

    @Unroll
    def "sets credentials configured in the publishing extension to the UPM extension"() {
        given: "existing UPM-ready folder"
        writeTestPackage(existingUPMPackage, existingUPMPackage)
        and: "configured build.gradle file"
        buildFile << """
        publishing {
            repositories {
                upm {
                    url = "https://artifactoryhost/artifactory/repository"
                    name = "any"
                    credentials {
                        ${property} = ${wrapValueBasedOnType(rawValue, type)}
                    }
                }
            }
        }
        upm {repository = "any"}
        """
        when:
        def propertyQuery = runPropertyQuery(get)

        then:
        propertyQuery.matches(rawValue)

        where:
        property   | type   | rawValue
        "username" | String | "username"
        "password" | String | "password"

        get = new PropertyGetterTaskWriter("upm.${property}")
    }

}
