package com.adorsysgis.lightbridge.keycloak.mapper;

import com.adorsysgis.lightbridge.keycloak.common.LightbridgeSessionNotes;
import org.junit.jupiter.api.Test;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.IDToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LightbridgeContextMapperTest {

    private final LightbridgeContextMapper mapper = new LightbridgeContextMapper();

    @Test
    void copiesSessionNotesIntoClaims() {
        UserSessionModel session = mock(UserSessionModel.class);
        when(session.getNote(LightbridgeSessionNotes.ACCOUNT_ID)).thenReturn("acc-456");
        when(session.getNote(LightbridgeSessionNotes.PROJECT_ID)).thenReturn("proj-789");

        IDToken token = new IDToken();
        mapper.setClaim(token, null, session, null, null);

        assertThat(token.getOtherClaims())
                .containsEntry(LightbridgeContextMapper.ACCOUNT_CLAIM, "acc-456")
                .containsEntry(LightbridgeContextMapper.PROJECT_CLAIM, "proj-789");
    }

    @Test
    void omitsClaimsWhenNotesAbsent() {
        UserSessionModel session = mock(UserSessionModel.class);

        IDToken token = new IDToken();
        mapper.setClaim(token, null, session, null, null);

        assertThat(token.getOtherClaims())
                .doesNotContainKey(LightbridgeContextMapper.ACCOUNT_CLAIM)
                .doesNotContainKey(LightbridgeContextMapper.PROJECT_CLAIM);
    }

    @Test
    void omitsBlankNotes() {
        UserSessionModel session = mock(UserSessionModel.class);
        when(session.getNote(LightbridgeSessionNotes.ACCOUNT_ID)).thenReturn("   ");
        when(session.getNote(LightbridgeSessionNotes.PROJECT_ID)).thenReturn("proj-789");

        IDToken token = new IDToken();
        mapper.setClaim(token, null, session, null, null);

        assertThat(token.getOtherClaims())
                .doesNotContainKey(LightbridgeContextMapper.ACCOUNT_CLAIM)
                .containsEntry(LightbridgeContextMapper.PROJECT_CLAIM, "proj-789");
    }

    @Test
    void isRegisteredForAllOidcTokenTypes() {
        assertThat(mapper.getId()).isEqualTo("lightbridge-context-mapper");
        assertThat(mapper.getProtocol()).isEqualTo("openid-connect");
    }
}
