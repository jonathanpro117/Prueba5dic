package eu.darkbot.verifier;

import com.github.manolo8.darkbot.utils.AuthAPI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.jar.JarFile;

/**
 * Simple built-in verifier that keeps authentication local to the machine.
 *
 * <p>This implementation avoids the external {@code verifier.jar} dependency so the
 * bot can boot without Discord or remote checks. It generates a persistent ID the
 * first time it runs and reuses it afterwards.</p>
 */
public class AuthAPIImpl implements AuthAPI {

    private static final Path LOCAL_AUTH_FILE = Paths.get("data", "auth.id");

    private boolean authenticated;
    private String authId;

    @Override
    public synchronized void setupAuth() {
        if (authenticated && authId != null) return;

        try {
            Path parent = LOCAL_AUTH_FILE.getParent();
            if (parent != null) Files.createDirectories(parent);

            if (Files.exists(LOCAL_AUTH_FILE)) {
                authId = Files.readString(LOCAL_AUTH_FILE, StandardCharsets.UTF_8).trim();
                if (authId.isEmpty()) authId = generateAuthId();
            } else {
                authId = generateAuthId();
                Files.writeString(LOCAL_AUTH_FILE, authId, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            authenticated = true;
        } catch (IOException e) {
            System.err.println("[AuthAPI] Failed to initialize local auth: " + e.getMessage());
            authenticated = false;
            authId = null;
        }
    }

    @Override
    public boolean isAuthenticated() {
        if (!authenticated) setupAuth();
        return authenticated;
    }

    @Override
    public boolean isDonor() {
        return isAuthenticated();
    }

    @Override
    public boolean requireDonor() {
        return isAuthenticated();
    }

    @Override
    public String getAuthId() {
        if (!authenticated) setupAuth();
        return authId;
    }

    @Override
    public Boolean checkPluginJarSignature(JarFile jarFile) {
        // Local verifier trusts plugins by default; return true to allow loading.
        return true;
    }

    private String generateAuthId() {
        return "local-" + UUID.randomUUID();
    }
}

