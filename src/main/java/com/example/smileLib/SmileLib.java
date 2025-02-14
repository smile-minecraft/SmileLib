package com.example.smileLib;

import org.bukkit.plugin.java.JavaPlugin;

public class SmileLib extends JavaPlugin {
	private static SmileLib instance;

	@Override
	public void onEnable() {
		instance = this;

		getLogger().info("SmileLib 啟動成功！");
	}

	public static SmileLib getInstance() {
		return instance;
	}
}

