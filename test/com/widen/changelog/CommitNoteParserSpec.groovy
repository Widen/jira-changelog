package com.widen.changelog

import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.IssueType
import net.rcarz.jiraclient.JiraClient
import spock.lang.Specification

class CommitNoteParserSpec extends Specification {
    def "cloneRepo properly determines repo name"() {
        when:
        CommitNoteParser commitNoteParser = Spy(CommitNoteParser) {
            executeCmd(_, _, _) >> []
        }
        System.setProperty("java.io.tmpdir", tempDir);

        then:
        commitNoteParser.cloneRepo(gitUrl).path == clonedRepoPath

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

""",
                """HASH:b7bff7f8e34c29c68478gd8d9187239532b31ea1
AUTHOR:Bob Weir
SUBJECT:fix(dng): PROJ-3002 always recalc aspect ratio from 160px preview after it is generated to fix dng crop issues, among many other aspect ratio problems
BODY:

"""
        ]

        when:
        CommitNoteParser commitNoteParser = new CommitNoteParser()
        List<CommitMessage> parsedCommits = commitNoteParser.parseCommits(rawCommits)

        then:
        parsedCommits.size() == 6

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
                ),

                new CommitMessage(
                        author: "Bob Weir",
                        body: null,
                        hash: "b7bff7f8e34c29c68478gd8d9187239532b31ea1",
                        jiraCases: ["PROJ-3002"],
                        module: "dng",
                        subject: "always recalc aspect ratio from 160px preview after it is generated to fix dng crop issues, among many other aspect ratio problems",
                        type: "fix"
                )
        ]
    }

    def "getJiraCases"() {
        setup:
        def epic = Mock(Issue) {
            getKey() >> "PROJ-0"
            getIssueType() >> Mock(IssueType) {
                getName() >> "Epic"
            }
        }

        def proj1Issue = Mock(Issue) {
            getKey() >> "PROJ-1"
            getIssueType() >> Mock(IssueType) {
                getName() >> "Story"
            }
        }
        def proj2Issue = Mock(Issue) {
            getKey() >> "PROJ-2"
            getIssueType() >> Mock(IssueType) {
                getName() >> "Story"
            }
        }
        def proj3Issue = Mock(Issue) {
            getKey() >> "PROJ-3"
            getIssueType() >> Mock(IssueType) {
                getName() >> "Bug"
            }
        }
        def proj4Issue = Mock(Issue) {
            getKey() >> "PROJ-4"
            getIssueType() >> Mock(IssueType) {
                getName() >> "Bug"
            }
        }

        def proj123Issue = Mock(Issue) {
            getKey() >> "PROJ-123"
            getParent() >> proj1Issue
            getIssueType() >> Mock(IssueType) {
                getName() >> "Story Bug"
            }
        }
        def proj124Issue = Mock(Issue) {
            getKey() >> "PROJ-124"
            getParent() >> proj2Issue
            getIssueType() >> Mock(IssueType) {
                getName() >> "Story"
            }
        }
        def proj125Issue = Mock(Issue) {
            getKey() >> "PROJ-125"
            getIssueType() >> Mock(IssueType) {
                getName() >> "Story"
            }
        }
        def proj126Issue = Mock(Issue) {
            getKey() >> "PROJ-126"
            getIssueType() >> Mock(IssueType) {
                getName() >> "Bug"
            }
        }
        def proj127Issue = Mock(Issue) {
            getKey() >> "PROJ-127"
            getParent() >> proj2Issue
            getIssueType() >> Mock(IssueType) {
                getName() >> "Story"
            }
        }
        def proj128Issue = Mock(Issue) {
            getKey() >> "PROJ-128"
            getParent() >> proj3Issue
            getIssueType() >> Mock(IssueType) {
                getName() >> "Story"
            }
        }
        def proj129Issue = Mock(Issue) {
            getKey() >> "PROJ-129"
            getParent() >> proj4Issue
            getIssueType() >> Mock(IssueType) {
                getName() >> "Story"
            }
        }

        def parsedCommits = [
                new CommitMessage(subject: "1", jiraCases: ["PROJ-123"]),
                new CommitMessage(subject: "2", jiraCases: ["PROJ-124"]),
                new CommitMessage(subject: "3", jiraCases: ["PROJ-123"]),
                new CommitMessage(subject: "4", jiraCases: ["PROJ-125", "PROJ-124"]),
                new CommitMessage(subject: "5", jiraCases: ["PROJ-126"]),
                new CommitMessage(subject: "6", jiraCases: ["PROJ-127"]),
                new CommitMessage(subject: "7", jiraCases: ["PROJ-128"]),
                new CommitMessage(subject: "8", jiraCases: ["PROJ-129"]),
        ]

        def jiraClient = Mock(JiraClient)
        def commitNoteParser = new CommitNoteParser()
        GroovyMock(JiraClient, global: true, useObjenesis: true)

        when:
        1 * new JiraClient("http://some-jira.endpoint.com", _) >> jiraClient

        1 * jiraClient.getIssue("PROJ-123") >> proj123Issue
        1 * jiraClient.getIssue("PROJ-124") >> proj124Issue
        1 * jiraClient.getIssue("PROJ-125") >> proj125Issue
        1 * jiraClient.getIssue("PROJ-126") >> proj126Issue
        1 * jiraClient.getIssue("PROJ-127") >> proj127Issue
        1 * jiraClient.getIssue("PROJ-128") >> proj128Issue
        1 * jiraClient.getIssue("PROJ-129") >> proj129Issue

        def cases = commitNoteParser.getJiraCases(parsedCommits, "http://some-jira.endpoint.com", "user", "pass")

        then:
        cases == [
                new ParentJiraIssue(issue: proj1Issue, commitMessages: [parsedCommits[0], parsedCommits[2]]),
                new ParentJiraIssue(issue: proj2Issue, commitMessages: [parsedCommits[1], parsedCommits[3], parsedCommits[5]]),
                new ParentJiraIssue(issue: proj125Issue, commitMessages: [parsedCommits[3]]),
                new ParentJiraIssue(issue: proj126Issue, commitMessages: [parsedCommits[4]]),
                new ParentJiraIssue(issue: proj3Issue, commitMessages: [parsedCommits[6]]),
                new ParentJiraIssue(issue: proj4Issue, commitMessages: [parsedCommits[7]]),
        ]
    }
}
