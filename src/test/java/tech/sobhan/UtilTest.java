package tech.sobhan;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static tech.sobhan.Util.convertToJSON;
import static tech.sobhan.Util.convertFromStringToArrayOfJSONObjects;

public class UtilTest {
    @Test
    public void convertToJsonTest(){
        JSONObject json = new JSONObject();
        json.put("type","text");
        json.put("body","salam kako");

        String string = json.toString();
        System.out.println(string);
        assertEquals(json, convertToJSON(string));
    }

    @Test
    public void convertToJsonTest2(){
        JSONObject json1 = new JSONObject();
        json1.put("type","text");
        json1.put("body","salam kako");

        JSONObject json2 = new JSONObject();
        json2.put("type","text");
        json2.put("body","salam kako");

        JSONObject json3 = new JSONObject();
        json3.put("type","text");
        json3.put("body","salam kako");

        JSONArray arr = new JSONArray();
        arr.add(json1);
        arr.add(json2);
        arr.add(json3);

        String string = arr.toString();
        System.out.println(string);
        System.out.println(Arrays.toString(convertFromStringToArrayOfJSONObjects(arr.toString())));
    }
}
