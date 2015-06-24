package com.widen.changelog

import collective.DeploymentContext
import collective.entities.asset.Asset
import collective.util.StandaloneHibernateApplication
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.SystemUtils
import org.apache.poi.hssf.usermodel.HSSFCell
import org.apache.poi.hssf.usermodel.HSSFRow
import org.apache.poi.hssf.usermodel.HSSFSheet
import org.apache.poi.hssf.usermodel.HSSFWorkbook

import java.sql.ResultSet
import java.sql.Statement

/**
 * EXAMPLE TAGGING COMMANDS
 * git tag -a 8.0.2+22 f4c4ace23bb0 -m '8.0.2 build 22'
 * git push origin 8.0.2+22
 *
 * LOG PREVIOUS COMMITS TO IGNORE
 * git log --pretty=format:%an%x09%s --no-merges 8.0.2..8.0.2+21  > prev-commits.txt
 *
 * LOG THIS BUILD'S COMMITS
 * git log --pretty=format:-----%n%an%x09%s%n%ad%x09%an%x09https://github.com/Widen/dam/commit/%h%x09%s%n%b --date=short --no-merges 8.0.2+21..8.0.2+22  > commits.txt
 */
class CommitNoteParser
{
	// Update these values
	static final String LATEST_TAG = "8.0.2+35"
	static final String PREVIOUS_TAG = "8.0.2+33";
	static final String BASE_TAG = "8.0.2";
	static final String LATEST_HASH = "60f4087c8608"

	static final String OUTPUT_PATH = '/releaseteam/commits/commits-' + LATEST_TAG + ".txt"
	FileWriter logWriter

	static final String COMMIT_DELIMITER = "-----";

	List<String> prevCommits = new ArrayList<String>()
	List<String> commitLines = new ArrayList<String>()

	Map<String, List<String>> output = new TreeMap<String, List<String>>()

	static void main(String[] args) throws Exception
	{
		CommitNoteParser app = new CommitNoteParser()
		app.tag()
		app.getRawLogs()
		app.parseLogs()
	}

	private void tag()
	{
		// Tag latest commit in build
		executeCmd("git tag -a " + LATEST_TAG + " " + LATEST_HASH + " -m '" + LATEST_TAG + "'")

		// Push tag to Github
		executeCmd("git push origin " + LATEST_TAG)
	}

	private void getRawLogs()
	{
		// Get previous commits to ignore this time
		prevCommits = executeCmd("git log --pretty=format:%an%x09%s --no-merges " + BASE_TAG + ".." + PREVIOUS_TAG)

		// Get this build's commits
		commitLines = executeCmd("git log --pretty=format:" + COMMIT_DELIMITER + "%n%an%x09%s%n%ad%x09%an%x09https://github.com/Widen/dam/commit/%h%x09%s%n%b --date=short --no-merges " + PREVIOUS_TAG + ".." + LATEST_TAG )
	}

	private List<String> executeCmd(String cmd)
	{
		println cmd
		def sout = new StringBuffer(), serr = new StringBuffer()

		def proc = cmd.execute()
		proc.consumeProcessOutput(sout, serr)
		proc.waitForOrKill(10000)
		println "out> $sout err> $serr"

		return splitIntoLines(sout.toString())
	}

	private void parseLogs() throws Exception
	{
		List<String> commit = new ArrayList<String>();
		for (String line : commitLines)
		{
			if (StringUtils.equals(line, COMMIT_DELIMITER))
			{
				processCommit(commit);
				commit = new ArrayList<String>();
				continue;
			}

			commit.add(line);
		}
		processCommit(commit);

		logWriter = new FileWriter(new File(OUTPUT_PATH))
		for (String tag : output.keySet())
		{
			logAndSysOut("");
			logAndSysOut(tag)
			for (String line : output.get(tag))
			{
				logAndSysOut(line)
			}
		}

		logWriter.flush()
		logWriter.close()
		println 'Done'
	}

	private void processCommit(List<String> commit)
	{
		if (CollectionUtils.isEmpty(commit) || commit.size() < 2)
		{
			println "EMPTY COMMIT!"
			return;
		}

		String id = commit.get(0);
		String log = commit.get(1)
		if (prevCommits.contains(id))
		{
			println "ALREADY COMMITTED: " + id
			return;
		}

		println id
		Set<String> jiraTags = new HashSet<String>();
		for (int i=1; i < commit.size(); ++ i)
		{
			String line = commit.get(i);
			println "\t" + line

			def rtMatcher = ( line =~ /(RT-[0-9]+)/ )
			def damMatcher = ( line =~ /(DAM-[0-9]+)/ )

			if (rtMatcher)
			{
				jiraTags.addAll(rtMatcher[0])
			}
			if (damMatcher)
			{
				jiraTags.addAll(damMatcher[0])
			}
		}

		println "----- " + jiraTags + " ------"
		if (CollectionUtils.isEmpty(jiraTags))
		{
			List<String> ids = output.get("zMISC");
			if (ids == null)
			{
				ids = new ArrayList<String>()
				output.put("zMISC", ids)
			}
			ids.add(log)
		}
		else
		{
			for (String tag : jiraTags)
			{
				List<String> ids = output.get(tag);
				if (ids == null)
				{
					ids = new ArrayList<String>()
					output.put(tag, ids)
				}
				ids.add(log)
			}
		}
	}


	private List<String> splitIntoLines(String input) throws Exception
	{
		BufferedReader reader = null
		List<String> lines = new ArrayList<String>()
		try
		{
			reader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(input)))

			String line = reader.readLine()
			while (line != null)
			{
				lines.add(line)
				line = reader.readLine()
			}
		}
		finally
		{
			IOUtils.closeQuietly(reader)
		}

		return lines
	}


	private void logAndSysOut(String text)
	{
		println text
		logToFile(text)
	}

	private void logToFile(String text)
	{
		try
		{
			logWriter.write("$text\n")
		}
		catch (IOException e)
		{
			System.err.println(e)
		}
	}
}
