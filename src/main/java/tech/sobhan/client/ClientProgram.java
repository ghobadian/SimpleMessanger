package tech.sobhan.client;

import static tech.sobhan.utils.DataGenerator.*;

public class ClientProgram {
    public static void main(String[] args){
        Client client = new Client();
        String phoneNumber = generatePhoneNumber();
        String password = generateToken();
        client.handleCommand("register " + phoneNumber + " " + password);
        client.handleCommand("login " + phoneNumber + " " + password);
        client.handleCommand("create-workspace company1");
//        client.handleCommand("connect-workspace company1");
//        client.handleCommand("create-group group1");
        client.run();
    }
}


