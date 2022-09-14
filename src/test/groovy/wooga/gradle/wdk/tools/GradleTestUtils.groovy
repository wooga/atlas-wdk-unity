package wooga.gradle.wdk.tools

import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder

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

    public <T> T evaluate(@DelegatesTo(Project) Closure<T> cls) {
        def result = cls(project)
        evaluate(project)
        return result
    }

    public void evaluate(Project project) {
        ((DefaultProject)project).evaluate()
    }

    public Project createSubproject(String name = "subproject") {
        def projDir = new File(project.projectDir, name)
        projDir.mkdirs()
        return ProjectBuilder.builder()
                            .withName(name)
                            .withProjectDir(new File(project.projectDir, name))
                            .withParent(project).build()
    }

}
