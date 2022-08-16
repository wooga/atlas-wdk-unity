package wooga.gradle.wdk.publish.internal

import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Repository
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class Git extends Grgit {

    static Git withGrgit(Grgit grgit) {
        return new Git(grgit.repository)
    }

    Git(Repository repository) {
        super(repository)
    }

    Provider<String> currentBranchName(Project project) {
        return project.provider { branch.current().name }
    }

    Provider<Boolean> areSameCommit(Provider<String> aCommitish, Provider<String> otherCommitish) {
        def firstSHA = aCommitish.map { commitish -> resolve.toCommit(commitish)?.id }
        def secondSHA = otherCommitish.map { commitish -> resolve.toCommit(commitish)?.id }

        if (secondSHA.present && firstSHA.present) {
            return secondSHA.zip(firstSHA) { first, second -> first == second }
        } else {
            return firstSHA.map { null as Boolean }
        }
    }

    Provider<Boolean> areDifferentCommits(Provider<String> aCommitish, Provider<String> otherCommitish) {
        return areSameCommit(aCommitish, otherCommitish).map{!it as Boolean}
    }
}
