package tech.sobhan.host;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HostTest {

    @Test
    void requestCreatingHost() {
        final Host hostMock = mock(Host.class);
        final Socket socketMock = mock(Socket.class);
        AtomicInteger i = new AtomicInteger(0);
        String[] messages = {"OK 12345", "OK"};
        hostMock.setSocketToServer(socketMock);

        when(hostMock.receiveSignalFromServer(any())).thenAnswer(input -> messages[i.getAndIncrement()]);
        when(hostMock.requestCode(anyInt())).thenReturn("OK c0de");
        when(hostMock.requestCreatingHost(anyString())).thenCallRealMethod();


        Assertions.assertTrue(hostMock.requestCreatingHost("adsf"));

        System.out.println("==============================");
        i.set(0);

        when(hostMock.receiveSignalFromServer(any())).thenReturn("ERROR code is not available");
        Assertions.assertFalse(hostMock.requestCreatingHost("adsf"));

    }
}