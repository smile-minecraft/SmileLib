# SmileLib - A Utility Library for Minecraft Paper Plugins

SmileLib 是一個專為 Minecraft Paper Plugin 開發者設計的工具庫，提供一系列常用功能，讓你可以更方便地進行插件開發。此 Library 包含以下模組：

- **Config**：提供統一的 YML 配置文件操作，方便讀取、修改、創建與更新配置。
- **Database**：包含 DBConfig 與 SQLManager，支持 MySQL 與 SQLite，提供簡化的資料庫連線、查詢與更新操作。
- **JSON**：基於 GSON 的 JSONManager，可輕鬆讀取、修改、創建、刪除 JSON 文件，並支持將 JSON 文件內容轉換成 Java 物件。
- **Logger**：自定義 Logger 工具包，利用 Paper API 的 ChatColor 實現彩色控制台輸出，支持將 log 緩存後寫入文件並透過 Discord Webhook 發送錯誤訊息。

---

## 安裝

將 SmileLib Library 打包成 JAR 文件，並將此 JAR 添加到你的 Minecraft Paper Plugin 的插件資料夾中。同時，確保你的專案中已引入所需的依賴庫（例如 GSON、SnakeYAML 等）。

---

## 工具包使用指南

### 1. Config 模組

**功能：**  
- **讀取配置：** 使用 `ConfigManager.readConfig(filePath)` 方法讀取指定 YML 文件並返回 `Map<String, Object>`。
- **解析配置：** 使用 `ConfigManager.loadConfig(filePath, Class<T> clazz)` 方法直接將 YML 文件內容解析為指定類型的對象（例如 DBConfig）。
- **寫入配置：** 使用 `ConfigManager.writeConfig(filePath, data)` 方法將數據寫入 YML 文件中。
- **創建文件：** 使用 `ConfigManager.createConfig(filePath, data)` 方法檢查文件是否存在，若不存在則創建並寫入初始數據。
- **更新配置：** 使用 `ConfigManager.updateConfigValue(filePath, key, value)` 方法更新文件中指定鍵的值。
- **刪除配置鍵：** 可自行擴展實現，參考更新邏輯。

**使用示例：**

```java
// 讀取 config.yml 並返回 Map 結構
Map<String, Object> configMap = ConfigManager.readConfig("config.yml");

// 將 config.yml 解析為 DBConfig 對象
DBConfig dbConfig = ConfigManager.loadConfig("config.yml", DBConfig.class);

// 更新配置中的 database.host 鍵
ConfigManager.updateConfigValue("config.yml", "database.host", "127.0.0.1");

// 創建新配置文件（如果不存在）
Map<String, Object> initialData = Map.of("appName", "MyApp", "version", "1.0.0");
ConfigManager.createConfig("new_config.yml", initialData);
```

---

### 2. Database 模組

**組成：**
- **DBConfig**：封裝資料庫連線資訊，包括 dbType（"mysql" 或 "sqlite"）、host、port、username、password 與 database。
- **SQLManager**：根據 DBConfig 自動組合 JDBC URL，支持 MySQL 與 SQLite，並提供以下方法：
    - `getConnection()`：根據 dbType 建立 JDBC 連線。
    - `executeQuery(String query)`：直接執行原始 SQL 查詢，返回 ResultSet（使用者需自行管理資源）。
    - 重載的 `executeQuery(...)`：根據表名、主鍵查詢特定欄位，返回結果轉換成 Map 或指定 Java 類型。
    - `executeUpdate(String query)`：直接執行更新語句，返回受影響行數。
    - 重載的 `executeUpdate(...)`：根據傳入的 Map 組合 UPDATE 語句，並執行更新操作。

**使用示例：**

```java
// 假設你已經從 YML 配置中載入了 DBConfig
DBConfig config = new DBConfig();
config.setDbType("mysql");  // 或 "sqlite"
config.setHost("localhost");
config.setPort(3306);
config.setUsername("root");
config.setPassword("yourpassword");
config.setDatabase("yourdb");

// 建立 SQLManager 實例
SQLManager sqlManager = new SQLManager(config);

// 執行原始 SQL 查詢
ResultSet rs = sqlManager.executeQuery("SELECT * FROM users");
// 請記得在使用完 rs 後手動關閉資源

// 利用簡易查詢根據主鍵查詢特定欄位資料，返回 Map
Map<String, Object> result = sqlManager.executeQuery("users", "id", 123, "name", "age", "email");

// 查詢單一欄位並轉換為 Integer
Integer age = sqlManager.executeQuery("users", "id", 123, "age", Integer.class);

// 執行更新操作：根據主鍵更新某些欄位
Map<String, Object> updateMap = new HashMap<>();
updateMap.put("name", "新名字");
updateMap.put("age", 30);
int rowsAffected = sqlManager.executeUpdate("users", "id", 123, updateMap);
```

---

### 3. JSON 模組

**功能：**
- **讀取 JSON 文件：** 使用 `JSONManager.readJson(filePath)` 方法讀取 JSON 文件並返回 `JsonElement` 對象。
- **寫入 JSON 文件：** 使用 `JSONManager.writeJson(filePath, jsonElement)` 方法將 `JsonElement` 寫入指定文件，若文件不存在則自動創建。
- **創建 JSON 文件：** 使用 `JSONManager.createJsonFile(filePath, initialData)` 方法創建新 JSON 文件並寫入初始數據。
- **更新 JSON 鍵值：** 使用 `JSONManager.updateJsonValue(filePath, key, value)` 方法更新指定 JSON 文件中頂層 JSON 物件的某個鍵值。
- **刪除 JSON 鍵：** 使用 `JSONManager.deleteJsonKey(filePath, key)` 方法刪除頂層 JSON 物件中的指定鍵。
- **轉換 JSON 至 Java 物件：** 使用 `JSONManager.fromJson(filePath, Class<T> clazz)` 方法將 JSON 文件內容轉換成指定 Java 物件。

**使用示例：**

```java
// 讀取 JSON 文件並解析成 JsonElement
JsonElement element = JSONManager.readJson("config.json");

// 更新 config.json 中 "appName" 鍵的值
JSONManager.updateJsonValue("config.json", "appName", JSONManager.gson.toJsonTree("MyNewApp"));

// 刪除 JSON 文件中的 "deprecatedKey" 鍵
JSONManager.deleteJsonKey("config.json", "deprecatedKey");

// 將 config.json 轉換成 Map
Map<?, ?> configMap = JSONManager.fromJson("config.json", Map.class);
```

---

### 4. Logger 模組

**功能：**
- **彩色控制台輸出：** 利用 Paper API 的 ChatColor 功能實現不同日誌級別的彩色輸出（INFO、WARN、ERROR）。
- **日誌緩存與文件輸出：** 在運行期間將所有日誌記錄緩存在內存中，伺服器關閉時自動將日誌寫入 TXT 文件，文件名稱以關閉時間命名並統一存放在指定目錄中。
- **Discord Webhook：** 當發生錯誤時，將錯誤訊息以 JSON 格式（利用 GSON 處理）發送到指定 Discord webhook。

**使用示例：**

```java
// 初始化 CustomLogger，指定日誌文件目錄與 Discord webhook URL
CustomLogger logger = new CustomLogger("logs", "https://discord.com/api/webhooks/你的WebhookID/你的WebhookToken");

// 輸出不同級別的日誌訊息
logger.info("伺服器啟動！");
logger.warn("這是一個警告訊息！");
try {
    throw new Exception("測試異常");
} catch (Exception e) {
    logger.error("發生錯誤：", e);
}
```

---

## 貢獻
- SmileLib 歡迎任何形式的貢獻，包括但不限於提交問題、修復錯誤、改進文檔、新增功能等。