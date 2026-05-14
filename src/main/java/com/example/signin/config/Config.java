package com.example.signin.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "signin.json");

    public List<String> servers = new ArrayList<>();
    public int currentServerIndex = 0;
    public List<String> accounts = new ArrayList<>();
    public int currentAccountIndex = 0;
    public String loginCommand = "/login 11111111";
    public String signinCommand = "/signin click";
    public String homeCommand = "/p v 1 1";
    public String quitCommand = "quit"; // 退出游戏命令

    // 新添加的配置项
    public boolean autoLeave = false; // 自动退出服务器功能
    public boolean autoPutItem = true; // 自动放置物品功能，默认开启
    public boolean autoHome = false; // 自动执行回家命令功能
    public boolean autoServer = true; // 自动进入服务器功能
    public boolean autoChangeIp = false; // 自动转IP功能，默认关闭
    public int handSlot = 0; // 开箱时使用的物品栏位 (0-8)

    public static Config load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, Config.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Config cfg = new Config();
        // Provide sensible defaults
        cfg.servers = new ArrayList<>();
        cfg.accounts = new ArrayList<>();
        cfg.autoLeave = false;
        cfg.autoPutItem = true;
        cfg.autoHome = false;
        cfg.autoServer = false;
        cfg.autoChangeIp = false;
        cfg.handSlot = 0;
        cfg.quitCommand = "quit"; // 设置默认退出游戏命令
        return cfg;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentServer() {
        if (servers == null || servers.isEmpty()) {
            return "";
        }
        int idx = Math.max(0, Math.min(currentServerIndex, servers.size() - 1));
        return servers.get(idx);
    }

    public String getCurrentAccount() {
        if (accounts == null || accounts.isEmpty()) {
            return "";
        }
        int idx = Math.max(0, Math.min(currentAccountIndex, accounts.size() - 1));
        return accounts.get(idx);
    }
}
