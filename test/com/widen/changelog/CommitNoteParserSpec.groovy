package com.widen.changelog

import spock.lang.Specification

class CommitNoteParserSpec extends Specification {
    def "cloneRepo properly determines repo name"() {
        when:
        CommitNoteParser commitNoteParser = Spy(CommitNoteParser) {
            executeCmd(_, _, _) >> []
        }
        System.setProperty("java.io.tmpdir", tempDir);

        then:
        commitNoteParser.cloneRepo(gitUrl) == clonedRepoPath

        where:
        gitUrl                                              | tempDir | clonedRepoPath
        "git@github.com:FineUploader/fine-uploader.git"     | "temp/" | "temp/fine-uploader"
        "git@github.com:FineUploader/fine-uploader.git"     | "temp"  | "temp/fine-uploader"
        "https://github.com/FineUploader/fine-uploader.git" | "temp/" | "temp/fine-uploader"
        "https://github.com/FineUploader/fine-uploader.git" | "temp"  | "temp/fine-uploader"
    }

    def "cloneRepo throws exception if passed invalid repo URL"() {
        when:
        CommitNoteParser commitNoteParser = new CommitNoteParser()
        commitNoteParser.cloneRepo("http://widen.com")

        then:
        thrown MalformedURLException
    }

    def "parseCommits"() {
        setup:
        List<String> rawCommits = [
                """HASH:da39a3ee5e6b4b0d3255bfef95601890afd80709
AUTHOR:John Doe
SUBJECT:chore(api): Re-remove unreferenced "errored" field in details API return
BODY:
""",
                """HASH:a94a8fe5ccb19ba61c4c0873d391e987982fbbd3
AUTHOR:Jane Doe
SUBJECT:fix(service-upload): Fix repeated uploads. Fix bug where sending an asset to service from the external page repeats indefinitely. PROJ-2414
BODY:
""",
                """HASH:b444ac06613fc8d63795be9ad0beaf55011936ac
AUTHOR:Ray Nicholus
SUBJECT:fix(hover-menu): hover over category menu item is a no-op
BODY:Due to an attempt to access the scope of an ng-repeat item in production mode, where this is not allowed due to disabling of angular debug info.

PROJ-2413

""",
                """HASH:109f4b3c50d7b0df729d299bc6f8e9ef9066971f
AUTHOR:Foo Bar
SUBJECT:feat(login): setup saml sso
BODY:PROJ-2417

""",
                """HASH:3ebfa301dc59196f18593c45e519287a23297589
AUTHOR:Mary Smith
SUBJECT:refactor(processing-previews): check up the converted file tree to see if any are in error to determine if preview is processing image is used instead of preview not available converted files are created off of parent files. If any of the parent files are in error the current converted file will still be considered processing. This fixes the inaccurate preview state so that if a parent is in error than the child is no longer processing the preview. PROJ-2352
BODY:
""",
                """HASH:1ff2b3704aede04eecb51e50ca698efd50a1379b
AUTHOR:Jerry Garcia
SUBJECT:fix(hover-menu): hover over category menu item is a no-op
BODY:Due to an attempt to PROJ-123 access the scope of an ng-repeat item in production mode, where this is not allowed due to disabling of angular debug info.

PROJ-2413
PROJ-3142

"""
        ]

        when:
        CommitNoteParser commitNoteParser = new CommitNoteParser()
        List<CommitMessage> parsedCommits = commitNoteParser.parseCommits(rawCommits)

        then:
        parsedCommits.size() == 5

        parsedCommits == [
                new CommitMessage(
                        author: "Jane Doe",
                        body: null,
                        hash: "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3",
                        jiraCases: ["PROJ-2414"],
                        module: "service-upload",
                        subject: "Fix repeated uploads. Fix bug where sending an asset to service from the external page repeats indefinitely.",
                        type: "fix"
                ),

                new CommitMessage(
                        author: "Ray Nicholus",
                        body: "Due to an attempt to access the scope of an ng-repeat item in production mode, where this is not allowed due to disabling of angular debug info.",
                        hash: "b444ac06613fc8d63795be9ad0beaf55011936ac",
                        jiraCases: ["PROJ-2413"],
                        module: "hover-menu",
                        subject: "hover over category menu item is a no-op",
                        type: "fix"
                ),

                new CommitMessage(
                        author: "Foo Bar",
                        body: null,
                        hash: "109f4b3c50d7b0df729d299bc6f8e9ef9066971f",
                        jiraCases: ["PROJ-2417"],
                        module: "login",
                        subject: "setup saml sso",
                        type: "feat"
                ),

                new CommitMessage(
                        author: "Mary Smith",
                        body: null,
                        hash: "3ebfa301dc59196f18593c45e519287a23297589",
                        jiraCases: ["PROJ-2352"],
                        module: "processing-previews",
                        subject: "check up the converted file tree to see if any are in error to determine if preview is processing image is used instead of preview not available converted files are created off of parent files. If any of the parent files are in error the current converted file will still be considered processing. This fixes the inaccurate preview state so that if a parent is in error than the child is no longer processing the preview.",
                        type: "refactor"
                ),

                new CommitMessage(
                        author: "Jerry Garcia",
                        body: "Due to an attempt to PROJ-123 access the scope of an ng-repeat item in production mode, where this is not allowed due to disabling of angular debug info.",
                        hash: "1ff2b3704aede04eecb51e50ca698efd50a1379b",
                        jiraCases: ["PROJ-3142", "PROJ-2413"],
                        module: "hover-menu",
                        subject: "hover over category menu item is a no-op",
                        type: "fix"
                )
        ]
    }
}
