package wooga.gradle.wdk.publish.tools

import org.gradle.api.Project

class GradleTestFixtures {

    final Project project

    GradleTestFixtures(Project project) {
        this.project = project
    }

    File getFakeReleaseNotes() {
        return new File(project.projectDir, "mockReleaseNotes.md").with {
            it.text = "mock release notes"
            return it
        }
    }

}
