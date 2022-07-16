package tech.sobhan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.Socket;

import static tech.sobhan.Util.*;

@Builder
@AllArgsConstructor
public class WorkspaceThread extends Thread{
    private Socket socketFromClient;
    private Workspace parent;
    private String currentClientUsername;

    public void run() {
        String responseFromClient = "";
        while(!responseFromClient.equals("disconnect")){
            responseFromClient = receiveSignal(socketFromClient);
            System.out.println(responseFromClient);
            handleCommand(responseFromClient);
        }
    }

    private void handleCommand(String command) {
        String[] parameters = command.split(" ",2);
        String mainCommand = parameters[0];
        switch (mainCommand) {
            case "connect" -> connectClient(parameters[1]);
            case "send-message" -> sendMessage(command);
            case "get-chats" -> getChats();
            case "get-messages" -> getMessages(parameters[1]);
            case "disconnect" -> disconnectClient();
            case "read-messages" -> showUnreadMessages();
            case "change-message" -> changeMessage(command);
            case "create-group" -> createGroup(parameters[1]);
            case "add-to-group" -> addToGroup(command);
            case "join-group" -> joinGroup(command);
        }
    }

    private void joinGroup(String command) {
        String[] parameters = command.split(" ");
        String groupName = parameters[1];
        Group group = parent.findGroup(groupName);
        if(group.getAttendees().contains(currentClientUsername)){
            sendSignal(socketFromClient, "ERROR you have already joined this group");
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

    private void addToGroup(String command) {
        String[] parameters = command.split(" ");
        String username = parameters[1];
        String groupName = parameters[2];
        Group group = parent.findGroup(groupName);
        if (checkForErrors(username, group)) {
            return;
        }
        group.addUser(username);
        sendSignal(socketFromClient, "OK user '"+username+"' added to group '"+groupName+"'");
    }

    private boolean checkForErrors(String username, Group group){
        if(group ==null){
            sendSignal(socketFromClient, "group not found");
            return true;
        }
        if(parent.findMessages(currentClientUsername, username).isEmpty()){
            sendSignal(socketFromClient, "you don't know this user");
            return true;
        }
        return false;
    }

    private void createGroup(String groupName) {
        Group group = Group.builder().name(groupName).build();
        group.addUser(currentClientUsername);
        parent.addGroup(group);
        sendSignal(socketFromClient, "OK group "+groupName+" created");
    }

    private void changeMessage(String command) {
        String[] parameters = command.split(" ",3);
        String seq = parameters[1];
        String newMessage = parameters[2];
        JSONObject oldMessage = parent.findMessage(seq);
        if(!oldMessage.get("from").equals(currentClientUsername)){
            sendSignal(socketFromClient, "ERROR access denied");
            return;
        }
        parent.replaceMessage(seq,newMessage);
        sendSignal(socketFromClient, "OK");
    }

    private void showUnreadMessages() {
        JSONArray unreadMessages = parent.getUnreadMessagesOf(currentClientUsername);
        sendSignal(socketFromClient, "OK " + unreadMessages);
    }

    private void disconnectClient() {
        parent.removeUsernameFromConnectedUsernames(currentClientUsername);
        sendSignal(socketFromClient, "disconnect");
    }

    private void getMessages(String usernameOfOtherClient) {
        JSONArray messages = parent.findMessages(currentClientUsername,usernameOfOtherClient);
        sendSignal(socketFromClient, "OK " + messages);
    }

    private void getChats() {
        JSONArray chats = parent.findChats(currentClientUsername);
        sendSignal(socketFromClient, "OK " + chats);
    }

    private void sendMessage(String command) {
        String[] parameters = command.split(" ",3);
        JSONObject message = convertToJSON(parameters[2]);
        message.put("from", currentClientUsername);
        message.put("to", parameters[1]);
        parent.addSeq(message);
        sendSignal(socketFromClient, "OK " + message.get("seq"));
        String receiverUsername = (String) message.get("to");
        if(parent.isGroup(receiverUsername)){
            sendToAllUsersOfGroup(message, receiverUsername);
        }else{
            sendToUser(message, receiverUsername);
        }
    }

    private void sendToAllUsersOfGroup(JSONObject message, String groupName){
        Group group = parent.findGroup(groupName);
        message.put("from", groupName);
        for (String attendee : group.getAttendees()) {
            message.put("to",attendee);
            sendToUser(message, attendee);
        }
    }

    private void sendToUser(JSONObject message, String receiverUsername){
        if(parent.isConnected(receiverUsername)){
            message.put("isRead",true);
            Socket socketToReceiver = parent.findSocketFromUsername(receiverUsername);
            String commandToReceiver = "receive-message " + message;
            sendSignal(socketToReceiver, commandToReceiver);
        }else{
            message.put("isRead",false);
        }
        parent.saveMessage(message);
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
            saveChats(senderUsername, receiverUsername);
        }

    }

    private void saveChats(String senderUsername, String receiverUsername) {
        JSONObject senderChat = new JSONObject();
        senderChat.put("name", receiverUsername);
        parent.saveChat(senderUsername,senderChat);

        JSONObject receiverChat = new JSONObject();
        receiverChat.put("name", senderUsername);
        parent.saveChat(receiverUsername,receiverChat);
    }

    private void connectClient(String token) {
        int idOfClient = requestIdFromServer(token);
        if(parent.findUsername(idOfClient)==null){
            askClientForUsername(idOfClient);
        }
        sendSignal(socketFromClient, "OK");
        currentClientUsername = parent.findUsername(idOfClient);
        parent.saveConnectedUsername(currentClientUsername);

    }

    private void askClientForUsername(int idOfClient){
        sendSignal(socketFromClient, "username?");
        String usernameOfClient = receiveSignal(socketFromClient);
        parent.saveIdAndUsername(idOfClient,usernameOfClient);
        parent.saveSocket(usernameOfClient, socketFromClient);
    }

    private int requestIdFromServer(String token) {
        String whoisRequest = "whois "+ token;
        Socket socketToServer = parent.getSocketToServer();
        sendSignal(socketToServer, whoisRequest);
        String responseForWhoisFromServer = receiveSignal(socketToServer);
        System.out.println(responseForWhoisFromServer);//
        assert responseForWhoisFromServer != null;
        if(responseForWhoisFromServer.startsWith("OK")){
            return Integer.parseInt(responseForWhoisFromServer.split(" ")[1]);
        }else{
            return -1;
        }
    }
}
