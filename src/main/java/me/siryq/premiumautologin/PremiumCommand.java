package me.siryq.premiumautologin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PremiumCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // Comando /premium (solo per giocatori)
        if (label.equalsIgnoreCase("premium")) {
            if (!(sender instanceof Player player)) {
                PremiumAutoLogin.getInstance().sendMessage(sender,"only-player");
                return true;
            }

            String name = player.getName();
            if (PremiumAutoLogin.getInstance().isPremium(name)) {
                PremiumAutoLogin.getInstance().sendMessage(player, "premium-already-enabled");
                return true;
            }

            PremiumAutoLogin.getInstance().setPremium(name, true);
            PremiumAutoLogin.getInstance().sendMessage(player, "premium-enabled");
            return true;
        }

        // Comando /sp
        if (label.equalsIgnoreCase("sp")) {

            if (args.length == 0) {
                // Modalità normale, per se stessi
                if (!(sender instanceof Player player)) {
                    PremiumAutoLogin.getInstance().sendMessage(sender, "sp-name");
                    return true;
                }

                String name = player.getName();
                if (!PremiumAutoLogin.getInstance().isPremium(name)) {
                    PremiumAutoLogin.getInstance().sendMessage(player, "sp-already-enabled");
                    return true;
                }

                PremiumAutoLogin.getInstance().setPremium(name, false);
                PremiumAutoLogin.getInstance().sendMessage(player, "sp-enabled");
                return true;

            } else if (args.length == 1) {
                // Modalità admin/console, disattivazione premium per un altro giocatore
                if (!(sender.hasPermission("premiumautologin.admin") || !(sender instanceof Player))) {
                    PremiumAutoLogin.getInstance().sendMessage(sender, "sp-other-nopermission");
                    return true;
                }

                String targetName = args[0];
                if (!PremiumAutoLogin.getInstance().isPremium(targetName)) {
                    PremiumAutoLogin.getInstance().sendMessage(sender, "notpremium");
                    return true;
                }

                PremiumAutoLogin.getInstance().setPremium(targetName, false);
                PremiumAutoLogin.getInstance().sendMessage(sender, "sp-other-success");
                return true;

            } else {
                PremiumAutoLogin.getInstance().sendMessage(sender, "sp-other-usage");
                return true;
            }
        }

        return true;
    }
}