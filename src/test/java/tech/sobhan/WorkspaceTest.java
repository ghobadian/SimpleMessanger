package tech.sobhan;

import lombok.SneakyThrows;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.Arrays;

import static tech.sobhan.Util.convertToJSON;

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
        Workspace workspace = new Workspace("name",1234, "address", null);
        JSONObject message = convertToJSON("{\"type\":\"text\",\"body\":\"ok\",\"seq\":23}");
        workspace.save(message);
        System.out.println(Arrays.toString(workspace.getMessages().toArray()));
        workspace.replaceMessage("23","dalpoozak");
        System.out.println(Arrays.toString(workspace.getMessages().toArray()));
    }
}
