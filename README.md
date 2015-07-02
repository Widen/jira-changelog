# Generate a changelog using git commits and JIRA issues

JIRA-Changelog will parse all git commits between two tags, lookup the associated JIRA issues, and generate a 
changelog in markdown or JSON format. It provides both a command-line interface and public methods, so you may generate
your changelogs from a shell script _or_ programmatically inside of a larger application.


## Features

- Ties all commit messages with their associated JIRA case
- Can identify type and code area (module) associated with each commit message
- Handles multiple JIRA cases associated with a commit message
- Identifies JIRA parent cases (as opposed to sub-tasks)
- Outputs results as markdown or JSON
- Usable from the command-line or programmatically though an API
- Verified with a [suite of automated tests](https://github.com/Widen/jira-changelog/tree/master/test)


## Requirements

- git repo (does not need to be public)
- JIRA installation (cloud or self-hosted)
- JIRA API access enabled
- git must be installed on the machine running JIRA-Changelog
- Java 7 or newer


## Getting started

1. Clone the repo or pull down the jar from maven central.
2. Follow the command-line or API instructions below, depending on your intended use.


## Command-line

The simplest way to generate a changelog from the command-line is to pull down the repository and then use gradle.

After you have cloned the repository, you can do simply accomplish this in one line:

`gradlew run -PappArgs='["-f", "{{FIRST_TAG}}", "-l", "{{LAST_TAG}}", "-u", "{{GIT_REPO_URL}}", "-j", "{{JIRA_URL}}", "-ju", "{{JIRA_API_USER}}", "-jp", "{{JIRA_API_PASSWORD}}", "-ot", "{{MARKDOWN_OR_JSON}}"]'`

The above command will clone the git repo specified by the GIT_REPO_URL, and then delete the clone once the changelog
has been generated. If you do not want JIRA-Changelog to do this, you can specify a directory that already contains
the cloned git repo to examine by using the `-d` option. In that case, assuming your git repo has been cloned to
"/code/repo", the command to generate a changelog will look slightly different:

`gradlew run -PappArgs='["-f", "{{FIRST_TAG}}", "-l", "{{LAST_TAG}}", "-d", "/code/repo", "-j", "{{JIRA_URL}}", "-ju", "{{JIRA_API_USER}}", "-jp", "{{JIRA_API_PASSWORD}}", "-ot", "{{MARKDOWN_OR_JSON}}"]'`


### Options

- `f`: First (most recent) tag in changelog range
- `l`: Last (least recent) tag in changelog range
- `u`: Git repository URL
- `d`: Git repository directory - overrides URL option
- `j`: JIRA instance URL
- `ju`: JIRA username
- `jp`: JIRA password
- `ot`: Output type for results, Valid values are 'markdown' or 'json'

All options are required. If an output type of "markdown" is specified, results will be saved to a new "changelog.md" file. 
If "json" is desired, a "changelog.json" file will be created instead.


## API

Please see the JavaDoc in the source for more details, but use of the API is quite simple. Here's an example case that
generates a changlog in both markdown and JSON format:

```java
import com.widen.changelog.*;

import java.util.List;

public class MyChangelogGenerator {
    public static void main(String[] args) {
        CommitNoteParser parser = new CommitNoteParser();
        ParserOptions options = new ParserOptions()
            .setFirstTag("{{FIRST_TAG}}")
            .setLastTag("{{LAST_TAG}}")
            .setRepoUrl("{{GIT_REPO_URL}}"
            .setJiraUrl("{{JIRA_URL}}")
            .setJiraUser("{{JIRA_API_USER}}")
            .setJiraPass("{{JIRA_API_PASSWORD}}")
        List<ParentJiraIssue> issues = parser.getJiraIssuesAndCommits(options);

        MarkdownOutput markdown = new MarkdownOutput(issues, options);
        System.out.println(markdown.toString());

        JsonOutput json = new JsonOutput(issues);
        System.out.println(json.toString());
    }
}
```

The above code will clone the git repo specified by the GIT_REPO_URL, and then delete the clone once the changelog
has been generated. If you do not want JIRA-Changelog to do this, you can specify a directory that already contains
the cloned git repo to examine by instead calling `setGitDir(File)`, passing the filesystem location of the cloned git repo.



## Tests

To run the tests, simple execute `gradlew test`. 


## License

JIRA-Changelog is MIT licensed, and is provided free of charge by [Widen](http://widen.com).  
![Widen](https://ssl.smartimagecdn.com/img/ve5m5p/200px/Widen-Avatar.jpeg)