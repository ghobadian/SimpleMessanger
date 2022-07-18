package tech.sobhan.host;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.*;
import tech.sobhan.utils.ScannerWrapper;
import tech.sobhan.workspace.Workspace;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;

import static tech.sobhan.server.Server.SERVER_ADDRESS;
import static tech.sobhan.server.Server.SERVER_PORT;
import static tech.sobhan.utils.Util.receiveSignal;
import static tech.sobhan.utils.Util.sendSignal;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@Getter
public class Host implements Serializable {
    @Serial private static final long serialVersionUID = 6529685098267757690L;
    private String address;
    private int[] portRange;
    @Setter private Socket socketToServer;
    @Getter private final ArrayList<Workspace> workspaces = new ArrayList<>();

    public boolean requestCreatingHost(String request){
        setSocketToServer(SERVER_ADDRESS, SERVER_PORT);
        sendSignal(socketToServer, request);
        String response = receiveSignalFromServer(socketToServer);
        System.out.println(response);//
        if(!response.startsWith("OK")){
            return false;
        }
        int portForSecondConnection = Integer.parseInt(response.split(" ")[1]);
        response = requestCode(portForSecondConnection);
        String code = response.split(" ")[1];
        sendSignal(socketToServer, code);
        response = receiveSignalFromServer(socketToServer);
        System.out.println(response);//
        return true;
    }

    public String receiveSignalFromServer(Socket socketToServer) {
        return receiveSignal(socketToServer);
    }

    public String requestCode(int portForSecondConnection) {
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
            handleCommand(command);
        }
    }

    public void handleCommand(String command) {
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        switch (mainCommand){
            case "create-workspace" -> createWorkspaceAndRun(parameters);
            case "create-host" -> createHostAndRun(command);
            case "connect-host" -> requestConnectingToHost(parameters);
            case "show-workspaces" -> sendWorkspacesToServer();
        }
    }

    public boolean createHostAndRun(String command) {
        if(!requestCreatingHost(command)){
            return false;
        }
        Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
        otherDevicesThread.start();//todo
        return true;
    }

    private void createWorkspaceAndRun(String[] parameters) {
        Workspace createdWorkspace = createWorkspace(parameters);

        if(createdWorkspace == null){
            return;
        }
        workspaces.add(createdWorkspace);
        createdWorkspace.run();
//                createdWorkspace.start();//todo ask
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
        setSocketToServer(SERVER_ADDRESS,SERVER_PORT);
        String address  = parameters[1];
        sendSignal(socketToServer,"connect-host " + address );
        String response = receiveSignal(socketToServer);
        System.out.println(response);
        if(response.startsWith("ERROR")){
            return;
        }
        Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
        otherDevicesThread.start();
    }

    private void setSocketToServer(String serverAddress, int serverPort) {
        try {
            socketToServer = new Socket(serverAddress, serverPort);
        } catch (IOException e) {
            System.err.println("ERROR connection to server failed");
        }
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

