package com.adorsysgis.lightbridge.keycloak.common;

/**
 * Namespaced keys for the user-session notes that the token-exchange layer writes and the
 * protocol mapper reads. Keeping them in one place is the contract between the "smart" layer
 * (which resolves context) and the "dumb" layer (which only copies notes into claims).
 */
public final class LightbridgeSessionNotes {

    /** Prefix applied to every Lightbridge-owned session note to avoid collisions. */
    public static final String NOTE_PREFIX = "lightbridge.";

    /** User-session note holding the resolved account id. */
    public static final String ACCOUNT_ID = NOTE_PREFIX + "account_id";

    /** User-session note holding the resolved project id. */
    public static final String PROJECT_ID = NOTE_PREFIX + "project_id";

    private LightbridgeSessionNotes() {
    }
}
