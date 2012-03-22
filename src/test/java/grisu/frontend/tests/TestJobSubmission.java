package grisu.frontend.tests;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import grisu.control.ServiceInterface;
import grisu.control.exceptions.JobPropertiesException;
import grisu.frontend.model.job.JobObject;
import grisu.frontend.tests.utils.TestConfig;
import grisu.model.FileManager;
import grisu.model.GrisuRegistryManager;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJobSubmission {

	public static Logger myLogger = LoggerFactory
			.getLogger(TestJobSubmission.class);

	private static TestConfig config;

	private static ServiceInterface si;
	private static FileManager fm;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		config = TestConfig.create();

		si = config.getServiceInterface();
		fm = GrisuRegistryManager.getDefault(si).getFileManager();

		// make sure remoteInputFile is populated
		fm.deleteFile(config.getGsiftpRemoteInputFile());
		fm.cp(config.getInputFile(), config.getGsiftpRemoteInputParent(), true);

		long localsize = new File(config.getInputFile()).length();
		long remotesize = fm.getFileSize(config.getGsiftpRemoteInputFile());

		if ( localsize != remotesize ) {
			throw new RuntimeException(
					"Can't setup remote input file: sizes differ");
		}

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

		if (si != null) {
			si.logout();
		}
	}

	@Before
	public void setUp() throws Exception {
		si.kill(config.getJobname(), true);
	}

	@Test
	public void simpleGenericJobWithLocalAndRemoteInput() throws Exception {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("cat " + config.getInputFileName() + " "
				+ config.getInputFileName2());
		job.setApplication("generic");

		job.addInputFileUrl(config.getInputFile2());
		job.addInputFileUrl(config.getGsiftpRemoteInputFile());

		job.createJob(config.getFqan());
		job.submitJob(true);

		job.waitForJobToFinish(4);

		String stdout = job.getStdOutContent();

		myLogger.debug("Content: " + stdout);

		assertThat(stdout, containsString("markus"));
		assertThat(stdout, containsString("great"));

	}

	@Test
	public void simpleGenericJobWithLocalInput() throws Exception {


		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("cat " + config.getInputFileName());
		job.setApplication("generic");

		job.addInputFileUrl(config.getInputFile());

		job.createJob(config.getFqan());
		job.submitJob(true);

		job.waitForJobToFinish(4);

		String stdout = job.getStdOutContent();
		myLogger.debug("Content: " + stdout);

		assertThat(stdout, containsString("markus"));

	}

	@Test
	public void simpleGenericJobWithRemoteInput() throws Exception {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("cat " + config.getInputFileName());
		job.setApplication("generic");

		job.addInputFileUrl(config.getGsiftpRemoteInputFile());

		job.createJob(config.getFqan());
		job.submitJob(true);

		job.waitForJobToFinish(4);

		String stdout = job.getStdOutContent();
		myLogger.debug("Content: " + stdout);

		assertThat(stdout, containsString("markus"));

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test(expected = JobPropertiesException.class)
	public void testPackageNotAvailable() throws JobPropertiesException {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("echo nothing");
		job.setApplication("Invalid");

		job.createJob(config.getFqan());

	}

	@Test
	public void testSimpleGenericJob() throws Exception {


		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("echo " + config.getContent());
		job.setApplication("generic");

		job.createJob(config.getFqan());

		job.submitJob(true);

		job.waitForJobToFinish(4);

		String stdout = job.getStdOutContent();
		myLogger.debug("Content: " + stdout);

		assertEquals(stdout.trim(), config.getContent().trim());

	}

	@Test
	public void testSimpleUnixCommandsJob() throws Exception {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("echo " + config.getContent());
		job.setApplication("UnixCommands");

		job.createJob(config.getFqan());

		job.submitJob(true);

		job.waitForJobToFinish(4);

		String stdout = job.getStdOutContent();
		myLogger.debug("Content: " + stdout);

		assertEquals(stdout.trim(), config.getContent().trim());

	}

	@Test(expected = JobPropertiesException.class)
	public void testVersionNotAvailable() throws JobPropertiesException {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("echo nothing");
		job.setApplication("UnixCommands");
		job.setApplicationVersion("Invalid");

		job.createJob(config.getFqan());

	}

}
