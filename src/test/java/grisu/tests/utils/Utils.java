package grisu.tests.utils;

import grisu.control.ServiceInterface;
import grisu.frontend.control.login.LoginManager;
import grith.jgrith.credential.Credential;
import grith.jgrith.credential.MyProxyCredential;

public class Utils {

	private static ServiceInterface si;

	public static ServiceInterface getServiceInterface() {
		if (si == null) {
			LoginManager.initEnvironment();
			Credential cred = new MyProxyCredential("test1",
					"test123".toCharArray());
			try {
				si = LoginManager.login(cred, "local", false);
			} catch (Exception e) {
				throw new RuntimeException("Can't create serviceinterface: "
						+ e.getLocalizedMessage(), e);
			}
		}
		return si;

	}

}
