package me.siryq.premiumautologin;

import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final AuthMeApi authMeApi;

    public PlayerJoinListener() {
        this.authMeApi = AuthMeApi.getInstance();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        // Controlliamo la nostra cache di ProtocolLib
        if (LoginPacketListener.isVerified(name)) {

            if (!authMeApi.isAuthenticated(player)) {
                authMeApi.forceLogin(player);
                player.sendMessage("§a[PremiumAutoLogin] Autenticazione Premium verificata con successo!");
            }

            // Rimuoviamo il giocatore dalla cache per pulizia
            LoginPacketListener.removeVerified(name);
        }
    }
}