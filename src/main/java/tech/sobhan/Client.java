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
public class Client implements Serializable{
    @Serial
    private static final long serialVersionUID = 6529685098267757691L;
    @Getter private String phoneNumber;
    @Getter private String password;
    @Getter private int id;
    private Socket socketToWorkspace;

    public void run(){
        Thread userThread = new Thread(this::getInputFromUser);
        userThread.start();
    }

    private void getInputFromUser() {
        String command;
        while(true){
            command = ScannerWrapper.nextLine();
            System.out.println("command = " + command);
            handleCommand(command);
        }
    }

    @SneakyThrows
    private void getInputFromOtherDevices() {
        String response = "";
//        DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
        while(!response.equals("disconnect")){
            response = receiveSignal(socketToWorkspace);
//            response = in.readUTF();
            if(response.startsWith("receive-message")){
                receiveMessage(response.split(" ",2)[1]);
            }else{
                System.out.println(response);
            }
        }
        socketToWorkspace = null;
//        in.close();
    }

    @SneakyThrows
    public void handleCommand(String command) {
        String mainCommand = command.split(" ")[0];
        if(isRelatedToServer(mainCommand)){
          handleServerRelatedCommand(command);
        } else if(isRelatedToWorkspace(mainCommand)){
            handleWorkspaceRelateCommand(command);
        }else if(mainCommand.equals("connect-workspace")){//todo delete this shit
            requestConnectToWorkspace(command);
        }
    }

    @SneakyThrows
    private void handleServerRelatedCommand(String command) {
        Socket socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
//        DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
//        DataInputStream in = new DataInputStream(socketToServer.getInputStream());
//        out.writeUTF(command);
        sendSignal(socketToServer, command);
        String response = receiveSignal(socketToServer);
//        String response = in.readUTF();
        System.out.println(response);//
        socketToServer.close();
//        out.close();
//        in.close(); //todo check for errors
    }

    private boolean isRelatedToServer(String mainCommand) {
        String[] mainCommands = {"register", "login", "create-workspace"};//todo maybe add connect-workspace
        for (String command : mainCommands) {
            if(command.equals(mainCommand)){
                return true;
            }
        }
        return false;
    }

    @SneakyThrows//todo delete
    private void handleWorkspaceRelateCommand(String command) {
        if(socketToWorkspace==null){
            System.out.println("ERROR You are not connected to a workspace");
            return;
        }
//        DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
//        out.writeUTF(command);
        sendSignal(socketToWorkspace,command);
    }

    private boolean isRelatedToWorkspace(String mainCommand) {
        String[] mainCommands = {"send-message", "get-chats", "get-messages", "read-messages", "disconnect",//todo move somewhere else
                "change-message", "create-group", "add-to-group", "join-group"};
        for (String command : mainCommands) {
            if(command.equals(mainCommand)){
                return true;
            }
        }
        return false;
    }

    private static void receiveMessage(String messageAsString) {
        JSONObject message = convertToJSON(messageAsString);
        assert message != null;
        String senderUsername = (String) message.get("from");
        if(message.get("type").equals("text")){
            System.out.println(senderUsername +": "+ message.get("body"));
        }
    }

    public void requestConnectToWorkspace(String request) {
        try{
            String responseForConnectingToWorkSpaceFromServer = requestTokenFromServer(request);
            assert responseForConnectingToWorkSpaceFromServer != null;
            String[] parameters = responseForConnectingToWorkSpaceFromServer.split(" ");
            String workspaceAddress = parameters[1];
            int workspacePort = Integer.parseInt(parameters[2]);
            String token = parameters[3];

            socketToWorkspace = new Socket(workspaceAddress,workspacePort);
//            DataOutputStream out = new DataOutputStream(socketToWorkspace.getOutputStream());
//            DataInputStream in = new DataInputStream(socketToWorkspace.getInputStream());
//            out.writeUTF("connect " + token);
            sendSignal(socketToWorkspace,"connect " + token );
//            String responseFromWorkspace = in.readUTF();
            String responseFromWorkspace = receiveSignal(socketToWorkspace);
            System.out.println(responseFromWorkspace);

            if(responseFromWorkspace.equals("username?")){
                String usernameOfClient = ScannerWrapper.nextLine();
                sendSignal(socketToWorkspace, usernameOfClient);
//                out.writeUTF(usernameOfClient);
//                responseFromWorkspace = in.readUTF();
                responseFromWorkspace = receiveSignal(socketToWorkspace);
                System.out.println(responseFromWorkspace);//
            }

            if(!responseFromWorkspace.equals("OK")){
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
        otherDevicesThread.start();
    }

    @SneakyThrows
    private String requestTokenFromServer(String request) {
        Socket socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
//        DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
//        DataInputStream in = new DataInputStream(socketToServer.getInputStream());
        sendSignal(socketToServer, request);
//        out.writeUTF(request);
//        String responseForConnectingToWorkSpaceFromServer = in.readUTF();
        String responseForConnectingToWorkSpaceFromServer = receiveSignal(socketToServer);
        System.out.println(responseForConnectingToWorkSpaceFromServer);//
        return responseForConnectingToWorkSpaceFromServer;
//        return null;
    }
}
