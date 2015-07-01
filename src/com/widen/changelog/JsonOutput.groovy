package com.widen.changelog

import groovy.json.JsonBuilder
import groovy.transform.TupleConstructor

@TupleConstructor
class JsonOutput {
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
