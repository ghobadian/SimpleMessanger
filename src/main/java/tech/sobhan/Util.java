package tech.sobhan;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Arrays;


public class Util {
    public static JSONObject convertToJson(String string) {
        JSONParser parser = new JSONParser();
        try {
            return (JSONObject) parser.parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject[] convertToJson(JSONArray jsonArray){
        String string = jsonArray.toJSONString();
        string = string.substring(1,string.length()-1);
//        System.out.println("string = " + string);

        String[] sth = string.split("}(,)\\{");
        sth[0] += "}";
        for(int i = 1 ;i<sth.length -1 ;i++){
            sth[i] = "{" + sth[i] + "}";
        }
        sth[sth.length -1] = "{" + sth[sth.length -1];
//        System.out.println("sth = " + Arrays.toString(sth));//todo error

        JSONObject[] jsons = new JSONObject[sth.length];
        for(int i=0;i<sth.length;i++){
            System.out.println("sth["+i+"] = " + sth[i]);
            jsons[i] = convertToJson(sth[i]);
            System.out.println("jsons["+i+"] = " + jsons[i]);
        }
        return jsons;
    }
}
