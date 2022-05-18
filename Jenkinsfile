#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@1.x') _

withCredentials([                    
                    string(credentialsId: 'atlas_plugins_sonar_token', variable: 'sonar_token'),
                    string(credentialsId: 'atlas_plugins_snyk_token', variable: 'SNYK_TOKEN'),
                    usernameColonPassword(credentialsId: 'atlas_upm_integration_user', variable: 'atlas_upm_integration_user')
                 ]) {
    def testEnvironment = [
                                "repositoryCredentials=${atlas_upm_integration_user}"
                          ]
    buildGradlePlugin platforms: ['macos', 'windows', 'linux'], sonarToken: sonar_token, testEnvironment : testEnvironment
}
