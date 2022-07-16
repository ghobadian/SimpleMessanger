package tech.sobhan;

import lombok.Synchronized;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
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

    public static JSONObject[] convertFromStringToArrayOfJSONObjects(String string) {
        string = string.replaceAll("},\\{", "} , \\{");
        string = string.replace("[", "");
        string = string.replace("]", "");

        String[] jsonsAsString = string.split(" , ");

        JSONObject[] jsons = new JSONObject[jsonsAsString.length];
        for (int i = 0; i < jsonsAsString.length; i++) {
            jsons[i] = convertToJSON(jsonsAsString[i]);
        }

        return jsons;
    }

    public static int[] convertToArray(String string) {
        string = string.replaceAll("\\[", "");
        string = string.replaceAll("]", "");
        String[] sth = string.split(",");
        return Arrays.stream(sth).mapToInt(a -> Integer.parseInt(a.trim())).toArray();
    }

//    private static final Object sendLock = new Object();
//
//    @Synchronized("sendLock")
    public static void sendSignal(Socket socket, String signal) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(signal);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    private static final Object receiveLock = new Object();
//
//    @Synchronized("receiveLock")
    public static String receiveSignal(Socket socket) {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            return in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
