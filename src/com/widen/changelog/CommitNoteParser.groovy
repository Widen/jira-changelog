package com.widen.changelog

import org.apache.commons.cli.*

class CommitNoteParser
{
	// TODO temp fetch of repo for git log
	static void main(String[] args) throws Exception {
		CommitNoteParser app = new CommitNoteParser()
		CliBuilder cli = new CliBuilder(usage: '-f [from-tag] -t [to-tag]')
		cli.with {
			h longOpt: 'help', 'Show usage info'
			f longOpt: 'firstTag', args: 1, 'First tag in changelog range'
			l longOpt: 'lastTag', args: 1, 'Last tag in changelog range'
            u longOpt: 'repoUrl', args: 1, 'Git repository URL'
		}

		def options = cli.parse(args)

		// TODO assume lack of f/t options indicates: -f LATEST_TAG -t SECOND_LATEST_TAG

		if (!options) {
			return
		}

        String tempRepoLocation
        try {
            tempRepoLocation = app.cloneRepo(options.u)
            List<String> logsByLine = app.getRawLogs(options.f, options.l, tempRepoLocation)
        }
        catch (Error er) {
            er.printStackTrace()
        }
        finally {
            if (tempRepoLocation) {
                new File(tempRepoLocation).deleteDir()
            }
        }
	}

    private String cloneRepo(String url) {
        String tempDir = System.getProperty("java.io.tmpdir");
        String repoName = (url =~ /\/(.+)\.git$/)[0][1]

        if (!repoName) {
            throw RuntimeException("Unable to parse repo name from url!")
        }

        println "[cloning git repo into $tempDir]"

        executeCmd("git clone $url", tempDir)

        println "[cloned $repoName]"

        return tempDir + repoName
    }

	private List<String> getRawLogs(firstTag, lastTag, tempRepoLocation) {
        println "[looking at repo cloned to $tempRepoLocation]"
        return executeCmd("git log --no-merges --pretty=format:%an%x09%s " + lastTag + ".." + firstTag, tempRepoLocation)
	}

	private List<String> executeCmd(String cmd, String workingDir) {
		println cmd
		def sout = new StringBuilder(), serr = new StringBuilder()

		def proc = cmd.execute(null, new File(workingDir))
		proc.consumeProcessOutput(sout, serr)
		proc.waitFor()
		println "out> $sout err> $serr"

		return sout.toString().split("\\n") as List
	}
}
