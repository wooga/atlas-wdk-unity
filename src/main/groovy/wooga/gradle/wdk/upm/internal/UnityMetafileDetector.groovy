package wooga.gradle.wdk.upm.internal

import java.nio.file.Files
import java.nio.file.Path

class UnityMetafileDetector {


    static Map<Path, Path> findMetafiles(File packageDir) {
        def files = new HashMap<Path, Path>()
        def metafiles = new HashMap<Path, Path>()
        def folderFiles = Files.walk(packageDir.toPath())
        def dirContentsStream = folderFiles.parallel().filter {it != packageDir.toPath() }.sorted()
        dirContentsStream.each {
            it.toString().endsWith(".meta")? metafiles.put(it, it) : files.put(it, null)
        }

        return files.collectEntries(files) {
            def candidateMetafile = it.key.parent.resolve("${it.key.fileName.toString()}.meta")
            return [(it.key): (candidateMetafile.toFile().file? metafiles[candidateMetafile] : null)]
        }
    }

    static List<Path> filesWithoutMetafile(File packageDir) {
        def fileMetafile = findMetafiles(packageDir)
        return fileMetafile.findAll {it.value == null}.collect {it.key}
    }

}
