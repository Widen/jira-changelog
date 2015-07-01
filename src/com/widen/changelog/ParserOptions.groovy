package com.widen.changelog

import groovy.transform.ToString
import groovy.transform.TupleConstructor

/**
 * Represents a set of options used by the parser to find commit messages and query JIRA.
 */
@ToString
@TupleConstructor
class ParserOptions {
    String firstTag
    String lastTag
    String repoUrl
    String jiraUrl
    String jiraUser
    String jiraPass
    File repoLocation
}
