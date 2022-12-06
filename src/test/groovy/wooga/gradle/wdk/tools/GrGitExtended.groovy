package wooga.gradle.wdk.tools

import com.wooga.spock.extensions.github.Repository
import org.ajoberstar.grgit.Credentials
import org.ajoberstar.grgit.Grgit

class GrGitExtended extends Grgit {

    static GrGitExtended initWithRemote(File dir, Repository testRepo, String[] ignore = ["*.gradle", "*.bat"], String... ignoreExtra) {
        if(!new File(dir, ".git").exists()) {
            init(dir: dir).close()
        }
        def git = open(dir: dir, credentials: new Credentials(testRepo.token))
        git.remote.add(name: "origin", url: testRepo.httpTransportUrl, pushUrl: testRepo.httpTransportUrl)
        git.fetch()
        git.checkout(branch: testRepo.defaultBranch.name, createBranch: true, startPoint: "origin/${testRepo.defaultBranch.name}")
        if(ignore.length > 0) {
            new File(dir, '.gitignore') << (ignore + ignoreExtra).join("\n")
            git.add(patterns: ['.gitignore'])
            git.commit(amend: true, message: "gitignore")
            git.push(force: true)
        }
        return new GrGitExtended(git)
    }

    static GrGitExtended initWithIgnoredFiles(File dir, String[] ignore = ["*.gradle", "*.bat"], String... ignoreExtra) {
        def git = init(dir: dir)
        git.commit(message: "initial commit")
        def allIgnore = ignore + ignoreExtra
        if(allIgnore.length > 0) {
            new File(dir, '.gitignore') << allIgnore.join("\n")
            git.add(patterns: ['.gitignore'])
            git.commit(amend: true, message: "gitignore")
        }
        return new GrGitExtended(git)
    }

    GrGitExtended(Grgit grgit) {
        super(grgit.repository)
    }

    File commitChange(File dir = repository.rootDir, String fileName="anyfile", String message = "any") {
        def changedFile = new File(dir, fileName). with {
            if(it.exists()) {
                it.createNewFile()
            }
            it.text = ["subproject/"].join("\n")
            return it
        }
        this.add(patterns: ['anyfile'])
        this.commit(message: message)
        return changedFile
    }
}
