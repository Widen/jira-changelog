package com.widen.changelog

import groovy.transform.ToString

/**
 * Represents a set of options used by the parser to find commit messages and query JIRA.
 */
@ToString
class ParserOptions {
    String firstTag
    String lastTag
    String repoUrl
    String jiraUrl
    String jiraUser
    String jiraPass
}
