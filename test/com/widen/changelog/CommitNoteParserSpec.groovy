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
        commitNoteParser.cloneRepo(gitUrl) == tempDir + repoName

        where:
        gitUrl                                              | repoName        | tempDir
        "git@github.com:FineUploader/fine-uploader.git"     | "fine-uploader" | "temp/"
        "https://github.com/FineUploader/fine-uploader.git" | "fine-uploader" | "temp/"
    }
}
