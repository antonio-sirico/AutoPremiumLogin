package me.siryq.premiumautologin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.plugin.Plugin;

import javax.crypto.Cipher;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LoginPacketListener {

    private static final Set<String> verifiedPremiumPlayers = ConcurrentHashMap.newKeySet();
    private static final Map<String, byte[]> verifyTokens = new ConcurrentHashMap<>();
    private static KeyPair keyPair;

    public static void register(Plugin plugin) {
        // Generazione chiavi RSA all'avvio
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024);
            keyPair = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) { e.printStackTrace(); }

        // 1. Intercettiamo LOGIN_START
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Login.Client.START) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String name = event.getPacket().getGameProfiles().read(0).getName();

                // Se l'utente è segnato come premium nel nostro config
                if (PremiumAutoLogin.getInstance().isPremium(name)) {
                    sendEncryptionRequest(event);
                }
            }
        });

        // 2. Intercettiamo ENCRYPTION_RESPONSE (La risposta del client alla sfida)
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Login.Client.ENCRYPTION_BEGIN) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String name = event.getPlayer().getName();
                byte[] sharedSecretEncrypted = event.getPacket().getByteArrays().read(0);

                // Decrittazione e verifica con Mojang (Asincrona)
                checkMojangSession(name, sharedSecretEncrypted, event);
            }
        });
    }

    private static void sendEncryptionRequest(PacketEvent event) {
        PacketContainer request = new PacketContainer(PacketType.Login.Server.ENCRYPTION_BEGIN);
        byte[] token = new byte[4];
        new Random().nextBytes(token);
        verifyTokens.put(event.getPlayer().getName(), token);

        request.getStrings().write(0, ""); // ServerID vuoto
        request.getSpecificModifier(PublicKey.class).write(0, keyPair.getPublic());
        request.getByteArrays().write(0, token);

        ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), request);
    }

    private static void checkMojangSession(String name, byte[] encryptedSecret, PacketEvent event) {
        try {
            // Decrittazione del segreto condiviso
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] sharedSecret = cipher.doFinal(encryptedSecret);

            // Calcolo dell'hash per Mojang
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update("".getBytes()); // serverId vuoto
            digest.update(sharedSecret);
            digest.update(keyPair.getPublic().getEncoded());
            String serverHash = new BigInteger(digest.digest()).toString(16);

            // Richiesta ai server Mojang
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + name + "&serverId=" + serverHash);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() == 200) {
                verifiedPremiumPlayers.add(name);
            }
        } catch (Exception e) {
            event.getPlayer().kickPlayer("§cErrore durante la verifica Premium.");
        }
    }

    public static boolean isVerified(String name) { return verifiedPremiumPlayers.contains(name); }
    public static void removeVerified(String name) { verifiedPremiumPlayers.remove(name); verifyTokens.remove(name); }
}