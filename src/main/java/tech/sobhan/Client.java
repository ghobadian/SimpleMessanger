package tech.sobhan;

import lombok.*;
import org.json.simple.JSONObject;

import java.net.*;
import java.io.*;

import static tech.sobhan.Constants.SERVER_ADDRESS;
import static tech.sobhan.Constants.SERVER_PORT;
import static tech.sobhan.Util.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Client implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757691L;
    @Getter
    private String phoneNumber;
    @Getter
    private String password;
    @Getter
    private int id;
    private Socket socketToWorkspace;
    @Setter
    private boolean isLoggedIn = false;

    public void run() {
        Thread userThread = new Thread(this::getInputFromUser);
        userThread.start();
    }

    private void getInputFromUser() {
        String command;
        while (true) {
            command = ScannerWrapper.nextLine();
            handleCommand(command);
        }
    }

    private void getInputFromOtherDevices() {
        String response = "";
        while (!response.equals("disconnect")) {
            response = receiveSignal(socketToWorkspace);
            assert response != null;
            if (response.startsWith("receive-message")) {
                System.out.println(sanitizeMessage(response.split(" ", 2)[1]));
            } else {
                System.out.println(response);
            }
        }
        socketToWorkspace = null;
    }

    private String sanitizeMessage(String messageAsString) {
        JSONObject message = convertToJSON(messageAsString);
        String senderUsername = (String) message.get("from");
        if (message.get("type").equals("text")) {
            return senderUsername + ": " + message.get("body");
        }
        return null;
    }

    private enum Relation {SERVER, WORKSPACE, ETC, NONE}

    public void handleCommand(String command) {
        String mainCommand = command.split(" ")[0];
        Relation relation = findRelation(mainCommand);
        switch (relation) {
            case SERVER -> handleServerRelatedCommand(command);
            case WORKSPACE -> handleWorkspaceRelatedCommand(command);
            case ETC -> {
                if (requestConnectToWorkspace(command)) {
                    Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
                    otherDevicesThread.start();
                }
            }
            default -> System.out.println("ERROR unknown command");
        }
    }

    private Relation findRelation(String mainCommand) {
        return isRelatedToServer(mainCommand) ? Relation.SERVER :
                isRelatedToWorkspace(mainCommand) ? Relation.WORKSPACE :
                        mainCommand.equals("connect-workspace") ? Relation.ETC :
                                Relation.NONE;
    }

    private void handleServerRelatedCommand(String command) {
        if (command.startsWith("create-workspace") && !isLoggedIn) {
            System.out.println("ERROR plz log in");
            return;
        }
        try {
            Socket socketToServer = new Socket(SERVER_ADDRESS, SERVER_PORT);
            sendSignal(socketToServer, command);
            String response = receiveSignal(socketToServer);
            System.out.println(response);//
            if (response.startsWith("OK") && command.startsWith("login")) {//todo not clean at allllll
                isLoggedIn = true;
            }
            socketToServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isRelatedToServer(String mainCommand) {
        String[] mainCommands = {"register", "login", "create-workspace", "show-workspaces", "show-connected-workspaces"};//todo maybe add connect-workspace
        for (String command : mainCommands) {
            if (command.equals(mainCommand)) {
                return true;
            }
        }
        return false;
    }

    private void handleWorkspaceRelatedCommand(String command) {
        if (socketToWorkspace == null) {
            System.out.println("ERROR You are not connected to a workspace");
            return;
        }
        sendSignal(socketToWorkspace, command);
    }

    private boolean isRelatedToWorkspace(String mainCommand) {
        String[] mainCommands = {"send-message", "get-chats", "get-messages", "read-messages", "disconnect",//todo move somewhere else
                "change-message", "create-group", "add-to-group", "join-group"};
        for (String command : mainCommands) {
            if (command.equals(mainCommand)) {
                return true;
            }
        }
        return false;
    }

    public boolean requestConnectToWorkspace(String request) {
        if (!isLoggedIn) {
            System.out.println("ERROR plz log in first");
            return false;
        }
        String responseFromServer = requestTokenFromServer(request);
        if (responseFromServer.startsWith("ERROR")) {
            return false;
        }

        return connectToWorkspace(responseFromServer);
    }

    public boolean connectToWorkspace(String responseFromServer) {
        if (socketToWorkspace != null) {
            System.out.println("ERROR You have already connected to a workspace");
            return false;
        }
        String[] parameters = responseFromServer.split(" ");
        String workspaceAddress = parameters[1];
        int workspacePort = Integer.parseInt(parameters[2]);
        String token = parameters[3];
        setSocketToWorkspace(workspaceAddress, workspacePort);
        sendSignal(socketToWorkspace, "connect " + token);
        String responseFromWorkspace = receiveSignal(socketToWorkspace);
        System.out.println(responseFromWorkspace);
        if (responseFromWorkspace.startsWith("ERROR")) {
            return false;
        }
        sendUsernameIfNeeded(responseFromWorkspace);
        return true;
    }

    public void setSocketToWorkspace(String workspaceAddress, int workspacePort) {
        try {
            socketToWorkspace = new Socket(workspaceAddress, workspacePort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendUsernameIfNeeded(String responseFromWorkspace) {
        if (responseFromWorkspace.equals("username?")) {
            String usernameOfClient = ScannerWrapper.nextLine();
            sendSignal(socketToWorkspace, usernameOfClient);
            responseFromWorkspace = receiveSignal(socketToWorkspace);
            System.out.println(responseFromWorkspace);//
        }
    }

    public String requestTokenFromServer(String request) {
        String responseFromServer = null;
        try (Socket socketToServer = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            sendSignal(socketToServer, request);
            responseFromServer = receiveSignal(socketToServer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(responseFromServer);//
        return responseFromServer;
    }
}
