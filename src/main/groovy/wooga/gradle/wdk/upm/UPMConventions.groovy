package wooga.gradle.wdk.upm

import com.wooga.gradle.PropertyLookup

class UPMConventions {

    static final PropertyLookup repository = new FixPropertyLookup(
            ["UPM_REPOSITORY"],
            ["upm.repository", "publish.repository"],
            null)

    static final PropertyLookup version = new FixPropertyLookup(
            ["UPM_PACKAGE_VERSION", "UPM_VERSION"],
            ["upm.version", "upm.package.version", "publish.version"],
            null)


    static final PropertyLookup username = new FixPropertyLookup(
            ["UPM_USR", "UPM_USERNAME"],
            ["upm.username", "publish.username"],
            null
    )

    static final PropertyLookup password = new FixPropertyLookup(
            ["UPM_PWD", "UPM_PASSWORD"],
            ["upm.password", "publish.password"],
            null
    )

    static final PropertyLookup packageDirectory = new FixPropertyLookup(
            ["UPM_PACKAGE_DIR", "UPM_PACKAGE_DIRECTORY"],
            ["upm.package.directory"],
            null
    )

    static final PropertyLookup generateMetaFiles = new FixPropertyLookup(
            ["UPM_GENERATE_METAFILES", "UPM_GENERATE_META_FILES"],
            ["upm.generate.metafiles"],
            false
    )
}
