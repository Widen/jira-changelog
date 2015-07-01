package com.widen.changelog

import groovy.transform.TupleConstructor

@TupleConstructor
class MarkdownOutput {
    List<ParentJiraIssue> parentIssues = []
    ParserOptions options

    @Override
    public String toString() {
        StringBuilder markdown = new StringBuilder()

        markdown.append("## $options.firstTag - $options.lastTag")
        markdown.append("   \n")

        parentIssues.sort()

        parentIssues.each { parentIssue ->
            markdown.append("![$parentIssue.issue.issueType.name]($parentIssue.issue.issueType.iconUrl)")
            markdown.append(" [$parentIssue.issue.key]($options.jiraUrl/browse/$parentIssue.issue.key)")
            markdown.append(" - $parentIssue.issue.summary")
            markdown.append("   \n")
        }

        return markdown.toString()
    }
}
