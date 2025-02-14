package com.example.smileLib.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

/**
 * ConfigManager 類是一個通用的工具類，用於處理 YML 配置文件。
 * 提供了以下功能：
 * 1. 讀取 YML 文件並返回 Map 結構的數據。
 * 2. 讀取 YML 文件並將其解析成指定類型的對象。
 * 3. 將數據寫入 YML 文件（覆蓋原有內容）。
 * 4. 如果文件不存在，則創建新文件並寫入初始數據。
 * 5. 更新已存在的 YML 文件中指定鍵的值（支持點號分隔的多層鍵）。
 * 這個類別利用 SnakeYAML 進行 YML 文件 * Java IO 操作來讀取與寫入文件。註釋中詳細說明了各個方法的用途與實現原理。的解析與生成，並通過
 */
public class ConfigManager {

	// 靜態 Yaml 物件，用於解析和生成 YML 格式的文件內容。
	private static final Yaml yaml;

	static {
		// 創建 DumperOptions 來設置 YAML 輸出的選項，例如縮進、格式風格等。
		DumperOptions options = new DumperOptions();

		// 設置縮進空格數，這裡設置為 2 個空格
		options.setIndent(2);

		// 設置是否使用漂亮流（即多行展示，每個條目獨立一行）
		options.setPrettyFlow(true);

		// 設置默認的輸出風格為 BLOCK（塊狀格式），這樣輸出結果更易讀
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

		// 根據配置的選項初始化 Yaml 物件
		yaml = new Yaml(options);
	}

	/**
	 * 讀取指定路徑的 YML 文件，並將其內容解析為 Map 結構。
	 *
	 * @param filePath 文件的路徑，可以是絕對路徑或相對於當前工作目錄的路徑
	 * @return 解析後的 Map 結構，其中鍵和值對應於 YML 文件中的配置項
	 * @throws RuntimeException 如果找不到文件或讀取失敗，則拋出運行時異常
	 */
	public static Map<String, Object> readConfig(String filePath) {
		// 使用 try-with-resources 確保 InputStream 正常關閉
		try (InputStream input = new FileInputStream(filePath)) {
			// 利用 yaml.load() 方法解析 YML 文件內容並返回 Map 結構
			return yaml.load(input);
		} catch (FileNotFoundException e) {
			// 當找不到指定文件時，拋出包含詳細信息的運行時異常
			throw new RuntimeException("找不到配置文件：" + filePath, e);
		} catch (IOException e) {
			// 當讀取文件過程中出現 IO 異常時，拋出運行時異常
			throw new RuntimeException("讀取配置文件失敗：" + filePath, e);
		}
	}

	/**
	 * 讀取指定路徑的 YML 文件，並將其內容直接解析為指定類型的對象。
	 *
	 * @param filePath 文件的路徑
	 * @param clazz    目標類型，該類型必須與 YML 文件中的數據結構相匹配
	 * @param <T>      目標類型的泛型參數
	 * @return 解析後的對象，類型為 T
	 * @throws RuntimeException 如果讀取或解析過程中發生異常
	 */
	public static <T> T loadConfig(String filePath, Class<T> clazz) {
		try (InputStream input = new FileInputStream(filePath)) {
			// 利用 yaml.loadAs() 方法，將 YML 文件內容解析為指定類型的對象
			return yaml.loadAs(input, clazz);
		} catch (IOException e) {
			throw new RuntimeException("讀取配置文件失敗：" + filePath, e);
		}
	}

	/**
	 * 將指定的數據寫入到一個 YML 文件中，覆蓋原有的內容。
	 *
	 * @param filePath 文件的路徑
	 * @param data     要寫入的數據，通常為 Map 或 POJO 對象
	 * @throws RuntimeException 如果寫入過程中出現 IO 異常，則拋出運行時異常
	 */
	public static void writeConfig(String filePath, Object data) {
		// 使用 FileWriter 並利用 try-with-resources 保證資源正確關閉
		try (Writer writer = new FileWriter(filePath)) {
			// 使用 yaml.dump() 方法將數據轉換為 YML 格式，並寫入文件
			yaml.dump(data, writer);
		} catch (IOException e) {
			throw new RuntimeException("寫入配置文件失敗：" + filePath, e);
		}
	}

	/**
	 * 如果指定的 YML 文件不存在，則創建該文件並寫入初始數據。
	 *
	 * @param filePath 文件的路徑
	 * @param data     初始數據，將寫入新文件中
	 * @throws RuntimeException 如果創建或寫入文件失敗
	 */
	public static void createConfig(String filePath, Object data) {
		File file = new File(filePath);
		// 如果文件已經存在，則不進行任何操作
		if (file.exists()) {
			return;
		}
		try {
			// 如果文件的父目錄不存在，則創建所有父目錄
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}
			// 創建新文件，createNewFile() 返回 true 表示文件成功創建
			if (file.createNewFile()) {
				// 將初始數據寫入剛創建的文件中
				writeConfig(filePath, data);
			}
		} catch (IOException e) {
			throw new RuntimeException("創建配置文件失敗：" + filePath, e);
		}
	}

	/**
	 * 更新指定 YML 文件中某個配置鍵的值。鍵可以使用點號分隔來表示多層次的結構，
	 * 例如 "database.host" 表示在 database 這個 Map 中的 host 鍵。
	 *
	 * @param filePath 文件的路徑
	 * @param key      要更新的配置鍵（支持使用點號分隔表示多層次）
	 * @param value    新的值
	 * @throws RuntimeException 如果讀取、更新或寫入過程中發生異常
	 */
	public static void updateConfigValue(String filePath, String key, Object value) {
		// 首先讀取當前的配置文件內容到 Map 結構中
		Map<String, Object> config = readConfig(filePath);
		// 調用內部方法 setValue() 根據點號分隔的鍵設置對應的值
		setValue(config, key, value);
		// 將更新後的 Map 寫回到原文件中
		writeConfig(filePath, config);
	}

	/**
	 * 遞歸地在 Map 結構中設置指定鍵的值。
	 * 支持鍵使用點號分隔的方式，對於多層嵌套的 Map，會依次進入每一層結構。
	 *
	 * @param map   要操作的 Map 對象
	 * @param key   配置鍵，支持用點號分隔，例如 "a.b.c"
	 * @param value 要設置的新值
	 * @throws RuntimeException 如果中間某一層的值不是 Map，則無法繼續設置並拋出異常
	 */
	@SuppressWarnings("unchecked")
	private static void setValue(Map<String, Object> map, String key, Object value) {
		// 將鍵根據點號分割成多個部分
		String[] parts = key.split("\\.");
		// 遍歷除最後一個部分外的所有部分，逐層深入 Map 結構
		for (int i = 0; i < parts.length - 1; i++) {
			Object obj = map.get(parts[i]);
			// 如果當前部分對應的值不是 Map，則無法繼續深入設置，拋出異常
			if (!(obj instanceof Map)) {
				throw new RuntimeException("鍵 " + parts[i] + " 對應的值不是一個 Map，無法進一步設置：" + key);
			}
			// 將 map 指向下一層的 Map
			map = (Map<String, Object>) obj;
		}
		// 設置最內層鍵的值為傳入的 value
		map.put(parts[parts.length - 1], value);
	}
}


