package wooga.gradle.wdk.upm

import com.wooga.gradle.PropertyLookup

class UPMPublishConventions {

    static final PropertyLookup repository = new PropertyLookup(
            ["UPM_PUBLISH_REPOSITORY"],
            ["upm.publish.repository", "publish.repository"],
            null)

    static final PropertyLookup version = new PropertyLookup(
            ["UPM_PUBLISH_VERSION"],
            ["upm.publish.version", "publish.version"],
            null)


    static final PropertyLookup username = new PropertyLookup(
            ["UPM_USR", "UPM_USERNAME"],
            ["upm.publish.username", "publish.username"],
            null
    )

    static final PropertyLookup password = new PropertyLookup(
            ["UPM_PWD", "UPM_PASSWORD"],
            ["upm.publish.password", "publish.password"],
            null
    )

    static final PropertyLookup packageDirectory = new PropertyLookup(
            ["UPM_PACKAGE_DIR", "UPM_PACKAGE_DIRECTORY"],
            ["upm.package.directory"],
            null
    )

    static final PropertyLookup generateMetaFiles = new PropertyLookup(
            ["UPM_GENERATE_METAFILES", "UPM_GENERATE_METAFILES"],
            ["upm.generate.metafiles"],
            true
    )
}
