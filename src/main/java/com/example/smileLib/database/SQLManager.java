package com.example.smileLib.database;


import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * SQLManager 提供統一的資料庫操作方法，不僅支持直接執行 SQL 語句，
 * 還支持透過簡單的 API 執行常見的查詢操作，如利用 primary key 查詢特定欄位的數據，
 * 並將結果轉換為 Java 物件格式。
 * 核心功能包括：
 * 1. 利用 ConfigManager 從 YML 配置文件中載入資料庫連線資訊，轉換成 DBConfig 對象。
 * 2. 提供 getConnection() 方法來建立 JDBC 連線。
 * 3. 提供 executeQuery(String query) 方法直接執行原始 SQL 查詢，返回 ResultSet。
 * 4. 提供重載的 executeQuery 方法，用於簡易查詢：
 *    - 根據 table 名稱、primary key 欄位和值以及要查詢的欄位查詢，返回結果 Map&lt;String, Object&gt;。
 *    - 根據 table 名稱、primary key 欄位和值查詢單一欄位，並轉換成指定的 Java 類型。
 */
public class SQLManager {

	// 保存透過建構子注入的 DBConfig 對象
	private DBConfig dbConfig;

	/**
	 * 構造函式，接收一個 DBConfig 參數。
	 * 根據傳入的 DBConfig，SQLManager 將根據 dbType 支持 MySQL 或 SQLite。
	 *
	 * @param dbConfig 資料庫配置對象，必須包含 dbType（例如 "mysql" 或 "sqlite"）以及其他連線資訊。
	 * @throws IllegalArgumentException 若 dbConfig 為 null 則拋出異常。
	 */
	public SQLManager(DBConfig dbConfig) {
		if (dbConfig == null) {
			throw new IllegalArgumentException("DBConfig 不能為 null");
		}
		this.dbConfig = dbConfig;
	}

	/**
	 * 根據 dbConfig 中的設定建立 JDBC 連線。
	 * 若 dbType 為 "mysql"，則連線 URL 為 "jdbc:mysql://host:port/database"，
	 * 若 dbType 為 "sqlite"，則連線 URL 為 "jdbc:sqlite:" + database。
	 *
	 * @return 與資料庫的 Connection 物件。
	 * @throws SQLException 若連線建立失敗則拋出 SQLException。
	 */
	public Connection getConnection() throws SQLException {
		String dbType = dbConfig.getDbType().toLowerCase();
		String jdbcUrl;
		if ("mysql".equals(dbType)) {
			// 組合 MySQL 的 JDBC URL
			jdbcUrl = String.format("jdbc:mysql://%s:%d/%s",
					dbConfig.getHost(), dbConfig.getPort(), dbConfig.getDatabase());
			return DriverManager.getConnection(jdbcUrl, dbConfig.getUsername(), dbConfig.getPassword());
		} else if ("sqlite".equals(dbType)) {
			// 組合 SQLite 的 JDBC URL，這裡的 database 為 SQLite 檔案路徑
			jdbcUrl = "jdbc:sqlite:" + dbConfig.getDatabase();
			return DriverManager.getConnection(jdbcUrl);
		} else {
			throw new UnsupportedOperationException("不支持的資料庫類型：" + dbType);
		}
	}

	/**
	 * 直接執行原始 SQL 查詢語句，返回查詢結果的 ResultSet。
	 * 注意：此方法返回的 ResultSet 不會自動關閉，使用者必須自行管理資源釋放。
	 *
	 * @param query SQL 查詢語句。
	 * @return ResultSet 查詢結果。
	 * @throws SQLException 若查詢過程中發生錯誤則拋出 SQLException。
	 */
	public ResultSet executeQuery(String query) throws SQLException {
		Connection conn = getConnection();
		Statement stmt = conn.createStatement();
		return stmt.executeQuery(query);
	}

	/**
	 * 利用指定的 table、primary key 欄位和值以及查詢的欄位列表，
	 * 執行簡易查詢，返回該筆記錄中各欄位的數據轉換成 Map&lt;String, Object&gt;。
	 *
	 * 範例：
	 *   Map&lt;String, Object&gt; result = executeQuery("users", "id", 123, "name", "age", "email");
	 *
	 * @param table            要查詢的資料表名稱
	 * @param primaryKeyColumn 主鍵欄位名稱
	 * @param primaryKeyValue  主鍵的值
	 * @param columns          要查詢的欄位列表，如果為空則查詢全部欄位 (*)
	 * @return Map&lt;String, Object&gt; 查詢結果的 Map；若沒有資料則返回 null
	 * @throws SQLException 若查詢過程中發生錯誤則拋出 SQLException 異常
	 */
	public Map<String, Object> executeQuery(String table, String primaryKeyColumn, Object primaryKeyValue, String... columns) throws SQLException {
		// 若未指定欄位，則查詢所有欄位
		String columnList = (columns == null || columns.length == 0) ? "*" : String.join(", ", columns);
		// 組合 SQL 語句，並使用 ? 作為主鍵值的佔位符（防止 SQL 注入）
		String sql = String.format("SELECT %s FROM %s WHERE %s = ?", columnList, table, primaryKeyColumn);
		try (Connection conn = getConnection();
		     PreparedStatement pstmt = conn.prepareStatement(sql)) {
			// 設定主鍵值
			pstmt.setObject(1, primaryKeyValue);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (!rs.next()) {
					return null; // 若無記錄則返回 null
				}
				Map<String, Object> result = new HashMap<>();
				ResultSetMetaData meta = rs.getMetaData();
				// 若查詢全部欄位，動態讀取 ResultSetMetaData 中的所有欄位名稱及值
				if (columns == null || columns.length == 0) {
					int columnCount = meta.getColumnCount();
					for (int i = 1; i <= columnCount; i++) {
						String colName = meta.getColumnLabel(i);
						Object value = rs.getObject(i);
						result.put(colName, value);
					}
				} else {
					// 否則，只讀取指定的欄位
					for (String col : columns) {
						Object value = rs.getObject(col);
						result.put(col, value);
					}
				}
				return result;
			}
		}
	}

	/**
	 * 利用指定的 table、primary key 欄位和值查詢單一欄位的資料，
	 * 並將結果轉換為指定的 Java 類型 T。
	 *
	 * 範例：
	 *   Integer age = executeQuery("users", "id", 123, "age", Integer.class);
	 *
	 * @param table            要查詢的資料表名稱
	 * @param primaryKeyColumn 主鍵欄位名稱
	 * @param primaryKeyValue  主鍵的值
	 * @param column           要查詢的欄位名稱
	 * @param clazz            要轉換成的 Java 類型（例如 Integer.class、String.class 等）
	 * @param <T>              泛型參數
	 * @return T 查詢結果轉換後的值，若無資料則返回 null
	 * @throws SQLException 若查詢過程中發生錯誤則拋出 SQLException 異常
	 */
	public <T> T executeQuery(String table, String primaryKeyColumn, Object primaryKeyValue, String column, Class<T> clazz) throws SQLException {
		Map<String, Object> resultMap = executeQuery(table, primaryKeyColumn, primaryKeyValue, column);
		if (resultMap == null || !resultMap.containsKey(column)) {
			return null;
		}
		Object value = resultMap.get(column);
		return clazz.cast(value);
	}

	/**
	 * 執行原始 SQL 更新語句（例如 INSERT、UPDATE、DELETE），返回受影響的行數。
	 * 此方法自動使用 try-with-resources 來關閉連線與 Statement。
	 *
	 * @param query SQL 更新語句
	 * @return 受影響的行數；若更新失敗則返回 -1
	 */
	public int executeUpdate(String query) {
		try (Connection conn = getConnection();
		     Statement stmt = conn.createStatement()) {
			return stmt.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * 利用指定的 table、primary key 欄位和值以及更新欄位對應的新值，
	 * 執行更新操作，並返回受影響的行數。
	 *
	 * 此方法會自動組合 UPDATE 語句，語句格式如下：
	 *   UPDATE table SET col1 = ?, col2 = ?, ... WHERE primaryKeyColumn = ?
	 *
	 * 使用 PreparedStatement 以防止 SQL 注入，同時自動管理資源。
	 *
	 * @param table            要更新的資料表名稱
	 * @param primaryKeyColumn 主鍵欄位名稱，用於定位要更新的記錄
	 * @param primaryKeyValue  主鍵的值，定位具體記錄
	 * @param updateColumns    一個 Map，其中鍵為要更新的欄位名稱，值為新值
	 * @return 受影響的行數；若更新失敗則拋出 SQLException 異常
	 * @throws SQLException 若在更新過程中發生錯誤則拋出 SQLException
	 */
	public int executeUpdate(String table, String primaryKeyColumn, Object primaryKeyValue, Map<String, Object> updateColumns) throws SQLException {
		// 如果 updateColumns 為空，則無法進行更新操作，拋出異常
		if (updateColumns == null || updateColumns.isEmpty()) {
			throw new IllegalArgumentException("更新的欄位資料不可為空！");
		}

		// 建立 UPDATE 語句，使用 PreparedStatement 的 ? 作為佔位符
		// 例如：UPDATE table SET col1 = ?, col2 = ? WHERE primaryKeyColumn = ?
		StringBuilder sql = new StringBuilder();
		sql.append("UPDATE ").append(table).append(" SET ");

		// 依序添加每個要更新的欄位及其佔位符
		int count = 0;
		for (String col : updateColumns.keySet()) {
			if (count > 0) {
				sql.append(", ");
			}
			sql.append(col).append(" = ?");
			count++;
		}

		// 添加 WHERE 條件，用於指定哪一筆記錄需要更新
		sql.append(" WHERE ").append(primaryKeyColumn).append(" = ?");

		// 利用 try-with-resources 自動關閉 Connection 與 PreparedStatement
		try (Connection conn = getConnection();
		     PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
			int index = 1;
			// 設置更新欄位的參數值
			for (String col : updateColumns.keySet()) {
				pstmt.setObject(index, updateColumns.get(col));
				index++;
			}
			// 設置主鍵參數值
			pstmt.setObject(index, primaryKeyValue);

			// 執行更新，並返回受影響的行數
			return pstmt.executeUpdate();
		}
	}
}
