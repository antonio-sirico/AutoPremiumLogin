package me.siryq.premiumautologin;

import org.bukkit.plugin.java.JavaPlugin;

public final class PremiumAutoLogin extends JavaPlugin {

    private static PremiumAutoLogin instance;

    @Override
    public void onEnable() {
        instance = this;

        // Verifica che AuthMe sia effettivamente caricato
        if (getServer().getPluginManager().getPlugin("AuthMe") == null) {
            getLogger().severe("AuthMe non trovato! Disabilitazione di PremiumAutoLogin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registra il listener per gestire l'ingresso dei giocatori
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);

        getLogger().info("PremiumAutoLogin abilitato con successo!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PremiumAutoLogin disabilitato.");
    }

    public static PremiumAutoLogin getInstance() {
        return instance;
    }
}