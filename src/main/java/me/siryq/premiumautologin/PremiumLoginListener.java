package me.siryq.premiumautologin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PremiumLoginListener implements Listener {

    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        PremiumAutoLogin.getInstance()
                .getVerifier()
                .handlePreLogin(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PremiumAutoLogin.getInstance()
                .getVerifier()
                .removeVerified(event.getPlayer().getName());
    }
}