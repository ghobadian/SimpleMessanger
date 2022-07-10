package tech.sobhan;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Workspace {
    private String name;
    private int port;
    private Host parent;
    private final HashMap<String,Socket> usernameAndSocket;
    private final HashMap<String,JSONArray> usernameAndChats;
    private final HashMap<Integer, String> idAndUsername;
    private final ArrayList<JSONObject> messages;
    private final ArrayList<String> connectedUsernames;

    private Workspace(){
        usernameAndSocket = new HashMap<>();
        usernameAndChats = new HashMap<>();
        messages = new ArrayList<>();
        idAndUsername = new HashMap<>();
        connectedUsernames = new ArrayList<>();
    }

    public Workspace(String name, int port, Host parent) {
        this();
        this.name = name;
        this.port = port;
        this.parent = parent;
    }

    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("workspace "+ name + " started");
            while(true){//todo disconnect
                System.out.println("====================");
                System.out.println("Waiting for a Client ...");
                Socket socketFromClient = serverSocket.accept();
                System.out.println("Client accepted");
                WorkspaceThread workspaceThread = new WorkspaceThread(socketFromClient,this);
                workspaceThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket findSocketFromUsername(String username) {
        return usernameAndSocket.get(username);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Host getParent() {
        return parent;
    }

    public void setParent(Host parent) {
        this.parent = parent;
    }

    public void save(String usernameOfClient, Socket socketFromClient) {
        usernameAndSocket.put(usernameOfClient, socketFromClient);
    }

    public void save(String username, JSONObject chat) {
        if(usernameAndChats.get(username)==null){
            usernameAndChats.put(username, new JSONArray());
        }
        usernameAndChats.get(username).add(chat);
    }

    public void save(JSONObject message){//todo add isRead to message
        messages.add(message);
    }

    public JSONArray findChats(String username) {
        JSONArray output = usernameAndChats.get(username);
        return output != null ? output : new JSONArray();
    }

//    public JSONObject findMessage(String sender,String receiver){
//        if(sender==null){
//            for(JSONObject message : messages){
//                if(message.get())
//            }
//        }
//    }

    public JSONArray findMessages(String username1, String username2){
        JSONArray output = new JSONArray();
        for(JSONObject message : messages){
            if(message.get("from").equals(username1) && message.get("to").equals(username2)){
                output.add(message);
            }else if(message.get("from").equals(username2) && message.get("to").equals(username1)){
                output.add(message);
            }
        }
        return output;
    }

    public String findUsername(int idOfClient) {
        return idAndUsername.get(idOfClient);
    }

    public void save(int idOfClient, String usernameOfClient) {
        idAndUsername.put(idOfClient, usernameOfClient);
    }

    public void removeUsernameFromConnectedUsernames(String username) {
        connectedUsernames.remove(username);
    }

    public boolean isConnected(String username) {
        return connectedUsernames.contains(username);
    }

    public void saveConnectedUsername(String username) {
        connectedUsernames.add(username);
    }

    public JSONArray getUnreadMessagesOf(String username) {
        JSONArray output = new JSONArray();
        for(JSONObject message : messages){
            System.out.println("message = " + message);
            System.out.println(message.get("to"));
            System.out.println(username);
            System.out.println(message.get("to").equals(username));
            System.out.println(message.get("isRead").equals(false));
            if(message.get("to").equals(username) && message.get("isRead").equals(false)){
                message.put("isRead",true);
                output.add(message);
            }
        }
        return output;
    }
}
