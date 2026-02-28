package me.siryq.premiumautologin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class PremiumAutoLogin extends JavaPlugin {
    private static PremiumAutoLogin instance;
    private File dataFile;

    // Mappa principale: Nome Giocatore -> Stato Premium
    private Map<String, Boolean> premiumPlayersMap;

    private FileConfiguration langConfig;
    private String language;
    private boolean debug;
    private boolean autoLoginMessage;
    private PremiumVerifier verifier;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Configurazione base
        saveDefaultConfig();
        loadSettings();

        // 2. Inizializzazione Dati e Lingua
        premiumPlayersMap = new HashMap<>();
        setupDataFile();
        setupLangFile();

        // 3. Controllo Dipendenze (ProtocolLib e AuthMe)
        if (!checkDependencies()) return;

        // 4. Inizializzazione Verifier (Modalità Standalone con Crittografia)
        String proxyMode = getConfig().getString("proxy-mode", "auto");
        if (proxyMode.equalsIgnoreCase("velocity")) {
            verifier = new VelocityVerifier();
        } else {
            // StandaloneVerifier ora gestisce la crittografia RSA via ProtocolLib
            verifier = new StandaloneVerifier(this);
        }

        getLogger().info("Sistema di verifica attivo: " + verifier.getClass().getSimpleName());

        // 5. Registrazione Comandi e Listener
        registerCommands();
        registerListeners();

        getLogger().info("PremiumAutoLogin caricato con successo per la versione 1.21.1!");
    }

    private void loadSettings() {
        language = getConfig().getString("language", "it");
        debug = getConfig().getBoolean("settings.debug", false);
        autoLoginMessage = getConfig().getBoolean("settings.auto-login-message", true);
    }

    private boolean checkDependencies() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("--- ERRORE CRITICO ---");
            getLogger().severe("ProtocolLib non trovato! Questo plugin è necessario per la crittografia standard.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        if (getServer().getPluginManager().getPlugin("AuthMe") == null) {
            getLogger().severe("--- ERRORE CRITICO ---");
            getLogger().severe("AuthMe non trovato! Il login automatico non potrà funzionare.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    private void registerCommands() {
        PremiumCommand premiumCommand = new PremiumCommand();
        if (getCommand("premium") != null) getCommand("premium").setExecutor(premiumCommand);
        if (getCommand("sp") != null) getCommand("sp").setExecutor(premiumCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PremiumLoginListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
    }

    /**
     * Gestione persistenza dati (File .dat)
     */
    private void setupDataFile() {
        File folder = getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) getLogger().warning("Impossibile creare la cartella.");

        dataFile = new File(folder, "premium_data.dat");
        if (!dataFile.exists()) return;

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(dataFile))) {
            Object obj = in.readObject();
            if (obj instanceof Map<?, ?> map) {
                map.forEach((k, v) -> {
                    if (k instanceof String key && v instanceof Boolean value)
                        premiumPlayersMap.put(key.toLowerCase(), value);
                });
            }
            getLogger().info("Caricati " + premiumPlayersMap.size() + " utenti premium dal database.");
        } catch (IOException | ClassNotFoundException e) {
            getLogger().log(Level.SEVERE, "Errore nel caricamento del file premium_data.dat", e);
        }
    }

    public void savePremiumData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            out.writeObject(premiumPlayersMap);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Errore durante il salvataggio dei dati premium.", e);
        }
    }

    /**
     * API interne per gli altri componenti
     */
    public boolean isPremium(String name) {
        if (name == null) return false;
        return premiumPlayersMap.getOrDefault(name.toLowerCase(), false);
    }

    public void setPremium(String name, boolean status) {
        if (name == null) return;
        premiumPlayersMap.put(name.toLowerCase(), status);
        savePremiumData();
    }

    public PremiumVerifier getVerifier() {
        return verifier;
    }

    public static PremiumAutoLogin getInstance() {
        return instance;
    }

    /**
     * Gestione messaggi e traduzioni
     */
    public void sendMessage(CommandSender sender, String path) {
        if (path.equals("premium-login-success") && !autoLoginMessage) return;

        List<String> lines = langConfig.getStringList("messages." + path);
        if (lines.isEmpty()) {
            String singleLine = langConfig.getString("messages." + path);
            if (singleLine == null) return;
            lines = List.of(singleLine);
        }

        for (String line : lines) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }
    }

    public Component getMessageAsComponent(String path) {
        List<String> lines = langConfig.getStringList("messages." + path);
        if (lines.isEmpty()) {
            String singleLine = langConfig.getString("messages." + path);
            if (singleLine != null) return LegacyComponentSerializer.legacyAmpersand().deserialize(singleLine);
            return Component.text("Messaggio mancante: " + path);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(String.join("\n", lines));
    }

    public void logDebug(String msg) {
        if (debug) getLogger().info("[DEBUG] " + msg);
    }

    private void setupLangFile() {
        String fileName = "lang_" + language + ".yml";
        File langFile = new File(getDataFolder(), fileName);
        if (!langFile.exists()) saveResource(fileName, false);
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }
}