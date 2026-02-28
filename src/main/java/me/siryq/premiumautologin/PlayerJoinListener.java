package me.siryq.premiumautologin;

import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (PremiumAutoLogin.getInstance().getVerifier().isVerified(player.getName())) {
            Bukkit.getScheduler().runTaskLater(PremiumAutoLogin.getInstance(), () -> {
                if (player.isOnline()) {
                    AuthMeApi.getInstance().forceLogin(player);
                    PremiumAutoLogin.getInstance().sendMessage(player, "premium-login-success");
                }
            }, 1L);
        }
    }
}