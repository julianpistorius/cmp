package de.skuzzle.cmp.auth;

import static org.mockito.Mockito.when;

import de.skuzzle.cmp.auth.TallyUser;

public class TestUserConfigurer {

    private final TallyUser currentUserMock;

    public TestUserConfigurer(TallyUser currentUser) {
        this.currentUserMock = currentUser;
    }

    public void anonymous() {
        when(currentUserMock.isLoggedIn()).thenReturn(false);
        when(currentUserMock.getName()).thenReturn("unknown");
    }

    public void authenticatedWithName(String name) {
        when(currentUserMock.isLoggedIn()).thenReturn(true);
        when(currentUserMock.getName()).thenReturn(name);
    }
}
