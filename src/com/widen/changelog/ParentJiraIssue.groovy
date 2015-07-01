package com.widen.changelog

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import net.rcarz.jiraclient.Issue

/**
 * Represents one JIRA issue and one or more associated git commit messages.
 */
@ToString
@EqualsAndHashCode
class ParentJiraIssue implements Comparable<ParentJiraIssue> {
    List<CommitMessage> commitMessages = []
    Issue issue

    @Override
    public int compareTo(ParentJiraIssue other) {
        return issue.key.compareToIgnoreCase(other.issue.key)
    }
}
