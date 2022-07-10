package tech.sobhan;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import static tech.sobhan.Util.convertToJson;

public class WorkspaceThread extends Thread{
    private Socket socketFromClient;
    private Workspace parent;
    private String currentClientUsername;
    private final ArrayList<Client> clients;

    public WorkspaceThread(){
        clients = new ArrayList<>();
    }

    public WorkspaceThread(Socket socketFromClient,Workspace parent){
        this();
        this.socketFromClient = socketFromClient;
        this.parent = parent;
    }


    public void run() {
        try {
            DataInputStream in = new DataInputStream(socketFromClient.getInputStream());
            DataOutputStream out = new DataOutputStream(socketFromClient.getOutputStream());
            String responseFromClient = "";
            while(!responseFromClient.equals("disconnect")){
                responseFromClient = in.readUTF();
                if(currentClientUsername!=null){
                    System.out.print(currentClientUsername + ": ");
                }
                System.out.println(responseFromClient);//
                handleCommand(responseFromClient, in, out, socketFromClient);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCommand(String command, DataInputStream in, DataOutputStream out, Socket socketFromClient) {
        String[] parameters = command.split(" ",2);
        String mainCommand = parameters[0];
        switch (mainCommand) {
            case "connect" -> {
                String token = parameters[1];
                connectClient(token, in, out, socketFromClient);
            }
            case "send-message" -> sendMessage(out, parameters[1]);
            case "get-chats" -> getChats(out);
            case "get-messages" -> getMessages(parameters[1],out);
            case "disconnect" -> disconnectClient(out);
            case "read-messages" -> showUnreadMessages(out);

        }
    }

    private void showUnreadMessages(DataOutputStream out) {
        JSONArray unreadMessages = parent.getUnreadMessagesOf(currentClientUsername);
        try {
            out.writeUTF("OK " + unreadMessages);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disconnectClient(DataOutputStream out) {
        parent.removeUsernameFromConnectedUsernames(currentClientUsername);
        try {
            out.writeUTF("disconnect");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getMessages(String usernameOfOtherClient, DataOutputStream out) {
        JSONArray messages = parent.findMessages(currentClientUsername,usernameOfOtherClient);
        try {
            out.writeUTF("OK " + messages);//todo sus
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getChats(DataOutputStream out) {
        JSONArray chats = parent.findChats(currentClientUsername);
        try {
            out.writeUTF("OK " + chats);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(DataOutputStream out, String messageString) {
        JSONObject message = convertToJson(messageString);
        message.put("from", currentClientUsername);
        addSeq(message);
        System.out.println("from: "+ currentClientUsername +" "+ "to: "+ message.get("to"));
        try {
            out.writeUTF("OK " + message.get("seq"));
            String receiverUsername = (String) message.get("to");
            if(parent.isConnected(receiverUsername)){
                message.put("isRead",true);
                Socket socketToReceiver = parent.findSocketFromUsername(receiverUsername);
                DataOutputStream out2 = new DataOutputStream(socketToReceiver.getOutputStream());
                String commandToReceiver = "receive-message " + message;
                out2.writeUTF(commandToReceiver);
            }else{
                message.put("isRead",false);
            }
            System.out.println("(temp)" + message);
            saveMessages(message, currentClientUsername, receiverUsername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveMessages(JSONObject message, String senderUsername, String receiverUsername) {
        parent.save(message);
        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("name", receiverUsername);
        jsonObject1.put("unread_count", 0);//todo add unread messages
        parent.save(senderUsername,jsonObject1);
        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("name", senderUsername);
        jsonObject2.put("unread_count", 0);//todo add unread messages
        parent.save(receiverUsername,jsonObject2);
    }

    private void addSeq(JSONObject message) {
        if(message.get("seq")==null){
            message.put("seq",1);
        }else{
            int seq = (int) message.get("seq");
            message.put("seq",seq+1);
        }
    }

    private void connectClient(String token, DataInputStream in, DataOutputStream out, Socket socketFromClient) {
        try{
            int idOfClient = requestIdFromServer(token);
            if(parent.findUsername(idOfClient)==null){
                out.writeUTF("username?");
                String usernameOfClient = in.readUTF();
                parent.save(idOfClient,usernameOfClient);
                parent.save(usernameOfClient,socketFromClient);
            }
            out.writeUTF("OK");
            currentClientUsername = parent.findUsername(idOfClient);
            parent.saveConnectedUsername(currentClientUsername);
        }catch(IOException e){
            e.printStackTrace();
        }

    }

//    private Client findClient(int idOfClient) {
//        for(Client client : clients){
//            if(client.getId() == idOfClient){
//                return client;
//            }
//        }
//        return null;
//    }

    private int requestIdFromServer(String token) {
        String whoisRequest = "whois "+ token;
        try {
            Socket socketToServer = parent.getParent().getSocketToServer();
            DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
            DataInputStream in = new DataInputStream(socketToServer.getInputStream());
            out.writeUTF(whoisRequest);
            String responseForWhoisFromServer = in.readUTF();
            System.out.println(responseForWhoisFromServer);//
            if(responseForWhoisFromServer.startsWith("OK")){
                int idOfClient = Integer.parseInt(responseForWhoisFromServer.split(" ")[1]);
                return idOfClient;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
