package grisu.frontend.tests.utils;

import grisu.control.ServiceInterface;
import grisu.frontend.model.job.JobObject;

public class TestJobObject extends JobObject {

	public TestJobObject(final ServiceInterface si) {
		super(si);
	}
	
	public void waitForJobToReachState(String state, int checkIntervalSeconds) {
		try {
			Thread.sleep(checkIntervalSeconds * 1000);
			if (state.equals(super.getStatusString(true))) {
				return;
			}
		} catch (final InterruptedException e) {}
	}
	
}
