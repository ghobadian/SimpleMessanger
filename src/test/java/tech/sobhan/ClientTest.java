package tech.sobhan;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ClientTest {
    private static final Client client = Mockito.mock(Client.class);

    @SneakyThrows
    @BeforeAll
    public static void init(){
        client.setLoggedIn(true);
        when(client.requestTokenFromServer(anyString())).thenReturn("OK 127.4.5.8 1234 abcdefghijk");
    }

    @Test
    void requestConnectToWorkspace() {
        client.connectToWorkspace("OK 127.4.5.8 1234 abcdefghijk");

    }
}