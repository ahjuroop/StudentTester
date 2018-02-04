import ee.ttu.java.studenttester.classes.StudentTesterAPI;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Collections;

public class ClassBlacklistTest {

	StudentTesterAPI api = StudentTesterAPI.getInstance(getClass());

	@Test
	public void testBlacklist() {
		ClassBlacklist b = new ClassBlacklist();
		b.onePlusOne();
		api.logMessagePublic(String.format("Before: %s", api.getClassBlacklist()));
		api.addClassToBlacklist(Math.class);
		api.getClassBlacklist().forEach(clazz -> {
			api.logMessagePublic(String.format("Contains: %s", clazz));
		});
		api.removeClassFromBlacklist(Math.class);
		api.logMessagePublic(String.format("After: %s", api.getClassBlacklist()));
	}

}