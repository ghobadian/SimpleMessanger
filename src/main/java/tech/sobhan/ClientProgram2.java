package tech.sobhan;

import org.json.simple.JSONObject;

import static tech.sobhan.DataGenerator.*;

public class ClientProgram2 {
    public static void main(String[] args){//todo use scanner instead of automation
        Client client = new Client();
        String phoneNumber = generatePhoneNumber();
        String password = generateToken();
//        String workspaceName = "company" + generateOneDigitNumber();
        String workspaceName = "company1";

        client.requestRegistering("register " + phoneNumber + " " + password);
        System.out.println("==============================");
        client.requestLogin("login " + phoneNumber + " " + password);
        System.out.println("==============================");
        if(client.requestConnectToWorkspace("connect-workspace " + workspaceName)){
            JSONObject message = new JSONObject();
            message.put("type","text");
            message.put("body","Salam chetori?");
            client.requestSendMessage("send-message david " + message);
//            client.requestGetChats();
            client.run();
        }
    }
}
