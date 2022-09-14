package wooga.gradle.wdk.publish.tools

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit
import wooga.gradle.wdk.tools.GradleTestUtils

class BaseGradleSpec extends ProjectSpec {

    Grgit getGrgit() {
        return project.extensions.findByType(Grgit)
    }

    GradleTestUtils getUtils() {
        return new GradleTestUtils(project)
    }

    GradleTestFixtures getFixtures() {
        return new GradleTestFixtures(project)
    }

}
