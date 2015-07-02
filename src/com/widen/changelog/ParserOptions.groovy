package com.widen.changelog

import groovy.transform.ToString
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

/**
 * Represents a set of options used by the parser to find commit messages and query JIRA.
 */
@ToString
@Builder(builderStrategy=SimpleStrategy)
class ParserOptions {
    String firstTag
    String lastTag
    String repoUrl
    String jiraUrl
    String jiraUser
    String jiraPass
    File repoDir
}
