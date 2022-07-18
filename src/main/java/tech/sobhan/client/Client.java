package tech.sobhan.client;

import lombok.*;
import org.json.simple.JSONObject;
import tech.sobhan.utils.ScannerWrapper;

import java.net.*;
import java.io.*;
import java.util.Objects;

import static tech.sobhan.server.Server.SERVER_ADDRESS;
import static tech.sobhan.server.Server.SERVER_PORT;
import static tech.sobhan.utils.Util.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@Getter
public class Client implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757691L;
    private String phoneNumber;
    private String password;
    private int id;
    private Socket socketToWorkspace;
    @Setter private boolean isLoggedIn = false;

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
            default -> System.err.println("ERROR unknown command");
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
            System.err.println("ERROR plz log in first");
            return;
        }
        try (Socket socketToServer = new Socket(SERVER_ADDRESS, SERVER_PORT)){
            sendSignal(socketToServer, command);
            String response = receiveSignal(socketToServer);
            System.out.println(response);//
            if (response.startsWith("OK") && command.startsWith("login")) {
                isLoggedIn = true;
            }
        } catch (IOException e) {
            System.err.println("ERROR Server is not up");
        }
    }

    private boolean isRelatedToServer(String mainCommand) {
        String[] mainCommands = {"register", "login", "create-workspace", "show-workspaces", "show-connected-workspaces"};
        for (String command : mainCommands) {
            if (command.equals(mainCommand)) {
                return true;
            }
        }
        return false;
    }

    private void handleWorkspaceRelatedCommand(String command) {
        if (socketToWorkspace == null) {
            System.err.println("ERROR You are not connected to a workspace");
            return;
        }
        sendSignal(socketToWorkspace, command);
    }

    private boolean isRelatedToWorkspace(String mainCommand) {
        String[] mainCommands = {"send-message", "get-chats", "get-messages", "read-messages", "disconnect",
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
            System.err.println("ERROR plz log in first");
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
            System.err.println("ERROR You have already connected to a workspace");
            return false;
        }
        String[] parameters = responseFromServer.split(" ");
        String workspaceAddress = parameters[1];
        int workspacePort = Integer.parseInt(parameters[2]);
        String token = parameters[3];
        setSocketToWorkspace(workspaceAddress, workspacePort);
        sendSignal(socketToWorkspace, "connect " + token);
        String responseFromWorkspace = receiveSignal(socketToWorkspace);
        if (responseFromWorkspace.startsWith("ERROR")) {
            System.err.println(responseFromWorkspace);
            return false;
        }
        System.out.println(responseFromWorkspace);
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

    public void setSocketToWorkspace(Socket socket) {
        socketToWorkspace = socket;
    }

    private void sendUsernameIfNeeded(String responseFromWorkspace) {
        if (responseFromWorkspace.equals("username?")) {
            askUsername();
        }
    }

    private void askUsername() {
        String responseFromWorkspace;
        String usernameOfClient = ScannerWrapper.nextLine();
        while(isDuplicate(usernameOfClient)){
            System.err.println("ERROR username already exists.");
            usernameOfClient = ScannerWrapper.nextLine();
        }
        sendSignal(socketToWorkspace, usernameOfClient);
        responseFromWorkspace = receiveSignal(socketToWorkspace);
        System.out.println(responseFromWorkspace);//
    }

    private boolean isDuplicate(String usernameOfClient) {
        sendSignal(socketToWorkspace, "is-duplicate " + usernameOfClient);
        return Objects.equals(receiveSignal(socketToWorkspace), "YES");
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
