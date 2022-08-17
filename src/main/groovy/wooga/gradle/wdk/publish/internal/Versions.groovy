package wooga.gradle.wdk.publish.internal

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import wooga.gradle.version.internal.release.base.ReleaseVersion
import wooga.gradle.version.strategies.SemverV2Strategies

class Versions {

    static Provider<ReleaseVersion> semver2Version(Project versionPluginProject, Grgit git) {
        def project = versionPluginProject
        def defaultStrategy = Optional.of(SemverV2Strategies.DEVELOPMENT).map {
            defaultStrat -> defaultStrat.defaultSelector(project, git)? defaultStrat : null
        }
        return project.provider {
            def availableStrategies = [ SemverV2Strategies.DEVELOPMENT,
                                        SemverV2Strategies.SNAPSHOT,
                                        SemverV2Strategies.PRE_RELEASE,
                                        SemverV2Strategies.FINAL]
            def nonDefaultStrat = availableStrategies.stream()
                    .filter {it.selector(project, git)}
                    .findFirst()
            def strategy = nonDefaultStrat.orElse(defaultStrategy.orElse(null))
            return Optional.ofNullable(strategy)
                    .map{ semver2Strategy ->
                        semver2Strategy.infer(project, git)
                    }
                    .orElse(null)
        }
    }
}
