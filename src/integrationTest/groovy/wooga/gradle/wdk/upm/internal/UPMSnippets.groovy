package wooga.gradle.wdk.upm.internal

import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType
import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType
import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType
import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType
import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType

class UPMSnippets {
}

trait UPMSnippetsTrait {

    static final String DEFAULT_PACKAGE_NAME = UPMTestTools.DEFAULT_PACKAGE_NAME
    static final String DEFAULT_REPOSITORY = "integration"

    static String minimalUPMConfiguration(File baseDir, boolean publishing) {
        return minimalUPMConfiguration(baseDir, DEFAULT_PACKAGE_NAME, DEFAULT_REPOSITORY, publishing)
    }

    static String minimalUPMConfiguration(File baseDir = null, String packageName = DEFAULT_PACKAGE_NAME, String repoName = DEFAULT_REPOSITORY, boolean publishing = false) {
        def upmTestTools = new UPMTestTools()
        if(baseDir != null) upmTestTools.writeTestPackage(baseDir, "Assets/$packageName", packageName)
        def (username, password) = publishing? UPMTestTools.credentialsFromEnv() : ["fakecred1", "fakecred2"]
        return """
        publishing {
            repositories {
                upm {
                    url = ${wrapValueBasedOnType(UPMTestTools.artifactoryURL(UPMTestTools.WOOGA_ARTIFACTORY_CI_REPO), String)}
                    name = ${wrapValueBasedOnType(repoName, String)}
                    credentials {
                        username = ${wrapValueBasedOnType(username, String)}
                        password = ${wrapValueBasedOnType(password, String)}
                    }
                }
            }
        }
        upm {
            repository = ${wrapValueBasedOnType(repoName, String)}
        }
        """
    }
}
