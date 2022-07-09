package tech.sobhan;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static tech.sobhan.Util.convertToJson;

public class WorkspaceThread extends Thread{
    private Socket socketFromClient;
//    private Host parent;
    private Workspace parent;
    private final HashMap<Integer, String> idAndUsername;
    private String currentClientUsername;
    private final ArrayList<Client> clients;

    public WorkspaceThread(){
        idAndUsername = new HashMap<>();
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
            while(true){
                String responseFromClient = in.readUTF();
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
            case "get-chats" -> getChats(in,out);
            case "get-messages" -> getMessages(parameters[1],out);
        }
    }

    private void getMessages(String usernameOfOtherClient, DataOutputStream out) {
        JSONArray chats = parent.findChats(currentClientUsername);
        System.out.println("chats = " + chats);
        JSONObject[] jsonArray = convertToJson(chats);
//        System.out.println("jsonArray = " + Arrays.toString(jsonArray));
        JSONArray output = new JSONArray();
        for(JSONObject jsonObject : jsonArray){
            if(jsonObject.get("from").equals(usernameOfOtherClient)){
                output.add(jsonObject);
            }
        }

        try {
            out.writeUTF("OK " + output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getChats(DataInputStream in, DataOutputStream out) {
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

        try {
            out.writeUTF("OK " + message.get("seq"));
            String receiverUsername = (String) message.get("to");
            Socket socketToReceiver = parent.findSocketFromUsername(receiverUsername);
//            DataInputStream in = new DataInputStream(socketToReceiver.getInputStream());
            DataOutputStream out2 = new DataOutputStream(socketToReceiver.getOutputStream());
            String commandToReceiver = "receive-message " + message;
            out2.writeUTF(commandToReceiver);
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("name", receiverUsername);
            jsonObject1.put("unread_count", 0);//todo add unread messages
            parent.save(currentClientUsername,jsonObject1);
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("name", currentClientUsername);
            jsonObject2.put("unread_count", 0);//todo add unread messages
            parent.save(receiverUsername,jsonObject2);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            String usernameOfClient;
            if(idAndUsername.get(idOfClient)==null){
                out.writeUTF("username?");
                usernameOfClient = in.readUTF();
                idAndUsername.put(idOfClient,usernameOfClient);
                currentClientUsername = usernameOfClient;
                out.writeUTF("OK");
            }else{
                return;
            }
            parent.save(usernameOfClient,socketFromClient);
//            usernameAndSocket.put(usernameOfClient,socketFromClient);
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
