package com.github.manolo8.darkbot.utils;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

public interface AuthAPI extends eu.darkbot.api.managers.AuthAPI {

    Path DEFAULT_VERIFIER_PATH = Paths.get("lib", "verifier.jar");
    AuthAPI INSTANCE = createInstance();

    static AuthAPI getInstance() {
        return INSTANCE;
    }

    static AuthAPI createInstance() {
        AuthAPI builtIn = new eu.darkbot.verifier.AuthAPIImpl();

        if (shouldForceBuiltIn()) {
            System.err.println("[AuthAPI] Using built-in verifier as requested via darkbot.verifier.mode=DARKBOT_VERIFIER_MODE");
            return builtIn;
        }

        Path verifierPath = getVerifierPath();
        boolean explicitPath = hasCustomVerifierPath();

        if (Files.exists(verifierPath)) {
            try {
                AuthAPI external = ReflectionUtils.createInstance("eu.darkbot.verifier.AuthAPIImpl", verifierPath);
                return new FallbackAuthAPI(external, builtIn, verifierPath, explicitPath);
            } catch (RuntimeException e) {
                System.err.println("[AuthAPI] Failed to load verifier from " + verifierPath + ": " + e.getMessage());
                if (explicitPath)
                    throw e;
                System.err.println("[AuthAPI] Falling back to built-in verifier implementation.");
            }
        } else if (explicitPath) {
            System.err.println("[AuthAPI] Custom verifier path not found: " + verifierPath);
        } else {
            System.err.println("[AuthAPI] No verifier.jar found at " + verifierPath + ", using built-in verifier implementation.");
        }

        return builtIn;
    }

    /**
     * Returns the path to the verifier implementation.
     * Can be overridden with the system property {@code darkbot.verifier.path}
     * or the environment variable {@code DARKBOT_VERIFIER_PATH}.
     */
    static Path getVerifierPath() {
        String override = System.getProperty("darkbot.verifier.path");
        if (override == null || override.isBlank()) override = System.getenv("DARKBOT_VERIFIER_PATH");

        if (override == null || override.isBlank()) return DEFAULT_VERIFIER_PATH;
        return Paths.get(override);
    }

    /**
     * Returns true when the verifier path was explicitly provided via property or env var.
     */
    static boolean hasCustomVerifierPath() {
        String override = System.getProperty("darkbot.verifier.path");
        if (override == null || override.isBlank()) override = System.getenv("DARKBOT_VERIFIER_PATH");
        return override != null && !override.isBlank();
    }

    /**
     * Allows users to bypass any bundled verifier.jar and use the built-in verifier.
     *
     * Mode can be set to "builtin" via:
     * <ul>
     *     <li>System property {@code -Ddarkbot.verifier.mode=builtin}</li>
     *     <li>Environment variable {@code DARKBOT_VERIFIER_MODE=builtin}</li>
     * </ul>
     */
    static boolean shouldForceBuiltIn() {
        String mode = System.getProperty("darkbot.verifier.mode");
        if (mode == null || mode.isBlank()) mode = System.getenv("DARKBOT_VERIFIER_MODE");
        return mode != null && mode.equalsIgnoreCase("builtin");
    }

    /**
     * Sets up initial auth. Some environments (like servers) may have
     * less user friction by not requiring discord authentication.
     */
    void setupAuth();

    /**
     * Returns if the user has been validly authenticated with discord
     * @return true if auth was performed & valid, false otherwise.
     */
    boolean isAuthenticated();

    /**
     * If the user didn't authenticate beforehand, it will prompt the user to authenticate.
     * @return true if the user is a donor in the official darkbot discord server, false otherwise.
     */
    boolean isDonor();

    /**
     * If the user didn't authenticate beforehand, it will prompt the user to authenticate.
     * Will prompt the user to join the discord & donate if he hasn't done so yet.
     * @return true if the user is a donor in the official darkbot discord server, false otherwise.
     */
    boolean requireDonor();

    /**
     * Returns a unique id for the player, the only guarantee is the same player will keep the same id
     * as long as the auth method is the same, and that no 2 players will share it.
     * @return A unique string representing this user, null if no auth was done.
     */
    @Nullable
    String getAuthId();

    /**
     * Checks if a jar file has been signed by an authorized key
     * @param jarFile The jar file to check
     * @throws IOException If the file can't be found or read.
     * @return true if signed & known signature, null if not signed, false if signed by untrusted key.
     */
    Boolean checkPluginJarSignature(JarFile jarFile) throws IOException;

    /**
     * Decorates an AuthAPI and falls back to the built-in verifier when the external one
     * rejects the build (for example, unsigned JARs).
     */
    class FallbackAuthAPI implements AuthAPI {

        private final AuthAPI builtIn;
        private final Path path;
        private final boolean explicitPath;

        private volatile AuthAPI active;

        public FallbackAuthAPI(AuthAPI external, AuthAPI builtIn, Path path, boolean explicitPath) {
            this.active = external;
            this.builtIn = builtIn;
            this.path = path;
            this.explicitPath = explicitPath;
        }

        private AuthAPI fallback(Throwable reason) {
            if (active == builtIn) return builtIn;

            synchronized (this) {
                if (active == builtIn) return builtIn;
                System.err.println("[AuthAPI] External verifier from " + path + " rejected this build: " + reason.getMessage());
                if (!explicitPath) System.err.println("[AuthAPI] Switching to built-in verifier.");
                active = builtIn;
                return active;
            }
        }

        private <T> T withFallback(AuthFunction<T> supplier) {
            AuthAPI current = active;
            try {
                return supplier.get(current);
            } catch (SecurityException e) {
                AuthAPI fallback = fallback(e);
                return supplier.get(fallback);
            }
        }

        private <T> T withFallbackIo(AuthIoFunction<T> supplier) throws IOException {
            AuthAPI current = active;
            try {
                return supplier.get(current);
            } catch (SecurityException e) {
                AuthAPI fallback = fallback(e);
                return supplier.get(fallback);
            }
        }

        @Override
        public void setupAuth() {
            withFallback(api -> {
                api.setupAuth();
                return null;
            });
        }

        @Override
        public boolean isAuthenticated() {
            return withFallback(AuthAPI::isAuthenticated);
        }

        @Override
        public boolean isDonor() {
            return withFallback(AuthAPI::isDonor);
        }

        @Override
        public boolean requireDonor() {
            return withFallback(AuthAPI::requireDonor);
        }

        @Override
        public String getAuthId() {
            return withFallback(AuthAPI::getAuthId);
        }

        @Override
        public Boolean checkPluginJarSignature(JarFile jarFile) throws IOException {
            return withFallbackIo(api -> api.checkPluginJarSignature(jarFile));
        }

        @FunctionalInterface
        private interface AuthFunction<T> {
            T get(AuthAPI api);
        }

        @FunctionalInterface
        private interface AuthIoFunction<T> {
            T get(AuthAPI api) throws IOException;
        }
    }

}
