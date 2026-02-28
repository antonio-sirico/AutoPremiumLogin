package me.siryq.premiumautologin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StandaloneVerifier implements PremiumVerifier {

    private final PremiumAutoLogin plugin;
    private final Set<String> verified = ConcurrentHashMap.newKeySet();
    private final Map<String, String> pendingLogins = new ConcurrentHashMap<>();
    private KeyPair keyPair;

    public StandaloneVerifier(PremiumAutoLogin plugin) {
        this.plugin = plugin;
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024);
            this.keyPair = gen.generateKeyPair();
        } catch (Exception e) {
            plugin.getLogger().severe("Errore RSA (v1.21.11): " + e.getMessage());
        }
        registerListeners();
    }

    private void registerListeners() {
        // LISTENER 1: LOGIN_START (Sync per stabilità 1.21.11)
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                this.plugin,
                ListenerPriority.HIGHEST, // Massima priorità
                PacketType.Login.Client.START
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String name;
                if (event.getPacket().getGameProfiles().size() > 0) {
                    name = event.getPacket().getGameProfiles().read(0).getName();
                } else {
                    name = event.getPacket().getStrings().read(0);
                }

                if (!StandaloneVerifier.this.plugin.isPremium(name)) return;

                String connectionKey = Objects.requireNonNull(event.getPlayer().getAddress()).toString();
                pendingLogins.put(connectionKey, name);

                // CREAZIONE PACKET ENCRYPTION_BEGIN
                PacketContainer request = new PacketContainer(PacketType.Login.Server.ENCRYPTION_BEGIN);
                request.getStrings().write(0, ""); // Server ID
                request.getByteArrays().write(0, keyPair.getPublic().getEncoded());

                byte[] verifyToken = new byte[4];
                new SecureRandom().nextBytes(verifyToken);
                request.getByteArrays().write(1, verifyToken);

                // Invio immediato e cancellazione evento per prevenire desync
                event.setCancelled(true);
                ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), request);

                StandaloneVerifier.this.plugin.logDebug("Handshake crittografato avviato per: " + name);
            }
        });

        // LISTENER 2: ENCRYPTION_RESPONSE (Sync)
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                this.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Login.Client.ENCRYPTION_BEGIN
        ) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String connectionKey = Objects.requireNonNull(event.getPlayer().getAddress()).toString();
                String name = pendingLogins.remove(connectionKey);

                if (name == null) return;

                try {
                    byte[] sharedSecretEnc = event.getPacket().getByteArrays().read(0);

                    Cipher cipher = Cipher.getInstance("RSA");
                    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
                    byte[] sharedSecret = cipher.doFinal(sharedSecretEnc);
                    SecretKey secretKey = new SecretKeySpec(sharedSecret, "AES");

                    String hash = MojangHash.getServerIdHash("", keyPair.getPublic(), secretKey);

                    if (checkMojangSession(name, hash)) {
                        verified.add(name.toLowerCase());
                        StandaloneVerifier.this.plugin.logDebug("Autenticazione Mojang riuscita: " + name);
                    } else {
                        StandaloneVerifier.this.plugin.logDebug("Fallita autenticazione Mojang per: " + name);
                    }
                } catch (Exception e) {
                    StandaloneVerifier.this.plugin.logDebug("Errore critico crittografia: " + e.getMessage());
                }
            }
        });
    }

    private boolean checkMojangSession(String name, String hash) {
        try {
            URL url = java.net.URI.create("https://sessionserver.mojang.com/session/minecraft/hasJoined?username="
                    + name + "&serverId=" + hash).toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void handlePreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.isPremium(event.getName())) return;

        // Se è in lista premium ma non ha passato la sfida crittografica
        if (!isVerified(event.getName())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    plugin.getMessageAsComponent("premium-invalid"));
        }
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