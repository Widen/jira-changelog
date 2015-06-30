package com.widen.changelog

class MarkdownOutput {
    List<ParentJiraIssue> parentIssues = []
    String jiraUrl

    @Override
    public String toString() {
        StringBuilder markdown = new StringBuilder()

        parentIssues.each { parentIssue ->
            markdown.append("[$parentIssue.issue.key]($jiraUrl/browse/$parentIssue.issue.key) - $parentIssue.issue.summary")
            markdown.append("\n")
        }

        return markdown.toString()
    }
}
