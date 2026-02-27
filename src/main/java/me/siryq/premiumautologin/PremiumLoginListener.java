package me.siryq.premiumautologin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PremiumLoginListener implements Listener {

    private static final Set<String> verifiedPremiumPlayers = ConcurrentHashMap.newKeySet();

    public static boolean isVerified(String name) {
        return verifiedPremiumPlayers.contains(name.toLowerCase());
    }

    public static void removeVerified(String name) {
        verifiedPremiumPlayers.remove(name.toLowerCase());
    }

    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {

        String name = event.getName();

        if (!PremiumAutoLogin.getInstance().isPremium(name)) {
            return; // giocatore SP normale
        }

        try {
            String url = "https://api.mojang.com/users/profiles/minecraft/" + name;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);

            if (connection.getResponseCode() == 200) {
                verifiedPremiumPlayers.add(name.toLowerCase());
                PremiumAutoLogin.getInstance().getLogger().info(name + " verificato come premium.");
            } else {
                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        PremiumAutoLogin.getInstance().getMessageAsComponent("premium-invalid")
                );
            }

        } catch (Exception e) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    PremiumAutoLogin.getInstance().getMessageAsComponent("mojang-error")
            );
        }
    }
}