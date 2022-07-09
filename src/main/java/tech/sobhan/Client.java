package tech.sobhan;

// A Java program for a Client

import org.json.simple.JSONObject;

import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

import static tech.sobhan.Constants.SERVER_ADDRESS;
import static tech.sobhan.Constants.SERVER_PORT;
import static tech.sobhan.Util.convertToJson;

public class Client{
    private String phoneNumber;
    private String password;
    private int id;
    private String connectedWorkspaceAddress;
    private int connectedWorkspacePort;

    private final Scanner scanner = new Scanner(System.in);
    private Socket socketToWorkspace;

    public Client(String phoneNumber, String password, int id) {
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.id = id;
    }

    public Client() {

    }

    public void run(){
        try(DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream())) {
            String command = "";
            while(!command.equals("disconnect")){
                command= scanner.nextLine();//todo sus
//                command = in.readUTF();
                handleCommand(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//                out.writeUTF(command);
    }

    private void handleCommand(String command) {
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        switch (mainCommand){
            case "register" -> requestRegistering(command);
            case "login" -> requestLogin(command);
            case "create-workspace" -> requestCreateWorkspace(command);
            case "connect" -> requestConnectToWorkspace(command);
            case "send-message" -> requestSendMessage(command);
            case "receive-message" -> waitForMessage();
            case "get-chats" -> requestGetChats();
            case "get-messages" -> requestGetMessages(parameters[1]);
        }
    }

    public void requestGetMessages(String usernameOfOtherClient) {
        try {
            DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
            out.writeUTF("get-messages " + usernameOfOtherClient);
//            String responseChats = in.readUTF();
//            System.out.println(responseChats);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestGetChats() {
        try {
            DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
            out.writeUTF("get-chats");
            String responseChats = in.readUTF();
            System.out.println(responseChats);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitForMessage() {
        try{
            DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
            JSONObject message = convertToJson(in.readUTF().split(" ",2)[1]);
            message.remove("to");
            String senderUsername = (String) message.remove("from");
            if(message.get("type").equals("text")){
                System.out.println(senderUsername +": "+ message.get("body"));
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            String seqResponse = in.readUTF();
            System.out.println(seqResponse);

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
            }else{
                return false;
            }
            responseFromWorkspace = in.readUTF();
            System.out.println(responseFromWorkspace);
            if(responseFromWorkspace.equals("OK")){
                connectedWorkspaceAddress = workspaceAddress;
                connectedWorkspacePort = workspacePort;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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
