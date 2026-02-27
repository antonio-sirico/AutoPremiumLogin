package me.siryq.premiumautologin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class PremiumAutoLogin extends JavaPlugin {
    private static PremiumAutoLogin instance;
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        instance = this;
        setupDataFile();

        // Controllo dipendenze
        if (getServer().getPluginManager().getPlugin("AuthMe") == null ||
                getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("Dipendenze mancanti (AuthMe o ProtocolLib)! Il plugin verrà disabilitato.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registrazione comandi
        PremiumCommand premiumCommand = new PremiumCommand();
        if (getCommand("premium") != null) getCommand("premium").setExecutor(premiumCommand);
        if (getCommand("sp") != null) getCommand("sp").setExecutor(premiumCommand);

        // Registrazione listener
        LoginPacketListener.register(this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);

        getLogger().info("PremiumAutoLogin abilitato con successo!");
    }

    private void setupDataFile() {
        File folder = getDataFolder();
        if (!folder.exists()) {
            // Usiamo il risultato di mkdirs() per loggare un eventuale fallimento
            if (!folder.mkdirs()) {
                getLogger().warning("Non è stato possibile creare la cartella del plugin (potrebbe già esistere o mancare i permessi).");
            }
        }

        dataFile = new File(folder, "premium_data.yml");
        if (!dataFile.exists()) {
            try {
                if (dataFile.createNewFile()) {
                    getLogger().info("File premium_data.yml creato con successo.");
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Impossibile creare il file premium_data.yml!", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean isPremium(String name) {
        // È buona norma gestire i null per evitare NPE
        if (name == null) return false;
        return dataConfig.getBoolean("players." + name.toLowerCase(), false);
    }

    public void setPremium(String name, boolean status) {
        if (name == null) return;
        dataConfig.set("players." + name.toLowerCase(), status);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            // Logging robusto anche per il salvataggio
            getLogger().log(Level.SEVERE, "Errore durante il salvataggio di premium_data.yml per l'utente: " + name, e);
        }
    }

    public static PremiumAutoLogin getInstance() { return instance; }
}