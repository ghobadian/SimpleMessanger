package tech.sobhan;

import org.json.simple.JSONObject;

import static tech.sobhan.DataGenerator.*;

public class ClientProgram {
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
//        client.requestCreateWorkspace("create-workspace " + workspaceName);
//        System.out.println("==============================");
        if(!client.requestConnectToWorkspace("connect-workspace " + workspaceName)){
            return;
        }
//send-message david {"type":"text","body":"salam chetori"}
//send-message sadegh {"type":"text","body":"Salam khubam"}
        client.run();
    }
}

