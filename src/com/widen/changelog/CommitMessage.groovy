package com.widen.changelog

import groovy.transform.ToString

@ToString
/**
 * Represents one parsed git commit message.
 */
class CommitMessage {
    String author
    String body
    String hash
    Set<String> jiraCases = []
    String module
    String subject
    String type
}
