package tech.sobhan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static tech.sobhan.Util.convertToJSON;

@Builder
@AllArgsConstructor
public class WorkspaceThread extends Thread{
    private Socket socketFromClient;
    private Workspace parent;
    private String currentClientUsername;

    @SneakyThrows
    public void run() {
        DataInputStream in = new DataInputStream(socketFromClient.getInputStream());
        DataOutputStream out = new DataOutputStream(socketFromClient.getOutputStream());
        String responseFromClient = "";
        while(!responseFromClient.equals("disconnect")){
            responseFromClient = in.readUTF();
//            if(currentClientUsername!=null){
//                System.out.print(currentClientUsername + ": ");
//            }
//            System.out.println(responseFromClient);//
            handleCommand(responseFromClient, in, out);
        }
        in.close();
        out.close();
    }

    private void handleCommand(String command, DataInputStream in, DataOutputStream out) {
        String[] parameters = command.split(" ",2);
        String mainCommand = parameters[0];
        switch (mainCommand) {
            case "connect" -> connectClient(parameters[1], in, out);
            case "send-message" -> sendMessage(command, out);
            case "get-chats" -> getChats(out);
            case "get-messages" -> getMessages(parameters[1],out);
            case "disconnect" -> disconnectClient(out);
            case "read-messages" -> showUnreadMessages(out);
            case "change-message" -> changeMessage(command,out);
            case "create-group" -> createGroup(parameters[1], out);
            case "add-to-group" -> addToGroup(command, out);
            case "join-group" -> joinGroup(command, out);
        }
    }

    @SneakyThrows
    private void joinGroup(String command, DataOutputStream out) {
        String[] parameters = command.split(" ");
        String groupName = parameters[1];
        Group group = parent.findGroup(groupName);
        if(group.getAttendees().contains(currentClientUsername)){
            out.writeUTF("ERROR you have already joined this group");
            return;
        }
        group.addUser(currentClientUsername);
        sendNotification(groupName, group);
    }

    private void sendNotification(String groupName, Group group){
        JSONObject notification = new JSONObject();
        notification.put("from", group.getName());
        notification.put("type","text");
        notification.put("body", currentClientUsername + " joined " + groupName);
        sendToAllUsersOfGroup(notification, groupName);
    }

    @SneakyThrows
    private void addToGroup(String command, DataOutputStream out) {
        String[] parameters = command.split(" ");
        String username = parameters[1];
        String groupName = parameters[2];
        Group group = parent.findGroup(groupName);
        if (checkForErrors(username, out, group)) {
            return;
        }
        group.addUser(username);
        out.writeUTF("OK user '"+username+"' added to group '"+groupName+"'");
    }

    private boolean checkForErrors(String username, DataOutputStream out, Group group) throws IOException {
        if(group ==null){
            out.writeUTF("group not found");
            return true;
        }
        if(parent.findMessages(currentClientUsername, username).isEmpty()){
            out.writeUTF("you don't know this user");
            return true;
        }
        return false;
    }

    @SneakyThrows
    private void createGroup(String groupName, DataOutputStream out) {
        Group group = Group.builder().name(groupName).build();
        group.addUser(currentClientUsername);
        parent.addGroup(group);
        out.writeUTF("OK group "+groupName+" created");
    }

    @SneakyThrows
    private void changeMessage(String command, DataOutputStream out) {
        String[] parameters = command.split(" ",3);
        String seq = parameters[1];
        String newMessage = parameters[2];
        JSONObject oldMessage = parent.findMessage(seq);
        if(!oldMessage.get("from").equals(currentClientUsername)){
            out.writeUTF("ERROR access denied");
            return;
        }
        parent.replaceMessage(seq,newMessage);
        out.writeUTF("OK");
    }

    @SneakyThrows
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
            out.writeUTF("OK " + messages);
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

    @SneakyThrows
    private void sendMessage(String command, DataOutputStream out) {
        String[] parameters = command.split(" ",3);
        JSONObject message = convertToJSON(parameters[2]);
        assert message != null;
        message.put("from", currentClientUsername);
        message.put("to", parameters[1]);
        parent.addSeq(message);
        out.writeUTF("OK " + message.get("seq"));
        String receiverUsername = (String) message.get("to");
        if(parent.isGroup(receiverUsername)){
            sendToAllUsersOfGroup(message, receiverUsername);
        }else{
            sendToUser(message, receiverUsername);
        }
    }

    @SneakyThrows
    private void sendToAllUsersOfGroup(JSONObject message, String groupName){
        Group group = parent.findGroup(groupName);
        message.put("from", groupName);
        for (String attendee : group.getAttendees()) {
            message.put("to",attendee);
            sendToUser(message, attendee);
        }
    }

    @SneakyThrows
    private void sendToUser(JSONObject message, String receiverUsername){
        if(parent.isConnected(receiverUsername)){
            message.put("isRead",true);
            Socket socketToReceiver = parent.findSocketFromUsername(receiverUsername);
            DataOutputStream out = new DataOutputStream(socketToReceiver.getOutputStream());
            String commandToReceiver = "receive-message " + message;
            out.writeUTF(commandToReceiver);
        }else{
            message.put("isRead",false);
        }
        parent.save(message);
        saveChats(message, currentClientUsername, receiverUsername);
    }

    private void saveChats(JSONObject message, String senderUsername, String receiverUsername) {//todo check this method
        if(parent.alreadyChatting(senderUsername, receiverUsername)){
            if(message.get("isRead").equals(false)){
                JSONObject receiverChat = parent.findChatOf(receiverUsername,senderUsername);
                receiverChat.putIfAbsent("unread_count", "0");
                String currentUnreadCountString = (String) receiverChat.get("unread_count");
                int currentUnreadCount = Integer.parseInt(currentUnreadCountString);
                receiverChat.put("unread_count", String.valueOf(currentUnreadCount+1) );
            }
        }else{
            JSONObject senderChat = new JSONObject();
            senderChat.put("name", receiverUsername);
            parent.save(senderUsername,senderChat);

            JSONObject receiverChat = new JSONObject();
            receiverChat.put("name", senderUsername);
            parent.save(receiverUsername,receiverChat);
        }

    }

    private void connectClient(String token, DataInputStream in, DataOutputStream out) {
        try{
            int idOfClient = requestIdFromServer(token);
            if(parent.findUsername(idOfClient)==null){
                askClientForUsername(in, out, socketFromClient, idOfClient);
            }
            out.writeUTF("OK");
            currentClientUsername = parent.findUsername(idOfClient);
            parent.saveConnectedUsername(currentClientUsername);
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    private void askClientForUsername(DataInputStream in, DataOutputStream out, Socket socketFromClient, int idOfClient) throws IOException {
        out.writeUTF("username?");
        String usernameOfClient = in.readUTF();
        parent.save(idOfClient,usernameOfClient);
        parent.save(usernameOfClient, socketFromClient);
    }

    private int requestIdFromServer(String token) {
        String whoisRequest = "whois "+ token;
        try {
            Socket socketToServer = parent.getSocketToServer();
            DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
            DataInputStream in = new DataInputStream(socketToServer.getInputStream());
            out.writeUTF(whoisRequest);
            String responseForWhoisFromServer = in.readUTF();
            System.out.println(responseForWhoisFromServer);//
            if(responseForWhoisFromServer.startsWith("OK")){
                return Integer.parseInt(responseForWhoisFromServer.split(" ")[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
