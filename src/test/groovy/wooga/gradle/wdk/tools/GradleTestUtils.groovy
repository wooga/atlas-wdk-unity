package wooga.gradle.wdk.tools

import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder

class GradleTestUtils {

    Project project

    GradleTestUtils(Project project) {
        this.project = project
    }

    public <T> T requireExtension(Class<T> tClass) {
        return project.extensions.getByType(tClass)
    }
}
