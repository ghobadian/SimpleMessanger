package tech.sobhan;

// A Java program for a Server
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import static tech.sobhan.Constants.SERVER_PORT;
import static tech.sobhan.DataGenerator.*;

public class Server {
//    private final ServerSocket server = null;
    private final HashMap<Host,Socket> hostAndSocket;
    private final HashMap<Token,Integer> tokenAndID;
    private Socket currentSocket = null;//todo meh
    private final ArrayList<Host> hosts;
    private final ArrayList<Workspace> workspaces;
    private final ArrayList<Client> clients;
    private Client currentClient = null;


    public Server() {
        hosts = new ArrayList<>();
        workspaces = new ArrayList<>();
        clients = new ArrayList<>();
        hostAndSocket = new HashMap<>();
        tokenAndID = new HashMap<>();
    }

    public void run() {
        try (ServerSocket server = new ServerSocket(SERVER_PORT)){
            System.out.println("Server started");
            String response = "";
            while(true){
                System.out.println("====================");
                System.out.println("Waiting for a Host/Client ...");
                currentSocket = server.accept();
                DataInputStream in = new DataInputStream(currentSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(currentSocket.getOutputStream());
                response = in.readUTF();
                System.out.println(response);//
                handleCommand(response,in,out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleCommand(String command, DataInputStream in, DataOutputStream out){
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        switch(mainCommand){
            case "create-host" -> createHost(parameters,in,out);
            case "register" -> registerClient(parameters,out);
            case "login" -> loginClient(parameters);
            case "create-workspace" -> createWorkspace(parameters);
            case "connect-workspace" -> connectClientToWorkspace(parameters);
        }
    }

    private void connectClientToWorkspace(String[] parameters) {
        String workspaceName = parameters[1]; //todo workspace names should be identical
        connectClientToWorkSpace(workspaceName);
    }

    private void createWorkspace(String[] parameters) {
        String nameOfWorkspace = parameters[1];
        Host chosenHost = hosts.get(RANDOM.nextInt(0,hosts.size()));
        int chosenPort = RANDOM.nextInt(chosenHost.getPortRange()[0],chosenHost.getPortRange()[1]);
        String commandForHost = "create-workspace " + nameOfWorkspace + " " + chosenPort +" "+ currentClient.getId();
        if(requestCreateWorkSpaceFromHost(chosenHost, chosenPort,commandForHost)){
            workspaces.add(new Workspace(nameOfWorkspace,chosenPort,chosenHost));
        }
    }

    private void loginClient(String[] parameters) {
        String phoneNumber = parameters[1];
        String password = parameters[2];
        loginClient(phoneNumber,password);
    }

    private void registerClient(String[] parameters, DataOutputStream out) {
        String phoneNumber = parameters[1];
        String password = parameters[2];
        registerClient(phoneNumber, password, out);
    }

    private void createHost(String[] parameters, DataInputStream in, DataOutputStream out) {
        String ip = parameters[1];
        int portStartRange = Integer.parseInt(parameters[2]);
        int portEndRange = Integer.parseInt(parameters[3]);
        if(!createHost(ip, portStartRange, portEndRange, in, out)){
            return;
        }
        hostAndSocket.put(hosts.get(hosts.size()-1),currentSocket);
    }

    private boolean connectClientToWorkSpace(String workspaceName) {
        Workspace workspace = findWorkspace(workspaceName);
        Token token = new Token();
        tokenAndID.put(token,currentClient.getId());
        try {
            DataOutputStream out = new DataOutputStream(currentSocket.getOutputStream());//todo make currentSocket local or remove it
            String responseToClient = "OK "+ workspace.getParent().getAddress() +" "+ workspace.getPort() +" "+ token;
            out.writeUTF(responseToClient);//todo
            Socket socketToHostOfThisWorkspace = hostAndSocket.get(workspace.getParent());
            DataInputStream in = new DataInputStream(socketToHostOfThisWorkspace.getInputStream());
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

    private boolean loginClient(String phoneNumber, String password) {
//        if(foundRegisterationProblem){//todo
//            return;
//        }
        currentClient = findClient(phoneNumber, password);
        try {
            DataOutputStream out = new DataOutputStream(currentSocket.getOutputStream());
            if(currentClient==null) {
                out.writeUTF("User not found");
                return false;
            }
            out.writeUTF("OK");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private Client findClient(String phoneNumber, String password) {
        for(Client client: clients){
            if(client.getPhoneNumber().equals(phoneNumber) && client.getPassword().equals(password)){
                return client;
            }
        }
        return null;
    }

    public boolean createHost(String hostAddress, int portStartRange, int portEndRange, DataInputStream in, DataOutputStream out){
        try{
            int[] portRange = {portStartRange,portEndRange};
            if (foundHostCreationProblem(portStartRange, portEndRange,portRange, hostAddress)){
                out.writeUTF("NO");
                return false;
            }
            int randomPort = RANDOM.nextInt(portStartRange,portEndRange);
            out.writeUTF("OK " + randomPort);
            String responseCheck = in.readUTF();
            System.out.println(responseCheck);//check
            if(!responseCheck.equals("check")){
                return false;
            }
            Socket socketToHost = new Socket(hostAddress, randomPort);
            out = new DataOutputStream(socketToHost.getOutputStream());
            in = new DataInputStream(socketToHost.getInputStream());
            String code = createRandomCode();
            out.writeUTF("OK " + code);
            socketToHost.close();
            out = new DataOutputStream(currentSocket.getOutputStream());
            in = new DataInputStream(currentSocket.getInputStream());
            String responseCode = in.readUTF();
            System.out.println(responseCode);//
            if(!responseCode.equals(code)){
                out.writeUTF("ERROR invalid code");
                return false;
            }

            out.writeUTF("OK");
//            Thread t = new HostHandler(socket);//todo work with threads
//            t.start();
            hosts.add(new Host(hostAddress , portRange));//todo add host to server
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
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

    public void registerClient(String phoneNumber, String password, DataOutputStream out){
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
