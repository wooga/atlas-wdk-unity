#!groovy
@Library('github.com/wooga/atlas-jenkins-pipeline@1.x') _

withCredentials([string(credentialsId: 'atlas_wdk_unity_coveralls_token', variable: 'coveralls_token'),
                 string(credentialsId: 'atlas_plugins_sonar_token', variable: 'sonar_token')]) {
    buildGradlePlugin platforms: ['macos', 'windows', 'linux'],
            coverallsToken: coveralls_token, sonarToken: sonar_token
}
