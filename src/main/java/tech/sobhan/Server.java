package tech.sobhan;

import lombok.Getter;
import lombok.SneakyThrows;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static tech.sobhan.Constants.SERVER_PORT;
import static tech.sobhan.DataGenerator.*;
import static tech.sobhan.FileHandler.loadFromFile;
import static tech.sobhan.FileHandler.saveToFile;
import static tech.sobhan.Util.sendSignal;

public class Server implements Serializable{
    private final HashMap<String,Socket> hostAndSocket = new HashMap<>();
    @Getter private final ArrayList<Host> hosts = new ArrayList<>();
    @Getter private final ArrayList<Client> clients = new ArrayList<>();
    private Client currentClient = null;

    public void run() {
        Thread userThread = new Thread(this::getInputFromUser);
        userThread.start();

        Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
        otherDevicesThread.start();
    }

    private void getInputFromOtherDevices() {
        loadFromFile(this);
        try (ServerSocket server = new ServerSocket(SERVER_PORT)){
            System.out.println("Server started");
            String response = "";
            while(true){
                System.out.println("====================");
                System.out.println("Waiting for a Host/Client ...");
                Socket currentSocket = server.accept();
                DataInputStream in = new DataInputStream(currentSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(currentSocket.getOutputStream());
                response = in.readUTF();
                System.out.println(response);//
                handleCommand(response,in,out,currentSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void getInputFromUser() {
        String command = "";
        while(!command.equals("shutdown")){
            command = ScannerWrapper.nextLine();
            System.out.println("command = " + command);
            handleLocalCommand(command);
        }
        saveToFile(hosts,clients);
    }

    private void handleLocalCommand(String command) {
        switch(command){
            case "show-hosts" -> hosts.forEach(System.out::println);
        }
    }


    public void handleCommand(String command, DataInputStream in, DataOutputStream out, Socket currentSocket){
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        switch(mainCommand){
            case "create-host" -> createHost(parameters,in,out,currentSocket);
            case "connect-host" -> connectHost(parameters, currentSocket);
            case "register" -> registerClient(parameters,out);
            case "login" -> loginClient(parameters, currentSocket);//todo sus for socket
            case "create-workspace" -> createWorkspace(parameters,currentSocket);
            case "connect-workspace" -> connectClientToWorkspace(parameters, currentSocket);
        }
    }

    @SneakyThrows
    private void connectHost(String[] parameters, Socket currentSocket) {
        DataOutputStream out = new DataOutputStream(currentSocket.getOutputStream());
        String address = parameters[1];
        System.out.println(Arrays.toString(hostAndSocket.entrySet().toArray()));
        hostAndSocket.put(address,currentSocket);
        System.out.println(Arrays.toString(hostAndSocket.entrySet().toArray()));
        out.writeUTF("OK");

    }

    private void connectClientToWorkspace(String[] parameters, Socket socketToClient) {
        String workspaceName = parameters[1]; //todo workspace names should be identical
        Workspace workspace = findWorkspace(workspaceName);
        Token token = Token.builder().build();
        try {
            DataOutputStream out = new DataOutputStream(socketToClient.getOutputStream());//todo make currentSocket local or remove it
            String responseToClient = "OK "+ workspace.getAddress() +" "+ workspace.getPort() +" "+ token.getToken();
            out.writeUTF(responseToClient);//todo
            Socket socketToHostOfThisWorkspace = hostAndSocket.get(workspace.getAddress());
            DataInputStream in = new DataInputStream(socketToHostOfThisWorkspace.getInputStream());
            out = new DataOutputStream(socketToHostOfThisWorkspace.getOutputStream());
            String whois = in.readUTF();
            System.out.println(whois);//
            if(!whois.startsWith("whois") || !token.getToken().equals(whois.split(" ")[1])  ||
                    token.checkExpiration()){
                return ;
            }
            out.writeUTF("OK " + currentClient.getId());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createWorkspace(String[] parameters, Socket currentSocket) {
        String nameOfWorkspace = parameters[1];
        Host chosenHost = chooseARandomHost();
        int chosenPort = RANDOM.nextInt(chosenHost.getPortRange()[0],chosenHost.getPortRange()[1]);
        String commandForHost = "create-workspace " + nameOfWorkspace + " " + chosenPort +" "+ currentClient.getId();
        if(!requestCreateWorkSpaceFromHost(chosenHost, chosenPort,commandForHost, currentSocket)){
            System.out.println("error");
            return;
        }
    }

    private Host chooseARandomHost() {
        Host host = hosts.get(RANDOM.nextInt(0, hosts.size()));
        if(hostAndSocket.get(host.getAddress())==null){
            return chooseARandomHost();
        }
        return host;
    }

    private void loginClient(String[] parameters, Socket currentSocket) {
        String phoneNumber = parameters[1];
        String password = parameters[2];
        currentClient = findClient(phoneNumber, password);
        if(foundLoginProblem(currentSocket)){//todo is already connected
            sendSignal(currentSocket, "User not found");
            return;
        }
        sendSignal(currentSocket, "OK");
//        try {
//            DataOutputStream out = new DataOutputStream(currentSocket.getOutputStream());
//            out.writeUTF("OK");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private boolean foundLoginProblem(Socket currentSocket) {
        if(currentClient==null) {
            return true;
        }
        return false;
    }

    private void registerClient(String[] parameters, DataOutputStream out) {
        String phoneNumber = parameters[1];
        String password = parameters[2];
        registerClient(phoneNumber, password, out);
    }

    private void createHost(String[] parameters, DataInputStream in, DataOutputStream out, Socket currentSocket) {
        String ip = parameters[1];
        int portStartRange = Integer.parseInt(parameters[2]);
        int portEndRange = Integer.parseInt(parameters[3]);
        if(!createHost(ip, portStartRange, portEndRange, in, out, currentSocket)){
            return;
        }
        hostAndSocket.put(hosts.get(hosts.size()-1).getAddress(),currentSocket);
    }

    private Workspace findWorkspace(String workspaceName) {
        for(Host host : hosts){
            for(Workspace workspace : host.getWorkspaces()){
                if(workspace.getName().equals(workspaceName)){
                    return workspace;
                }
            }
        }
        return null;
    }

    private boolean requestCreateWorkSpaceFromHost(Host chosenHost, int chosenPort, String commandForHost, Socket currentSocket) {
        try {
            Socket socketToHost = hostAndSocket.get(chosenHost.getAddress());
            DataInputStream in = new DataInputStream(socketToHost.getInputStream());
            DataOutputStream out = new DataOutputStream(socketToHost.getOutputStream());
            out.writeUTF(commandForHost);
            String response = in.readUTF();
            if(!response.equals("OK")){
                System.out.println("error");
                return false;
            }
            out = new DataOutputStream(currentSocket.getOutputStream());
            out.writeUTF("OK " + chosenHost.getAddress() + " " + chosenPort);//todo why address
            saveWorkspace(chosenHost, commandForHost);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void saveWorkspace(Host parent, String command) {
        String[] parameters = command.split(" ");
        String nameOfWorkSpace = parameters[1];//todo it is duplicate with the ones in createWorkspace() in Host class
        int port = Integer.parseInt(parameters[2]);
        int clientID = Integer.parseInt(parameters[3]);//todo find usage
        parent.addWorkspace(new Workspace(nameOfWorkSpace,port, parent.getAddress(), parent.getSocketToServer()));//todo i don't like it this way

    }

    private Host findHost(String address) {
        for(Host host: hosts){
            if(host.getAddress().equals(address)){
                return host;
            }
        }
        return null;
    }

    private Client findClient(String phoneNumber, String password) {
        for(Client client: clients){
            if(client.getPhoneNumber().equals(phoneNumber) && client.getPassword().equals(password)){
                return client;
            }
        }
        return null;
    }

    public boolean createHost(String hostAddress, int portStartRange, int portEndRange, DataInputStream in, DataOutputStream out, Socket currentSocket){
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
            String code = createRandomCode();
            sendCodeToHost(hostAddress, randomPort, code);
            String responseCode = in.readUTF();
            System.out.println(responseCode);//
            if(!responseCode.equals(code)){
                out.writeUTF("ERROR invalid code");
                return false;
            }
            out.writeUTF("OK");
            hosts.add(Host.builder().address(hostAddress).portRange(portRange).build());//todo add host to server
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void sendCodeToHost(String hostAddress, int randomPort, String code) throws IOException {
        Socket socketToHost = new Socket(hostAddress, randomPort);
        DataOutputStream out2 = new DataOutputStream(socketToHost.getOutputStream());
        out2.writeUTF("OK " + code);
        socketToHost.close();
        out2.close();
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
        clients.add(Client.builder().phoneNumber(phoneNumber).password(password).id(generateID()).build());
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

    public void addHost(Host host){
        hosts.add(host);
    }


    public void addClient(Client client){
        clients.add(client);
    }


}
