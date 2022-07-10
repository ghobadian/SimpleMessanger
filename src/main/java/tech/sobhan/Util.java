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
        string = string.replaceAll("},\\{","} , \\{");
        string = string.replace("[","");
        string = string.replace("]","");

        String[] jsonsAsString = string.split(" , ");

        JSONObject[] jsons = new JSONObject[jsonsAsString.length];
        for(int i=0;i<jsonsAsString.length;i++){
            jsons[i] = convertToJson(jsonsAsString[i]);
        }

        return jsons;
    }
}
