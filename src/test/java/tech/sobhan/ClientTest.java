package tech.sobhan;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import tech.sobhan.client.Client;
import tech.sobhan.utils.Util;

import java.net.Socket;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

class ClientTest {
    private static final MockedStatic<Util> utilMock = Mockito.mockStatic(Util.class);
    private static final Client client = mock(Client.class);

    private static final Socket socketMock = mock(Socket.class);
    @SneakyThrows
    @BeforeAll
    public static void init(){
        client.setLoggedIn(true);
        client.setSocketToWorkspace(null);
//        when(client.requestTokenFromServer(anyString())).thenReturn("OK 127.4.5.8 1234 abcdefghijk");
        utilMock.when(() -> Util.receiveSignal(socketMock)).thenReturn("salam");
    }


    @Test
    void requestConnectToWorkspace() {

        boolean shouldBeTrue = client.connectToWorkspace("OK 127.4.5.8 1234 abcdefghijk");
//        boolean shouldBeFalse = client.connectToWorkspace("OK 1234 abcdefghijk");

        assertTrue(shouldBeTrue);
//        assertFalse(shouldBeFalse);
    }
}