package wooga.gradle.wdk.internal

import com.wooga.spock.extensions.github.Repository

class GithubSnippets implements GithubSnippetsTrait {}

trait GithubSnippetsTrait {
    static String configureGithubPlugin(Repository testRepo) {
        return BasicSnippets.extension("github") {
            it.repositoryName = testRepo.fullName
            it.username = testRepo.userName
            it.password = testRepo.token
        }
    }
}