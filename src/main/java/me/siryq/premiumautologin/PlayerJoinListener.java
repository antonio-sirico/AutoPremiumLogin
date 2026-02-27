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

        // [!] QUI VA INSERITA LA LOGICA DI VERIFICA
        // Poiché siamo in modalità SP, dobbiamo sapere se la sessione
        // è stata validata precedentemente (tramite ProtocolLib o Proxy).
        // Per questo esempio, usiamo un metodo fittizio che dovrai collegare.
        boolean isVerifiedPremiumSession = checkPremiumSession(player);

        if (isVerifiedPremiumSession) {
            // Controlla se il giocatore non è ancora loggato su AuthMe
            if (!authMeApi.isAuthenticated(player)) {

                // Forza il login (se l'utente è già registrato)
                // o lo registra automaticamente e lo logga
                authMeApi.forceLogin(player);

                player.sendMessage("§aAutologin effettuato con successo tramite account Microsoft (Premium)!");
            }
        }
    }

    /**
     * Metodo segnaposto. In un server Offline (SP) reale, la verifica
     * NON può essere fatta solo con il nome. Serve ProtocolLib per gestire i pacchetti
     * di Login o ricevere un forward dal proxy (es. Velocity/BungeeCord).
     */
    private boolean checkPremiumSession(Player player) {
        // TODO: Integrare ProtocolLib o leggere le proprietà del proxy.
        // Se usi Velocity/BungeeCord, spesso il proxy passa l'UUID premium reale,
        // che puoi verificare qui.
        return false;
    }
}