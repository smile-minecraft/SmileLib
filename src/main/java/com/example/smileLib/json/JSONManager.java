package com.example.smileLib.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * JSONManager 是一個簡單的 JSON 操作工具庫，
 * 讓你可以像在 JavaScript 中一樣輕鬆操作 JSON 文件。
 *
 * 主要功能包括：
 * 1. 讀取 JSON 文件並返回解析後的 JsonElement。
 * 2. 將給定的 JsonElement 寫入到指定的 JSON 文件中（可自動創建文件）。
 * 3. 更新 JSON 文件中頂層 JSON 物件的指定鍵值。
 * 4. 刪除 JSON 文件中頂層 JSON 物件中的指定鍵。
 * 5. 從 JSON 文件中讀取數據，並將其轉換成指定的 Java 物件。
 *
 * 這個工具庫基於 GSON 庫實現，並使用 UTF-8 編碼進行文件讀寫，
 * 同時支持 pretty printing 以便於查看生成的 JSON 格式。
 */
public class JSONManager {

	// GSON 實例，啟用 pretty printing（整齊格式化）
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * 從指定的文件路徑讀取 JSON 文件，並返回解析後的 JsonElement 對象。
	 *
	 * @param filePath JSON 文件的路徑（可以是絕對路徑或相對於工作目錄的路徑）
	 * @return 解析後的 JsonElement 對象
	 * @throws IOException 若在讀取文件時發生錯誤，則拋出 IOException
	 */
	public static JsonElement readJson(String filePath) throws IOException {
		try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
			return gson.fromJson(reader, JsonElement.class);
		}
	}

	/**
	 * 將給定的 JsonElement 寫入到指定的 JSON 文件中。
	 * 如果文件不存在，則會自動創建新文件（同時建立父目錄）。
	 *
	 * @param filePath    JSON 文件的路徑
	 * @param jsonElement 要寫入的 JsonElement 對象
	 * @throws IOException 若在寫入文件時發生錯誤，則拋出 IOException
	 */
	public static void writeJson(String filePath, JsonElement jsonElement) throws IOException {
		// 如果文件不存在，則自動創建文件
		File file = new File(filePath);
		if (!file.exists()) {
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			file.createNewFile();
		}
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			gson.toJson(jsonElement, writer);
		}
	}

	/**
	 * 如果指定的 JSON 文件不存在，則創建新文件並寫入初始數據。
	 *
	 * @param filePath    JSON 文件的路徑
	 * @param initialData 初始的 JSON 數據（JsonElement 格式）
	 * @throws IOException 若在創建或寫入文件時發生錯誤，則拋出 IOException
	 */
	public static void createJsonFile(String filePath, JsonElement initialData) throws IOException {
		File file = new File(filePath);
		if (!file.exists()) {
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}
			file.createNewFile();
			writeJson(filePath, initialData);
		}
	}

	/**
	 * 更新指定 JSON 文件中頂層 JSON 物件中的某個鍵值。
	 * 如果該鍵不存在，則新增該鍵；如果原本內容不是一個 JSON 物件，則會初始化為空 JSON 物件。
	 *
	 * @param filePath JSON 文件的路徑
	 * @param key      要更新或新增的鍵
	 * @param value    新的值（JsonElement 格式）
	 * @throws IOException 若在讀取或寫入文件時發生錯誤，則拋出 IOException
	 */
	public static void updateJsonValue(String filePath, String key, JsonElement value) throws IOException {
		// 讀取現有的 JSON 數據
		JsonElement jsonElement = readJson(filePath);
		JsonObject jsonObject;
		if (jsonElement != null && jsonElement.isJsonObject()) {
			jsonObject = jsonElement.getAsJsonObject();
		} else {
			// 如果文件內容不是 JSON 物件，初始化為空 JSON 物件
			jsonObject = new JsonObject();
		}
		// 更新或新增指定的鍵值
		jsonObject.add(key, value);
		// 寫回文件
		writeJson(filePath, jsonObject);
	}

	/**
	 * 刪除指定 JSON 文件中頂層 JSON 物件中的某個鍵。
	 *
	 * @param filePath JSON 文件的路徑
	 * @param key      要刪除的鍵
	 * @throws IOException 若在讀取或寫入文件時發生錯誤，則拋出 IOException
	 */
	public static void deleteJsonKey(String filePath, String key) throws IOException {
		JsonElement jsonElement = readJson(filePath);
		if (jsonElement != null && jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			jsonObject.remove(key);
			writeJson(filePath, jsonObject);
		}
	}

	/**
	 * 從指定的 JSON 文件中讀取數據，並將其轉換成指定的 Java 類型。
	 *
	 * @param filePath JSON 文件的路徑
	 * @param clazz    目標 Java 類型，例如 MyClass.class
	 * @param <T>      泛型參數
	 * @return 轉換後的 Java 物件，如果文件為空則返回 null
	 * @throws IOException 若在讀取或解析文件時發生錯誤，則拋出 IOException
	 */
	public static <T> T fromJson(String filePath, Class<T> clazz) throws IOException {
		try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
			return gson.fromJson(reader, clazz);
		}
	}
}
