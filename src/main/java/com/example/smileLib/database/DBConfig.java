package com.example.smileLib.database;

/**
 * DBConfig 用於封裝資料庫連線的基本配置信息。
 * 除了傳統的 host、port、username、password 與 database，
 * 此外還增加了 dbType 屬性，用於標識資料庫類型（例如 "mysql" 或 "sqlite"）。
 */
public class DBConfig {
	public DBConfig(String dbType) {
		this.dbType = dbType;
	}

	// 資料庫類型，支持 "mysql" 與 "sqlite"
	private String dbType;
	// 資料庫伺服器主機位址（對於 SQLite 此欄位可忽略）
	private String host;
	// 資料庫伺服器端口號（對於 SQLite 此欄位可忽略）
	private int port;
	// 連線使用者名稱（對於 SQLite 此欄位可忽略）
	private String username;
	// 連線密碼（對於 SQLite 此欄位可忽略）
	private String password;
	// 資料庫名稱；對於 SQLite，此值通常為資料庫檔案的路徑
	private String database;

	// Getter 與 Setter 方法
	public String getDbType() {
		return dbType;
	}
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDatabase() {
		return database;
	}
	public void setDatabase(String database) {
		this.database = database;
	}

	@Override
	public String toString() {
		return "DBConfig{" +
				"dbType='" + dbType + '\'' +
				", host='" + host + '\'' +
				", port=" + port +
				", username='" + username + '\'' +
				", password='******'" +
				", database='" + database + '\'' +
				'}';
	}
}
