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
}
