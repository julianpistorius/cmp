package de.skuzzle.tally.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class UserIdTest {

    @Test
    void testFromLegacyIdWithoutPrefix() throws Exception {
        final String idString = UUID.randomUUID().toString();
        final UserId userId = UserId.fromLegacyStringId(idString);
        assertThat(userId.isAnonymous()).isTrue();
        assertThat(userId.getSource()).isEqualTo("unknown");
        assertThat(userId.getId()).isEqualTo(idString);
    }

    @Test
    void testFromLegacyId() throws Exception {
        final UserId userId = UserId.fromLegacyStringId("google:foo@gmail.com");
        assertThat(userId.isAnonymous()).isFalse();
        assertThat(userId.getSource()).isEqualTo("google");
        assertThat(userId.getId()).isEqualTo("foo@gmail.com");
    }
}
