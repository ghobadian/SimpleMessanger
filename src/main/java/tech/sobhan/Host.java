package tech.sobhan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;

import static tech.sobhan.Constants.SERVER_ADDRESS;
import static tech.sobhan.Constants.SERVER_PORT;
import static tech.sobhan.Util.receiveSignal;
import static tech.sobhan.Util.sendSignal;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Host implements Serializable {
    @Serial private static final long serialVersionUID = 6529685098267757690L;
    @Getter private String address;
    @Getter private int[] portRange;
    @Getter @Setter private Socket socketToServer;

    @Getter private final ArrayList<Workspace> workspaces = new ArrayList<>();

    public boolean requestCreatingHost(String request){//todo clean it
        try {
            socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendSignal(socketToServer, request);
        String response = receiveSignal(socketToServer);
        System.out.println(response);//
        if(!response.startsWith("OK")){
            return false;
        }
        int portForSecondConnection = Integer.parseInt(response.split(" ")[1]);
        response = requestCode(portForSecondConnection);
        if (response == null) return false;
        String code = response.split(" ")[1];
        sendSignal(socketToServer, code);
        response = receiveSignal(socketToServer);
        System.out.println(response);//
        return true;
    }

    private String requestCode(int portForSecondConnection) {
        String response = null;
        try(ServerSocket tempServer = new ServerSocket(portForSecondConnection)) {
            sendSignal(socketToServer, "check");
            Socket tempSocket = tempServer.accept();
            System.out.println("server accepted");
            response = receiveSignal(tempSocket);
            System.out.println(response);//
            assert response != null;
            if(!response.startsWith("OK")){
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public void run() {
//        loadFromFile();//todo
        Thread userThread = new Thread(this::getInputFromUser);
        userThread.start();
    }

    private void getInputFromUser() {
        String command = "";
        while(!command.equals("shutdown")){
            command = ScannerWrapper.nextLine();
            handleLocalCommand(command);
        }
        saveToFile();
    }

    private void handleLocalCommand(String command) {
        switch(command){
            case "show-workspaces" -> workspaces.forEach(System.out::println);
            default -> System.out.println("ERROR unknown command");
            //            case "shutdown" -> shutdownHost();//todo
        }
    }

    private void saveToFile() {//todo move to file handler
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            mapper.writeValue(Paths.get("src/main/resources/host/workspaces.txt").toFile(), workspaces);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getInputFromOtherDevices() {
        while(true){
            String command = receiveSignal(socketToServer);
            System.out.println(command);//
            assert command != null;
            handleCommand(command);
        }
    }

    public boolean handleCommand(String command) {
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        switch (mainCommand){
            case "create-workspace" -> {
                Workspace createdWorkspace = createWorkspace(parameters);

                if(createdWorkspace == null){
                    return false;
                }
                workspaces.add(createdWorkspace);
                createdWorkspace.start();//todo
            }
            case "create-host" -> {
                if(!requestCreatingHost(command)){
                    return false;
                }
                getInputFromOtherDevices();
                Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
                otherDevicesThread.start();//todo
            }
            case "connect-host" -> requestConnectingToHost(parameters);
            case "show-workspaces" -> sendWorkspacesToServer();
        }
        return true;
    }

    private void sendWorkspacesToServer() {
        if(workspaces.isEmpty()){
            sendSignal(socketToServer, "ERROR no workspaces were found");
            return;
        }
        StringBuilder workspacesNames = new StringBuilder();
        workspaces.forEach(workspace -> workspacesNames.append(workspace.getWorkspaceName()).append("\n"));
        workspacesNames.delete(workspaces.size()-2, workspaces.size());
        sendSignal(socketToServer, "OK " + workspacesNames);
    }

    private void requestConnectingToHost(String[] parameters) {
        try {
            socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String address  = parameters[1];
        sendSignal(socketToServer,"connect-host " + address );
        String response = receiveSignal(socketToServer);
        System.out.println(response);//todo move all out and in to a single method
        if(response.startsWith("ERROR")){
            return;
        }
        Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
        otherDevicesThread.start();
    }

    private Workspace createWorkspace(String[] parameters){
        String nameOfWorkSpace = parameters[1];
        int port = Integer.parseInt(parameters[2]);
        int clientID = Integer.parseInt(parameters[3]);//todo find usage

        if(foundWorkspaceCreationProblem(port)){
            return null;
        }

        sendSignal(socketToServer , "OK");
        return Workspace.builder().workspaceName(nameOfWorkSpace).port(port).
                address(address).socketToServer(socketToServer).build();
    }



    private boolean foundWorkspaceCreationProblem(int port) {
        if(portInUse(port)){
            sendSignal(socketToServer, "ERROR port already in use by another workspace");
            return true;
        }

        return false;
    }

    private boolean portInUse(int port) {
        for (Workspace workspace : workspaces) {
            if(workspace.getPort() == port){
                return true;
            }
        }
        return false;
    }

    private boolean duplicateWorkspaceFound(String nameOfWorkSpace) {
        for (Workspace workspace : workspaces) {
            if(workspace.getWorkspaceName().equals(nameOfWorkSpace)){
                return true;
            }
        }
        return false;
    }

    public void addWorkspace(Workspace workspace){
        workspaces.add(workspace);
    }
}

