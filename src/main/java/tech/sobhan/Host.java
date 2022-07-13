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

    public void requestCreatingHost(String request){
        try {
            socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
//        DataInputStream in = new DataInputStream(socketToServer.getInputStream());
        sendSignal(socketToServer,request);
//            out.writeUTF(request);
//            String response = in.readUTF();
        String response = receiveSignal(socketToServer);
        System.out.println(response);//
        ServerSocket tempServer;//todo sus
        if(!response.startsWith("OK")){
            return;
        }
        int portForSecondConnection = Integer.parseInt(response.split(" ")[1]);
        sendSignal(socketToServer, "check");
//        out.writeUTF("check");
        response = requestCode(portForSecondConnection);
        if (response == null) return;
        String code = response.split(" ")[1];
//            in = new DataInputStream(socketToServer.getInputStream());
//            out = new DataOutputStream(socketToServer.getOutputStream());
        sendSignal(socketToServer, code);
//            out.writeUTF(code);
//            response = in.readUTF();
        response = receiveSignal(socketToServer);
        System.out.println(response);//
        Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
        otherDevicesThread.start();
    }

    @SneakyThrows
    private String requestCode(int portForSecondConnection) {
        ServerSocket tempServer;
        String response;
        tempServer = new ServerSocket(portForSecondConnection);
        Socket tempSocket = tempServer.accept();
        System.out.println("server accepted");
//            in = new DataInputStream(tempSocket.getInputStream());
//            response = in.readUTF();
        response = receiveSignal(tempSocket);
        System.out.println(response);//
        if(!response.startsWith("OK")){
            return null;
        }
        tempSocket.close();
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
            System.out.println("command = " + command);
            handleLocalCommand(command);
        }
        saveToFile();
    }

    private void handleLocalCommand(String command) {
        switch(command){
            case "show-workspaces" -> workspaces.forEach(System.out::println);
        }
    }


    @SneakyThrows
    private void saveToFile() {//todo move to file handler
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.writeValue(Paths.get("src/main/resources/host/workspaces.txt").toFile(), workspaces);
    }

    private void getInputFromOtherDevices() {
        try {
            DataInputStream in = new DataInputStream(socketToServer.getInputStream());
            while(true){
                String command = in.readUTF();
                System.out.println(command);//
                handleCommand(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void handleCommand(String command) {
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        switch (mainCommand){
            case "create-workspace" -> {
                Workspace createdWorkspace = createWorkspace(parameters);
                assert createdWorkspace != null;
                workspaces.add(createdWorkspace);
                createdWorkspace.run();
//                findWorkspace(parameters[1]).run();
            }
            case "create-host" -> requestCreatingHost(command);
            case "connect-host" -> requestConnectingToHost(parameters);
//            case "shutdown" -> shutdownHost();//todo
        }
    }

    @SneakyThrows
    private void requestConnectingToHost(String[] parameters) {
        socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
        String address  = parameters[1];
        DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
        DataInputStream in = new DataInputStream(socketToServer.getInputStream());
        out.writeUTF("connect-host " + address );
        String response = in.readUTF();
        System.out.println(response);//todo move all out and in to a single method
        Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
        otherDevicesThread.start();
    }

    private Workspace createWorkspace(String[] parameters){
        String nameOfWorkSpace = parameters[1];
        int port = Integer.parseInt(parameters[2]);
        int clientID = Integer.parseInt(parameters[3]);//todo find usage

        if(foundWorkspaceCreationProblem(nameOfWorkSpace, port)){
            return null;
        }

        sendSignal(socketToServer , "OK");
//        try {
//            DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
//            out.writeUTF("OK");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return new Workspace(nameOfWorkSpace, port, address, socketToServer);
    }



    private boolean foundWorkspaceCreationProblem(String nameOfWorkSpace, int port) {
        if(duplicateWorkspaceFound(nameOfWorkSpace)){
            System.out.println("ERROR workspace name is already in use");
            return true;
        }
        if(portInUse(port)){
            System.out.println("ERROR port already in use by another user");
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
            if(workspace.getName().equals(nameOfWorkSpace)){
                return true;
            }
        }
        return false;
    }

    public Workspace findWorkspace(String nameOfWorkSpace){
        for(Workspace workspace : workspaces){
            if(workspace.getName().equals(nameOfWorkSpace)){
                return workspace;
            }
        }
        return null;
    }

    public void addWorkspace(Workspace workspace){
        workspaces.add(workspace);
    }
}

