package tech.sobhan;

import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import tech.sobhan.workspace.Workspace;
import tech.sobhan.workspace.WorkspaceThread;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static tech.sobhan.server.Server.SERVER_ADDRESS;
import static tech.sobhan.server.Server.SERVER_PORT;
import static tech.sobhan.utils.Util.convertToJSON;

public class WorkspaceThreadTest {
    @Test
    @SneakyThrows
    public void addSeqTest(){
        Socket socket = new Socket(SERVER_ADDRESS,SERVER_PORT);
        Workspace workspace = Workspace.builder().workspaceName("name").port(1234)
                .address("address").socketToServer(socket).build();
        JSONObject message = convertToJSON("{\"type\":\"text\",\"body\":\"khub\"}");
        System.out.println(message);
        assert message != null;
        workspace.addSeq(message);
        workspace.addSeq(message);
        workspace.addSeq(message);
        System.out.println(message);
    }

    @Test
    public void connectClientTest(){
        WorkspaceThread workspaceThreadMock = Mockito.mock(WorkspaceThread.class);
        Workspace workspaceMock = Mockito.mock(Workspace.class);
        when(workspaceThreadMock.getParent()).thenReturn(workspaceMock);
        when(workspaceThreadMock.requestIdFromServer(anyString())).thenReturn(1234);
        when(workspaceThreadMock.getParent().findUsername(anyInt())).thenReturn("mmd");
        workspaceThreadMock.connectClient("");
        assertEquals(workspaceThreadMock.getCurrentClientUsername(), "mmd");
        assertTrue(workspaceThreadMock.getParent().getConnectedUsernames().contains("mmd"));

    }
}
