package com.widen.changelog
import net.rcarz.jiraclient.BasicCredentials
import net.rcarz.jiraclient.Issue
import net.rcarz.jiraclient.JiraClient
import net.rcarz.jiraclient.JiraException

import java.util.logging.Logger

class CommitNoteParser
{
    private static final Logger LOGGER = Logger.getLogger(CommitNoteParser.class.name)
    private static final String COMMIT_DELIMITER = "-----ENDOFCOMMIT-----";

    public static void main(String[] args) throws Exception {
        CommitNoteParser app = new CommitNoteParser()
        CliBuilder cli = new CliBuilder(usage: '-f [from-tag] -t [to-tag]')
        cli.with {
            h longOpt: 'help', 'Show usage info'
            f longOpt: 'firstTag', args: 1, 'First tag in changelog range'
            l longOpt: 'lastTag', args: 1, 'Last tag in changelog range'
            u longOpt: 'repoUrl', args: 1, 'Git repository URL'
            d longOpt: 'repoDir', args: 1, 'Git repository directory - overrides URL option'
            j longOpt: 'jiraUrl', args: 1, 'JIRA URL'
            ju longOpt: 'jiraUser', args: 1, 'JIRA username'
            jp longOpt: 'jiraPass', args: 1, 'JIRA password'
            ot longOpt: 'outputType', args: 1, "Output results as 'markdown' or 'json'"
        }

        def options = cli.parse(args)

        if (!options) {
            return
        }

        if (options.h) {
            cli.usage()
            return
        }

        def parserOptions = new ParserOptions(
                repoUrl: options.u,
                firstTag: options.f,
                lastTag: options.l,
                jiraUrl: options.j,
                jiraUser: options.ju,
                jiraPass: options.jp
        )
        if (options.d) {
            parserOptions.repoDir = new File(options.d)
        }

        def issues = app.getJiraIssuesAndCommits(parserOptions)

        if (!issues.empty) {
            String output, extension
            if (options.ot == "json") {
                LOGGER.info("Writing results to changelog.json file...")
                output = new JsonOutput(parentIssues: issues)
                extension = "json"
            }
            else if (options.ot == "markdown") {
                LOGGER.info("Writing results to changelog.md file...")
                output = new MarkdownOutput(parentIssues: issues, options: parserOptions)
                extension = "md"
            }

            def outputFile = new File("changelog.$extension")
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile << output
        }
    }

    /**
     * Parses all commit messages between the two tags specified in the options and generates a List of
     * top-level/parent JIRA cases along with all associated commit messages.
     *
     * @param options
     * @return List of all top-level issues (not epics) and their associated commits
     */
    public List<ParentJiraIssue> getJiraIssuesAndCommits(ParserOptions options) {
        File repoDir
        try {
            if (options.repoDir) {
                repoDir = options.repoDir
            }
            else {
                repoDir = cloneRepo(options.repoUrl)
            }

            LOGGER.info("gathering commit messages at repo cloned to $repoDir...")
            List<String> totalCommits = getRawLogs(options.firstTag, options.lastTag, repoDir)
            LOGGER.info("...gathered $totalCommits.size total commits")

            LOGGER.info("parsing $totalCommits.size commit messages...")
            List<CommitMessage> commitMessages = parseCommits(totalCommits)
            LOGGER.info("...found $commitMessages.size valid commit messages")

            LOGGER.info("looking up JIRA cases for $commitMessages.size valid commit messages...")
            List<ParentJiraIssue> jiraCases = getJiraCases(commitMessages, options.jiraUrl, options.jiraUser, options.jiraPass)
            LOGGER.info("...$jiraCases.size top-level JIRA cases located")

            return jiraCases
        }
        catch (Error er) {
            er.printStackTrace()
        }
        finally {
            // only delete the repo if _we_ created it
            if (repoDir && !options.repoDir) {
                repoDir.deleteDir()
            }
        }
    }

    File cloneRepo(String url) {
        String tempDir = System.getProperty("java.io.tmpdir");
        def repoMatcher = url =~ /^(?:git@|https:\/\/).*\/(.+)\.git$/

        if (!repoMatcher) {
            throw new MalformedURLException("Unable to parse repo name from url!")
        }

        String repoName = repoMatcher[0][1]

        File tempRepo = new File(tempDir, repoName)
        tempRepo.deleteDir()

        LOGGER.info("cloning $repoName...")
        executeCmd("git clone $url $repoName", new File(tempDir))
        LOGGER.info("...cloned $repoName")

        return tempRepo
    }

    List<String> getRawLogs(String firstTag, String lastTag, File tempRepoLocation) {
        return executeCmd("git --no-pager log --no-merges --pretty=format:HASH:%H%nAUTHOR:%an%nSUBJECT:%s%nBODY:%b%n$COMMIT_DELIMITER $lastTag..$firstTag", tempRepoLocation, COMMIT_DELIMITER)
    }

    List<String> executeCmd(String cmd, File workingDir, String delim="\\n") {
        LOGGER.info("Executing command: $cmd")

        def proc = cmd.execute(null, workingDir)
        def sout = new StringBuilder()
        def serr = new StringBuilder()

        proc.consumeProcessOutput(sout, serr)
        proc.waitForOrKill(10000)

        if (serr.length() > 0) {
            LOGGER.severe("Errors detected during commit parsing: $serr")
        }

        return sout.toString().split(delim)
    }

    List<CommitMessage> parseCommits(List<String> rawCommits) {
        List<CommitMessage> parsedMessages = []

        rawCommits.each { String rawCommit ->
            def matcher = rawCommit =~ /(?s)HASH:(.+)\nAUTHOR:(.+)\nSUBJECT:(.*)\((.+)\):\s*(.+)\nBODY:(.*)/
            if (matcher) {
                String subject, body
                Set<String> subjectIds, bodyIds
                Set<String> ids = []

                (subject, subjectIds) = parseCommitSubject(matcher[0][5])
                if (subjectIds) {
                    ids.addAll(subjectIds)
                }

                (body, bodyIds) = parseCommitBody(matcher[0][6])
                if (bodyIds) {
                    ids.addAll(bodyIds)
                }
                if (body != null && body.length() == 0) {
                    body = null
                }

                if (!ids.empty) {
                    parsedMessages << new CommitMessage(
                            hash: matcher[0][1],
                            author: matcher[0][2],
                            type: matcher[0][3],
                            module: matcher[0][4],
                            subject: subject,
                            body: body,
                            jiraCases: ids
                    )
                }
            }
        }

        return parsedMessages
    }

    def parseCommitSubject(String subject)
    {
        Set<String> ids
        String message

        def startingIssueKey = subject.replaceAll("\\n", "").trim() =~ /^([A-Z0-9_]+-\d+)(.*?)$/
        def maybeEndingIssueKey = subject.replaceAll("\\n", "").trim() =~ /^(.*?)([A-Z0-9_]+-\d+)?$/

        if (startingIssueKey.matches()) {
            ids = startingIssueKey[0][1].split(/\s+/) as Set
            message = startingIssueKey[0][2].trim()
        }
        else {
            if (maybeEndingIssueKey[0][2]) {
                ids = maybeEndingIssueKey[0][2].split(/\s+/) as Set
            }
            message = maybeEndingIssueKey[0][1].trim()
        }

        return [message, ids]
    }

    def parseCommitBody(String body)
    {
        Set<String> ids
        String parsedBody = ""

        body = body.trim()

        if (body.length() == 0) {
            return [null, null]
        }

        def matcher = body =~ /(?s)^(.*?)((?:[A-Z0-9]+\-\d+\n?)+)?(\s#\D+)?$/

        if (matcher[0][2]) {
            ids = matcher[0][2].split(/\s+|\n+/) as Set
        }

        if (matcher[0][1]) {
            parsedBody = matcher[0][1]
        }
        if (matcher[0][3]) {
            parsedBody += matcher[0][3]
        }

        return [parsedBody.trim().replaceAll("[\n\r]", ""), ids]
    }

    List<ParentJiraIssue> getJiraCases(List<CommitMessage> commitMessages, String url, String user, String pass) {
        BasicCredentials credentials = new BasicCredentials(user, pass)
        JiraClient jiraClient = new JiraClient(url, credentials)
        Map<String, ParentJiraIssue> parentJiraCaseMap = [:]
        Map<String, String> childParentRelations = [:]
        Set<String> retrievedCases = []

        commitMessages.each { CommitMessage commitMessage ->
            commitMessage.jiraCases.each { String jiraCaseId ->
                if (retrievedCases.contains(jiraCaseId)) {
                    String parentId = childParentRelations.get(jiraCaseId)
                    if (!parentId) {
                        parentId = jiraCaseId
                    }
                    parentJiraCaseMap.get(parentId).commitMessages.add(commitMessage)
                }
                else {
                    try {
                        Issue issue = jiraClient.getIssue(jiraCaseId)
                        while (issue.parent && issue.parent.issueType.name != "Epic") {
                            issue = issue.getParent()
                        }

                        ParentJiraIssue parentJiraIssue = parentJiraCaseMap.get(issue.getKey())
                        if (!parentJiraIssue) {
                            parentJiraIssue = new ParentJiraIssue(issue: issue)
                        }

                        parentJiraIssue.commitMessages.add(commitMessage)

                        parentJiraCaseMap.put(issue.getKey(), parentJiraIssue)
                        retrievedCases.addAll([jiraCaseId, issue.getKey()])
                        childParentRelations.put(jiraCaseId, issue.getKey())
                    }
                    catch (JiraException ex) {
                        LOGGER.warning("Unable to lookup case $jiraCaseId, commit $commitMessage")
                    }
                }
            }
        }

        return parentJiraCaseMap.values() as List
    }
}
