package me.siryq.premiumautologin;

import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityVerifier implements PremiumVerifier {

    private final Set<String> verified = ConcurrentHashMap.newKeySet();

    @Override
    public void handlePreLogin(AsyncPlayerPreLoginEvent event) {

        String name = event.getName();

        if (!PremiumAutoLogin.getInstance().isPremium(name)) return;

        verified.add(name.toLowerCase());

        PremiumAutoLogin.getInstance().logDebug(
                name + " verificato tramite Velocity."
        );
    }

    @Override
    public boolean isVerified(String name) {
        return verified.contains(name.toLowerCase());
    }

    @Override
    public void removeVerified(String name) {
        verified.remove(name.toLowerCase());
    }
}