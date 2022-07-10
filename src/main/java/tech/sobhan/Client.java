package tech.sobhan;

// A Java program for a Client

import org.json.simple.JSONObject;

import java.net.*;
import java.io.*;
import java.util.Scanner;

import static tech.sobhan.Constants.SERVER_ADDRESS;
import static tech.sobhan.Constants.SERVER_PORT;
import static tech.sobhan.Util.convertToJson;

public class Client{
    private String phoneNumber;
    private String password;
    private int id;

    private final Scanner scanner = new Scanner(System.in);
    private Socket socketToWorkspace;
    private Thread otherDevicesThread;

    public Client(String phoneNumber, String password, int id) {
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.id = id;
    }

    public Client() {

    }

    public void run(){
        Thread userThread = new Thread(this::getInputFromUser);
        userThread.start();

//        otherDevicesThread = new Thread(this::getInputFromOtherDevices);
//        otherDevicesThread.start();
    }

    private void getInputFromUser() {
        String command;
        while(true){
            command = scanner.nextLine();
            System.out.println("command = " + command);
            handleCommand(command);
            System.out.println("from after handleCommand()");
        }
    }

    private void getInputFromOtherDevices() {
        System.out.println("getInputFromOtherDevices() is opened");
        String response = "";
        try {
            DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
            while(!response.equals("disconnect")){//todo
                try {
                    response = in.readUTF();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(response.startsWith("receive message")){
                    receiveMessage(response.split(" ",2)[1]);
                }else{
                System.out.println(response);
                }
//                handleCommand(response);//todo it is only used for receiving messages therefore useless
            }
            socketToWorkspace = null;
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println("getInputFromOtherDevices() is closed");
    }

    private void handleCommand(String command) {
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        if(isRelatedToWorkspace(mainCommand) && socketToWorkspace==null){
            System.out.println("You are not connected to a workspace");
            return;
        }
        switch (mainCommand){
            case "register" -> requestRegistering(command);
            case "login" -> requestLogin(command);
            case "create-workspace" -> requestCreateWorkspace(command);
            case "connect-workspace" -> requestConnectToWorkspace(command);
            case "send-message" -> requestSendMessage(command);
//            case "receive-message" -> receiveMessage(command.split(" ",2)[1]);
            case "get-chats" -> requestGetChats();
            case "get-messages" -> requestGetMessages(parameters[1]);
            case "read-messages" -> requestSeeUnreadMessages();
            case "disconnect" -> requestDisconnect();
        }
    }

    private void requestSeeUnreadMessages() {
        try {
//            DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
            out.writeUTF("read-messages");
//            String response = in.readUTF();//todo error when i uncomment them
//            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isRelatedToWorkspace(String mainCommand) {
        return mainCommand.equals("send-message") || mainCommand.equals("receive-message") ||
                mainCommand.equals("get-chats") || mainCommand.equals("get-messages") ||
                mainCommand.equals("disconnect");
    }

    private void requestDisconnect() {
        try {
            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
            out.writeUTF("disconnect");
//            socketToWorkspace = null;
//            System.out.println("disconnected from workspace");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void requestGetMessages(String usernameOfOtherClient) {
        try {
            DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
            out.writeUTF("get-messages " + usernameOfOtherClient);
            String response = in.readUTF();
//            System.out.println("i am after (in)");
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestGetChats() {
        try {
            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
            out.writeUTF("get-chats");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessage(String messageAsString) {
        JSONObject message = convertToJson(messageAsString);
        assert message != null;
        message.remove("to");
        String senderUsername = (String) message.remove("from");
        if(message.get("type").equals("text")){
            System.out.println(senderUsername +": "+ message.get("body"));
        }
    }

    public void requestSendMessage(String command) {
        try {
            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
            DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
            String[] parameters = command.split(" ",3);
            String mainCommand = parameters[0];
            String receiverUserName = parameters[1];
            JSONObject message = convertToJson(parameters[2]);
            message.put("to",receiverUserName);
            out.writeUTF(mainCommand + " " + message);
//            String seqResponse = in.readUTF();
//            System.out.println(seqResponse);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }//todo String to Class for token


    public void requestRegistering(String request) {
        try {
            Socket socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
            DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
            DataInputStream in = new DataInputStream(socketToServer.getInputStream());
            out.writeUTF(request);
            String response = in.readUTF();
            System.out.println(response);//
            socketToServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestLogin(String request) {
        try {
            Socket socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
            DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
            DataInputStream in = new DataInputStream(socketToServer.getInputStream());
            out.writeUTF(request);
            String response = in.readUTF();
            System.out.println(response);//
            socketToServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestCreateWorkspace(String request) {
        try(Socket socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
            DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
            DataInputStream in = new DataInputStream(socketToServer.getInputStream())){

            out.writeUTF(request);
            String responseForCreatingWorkspace = in.readUTF();
            System.out.println(responseForCreatingWorkspace);//
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean requestConnectToWorkspace(String request) {
        try{
            String responseForConnectingToWorkSpaceFromServer = requestTokenFromServer(request);
            String[] parameters = responseForConnectingToWorkSpaceFromServer.split(" ");
            String workspaceAddress = parameters[1];
            int workspacePort = Integer.parseInt(parameters[2]);
            String token = parameters[3];

            socketToWorkspace = new Socket(workspaceAddress,workspacePort);
            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
            DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
            out.writeUTF("connect " + token);
            String responseFromWorkspace = in.readUTF();
            System.out.println(responseFromWorkspace);

            if(responseFromWorkspace.equals("username?")){
                String usernameOfClient = scanner.nextLine();
                out.writeUTF(usernameOfClient);
                responseFromWorkspace = in.readUTF();
                System.out.println(responseFromWorkspace);
            }

            if(!responseFromWorkspace.equals("OK")){
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
        otherDevicesThread.start();
//        if(!otherDevicesThread.isAlive()){
//        }
        return true;
    }

    private String requestTokenFromServer(String request) {
        try(Socket socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
             DataInputStream in = new DataInputStream(socketToServer.getInputStream())){

            out.writeUTF(request);
            String responseForConnectingToWorkSpaceFromServer = in.readUTF();
            System.out.println(responseForConnectingToWorkSpaceFromServer);//
            return responseForConnectingToWorkSpaceFromServer;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
