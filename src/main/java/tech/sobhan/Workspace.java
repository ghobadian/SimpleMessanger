package tech.sobhan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

@AllArgsConstructor
@Builder
public class Workspace /*extends Thread*/ implements Serializable{
    @Getter private String workspaceName;
    @Getter private String address;
    @Getter private int port;
    @Getter private Socket socketToServer;
    private int seq;
    private final HashMap<String,Socket> usernameAndSocket = new HashMap<>();
    private final HashMap<String,JSONArray> usernameAndChats = new HashMap<>();
    private final HashMap<Integer, String> idAndUsername = new HashMap<>();
    @Getter private final ArrayList<JSONObject> messages = new ArrayList<>();
    private final ArrayList<String> connectedUsernames = new ArrayList<>();
    @Getter private final ArrayList<Group> groups = new ArrayList<>();

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("workspace "+ workspaceName + " started");
            while(true){
                acceptClient(serverSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void acceptClient(ServerSocket serverSocket) throws IOException {
        System.out.println("====================");
        System.out.println("Waiting for a Client ...");
        Socket socketFromClient = serverSocket.accept();
        System.out.println("Client accepted");
        WorkspaceThread workspaceThread = WorkspaceThread.builder()
                .socketFromClient(socketFromClient)
                .parent(this).build();
        workspaceThread.start();
    }

    public Socket findSocketFromUsername(String username) {
        return usernameAndSocket.get(username);
    }

    public void saveSocket(String usernameOfClient, Socket socketFromClient) {
        usernameAndSocket.put(usernameOfClient, socketFromClient);
    }

    public void saveChat(String username, JSONObject chat) {
        if(usernameAndChats.get(username)==null){
            usernameAndChats.put(username, new JSONArray());
        }
        usernameAndChats.get(username).add(chat);
    }

    public void saveMessage(JSONObject message){//todo add isRead to message
        messages.add(message);
    }

    public JSONArray findChats(String username) {
        JSONArray output = usernameAndChats.get(username);
        return output != null ? output : new JSONArray();
    }

    public boolean alreadyChatting(String senderUsername, String receiverUsername){
        JSONArray chatsOfSender = usernameAndChats.get(senderUsername);
        for (Object object : chatsOfSender) {
            JSONObject chat = (JSONObject) object;
            if(chat.get("name").equals(receiverUsername)){
                return true;
            }
        }
        return false;
    }

    public JSONObject findChatOf(String username1, String username2){
        for (Object object : findChats(username1)) {
            JSONObject chat = (JSONObject) object;
            if(chat.get("name").equals(username2)){
                return chat;
            }
        }
        return null;
    }

    public JSONArray findMessages(String username1, String username2){
        JSONArray output = new JSONArray();
        for(JSONObject message : messages){
            String sender = String.valueOf(message.get("from"));
            String receiver = String.valueOf(message.get("to"));
            if((sender.equals(username1) && receiver.equals(username2)) ||
                    sender.equals(username2) && receiver.equals(username1)){
                output.add(message);
            }
        }
        return output;
    }

    public void addSeq(JSONObject message) {
        message.put("seq",String.valueOf(seq));
        seq++;
    }

    public String findUsername(int idOfClient) {
        return idAndUsername.get(idOfClient);
    }

    public void saveIdAndUsername(int idOfClient, String usernameOfClient) {
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
            if(message.get("to").equals(username) && message.get("isRead").equals(false)){
                message.put("isRead",true);
                output.add(message);
            }
        }
        return output;
    }

    public JSONObject findMessage(String seq) {
        for (JSONObject message : messages) {
            if(message.get("seq").equals(seq)){
                return message;
            }
        }
        return null;
    }

    public void replaceMessage(String seq, String newMessage) {
        JSONObject oldMessage = findMessage(seq);
        oldMessage.put("body", newMessage);
    }

    public void addGroup(Group group){
        groups.add(group);
    }

    public boolean isGroup(String username) {
        for (Group group : groups) {
            if(group.getName().equals(username)){
                return true;
            }
        }
        return false;
    }

    public Group findGroup(String username) {
        for (Group group : groups) {
            if(group.getName().equals(username)){
                return group;
            }
        }
        return null;
    }
}
