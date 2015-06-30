package com.widen.changelog

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Represents one parsed git commit message.
 */
@ToString
@EqualsAndHashCode
class CommitMessage {
    String author
    String body
    String hash
    Set<String> jiraCases = []
    String module
    String subject
    String type
}
