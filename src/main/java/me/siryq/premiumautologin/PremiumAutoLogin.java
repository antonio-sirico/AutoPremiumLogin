package me.siryq.premiumautologin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.io.*;
import java.util.HashMap;
import java.util.logging.Level;

public final class PremiumAutoLogin extends JavaPlugin {
    private static PremiumAutoLogin instance;
    private File dataFile;
    private Map<String, Boolean> premiumPlayersMap;
    private Map<String, String> premiumIPMap;
    private FileConfiguration langConfig;
    private String language;
    private boolean debug;
    private boolean autoLoginMessage;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig(); // <-- CREA config.yml se non esiste
        // Leggi lingua
        this.language = getConfig().getString("language", "it");

        // Leggi settings
        this.debug = getConfig().getBoolean("settings.debug", false);
        this.autoLoginMessage = getConfig().getBoolean("settings.auto-login-message", true);

        setupDataFile();
        setupLangFile();

        if (getServer().getPluginManager().getPlugin("AuthMe") == null) {
            getLogger().severe("AuthMe mancante! Il plugin verrà disabilitato.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PremiumCommand premiumCommand = new PremiumCommand();
        if (getCommand("premium") != null) getCommand("premium").setExecutor(premiumCommand);
        if (getCommand("sp") != null) getCommand("sp").setExecutor(premiumCommand);

        // NUOVO listener
        getServer().getPluginManager().registerEvents(new PremiumLoginListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);

        getLogger().info("PremiumAutoLogin abilitato correttamente!");
    }

    private void setupDataFile() {
        File folder = getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            getLogger().warning("Non è stato possibile creare la cartella del plugin.");
        }

        dataFile = new File(folder, "premium_data.dat");

        // Carica i dati esistenti, se presenti
        if (dataFile.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(dataFile))) {

                Object obj = in.readObject();

                if (obj instanceof Map<?, ?> map) {
                    premiumPlayersMap = new HashMap<>();

                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getKey() instanceof String key &&
                                entry.getValue() instanceof Boolean value) {

                            premiumPlayersMap.put(key, value);
                        }
                    }

                    getLogger().info("Dati premium caricati: " + premiumPlayersMap.size() + " giocatori.");
                } else {
                    premiumPlayersMap = new HashMap<>();
                }

            } catch (IOException | ClassNotFoundException e) {
                getLogger().log(Level.SEVERE, "Errore caricamento dati premium.", e);
                premiumPlayersMap = new HashMap<>();
            }
        }
    }

    public boolean isPremium(String name) {
        if (name == null) return false;
        return premiumPlayersMap.getOrDefault(name.toLowerCase(), false);
    }

    public void setPremium(String name, boolean status) {
        if (name == null) return;
        premiumPlayersMap.put(name.toLowerCase(), status);
        savePremiumData(); // salva immediatamente
    }
    private void setupLangFile() {
        // Usa la lingua letta da config.yml
        String fileName = "lang_" + language + ".yml";
        File langFile = new File(getDataFolder(), fileName);

        if (!langFile.exists()) {
            saveResource(fileName, false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);

        logDebug("Lingua caricata: " + language);
    }
    public void sendMessage(CommandSender sender, String path) {
        // Se il messaggio è premium-login-success e autoLoginMessage è false, salta
        if (path.equals("premium-login-success") && !autoLoginMessage) return;
        List<String> lines = langConfig.getStringList("messages." + path);
        if (lines.isEmpty()) {
            // Proviamo a vedere se c'è una stringa
            String singleLine = langConfig.getString("messages." + path);
            if (singleLine == null) {
                getLogger().info("Messaggio mancante: " + path);
                return;
            }
            lines = List.of(singleLine);
        }

        for (String line : lines) {
            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(line);
            sender.sendMessage(component);
        }
    }
    public Component getMessageAsComponent(String path) {
        List<String> lines = langConfig.getStringList("messages." + path);
        if (lines.isEmpty()) return Component.text("Messaggio mancante: " + path);

        // Unisce tutte le linee in un unico Component separato da \n
        String combined = String.join("\n", lines);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(combined);
    }
    public void logDebug(String msg) {
        if (debug) {
            getLogger().info("[DEBUG] " + msg);
        }
    }
    public void savePremiumData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            out.writeObject(premiumPlayersMap);
            out.writeObject(premiumIPMap);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Errore nel salvataggio dei dati premium", e);
        }
    }
    public void setLastIP(String name, String ip) {
        premiumIPMap.put(name.toLowerCase(), ip);
        savePremiumData();
    }

    public String getLastIP(String name) {
        return premiumIPMap.get(name.toLowerCase());
    }
    public static PremiumAutoLogin getInstance() { return instance; }

}