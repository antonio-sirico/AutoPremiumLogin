package me.siryq.premiumautologin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PremiumCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        PremiumAutoLogin plugin = PremiumAutoLogin.getInstance();

        // --- COMANDO /PREMIUM ---
        if (label.equalsIgnoreCase("premium")) {
            if (!(sender instanceof Player player)) {
                plugin.sendMessage(sender, "only-player");
                return true;
            }

            String name = player.getName();

            // Se è già premium, inutile riattivarlo
            if (plugin.isPremium(name)) {
                plugin.sendMessage(player, "premium-already-enabled");
                return true;
            }

            // Impostiamo lo stato premium nel database locale (.dat)
            plugin.setPremium(name, true);
            plugin.sendMessage(player, "premium-enabled");

            // Log di debug per sicurezza
            plugin.logDebug("Giocatore " + name + " ha attivato la modalità Premium.");
            return true;
        }

        // --- COMANDO /SP ---
        if (label.equalsIgnoreCase("sp")) {

            // Caso 1: /sp (l'utente lo esegue per se stesso)
            if (args.length == 0) {
                if (!(sender instanceof Player player)) {
                    plugin.sendMessage(sender, "sp-name");
                    return true;
                }

                String name = player.getName();
                if (!plugin.isPremium(name)) {
                    plugin.sendMessage(player, "sp-already-enabled"); // Messaggio: "Sei già in modalità SP"
                    return true;
                }

                plugin.setPremium(name, false);
                plugin.sendMessage(player, "sp-enabled");
                return true;
            }

            // Caso 2: /sp <giocatore> (Admin o Console)
            if (args.length == 1) {
                if (!sender.hasPermission("premiumautologin.admin") && sender instanceof Player) {
                    plugin.sendMessage(sender, "sp-other-nopermission");
                    return true;
                }

                String targetName = args[0];
                if (!plugin.isPremium(targetName)) {
                    plugin.sendMessage(sender, "sp-error-notpremium");
                    return true;
                }

                plugin.setPremium(targetName, false);
                plugin.sendMessage(sender, "sp-other-success");
                return true;
            }

            // Caso default: utilizzo errato
            plugin.sendMessage(sender, "sp-other-usage");
            return true;
        }

        return true;
    }
}