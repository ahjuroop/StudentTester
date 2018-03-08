import ee.ttu.java.studenttester.classes.StudentSecurity;
import ee.ttu.java.studenttester.classes.StudentTesterAPI;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class PolicyCheck {

	public void whoami() {
		try {
			Process p = Runtime.getRuntime().exec("whoami");
			BufferedReader i = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = i.readLine()) != null) {
				System.err.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void procWithPath() {
		try {
			Process p = Runtime.getRuntime().exec("C:/Windows/Explorer.exe");
			BufferedReader i = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = i.readLine()) != null) {
				System.err.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void exit() {
		System.exit(1);
	}

	public void innocent() {
		Math.pow(2, 2);
	}

	public String writeAndReadInnocent(String text) {
		try {
			File f = new File("newfile.txt");
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(text.getBytes());
			fos.close();
			FileInputStream fis = new FileInputStream(f);
			byte[] contents = fis.readAllBytes();
			return new String(contents, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void sniffFile() {
		try {
			FileInputStream fis = new FileInputStream(new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
					+ "PolicyCheckTest.java"));
			byte[] contents = fis.readAllBytes();
			String str = new String(contents, "UTF-8");
			System.err.println(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void hijackApi() {
		StudentTesterAPI api = StudentTesterAPI.getInstance(PolicyCheckTest.class);
		api.removeClassFromBlacklist(PolicyCheck.class);
		api.logMessagePrivate("Hijacked the API lol");
		// this.sniffFile();
	}

	public void hijackSecurityManager() {
		StudentSecurity sec = StudentSecurity.getInstance();
		sec.removeClass(getClass());
	}

	public void openSocket() throws Exception {
		URL url = new URL("https://example.com");
		URLConnection conn = url.openConnection();
		InputStream is = conn.getInputStream();
		byte[] data = is.readAllBytes();
	}
}
