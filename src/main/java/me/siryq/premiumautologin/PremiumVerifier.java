package me.siryq.premiumautologin;

import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public interface PremiumVerifier {

    void handlePreLogin(AsyncPlayerPreLoginEvent event);

    boolean isVerified(String name);

    void removeVerified(String name);
}