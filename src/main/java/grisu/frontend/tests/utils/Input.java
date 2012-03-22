package grisu.frontend.tests.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class Input {

	public static File INPUT_FILES_DIR = new File(System.getProperty("java.io.tmpdir"), "grisu-integration-input");


	static String getFile(String fileName) {

		File file = new File(INPUT_FILES_DIR, fileName);

		if (!file.exists()) {
			INPUT_FILES_DIR.mkdirs();
			InputStream inS = Input.class.getResourceAsStream("/" + fileName);
			try {
				IOUtils.copy(inS, new FileOutputStream(file));
				inS.close();
			} catch (Exception e) {
				throw new RuntimeException(
						"Can't create temporary input files: "
								+ e.getLocalizedMessage());
			}
		}

		return file.getAbsolutePath();
	}

}
