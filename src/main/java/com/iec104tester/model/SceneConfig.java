package com.iec104tester.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 场景配置 —— 保存客户端连接列表与服务端配置，便于复用。
 * 使用 Gson 进行 JSON 序列化/反序列化。
 */
public class SceneConfig {

    private String sceneName;
    private String saveTime;
    private List<ClientEntry> clients = new ArrayList<>();
    private ServerConfig serverConfig;

    /**
     * 单个客户端连接条目：名称 + 连接配置。
     */
    public static class ClientEntry {
        private String name;
        private ConnectionConfig config;

        public ClientEntry() {
        }

        public ClientEntry(String name, ConnectionConfig config) {
            this.name = name;
            this.config = config;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public ConnectionConfig getConfig() { return config; }
        public void setConfig(ConnectionConfig config) { this.config = config; }
    }

    public SceneConfig() {
    }

    public SceneConfig(String sceneName) {
        this.sceneName = sceneName;
    }

    public String getSceneName() { return sceneName; }
    public void setSceneName(String sceneName) { this.sceneName = sceneName; }

    public String getSaveTime() { return saveTime; }
    public void setSaveTime(String saveTime) { this.saveTime = saveTime; }

    public List<ClientEntry> getClients() { return clients; }
    public void setClients(List<ClientEntry> clients) { this.clients = clients; }

    public ServerConfig getServerConfig() { return serverConfig; }
    public void setServerConfig(ServerConfig serverConfig) { this.serverConfig = serverConfig; }

    /** 添加一个客户端连接条目 */
    public void addClient(String name, ConnectionConfig config) {
        clients.add(new ClientEntry(name, config));
    }

    /** 记录当前保存时间戳 */
    public void stampSaveTime() {
        this.saveTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * 将场景配置保存到 JSON 文件。
     */
    public void saveToFile(File file) throws IOException {
        if (saveTime == null || saveTime.isEmpty()) {
            stampSaveTime();
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = new FileWriter(file)) {
            gson.toJson(this, writer);
        }
    }

    /**
     * 从 JSON 文件加载场景配置。
     */
    public static SceneConfig loadFromFile(File file) throws IOException {
        Gson gson = new GsonBuilder().create();
        try (Reader reader = new FileReader(file)) {
            SceneConfig config = gson.fromJson(reader, SceneConfig.class);
            if (config == null) {
                throw new IOException("文件内容为空或格式无效");
            }
            if (config.getClients() == null) {
                config.setClients(new ArrayList<>());
            }
            return config;
        }
    }
}
