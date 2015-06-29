package com.widen.changelog

import groovy.transform.ToString

@ToString
class CommitMessage {
    String author
    String body
    String hash
    Set<String> jiraCases = []
    String module
    String subject
    String type
}
