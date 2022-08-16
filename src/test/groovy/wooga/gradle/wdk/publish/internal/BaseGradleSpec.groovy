package wooga.gradle.wdk.publish.internal

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit

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
