import com.example.smileLib.json.JSONManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class JSONManagerTest {

	private static final String TEST_JSON = "temp_test.json";

	@Test
	public void testJsonOperations() throws IOException {
		// 建立初始 JSON 物件
		JsonObject obj = new JsonObject();
		obj.addProperty("appName", "TestApp");
		// 寫入 JSON 文件
		JSONManager.writeJson(TEST_JSON, obj);

		// 讀取 JSON 文件
		JsonElement element = JSONManager.readJson(TEST_JSON);
		Assertions.assertTrue(element.isJsonObject());
		JsonObject readObj = element.getAsJsonObject();
		Assertions.assertEquals("TestApp", readObj.get("appName").getAsString());

		// 更新 JSON 文件中的鍵值
		JSONManager.updateJsonValue(TEST_JSON, "version", JSONManager.getGson().toJsonTree("1.0.0"));
		JsonElement updated = JSONManager.readJson(TEST_JSON);
		Assertions.assertEquals("1.0.0", updated.getAsJsonObject().get("version").getAsString());

		// 刪除 JSON 文件中的鍵
		JSONManager.deleteJsonKey(TEST_JSON, "appName");
		JsonElement deleted = JSONManager.readJson(TEST_JSON);
		Assertions.assertFalse(deleted.getAsJsonObject().has("appName"));
	}

	@AfterEach
	public void cleanup() {
		File file = new File(TEST_JSON);
		if (file.exists()) {
			file.delete();
		}
	}
}

