package tech.sobhan;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

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
}
