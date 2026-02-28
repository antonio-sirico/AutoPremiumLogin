package me.siryq.premiumautologin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PremiumLoginListener implements Listener {

    private static final Set<String> verifiedPremiumPlayers = ConcurrentHashMap.newKeySet();

    public static boolean isVerified(String name) {
        return verifiedPremiumPlayers.contains(name.toLowerCase());
    }

    public static void addVerified(String name) {
        verifiedPremiumPlayers.add(name.toLowerCase());
    }

    public static void removeVerified(String name) {
        verifiedPremiumPlayers.remove(name.toLowerCase());
    }

    // ==========================
    // AsyncPlayerPreLoginEvent
    // ==========================
    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {

        String name = event.getName();
        String ip = event.getAddress().getHostAddress();

        if (!PremiumAutoLogin.getInstance().isPremium(name)) return;

        try {
            URI uri = URI.create("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);

            if (connection.getResponseCode() == 200) {

                String savedIP = PremiumAutoLogin.getInstance().getLastIP(name);

                // Primo login o stesso IP
                if (savedIP == null || savedIP.equals(ip)) {

                    PremiumAutoLogin.getInstance().setLastIP(name, ip);
                    addVerified(name);

                    PremiumAutoLogin.getInstance().logDebug(
                            name + " verificato premium con IP: " + ip
                    );

                } else {

                    // IP diverso → probabilmente TLauncher
                    event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            PremiumAutoLogin.getInstance()
                                    .getMessageAsComponent("premium-invalid")
                    );
                }

            } else {
                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        PremiumAutoLogin.getInstance()
                                .getMessageAsComponent("premium-invalid")
                );
            }

        } catch (Exception e) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    PremiumAutoLogin.getInstance()
                            .getMessageAsComponent("mojang-error")
            );
        }
    }

    // ==========================
    // Pulizia cache quando il giocatore esce
    // ==========================
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PremiumLoginListener.removeVerified(event.getPlayer().getName());
    }
}