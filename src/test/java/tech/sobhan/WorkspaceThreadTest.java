package tech.sobhan;

import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.net.Socket;

import static tech.sobhan.Constants.SERVER_ADDRESS;
import static tech.sobhan.Constants.SERVER_PORT;
import static tech.sobhan.Util.convertToJSON;

public class WorkspaceThreadTest {
    @Test
    @SneakyThrows
    public void addSeqTest(){
        Socket socket = new Socket(SERVER_ADDRESS,SERVER_PORT);
        Workspace workspace = new Workspace("name",1234, "address", socket);
        JSONObject message = convertToJSON("{\"type\":\"text\",\"body\":\"khub\"}");
        System.out.println(message);
        workspace.addSeq(message);
        workspace.addSeq(message);
        workspace.addSeq(message);
        System.out.println(message);
    }
}
