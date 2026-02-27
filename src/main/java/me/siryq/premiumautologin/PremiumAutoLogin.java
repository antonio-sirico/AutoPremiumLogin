package me.siryq.premiumautologin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;

public final class PremiumAutoLogin extends JavaPlugin {
    private static PremiumAutoLogin instance;
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        instance = this;
        setupDataFile();

        if (getServer().getPluginManager().getPlugin("AuthMe") == null ||
                getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("Dipendenze mancanti!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        LoginPacketListener.register(this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        // Registra qui anche il comando /premium (ricordati di aggiungerlo nel plugin.yml)
    }

    private void setupDataFile() {
        dataFile = new File(getDataFolder(), "premium_data.yml");
        if (!dataFile.exists()) {
            getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean isPremium(String name) {
        return dataConfig.getBoolean("players." + name.toLowerCase(), false);
    }

    public void setPremium(String name, boolean status) {
        dataConfig.set("players." + name.toLowerCase(), status);
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public static PremiumAutoLogin getInstance() { return instance; }
}