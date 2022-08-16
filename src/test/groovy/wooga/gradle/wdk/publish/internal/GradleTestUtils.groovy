package wooga.gradle.wdk.publish.internal

import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject

class GradleTestUtils {

    Project project

    GradleTestUtils(Project project) {
        this.project = project
    }

    public <T, U> Tuple2<T, U> requireExtensions(Class<T> tClass, Class<U> uClass) {
        return [project.extensions.getByType(tClass), project.extensions.getByType(uClass)]
    }

    public <T> T requireExtension(Class<T> tClass) {
        return project.extensions.getByType(tClass)
    }

    public void evaluate(Project project) {
        ((DefaultProject)project).evaluate()
    }
}
