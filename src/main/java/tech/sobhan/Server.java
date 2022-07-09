package tech.sobhan;

// A Java program for a Server
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import static tech.sobhan.Constants.SERVER_PORT;
import static tech.sobhan.DataGenerator.*;

public class Server {
    private ServerSocket server = null;
    private HashMap<Host,Socket> hostAndSocket;
    private HashMap<Token,Integer> tokenAndID;
    private Socket currentSocket = null;
    private DataInputStream in	 = null;
    private DataOutputStream out	 = null;
    private ArrayList<Host> hosts;
    private ArrayList<Workspace> workspaces;
    private ArrayList<Client> clients;
    private Client currentClient = null;


    public Server() {
        hosts = new ArrayList<>();
        workspaces = new ArrayList<>();
        clients = new ArrayList<>();
        hostAndSocket = new HashMap<>();
        tokenAndID = new HashMap<>();
    }

    public void run() {
        try (ServerSocket server = new ServerSocket(SERVER_PORT);){
            System.out.println("Server started");
            while(true){
                System.out.println("====================");
                System.out.println("Waiting for a Host/Client ...");
                currentSocket = server.accept();
                System.out.println("socket = " + currentSocket);
                in = new DataInputStream(currentSocket.getInputStream());
                out = new DataOutputStream(currentSocket.getOutputStream());
                String response = in.readUTF();
                System.out.println(response);//
                handleCommand(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleCommand(String command){
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        if(mainCommand.equals("create-host")){
            String ip = parameters[1];
            int portStartRange = Integer.parseInt(parameters[2]);
            int portEndRange = Integer.parseInt(parameters[3]);
            try {
                if(createHost(ip,portStartRange,portEndRange)){
                    hostAndSocket.put(hosts.get(hosts.size()-1),currentSocket);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(mainCommand.equals("register")){
            String phoneNumber = parameters[1];
            String password = parameters[2];
            registerClient(phoneNumber,password);
        }else if(mainCommand.equals("login")){
            String phoneNumber = parameters[1];
            String password = parameters[2];
            loginClient(phoneNumber,password);
        }else if(mainCommand.equals("create-workspace")){
            String nameOfWorkspace = parameters[1];
            Host chosenHost = hosts.get(RANDOM.nextInt(0,hosts.size()));
            int chosenPort = RANDOM.nextInt(chosenHost.getPortRange()[0],chosenHost.getPortRange()[1]);
            String commandForHost = "create-workspace " + nameOfWorkspace + " " + chosenPort +" "+ currentClient.getId();
            if(requestCreateWorkSpaceFromHost(chosenHost, chosenPort,commandForHost)){
                workspaces.add(new Workspace(nameOfWorkspace,chosenPort,chosenHost));
            }
        }else if(mainCommand.equals("connect-workspace")){
            String workspaceName = parameters[1]; //todo workspace names should be identical
            if(!connectClientToWorkSpace(findWorkspace(workspaceName))){
                //todo
            }
        }
    }

    private boolean connectClientToWorkSpace(Workspace workspace) {
        Token token = new Token();
        tokenAndID.put(token,currentClient.getId());
        try {
//            out = new DataOutputStream(socketToHostOfThisWorkspace.getOutputStream());
//            out.writeUTF("run-workspace " + workspace.getName());
            out = new DataOutputStream(currentSocket.getOutputStream());//todo make currentSocket local or remove it
            String responseToClient = "OK "+ workspace.getParent().getAddress() +" "+ workspace.getPort() +" "+ token;
            out.writeUTF(responseToClient);//todo
            Socket socketToHostOfThisWorkspace = hostAndSocket.get(workspace.getParent());
            in = new DataInputStream(socketToHostOfThisWorkspace.getInputStream());
            out = new DataOutputStream(socketToHostOfThisWorkspace.getOutputStream());
            String whois = in.readUTF();
            System.out.println(whois);
            if(whois.startsWith("whois") && token.getToken().equals(whois.split(" ")[1])  && !token.checkExpiration()){
                //todo make a Token class and timer
                int idOfClient = tokenAndID.get(token);
                out.writeUTF("OK " + idOfClient);
            }else{
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private Workspace findWorkspace(String workspaceName) {
        for(Workspace workspace : workspaces){
            if(workspace.getName().equals(workspaceName)){
                return workspace;
            }
        }
        return null;
    }

    private boolean requestCreateWorkSpaceFromHost(Host chosenHost, int chosenPort, String commandForHost) {
        try {
            Socket socketToHost = hostAndSocket.get(chosenHost);
            DataInputStream in = new DataInputStream(socketToHost.getInputStream());
            DataOutputStream out = new DataOutputStream(socketToHost.getOutputStream());
            out.writeUTF(commandForHost);
            String response = in.readUTF();
            if(response.equals("OK")){
                out = new DataOutputStream(currentSocket.getOutputStream());
                out.writeUTF("OK " + chosenHost.getAddress() + " " + chosenPort);//todo why address
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private Host findHost(String address) {
        for(Host host: hosts){
            if(host.getAddress().equals(address)){
                return host;
            }
        }
        return null;
    }

    private void loginClient(String phoneNumber, String password) {
//        if(foundRegisterationProblem){//todo
//            return;
//        }
        try {
            in = new DataInputStream(currentSocket.getInputStream());
            out = new DataOutputStream(currentSocket.getOutputStream());

            currentClient = findClient(phoneNumber, password);
            if(currentClient!=null) {
                out.writeUTF("OK");
            }else {
                out.writeUTF("User not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Client findClient(String phoneNumber, String password) {
        for(Client client: clients){
            if(client.getPhoneNumber().equals(phoneNumber) && client.getPassword().equals(password)){
                return client;
            }
        }
        return null;
    }

    public boolean createHost(String hostAddress,int portStartRange,int portEndRange) throws IOException {
        int[] portRange = {portStartRange,portEndRange};
        if (foundHostCreationProblem(portStartRange, portEndRange,portRange, hostAddress)){
            out.writeUTF("NO");
            return false;
        }
        int randomPort = RANDOM.nextInt(portStartRange,portEndRange);
        out.writeUTF("OK " + randomPort);
        String responseCheck = in.readUTF();
        System.out.println(responseCheck);//check
        String code = null;
        if(responseCheck.equals("check")){
            Socket socketToHost = new Socket(hostAddress, randomPort);
            out = new DataOutputStream(socketToHost.getOutputStream());
            in = new DataInputStream(socketToHost.getInputStream());
            code = createRandomCode();
            out.writeUTF("OK " + code);
            socketToHost.close();
        }else{
            return false;
        }
        out = new DataOutputStream(currentSocket.getOutputStream());
        in = new DataInputStream(currentSocket.getInputStream());
        String responseCode = in.readUTF();
        System.out.println(responseCode);//
        System.out.println("=============");//
        if(responseCode.equals(code)){
            out.writeUTF("OK");
//            Thread t = new HostHandler(socket);//todo work with threads
//            t.start();
            hosts.add(new Host(hostAddress , portRange));//todo add host to server
            return true;
        }else{
            out.writeUTF("ERROR invalid code");
            return false;
        }
    }

    private boolean foundHostCreationProblem(int portStartRange, int portEndRange, int[] portRange, String address) {
        if(portRangeConflict(portRange)){
            System.out.println("ERROR Port in use by another host");
            return true;
        }
        if(portStartRange < 10_000) {
            System.out.println("ERROR Port number must be at least 10000");
            return true;
        }
        if((portEndRange - portStartRange) > 1000){
            System.out.println("ERROR At most 1000 ports is allowed");
            return true;
        }
        if(foundDuplicateAddresses(address)){
            System.out.println("Error duplicate addresses detected");
            return true;
        }
        return false;
    }

    private boolean foundDuplicateAddresses(String address) {
        for(Host host: hosts){
            if(host.getAddress().equals(address)){
                return true;
            }
        }
        return false;
    }

    public void registerClient(String phoneNumber, String password){
//        if(foundRegisterationProblem){//todo
//            return;
//        }
        clients.add(new Client(phoneNumber, password,generateID()));
        try {
            out.writeUTF("OK");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean portRangeConflict(int[] portRange){
        if(hosts.isEmpty()){
            return false;
        }
        for(Host host: hosts){
            int hostPortRangeStart = host.getPortRange()[0];
            int hostPortRangeEnd = host.getPortRange()[1];
            int portRangeStart = portRange[0];
            int portRangeEnd = portRange[1];
//            if(hostPortRangeStart < portRangeStart && hostPortRangeEnd > hostPortRangeStart ){
////                System.out.println("2) "+Arrays.toString(host.getPortRange()) +"|"+ Arrays.toString(portRange));
//                return true;
//            }
            if(hostPortRangeStart == portRangeStart && hostPortRangeEnd == portRangeEnd){
                return true;
            }

//            if(host.getPortRange()[0] < portRange[0] && host.getPortRange()[1] > portRange[1] ){
//                return true;
//            }
//            if(host.getPortRange()[0] > portRange[0] && host.getPortRange()[1] < portRange[1]){
//                return true;
//            }
//            if(host.getPortRange()[0] < portRange[0] && host.getPortRange()[1] < portRange[1]){
//                return true;
//            }
//            if(host.getPortRange()[0] > portRange[0] && host.getPortRange()[1] > portRange[1]){
//                return true;
//            }
        }
        return false;
    }
}
