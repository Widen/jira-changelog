package com.widen.changelog

import groovy.json.JsonBuilder

class JSONOutput {
    List<ParentJiraIssue> parentIssues = []

    @Override
    public String toString() {
        def json = new JsonBuilder()

        json {
            parentIssues parentIssues
        }

        return json.toString()
    }
}
