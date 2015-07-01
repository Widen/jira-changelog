package com.widen.changelog

class MarkdownOutput {
    List<ParentJiraIssue> parentIssues = []
    String jiraUrl
    String startingTag, endingTag

    @Override
    public String toString() {
        StringBuilder markdown = new StringBuilder()

        markdown.append("## $startingTag - $endingTag")
        markdown.append("   \n")

        parentIssues.sort()

        parentIssues.each { parentIssue ->
            markdown.append("![$parentIssue.issue.issueType.name]($parentIssue.issue.issueType.iconUrl)")
            markdown.append(" [$parentIssue.issue.key]($jiraUrl/browse/$parentIssue.issue.key)")
            markdown.append(" - $parentIssue.issue.summary")
            markdown.append("   \n")
        }

        return markdown.toString()
    }
}
