package com.example.smileLib.logger;

import org.bukkit.ChatColor;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomLogger 是專為 Minecraft Paper Plugin 設計的自定義日誌工具類，具備以下功能：
 * 1. 控制台彩色輸出：利用 Paper API 的 ChatColor 實現不同級別訊息的彩色顯示。
 * 2. 日誌緩存與文件輸出：在運行期間緩存所有日誌，伺服器關閉時自動將緩存內容寫入一個以關閉時間命名的 TXT 文件，
 *    並統一存放在指定的日誌目錄中。
 * 3. Discord Webhook 發送：當出現錯誤訊息時，會使用 GSON 庫將訊息構造成 JSON 負載，透過 HTTP POST 發送到 Discord 指定頻道。
 */
public class CustomLogger {

	// 緩存所有日誌訊息，待伺服器關閉時寫入文件
	private List<String> logBuffer;

	// 日誌文件存放目錄
	private File logDirectory;

	// Discord webhook 的 URL，若未設定可傳入空字串或 null
	private String discordWebhookUrl;

	// 使用 Paper API 的 ChatColor 實現彩色輸出，這裡定義不同級別使用的顏色代碼（以 '&' 作為前綴，稍後轉換）
	private static final String INFO_COLOR = "&a";   // 綠色
	private static final String WARN_COLOR = "&e";   // 黃色
	private static final String ERROR_COLOR = "&c";  // 紅色

	// GSON 實例，用於處理 JSON 資料
	private Gson gson;

	/**
	 * 構造函式
	 *
	 * @param logDirPath        日誌文件存放目錄的路徑，如果目錄不存在會自動建立。
	 * @param discordWebhookUrl Discord webhook 的 URL，用於發送錯誤訊息，若未配置可設為 null 或空字串。
	 */
	public CustomLogger(String logDirPath, String discordWebhookUrl) {
		this.logBuffer = new ArrayList<>();
		this.logDirectory = new File(logDirPath);
		if (!logDirectory.exists()) {
			logDirectory.mkdirs();
		}
		this.discordWebhookUrl = discordWebhookUrl;
		this.gson = new Gson();
		// 添加 JVM 關閉鉤子，當伺服器關閉時自動寫入所有緩存的日誌到文件中
		Runtime.getRuntime().addShutdownHook(new Thread(this::writeLogBufferToFile));
	}

	/**
	 * 輸出 INFO 級別的日誌訊息，控制台以綠色顯示。
	 *
	 * @param message 要輸出的訊息內容
	 */
	public void info(String message) {
		String formatted = formatMessage("INFO", message, INFO_COLOR);
		System.out.println(formatted);
		logBuffer.add(stripColor(formatted));
	}

	/**
	 * 輸出 WARN 級別的日誌訊息，控制台以黃色顯示。
	 *
	 * @param message 要輸出的訊息內容
	 */
	public void warn(String message) {
		String formatted = formatMessage("WARN", message, WARN_COLOR);
		System.out.println(formatted);
		logBuffer.add(stripColor(formatted));
	}

	/**
	 * 輸出 ERROR 級別的日誌訊息，控制台以紅色顯示，同時發送 Discord webhook 訊息。
	 *
	 * @param message 要輸出的訊息內容
	 * @param t       異常對象，可為 null
	 */
	public void error(String message, Throwable t) {
		String formatted = formatMessage("ERROR", message, ERROR_COLOR);
		System.err.println(formatted);
		if (t != null) {
			t.printStackTrace();
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			formatted += "\n" + sw.toString();
		}
		logBuffer.add(stripColor(formatted));
		sendDiscordWebhook(formatted);
	}

	/**
	 * 格式化日誌訊息，添加當前時間、日誌級別與顏色。
	 * 利用 Paper API 的 ChatColor.translateAlternateColorCodes() 將 "&" 前綴轉換為 Minecraft 色碼。
	 *
	 * @param level   日誌級別，如 INFO、WARN、ERROR
	 * @param message 日誌內容
	 * @param color   使用的顏色代碼（例如 "&a"）
	 * @return 格式化後的訊息字串
	 */
	private String formatMessage(String level, String message, String color) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		String rawMessage = String.format("[%s] [%s] %s", timestamp, level, message);
		String coloredMessage = color + rawMessage + "&r";
		return ChatColor.translateAlternateColorCodes('&', coloredMessage);
	}

	/**
	 * 去除訊息中的 Minecraft 色碼（以 § 字符開始），以便寫入文件時使用純文字。
	 *
	 * @param input 包含色碼的訊息字串
	 * @return 去除色碼後的純文字字串
	 */
	private String stripColor(String input) {
		return ChatColor.stripColor(input);
	}

	/**
	 * 將緩存中的所有日誌訊息寫入一個 TXT 文件中，
	 * 文件名稱以伺服器關閉時間命名，並存放於指定的日誌目錄中。
	 */
	private void writeLogBufferToFile() {
		try {
			String shutdownTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			File logFile = new File(logDirectory, "log_" + shutdownTime + ".txt");
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
				for (String log : logBuffer) {
					writer.write(log);
					writer.newLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 發送 Discord webhook 訊息，使用 GSON 將訊息構造成 JSON 負載並以 HTTP POST 發送。
	 * 若未配置 webhook URL 則不進行發送。
	 *
	 * @param content 要發送的訊息內容
	 */
	private void sendDiscordWebhook(String content) {
		if (discordWebhookUrl == null || discordWebhookUrl.isEmpty()) {
			return; // 未配置 webhook URL，直接返回
		}
		try {
			// 利用 GSON 建立 JSON 負載
			JsonObject json = new JsonObject();
			json.addProperty("content", content);
			String payload = gson.toJson(json);
			byte[] postData = payload.getBytes(StandardCharsets.UTF_8);

			URL url = new URL(discordWebhookUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");

			// 將 JSON 負載寫入請求輸出流
			try (OutputStream os = conn.getOutputStream()) {
				os.write(postData);
			}
			int responseCode = conn.getResponseCode();
			// Discord 成功返回 204 (No Content) 或 200
			if (responseCode != 204 && responseCode != 200) {
				System.err.println("Failed to send Discord webhook: HTTP " + responseCode);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
