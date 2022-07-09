package tech.sobhan;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import static tech.sobhan.Constants.SERVER_ADDRESS;
import static tech.sobhan.Constants.SERVER_PORT;

public class Host {
    private String address;
    private Socket socketToServer;//todo check
    private int[] portRange;//todo check for used ports by workspace
    private ArrayList<Workspace> workspaces;

    public Host(){
        workspaces = new ArrayList<>();
    }

    public Host(String address, int[] portRange) {
        this();
        this.address = address;
        this.portRange = portRange;
    }

    public boolean requestCreatingHost(String request){
        try {
            socketToServer = new Socket(SERVER_ADDRESS,SERVER_PORT);
            System.out.println("currentSocket = " + socketToServer);//
            DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
            DataInputStream in = new DataInputStream(socketToServer.getInputStream());
            out.writeUTF(request);
            String response = in.readUTF();
            System.out.println(response);//
            ServerSocket tempServer;
            if(response.startsWith("OK")){
                int portForSecondConnection = Integer.parseInt(response.split(" ")[1]);
                tempServer = new ServerSocket(portForSecondConnection);
                out.writeUTF("check");
            }else{
                return false;
            }
            Socket tempSocket = tempServer.accept();
            System.out.println("server accepted");
            in = new DataInputStream(tempSocket.getInputStream());
            out = new DataOutputStream(tempSocket.getOutputStream());
            response = in.readUTF();
            System.out.println(response);//
            if(response.startsWith("OK")){
                String code = response.split(" ")[1];
                tempSocket.close();
                in = new DataInputStream(socketToServer.getInputStream());
                out = new DataOutputStream(socketToServer.getOutputStream());
                out.writeUTF(code);
            }else{
                return false;
            }
            response = in.readUTF();
            System.out.println(response);//
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
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

    private void handleCommand(String command) {
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        if(mainCommand.equals("create-workspace")){
            String nameOfWorkSpace = parameters[1];
            int port = Integer.parseInt(parameters[2]);
            int clientID = Integer.parseInt(parameters[3]);//todo find usage
            createWorkspace(nameOfWorkSpace ,port);
            findWorkspace(nameOfWorkSpace).run();
//        }else if(mainCommand.equals("run-workspace")){
//            String nameOfWorkSpace = parameters[1];//todo check for error
        }
    }

    private boolean createWorkspace(String nameOfWorkSpace, int port){
//        if(foundWorkspaceCreationProblem()){
//            return false;
//        }
        Workspace workspace = new Workspace(nameOfWorkSpace, port, this);
        workspaces.add(workspace);
        try {
            DataOutputStream out = new DataOutputStream(socketToServer.getOutputStream());
            out.writeUTF("OK");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public Workspace findWorkspace(String nameOfWorkSpace){
        for(Workspace workspace : workspaces){
            if(workspace.getName().equals(nameOfWorkSpace)){
                return workspace;
            }
        }
        return null;
    }

    public Socket getSocketToServer() {
        return socketToServer;
    }

    public void setSocketToServer(Socket socketToServer) {
        this.socketToServer = socketToServer;
    }

    public int[] getPortRange() {
        return portRange;
    }

    public void setPortRange(int[] portRange) {
        this.portRange = portRange;
    }

    public ArrayList<Workspace> getWorkSpaces() {
        return workspaces;
    }

    public void setWorkSpaces(ArrayList<Workspace> workspaces) {
        this.workspaces = workspaces;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ArrayList<Workspace> getWorkspaces() {
        return workspaces;
    }

    public void setWorkspaces(ArrayList<Workspace> workspaces) {
        this.workspaces = workspaces;
    }
}

