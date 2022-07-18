package tech.sobhan;

import lombok.SneakyThrows;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import tech.sobhan.host.HostProgram;
import tech.sobhan.server.ServerProgram;
import tech.sobhan.workspace.Workspace;

import java.util.Arrays;

import static tech.sobhan.utils.Util.convertToJSON;

public class WorkspaceTest {
    @Test
    public void JSONArrayTest(){
        JSONArray arr = new JSONArray();
        System.out.println(arr);
        JSONObject obj = new JSONObject();
        obj.put("from","mehdi");
        arr.add(obj);
        System.out.println(arr);
    }

    @Test
    public void duplicateClientsTest(){
        ServerProgram.main(new String[0]);//todo use integration testing
        System.out.println("**************8");
        HostProgram.main(new String[0]);

    }

    @Test
    @SneakyThrows
    public void replaceMessageTest(){
        Workspace workspace = Workspace.builder().workspaceName("name").port(1234)
                .address("address").socketToServer(null).build();
        JSONObject message = convertToJSON("{\"type\":\"text\",\"body\":\"ok\",\"seq\":23}");
        workspace.saveMessage(message);
        System.out.println(Arrays.toString(workspace.getMessages().toArray()));
        workspace.replaceMessage("23","dalpoozak");
        System.out.println(Arrays.toString(workspace.getMessages().toArray()));
    }
}
