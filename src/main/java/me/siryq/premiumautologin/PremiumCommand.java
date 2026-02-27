package me.siryq.premiumautologin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PremiumCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Controllo che sia un giocatore
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cComando eseguibile solo dai giocatori in gioco.");
            return true;
        }

        String name = player.getName();

        // Comando /premium
        if (label.equalsIgnoreCase("premium")) {
            if (PremiumAutoLogin.getInstance().isPremium(name)) {
                player.sendMessage("§c[!] Il tuo account è già impostato come Premium.");
                return true;
            }

            // Attiviamo la modalità premium
            PremiumAutoLogin.getInstance().setPremium(name, true);

            player.sendMessage("§a§m---------------------------------------");
            player.sendMessage("§a§lMODALITÀ PREMIUM ATTIVATA");
            player.sendMessage("§fDa ora verrai loggato automaticamente.");
            player.sendMessage("§eAssicurati di usare un account Microsoft!");
            player.sendMessage("§a§m---------------------------------------");
            return true;
        }

        // Comando /sp
        if (label.equalsIgnoreCase("sp")) {
            if (!PremiumAutoLogin.getInstance().isPremium(name)) {
                player.sendMessage("§c[!] Sei già in modalità SP (Manuale).");
                return true;
            }

            // Disattiviamo la modalità premium
            PremiumAutoLogin.getInstance().setPremium(name, false);

            player.sendMessage("§6§m---------------------------------------");
            player.sendMessage("§6§lMODALITÀ SP RIPRISTINATA");
            player.sendMessage("§fDovrai inserire la password al prossimo login.");
            player.sendMessage("§6§m---------------------------------------");
            return true;
        }

        return true;
    }
}