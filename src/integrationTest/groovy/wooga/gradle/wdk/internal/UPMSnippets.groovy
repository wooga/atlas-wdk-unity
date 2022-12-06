package wooga.gradle.wdk.internal


import static com.wooga.gradle.test.PropertyUtils.wrapValueBasedOnType

class UPMSnippets implements UPMSnippetsTrait {
}

trait UPMSnippetsTrait {

    static final String WOOGA_ARTIFACTORY_CI_REPO = UPMTestTools.WOOGA_ARTIFACTORY_CI_REPO
    static final String DEFAULT_PACKAGE_NAME = UPMTestTools.DEFAULT_PACKAGE_NAME
    static final String DEFAULT_REPOSITORY = "integration"
    static final String UPM_PROJECT_NAME = "defaultProj"


    static String minimalUPMConfiguration(File baseDir = null, String packageName = DEFAULT_PACKAGE_NAME, String repoName = DEFAULT_REPOSITORY, boolean publishing = false) {
        def upmTestTools = new UPMTestTools()
        if (baseDir != null) upmTestTools.writeTestPackage(baseDir, "Assets/$packageName", packageName)
        return """
        ${singlePublishingUPMRepository(repoName, "fake1", "fake2", publishing)}
        upm {
            projects {
                $UPM_PROJECT_NAME {
                    packageDirectory = ${wrapValueBasedOnType("Assets/$packageName", File)}
                }
            }
            repository = ${wrapValueBasedOnType(repoName, String)}
        }
        """
    }
    static String singlePublishingUPMRepository(String repoName = DEFAULT_REPOSITORY, boolean publishing) {
        singlePublishingUPMRepository(repoName, null, null, publishing)
    }

    static String singlePublishingUPMRepository(String repoName = DEFAULT_REPOSITORY, String username = null, String password = null, boolean publishing = false) {
        if (publishing) {
            (username, password) = UPMTestTools.credentialsFromEnv()
        }
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
        """
    }
}
