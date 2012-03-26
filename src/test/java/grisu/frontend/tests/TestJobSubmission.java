package grisu.frontend.tests;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import grisu.control.ServiceInterface;
import grisu.control.exceptions.JobPropertiesException;
import grisu.frontend.model.job.JobObject;
import grisu.frontend.tests.utils.Input;
import grisu.frontend.tests.utils.TestConfig;
import grisu.model.FileManager;
import grisu.model.GrisuRegistryManager;

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

	private static TestConfig config;

	private static Map<String, ServiceInterface> sis;

	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> result = Lists.newArrayList();

		for (String backend : getConfig().getServiceInterfaces().keySet()) {
			result.add(new Object[] { backend,
					config.getServiceInterfaces().get(backend) });
		}

		return result;
	}

	public synchronized static TestConfig getConfig() {
		if ( config == null ) {
			try {
				config = TestConfig.create();

				sis = config.getServiceInterfaces();
			} catch (Exception e) {
				throw new RuntimeException("Can't setup test config: "
						+ e.getLocalizedMessage(), e);
			}
		}
		return config;
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		// delete temp dir
		FileUtils.deleteDirectory(Input.INPUT_FILES_DIR);
		getConfig();

		for (String backend : sis.keySet()) {
			ServiceInterface si = sis.get(backend);
			System.out.println("Setting up backend: " + backend);
			FileManager fm = GrisuRegistryManager.getDefault(si)
					.getFileManager();
			// make sure remoteInputFile is populated
			fm.deleteFile(getConfig().getGsiftpRemoteInputFile());
			fm.cp(getConfig().getInputFile(),
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
