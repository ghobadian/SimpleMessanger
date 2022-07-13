package tech.sobhan;

import lombok.Synchronized;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public final class Util {
    public static JSONObject convertToJSON(String string) {
        JSONParser parser = new JSONParser();
        try {
            return (JSONObject) parser.parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject[] convertFromStringToArrayOfJSONObjects(String string){
//        String string = jsonArray.toJSONString();
        string = string.replaceAll("},\\{","} , \\{");
        string = string.replace("[","");
        string = string.replace("]","");

        String[] jsonsAsString = string.split(" , ");

        JSONObject[] jsons = new JSONObject[jsonsAsString.length];
        for(int i=0;i<jsonsAsString.length;i++){
            jsons[i] = convertToJSON(jsonsAsString[i]);
        }

        return jsons;
    }

    public static int[] convertToArray(String string) {
        string = string.replaceAll("\\[","");
        string = string.replaceAll("]","");
        String[] sth = string.split(",");
        return Arrays.stream(sth).mapToInt(a -> Integer.parseInt(a.trim())).toArray();
    }

    @Synchronized
    public static void sendSignal(Socket socket, String signal) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(signal);
//            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Synchronized
    public static String receiveSignal(Socket socket) {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            return in.readUTF();
            //            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
