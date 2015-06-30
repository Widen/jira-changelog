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
            j longOpt: 'jiraUrl', args: 1, 'JIRA URL'
            ju longOpt: 'jiraUser', args: 1, 'JIRA username'
            jp longOpt: 'jiraPass', args: 1, 'JIRA password'
            ot longOpt: 'outputType', args: 1, "Output results as 'markdown' or 'json'"
        }

        def options = cli.parse(args)

        // TODO assume lack of f/t options indicates: -f LATEST_TAG -t SECOND_LATEST_TAG

        if (!options) {
            return
        }

        if (options.h) {
            cli.usage()
            return
        }

        def issues = app.getJiraIssuesAndCommits(new ParserOptions(
                repoUrl: options.u,
                firstTag: options.f,
                lastTag: options.l,
                jiraUrl: options.j,
                jiraUser: options.ju,
                jiraPass: options.jp
        ))

        String output
        if (options.ot == "json") {
            output = new JSONOutput(parentIssues: issues)
        }
        else if (options.ot == "markdown") {
            output = new MarkdownOutput(parentIssues: issues, jiraUrl: options.ju)
        }

        if (output) {
            new File("changelog.$options.ot") << output
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
        String tempRepoLocation
        try {
            tempRepoLocation = cloneRepo(options.repoUrl)

            LOGGER.info("gathering commit messages at repo cloned to $tempRepoLocation...")
            List<String> totalCommits = getRawLogs(options.firstTag, options.lastTag, tempRepoLocation)
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
            if (tempRepoLocation) {
                new File(tempRepoLocation).deleteDir()
            }
        }
    }

    String cloneRepo(String url) {
        String tempDir = System.getProperty("java.io.tmpdir");
        def repoMatcher = url =~ /^(?:git@|https:\/\/).*\/(.+)\.git$/

        if (!repoMatcher) {
            throw new MalformedURLException("Unable to parse repo name from url!")
        }

        String repoName = repoMatcher[0][1]
        LOGGER.info("cloning $repoName...")
        executeCmd("git clone $url", tempDir)
        LOGGER.info("...cloned $repoName")

        return new File(tempDir, repoName).path
    }

    List<String> getRawLogs(firstTag, lastTag, tempRepoLocation) {
        executeCmd(["bash", "-c", "git --no-pager log --no-merges --pretty=format:HASH:%H%nAUTHOR:%an%nSUBJECT:%s%nBODY:%b%n$COMMIT_DELIMITER $lastTag..$firstTag > jira-changelog.txt"], tempRepoLocation)
        return new File(tempRepoLocation + '/jira-changelog.txt').text.split(COMMIT_DELIMITER) as List
    }

    List<String> executeCmd(def cmd, String workingDir, String delim="\\n") {
        LOGGER.fine("Executing command: $cmd")
        def sout = new StringBuilder(), serr = new StringBuilder()

        def proc = cmd.execute(null, new File(workingDir))
        proc.consumeProcessOutput(sout, serr)
        proc.waitForProcessOutput(sout, serr)
        LOGGER.finer(sout.toString())
        return sout.toString().split(delim) as List
    }

    List<CommitMessage> parseCommits(List<String> rawCommits) {
        List<CommitMessage> parsedMessages = []

        rawCommits.each { String rawCommit ->
            def matcher = rawCommit =~ /(?s)HASH:(.+)\nAUTHOR:(.+)\nSUBJECT:(feat|fix|docs|style|refactor|perf|test|chore|customer)\((.+)\):\s*(.+)\nBODY:(.*)/
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

        def matcher = subject.replaceAll("\\n", "").trim() =~ /^(.*?)([A-Z0-9_]+-\d+)?$/

        if (matcher[0][2]) {
            ids = matcher[0][2].split(/\s+/) as Set
        }

        return [matcher[0][1].trim(), ids]
    }

    def parseCommitBody(String body)
    {
        Set<String> ids

        body = body.trim()

        if (body.length() == 0) {
            return [null, null]
        }

        def matcher = body =~ /(?s)^(.*?)((?:[A-Z0-9]+\-\d+\n?)+)?$/

        if (matcher[0][2]) {
            ids = matcher[0][2].split(/\s+|\n+/) as Set
        }

        return [matcher[0][1].trim(), ids]
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
