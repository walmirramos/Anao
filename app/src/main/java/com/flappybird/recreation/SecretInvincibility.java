package com.flappybird.recreation;

/** Shared only for this running app process; never restored from settings or profiles. */
final class SecretInvincibility {
    private static volatile boolean enabled;

    private SecretInvincibility() {}

    static boolean isEnabled() { return enabled; }

    static synchronized boolean toggle() {
        enabled=!enabled;
        return enabled;
    }
}
