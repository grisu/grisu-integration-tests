package grisu.frontend.tests;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import grisu.control.JobConstants;
import grisu.control.ServiceInterface;
import grisu.control.exceptions.JobPropertiesException;
import grisu.frontend.model.job.JobObject;
import grisu.frontend.tests.utils.Input;
import grisu.frontend.tests.utils.TestConfig;
import grisu.frontend.tests.utils.TestJobObject;
import grisu.model.FileManager;
import grisu.model.GrisuRegistryManager;
import grisu.model.dto.DtoStringList;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.python.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(value = Parameterized.class)
public class TestJobSubmission {

	public static Logger myLogger = LoggerFactory
			.getLogger(TestJobSubmission.class);

	private static final TestConfig config = TestConfig.getTestConfig();

	private static final Map<String, ServiceInterface> sis = config
			.getServiceInterfaces();

	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> result = Lists.newArrayList();

		for (String backend : config.getServiceInterfaces().keySet()) {
			result.add(new Object[] { backend,
					config.getServiceInterfaces().get(backend) });
		}

		return result;
	}


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		// delete temp dir
		FileUtils.deleteDirectory(Input.INPUT_FILES_DIR);
		// getConfig();

		for (String backend : sis.keySet()) {

			ServiceInterface si = sis.get(backend);
			// killing all jobs
			System.out.println("Killing all jobs on backend: " + backend);
			DtoStringList allJobnames = si.getAllJobnames(null);
			if ((allJobnames != null)
					&& (allJobnames.getStringList().size() > 0)) {
				si.killJobs(allJobnames, true);
			}

			System.out.println("Setting up backend: " + backend);
			FileManager fm = GrisuRegistryManager.getDefault(si)
					.getFileManager();
			// make sure remoteInputFile is populated
			fm.deleteFile(config.getGsiftpRemoteInputFile());
			fm.cp(config.getInputFile(),
					config.getGsiftpRemoteInputParent(), true);

			long localsize = new File(config.getInputFile()).length();
			long remotesize = fm.getFileSize(config.getGsiftpRemoteInputFile());

			if (localsize != remotesize) {
				throw new RuntimeException(
						"Can't setup remote input file: sizes differ");
			}
		}

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

		for (ServiceInterface si : sis.values()) {
			DtoStringList allJobnames = si.getAllJobnames(null);
			if ((allJobnames != null)
					&& (allJobnames.getStringList().size() > 0)) {
				si.killJobs(allJobnames, true);
			}
			si.logout();
		}
	}

	private final ServiceInterface si;
	private final String backendname;
	private final FileManager fm;

	public TestJobSubmission(String backendname, ServiceInterface si) {
		this.backendname = backendname;
		this.si = si;
		this.fm = GrisuRegistryManager.getDefault(si).getFileManager();
	}

	@Before
	public void setUp() throws Exception {
		si.kill(config.getJobname(), true);
	}

	/**
	 * Submits a generic job with both local and remote input files.
	 *
	 * Tests direct file upload and 3rd party gridftp transfer as well as job
	 * submission.
	 *
	 * @throws Exception
	 */
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

	/**
	 * Submits a generic cat job with a local input file.
	 *
	 * @throws Exception
	 */
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

	/**
	 * Submits a generic cat job with remote input file in order to test staging
	 * using gridftp 3rd party transfer.
	 *
	 * @throws Exception
	 */
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

	/**
	 * Submits a python job in order to test whether issue with stdin
	 * interfering is disappeared.
	 *
	 * @throws Exception
	 */
	@Test
	public void testPythonStdinIssue() throws Exception {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("python " + config.getPythonScriptName());
		job.setApplication("Python");
		job.addInputFileUrl(config.getPythonScript());

		job.createJob(config.getFqan());

		job.submitJob(true);

		job.waitForJobToFinish(4);

		String stderr = job.getStdErrContent();

		assertTrue("Stderr for job not empty.", StringUtils.isBlank(stderr));

		String stdout = job.getStdOutContent();
		myLogger.debug("Content: " + stdout);

		assertEquals("Hello Python World!", stdout.trim());

	}

	/**
	 * Submits a generic job with auto-queue selection.
	 *
	 * @throws Exception
	 */
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


	/**
	 * Submits a job with specifying the package to use but not the queue.
	 *
	 * @throws Exception
	 */
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

	/**
	 * Tries to submit a non-valid job where a wrong package version is
	 * specified.
	 *
	 * @throws JobPropertiesException
	 */
	@Test(expected = JobPropertiesException.class)
	public void testVersionNotAvailable() throws JobPropertiesException {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("echo nothing");
		job.setApplication("UnixCommands");
		job.setApplicationVersion("Invalid");

		job.createJob(config.getFqan());

	}

	/**
	 * Verify that environment variables are passed along.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEnvironmentVariables() throws Exception {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		job.setCommandline("env");
		job.setApplication("generic");
		job.addEnvironmentVariable("var1","hello, world!");
		job.addEnvironmentVariable("var2","/tmp/test");

		job.createJob(config.getFqan());
		job.submitJob(true);
		job.waitForJobToFinish(4);
		assertEquals(JobConstants.DONE_STRING, job.getStatusString(true));
		
		String stdout = job.getStdOutContent();
		assertThat(stdout, containsString("var1=hello, world!"));
		assertThat(stdout, containsString("var2=/tmp/test"));
		assertThat(stdout, containsString("GRISU_APPLICATION=generic"));
		assertThat(stdout, containsString("GRISU_EXECUTABLE=env"));
	}

	/**
	 * Verify killing a job works
	 * 
	 * @throws Exception
	 */
	@Test
	public void testKill() throws Exception {

		TestJobObject job = new TestJobObject(si);
		job.setJobname(config.getJobname());
		String command = "bash " + config.getKillmeScriptName() + " 1234";
		job.setCommandline(command);
		job.setApplication("generic");
		job.addInputFileUrl(config.getKillmeScript());
		
		job.createJob(config.getFqan());
		job.submitJob(true);

		job.waitForJobToReachState(JobConstants.ACTIVE_STRING, 4);
		job.kill(false);
		job.waitForJobToFinish(3);

		String stdout = job.getStdOutContent();
		String status = job.getStatusString(true);
		myLogger.warn("stdout: " + stdout);
		myLogger.warn("stderr: " + job.getStdErrContent());
		myLogger.warn("status: " + status);
		assertEquals(JobConstants.KILLED_STRING, status);
		assertEquals("GOT_KILLED", stdout.trim());
	}

	/**
	 * Verify that we get the right status of a job if Gram has to start
	 * a new job manager for the job if we ask for job status.
	 * Prerequisites for this test to work:
	 * The job must run on the same machine like gram
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetStatusSurvivesJobManagerRestart() throws Exception {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		String command = "bash " + config.getKillJobManagersScriptName() + " 1";
		job.setCommandline(command);
		job.setApplication("generic");
		job.addInputFileUrl(config.getKillJobManagersScript());
		
		job.createJob(config.getFqan());
		job.submitJob(true);

		Thread.sleep(15000);
		job.waitForJobToFinish(3);

		String stdout = job.getStdOutContent();
		String stderr = job.getStdErrContent();
		String status = job.getStatusString(true);
		myLogger.warn("stdout: " + stdout);
		myLogger.warn("stderr: " + stderr);
		myLogger.warn("status: " + status);
		assertEquals(JobConstants.DONE_STRING, status);
		assertThat(stdout, containsString("KILLED_JOBMANAGERS_SUCCESS"));
		assertThat(stdout, containsString("NORMAL_TERMINATION"));
	}

	/**
	 * Verify that we can kill a job if Gram has to start a new job manager
	 * for the job when we kill it.
	 * Prerequisites for this test to work:
	 * The job must run on the same machine like gram.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testKillSurvivesJobManagerRestart() throws Exception {

		JobObject job = new JobObject(si);
		job.setJobname(config.getJobname());
		String command = "bash " + config.getKillJobManagersScriptName() + " 12345";
		job.setCommandline(command);
		job.setApplication("generic");
		job.addInputFileUrl(config.getKillJobManagersScript());
		
		job.createJob(config.getFqan());
		job.submitJob(true);

		Thread.sleep(15000);
		job.kill(false);
		job.waitForJobToFinish(3);

		String stdout = job.getStdOutContent();
		String stderr = job.getStdErrContent();
		String status = job.getStatusString(true);
		myLogger.warn("stdout: " + stdout);
		myLogger.warn("stderr: " + stderr);
		myLogger.warn("status: " + status);
		assertEquals(JobConstants.KILLED_STRING, status);
		assertThat(stdout, containsString("KILLED_JOBMANAGERS_SUCCESS"));
		assertThat(stdout, containsString("GOT_KILLED"));
	}

}
