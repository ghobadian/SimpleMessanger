package tech.sobhan.server;

import lombok.Getter;
import tech.sobhan.utils.ScannerWrapper;
import tech.sobhan.models.Token;
import tech.sobhan.utils.Util;
import tech.sobhan.workspace.Workspace;
import tech.sobhan.client.Client;
import tech.sobhan.host.Host;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import static tech.sobhan.utils.DataGenerator.*;
import static tech.sobhan.utils.FileHandler.loadFromFile;
import static tech.sobhan.utils.FileHandler.saveToFile;
import static tech.sobhan.utils.Util.receiveSignal;
import static tech.sobhan.utils.Util.sendSignal;

public class Server implements Serializable {
    public static final int SERVER_PORT = 8000;
    public static final String SERVER_ADDRESS = "127.10.10.10";
    private final HashMap<String, Socket> hostAndSocket = new HashMap<>();
    @Getter
    private final ArrayList<Host> hosts = new ArrayList<>();
    @Getter
    private final ArrayList<Client> clients = new ArrayList<>();
    private final ArrayList<Host> connectedHosts = new ArrayList<>();
    private final ArrayList<String> nameOfCreatedWorkspaces = new ArrayList<>();
    private final ArrayList<Integer> usedPorts = new ArrayList<>();
    private Client currentClient = null;

    public void run() {
        Thread userThread = new Thread(this::getInputFromUser);
        userThread.start();

        Thread otherDevicesThread = new Thread(this::getInputFromOtherDevices);
        otherDevicesThread.start();
    }

    private void getInputFromOtherDevices() {
        loadFromFile(this);
        try (ServerSocket server = new ServerSocket(SERVER_PORT)) {
            while (true) {
                Socket currentSocket = server.accept();
                String response = receiveSignal(currentSocket);
                System.out.println(response);//
                handleCommand(response, currentSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void getInputFromUser() {
        String command = "";
        while (!command.equals("shutdown")) {
            command = ScannerWrapper.nextLine();
            handleLocalCommand(command);
        }
        saveToFile(hosts, clients);
    }

    private void handleLocalCommand(String command) {
        switch(command){
            case "show-hosts" -> showList(hosts);
            case "show-active-hosts" -> showList(connectedHosts);
            case "show-clients" -> showList(clients);
            default -> System.out.println("ERROR unknown command");
        }
    }

    private void showList(ArrayList<?> list) {
        if(list.isEmpty()){
            System.out.println("ERROR nothing was found");
            return;
        }
        list.forEach(System.out::println);
    }

    public void handleCommand(String command, Socket currentSocket) {
        String[] parameters = command.split(" ");
        String mainCommand = parameters[0];
        switch (mainCommand) {
            case "create-host" -> createHost(parameters, currentSocket);
            case "connect-host" -> connectHost(parameters, currentSocket);
            case "register" -> registerClient(parameters, currentSocket);
            case "login" -> loginClient(parameters, currentSocket);//todo sus for socket
            case "create-workspace" -> createWorkspace(parameters, currentSocket);
            case "connect-workspace" -> connectClientToWorkspace(parameters, currentSocket);
            case "show-workspaces" -> showWorkspaces(currentSocket);
        }
    }

    private void showWorkspaces(Socket socketToClient) {
        StringBuilder signal = new StringBuilder();
        getNameOfWorkspacesFromHost(signal);
        if(signal.isEmpty()) {
            sendSignal(socketToClient, "no workspaces found");
            return;
        }
        sendSignal(socketToClient, signal.toString());
    }

    private void getNameOfWorkspacesFromHost(StringBuilder signal) {
        for(Host host : connectedHosts){
            Socket socketToHost = hostAndSocket.get(host.getAddress());
            sendSignal(socketToHost, "show-workspaces");
            String workspaces = receiveSignal(socketToHost);
            if(workspaces.startsWith("OK")){
                signal.append(workspaces.split(" ", 2)[1]);
            }
        }
    }

    private void connectHost(String[] parameters, Socket socketToHost) {
        if (foundHostConnectionProblem(parameters, socketToHost)) {
            return;
        }

        String address = parameters[1];

        hostAndSocket.put(address, socketToHost);
        sendSignal(socketToHost, "OK");
    }

    private boolean foundHostConnectionProblem(String[] parameters, Socket socketToHost) {
        if(Util.foundProblemInParameters(parameters.length, 2, socketToHost)){
            return true;
        }

        String address = parameters[1];
        if (address == null) {
            sendSignal(socketToHost, "ERROR Some parameters are missing");
            return true;
        }

        return false;
    }

    private void connectClientToWorkspace(String[] parameters, Socket socketToClient) {
        if(foundWorkspaceConnectionProblem(parameters, socketToClient)){
            return;
        }
        String workspaceName = parameters[1];
        Workspace workspace = findWorkspace(workspaceName);
        Token token = Token.builder().build();
        String responseToClient = "OK " + workspace.getAddress() + " " + workspace.getPort() + " " + token.getToken();
        sendSignal(socketToClient, responseToClient);
        Socket socketToHostOfThisWorkspace = hostAndSocket.get(workspace.getAddress());
        String whois = receiveSignal(socketToHostOfThisWorkspace);
        System.out.println(whois);//
        if (foundWorkspaceConnectionWhoisAuthorizationProblem(whois, token, socketToHostOfThisWorkspace)) {
            return;
        }

        sendSignal(socketToHostOfThisWorkspace, "OK " + currentClient.getId());
    }

    private boolean foundWorkspaceConnectionProblem(String[] parameters, Socket socketToClient) {
        if (Util.foundProblemInParameters(parameters.length, 2, socketToClient)) {
            return true;
        }

        String workspaceName = parameters[1];
        if (findWorkspace(workspaceName) == null) {
            sendSignal(socketToClient, "ERROR workspace not found");
            return true;
        }

        return false;
    }

    private boolean foundWorkspaceConnectionWhoisAuthorizationProblem(String whois, Token token, Socket socketToHostOfThisWorkspace) {
        if (!whois.startsWith("whois")) {
            sendSignal(socketToHostOfThisWorkspace, "ERROR wrong answer");
            return true;
        }

        String receivedToken = whois.split(" ")[1];
        if (!token.getToken().equals(receivedToken)) {
            sendSignal(socketToHostOfThisWorkspace, "ERROR tokens are identical");
            return true;
        }

        if(token.checkExpiration()){
            sendSignal(socketToHostOfThisWorkspace, "ERROR token is expired");
            return true;
        }
        return false;
    }

    private void createWorkspace(String[] parameters, Socket socketToClient) {
        Host chosenHost = chooseARandomHost();
        if(foundWorkspaceCreationProblem(chosenHost, parameters, socketToClient)){
            return;
        }

        String nameOfWorkspace = parameters[1];

        int chosenPort = chooseARandomPort(chosenHost.getPortRange()[0], chosenHost.getPortRange()[1]);
        String commandForHost = "create-workspace " + nameOfWorkspace + " " + chosenPort + " " + currentClient.getId();
        requestCreateWorkSpaceFromHost(chosenHost, chosenPort, commandForHost, socketToClient);
    }

    private boolean foundWorkspaceCreationProblem(Host chosenHost, String[] parameters, Socket socketToClient) {
        if (Util.foundProblemInParameters(parameters.length, 2, socketToClient)) {
            return true;
        }

        if(chosenHost == null){
            sendSignal(socketToClient, "ERROR No active hosts were found");
            return true;
        }

        String nameOfWorkspace = parameters[1];
        if(nameOfCreatedWorkspaces.stream().anyMatch(n -> n.equals(nameOfWorkspace))){
            sendSignal(socketToClient, "ERROR Workspace with this name already exists");
            return true;
        }

        return false;
    }

    public Host chooseARandomHost() {
        if (connectedHosts.isEmpty()) {
            System.err.println("no active hosts were found");
            return null;
        }
        return connectedHosts.get(RANDOM.nextInt(0, connectedHosts.size()));
    }

    private void loginClient(String[] parameters, Socket socketToClient) {
        if (foundLoginProblem(parameters, socketToClient)) {//todo is already connected
            return;
        }
        String phoneNumber = parameters[1];
        String password = parameters[2];
        Client client = findClient(phoneNumber, password);
        currentClient = client;
        sendSignal(socketToClient, "OK");
    }

    private boolean foundLoginProblem(String[] parameters, Socket socketToClient) {
        if (Util.foundProblemInParameters(parameters.length, 3, socketToClient)) {
            return true;
        }

        String phoneNumber = parameters[1];
        String password = parameters[2];
        if (findClient(phoneNumber, password) == null) {
            sendSignal(socketToClient, "User not found");
            return true;
        }

        return false;
    }

    private void registerClient(String[] parameters, Socket socket) {
        if (foundRegistrationProblem(parameters, socket)) {//todo
            return;
        }

        String phoneNumber = parameters[1];
        String password = parameters[2];

        clients.add(Client.builder().phoneNumber(phoneNumber).password(password).id(generateID()).build());
        sendSignal(socket, "OK");
    }

    private boolean foundRegistrationProblem(String[] parameters, Socket socket) {
        if (Util.foundProblemInParameters(parameters.length, 3, socket)) {
            return true;
        }
        String phoneNumber = parameters[1];
        String password = parameters[2];
        if (clients.stream().anyMatch(client -> client.getPhoneNumber().equals(phoneNumber))) {
            sendSignal(socket, "ERROR Somebody has already registered with this phone number");
            return true;
        }
        if (password.length() < 8) {
            sendSignal(socket, "ERROR password is too short");
            return true;
        }

        if (!phoneNumber.matches("\\d{10,11}")) {
            sendSignal(socket, "ERROR invalid phone number");
            return true;
        }

        return false;
    }

    private void createHost(String[] parameters, Socket socketToHost) {
        if (foundHostCreationProblem(parameters, socketToHost)) {
            return;
        }
        int portStartRange = Integer.parseInt(parameters[2]);
        int portEndRange = Integer.parseInt(parameters[3]);
        int randomPort = chooseARandomPort(portStartRange, portEndRange);
        sendSignal(socketToHost, "OK " + randomPort);
        String responseCheck = receiveSignal(socketToHost);
        System.out.println(responseCheck);//check
        if (foundProblemInChecking(socketToHost, responseCheck)) return;
        String hostAddress = parameters[1];
        if (!sendAndReceiveCodeToHost(socketToHost, hostAddress, randomPort)) return;
        sendSignal(socketToHost, "OK");
        saveHost(socketToHost, hostAddress, portStartRange, portEndRange);
    }

    private boolean foundProblemInChecking(Socket socketToHost, String responseCheck) {
        if (!responseCheck.equals("check")) {
            sendSignal(socketToHost, "Error sth wrong from host side happened");
            return true;
        }
        return false;
    }

    private int chooseARandomPort(int portStartRange, int portEndRange) {
        int port = RANDOM.nextInt(portStartRange, portEndRange);
        if(usedPorts.contains(port)){
            return chooseARandomPort(portStartRange, portEndRange);
        }else{
            usedPorts.add(port);
            return port;
        }
    }

    private void saveHost(Socket socketToHost, String hostAddress, int portStartRange, int portEndRange) {
        int[] portRange = {portStartRange, portEndRange};
        Host host = Host.builder().address(hostAddress).portRange(portRange).build();
        hosts.add(host);
        connectedHosts.add(host);
        hostAndSocket.put(host.getAddress(), socketToHost);
    }

    private boolean sendAndReceiveCodeToHost(Socket firstSocketToHost, String hostAddress, int randomPort) {
        String code = createRandomCode();
        try (Socket secondSocketToHost = new Socket(hostAddress, randomPort)){
            sendSignal(secondSocketToHost, "OK " + code);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String responseCode = receiveSignal(firstSocketToHost);
        System.out.println(responseCode);//
        if (!responseCode.equals(code)) {
            sendSignal(firstSocketToHost, "ERROR invalid code");
            return false;
        }
        return true;
    }

    private Workspace findWorkspace(String workspaceName) {
        for (Host host : connectedHosts) {
            for (Workspace workspace : host.getWorkspaces()) {
                if (workspace.getWorkspaceName().equals(workspaceName)) {
                    return workspace;
                }
            }
        }
        return null;
    }

    private boolean requestCreateWorkSpaceFromHost(Host chosenHost, int chosenPort, String commandForHost, Socket socketToClient) {
        Socket socketToHost = hostAndSocket.get(chosenHost.getAddress());
        sendSignal(socketToHost, commandForHost);
        String response = receiveSignal(socketToHost);
        System.out.println(response);
        if (response.equals("ERROR")) {
            sendSignal(socketToClient, response);
            return false;
        }
        sendSignal(socketToClient, "OK " + chosenHost.getAddress() + " " + chosenPort);//todo why address
        saveWorkspace(chosenHost, commandForHost);
        return true;
    }

    private void saveWorkspace(Host parent, String command) {
        String[] parameters = command.split(" ");
        String nameOfWorkSpace = parameters[1];//todo it is duplicate with the ones in createWorkspace() in Host class
        int port = Integer.parseInt(parameters[2]);
        int clientID = Integer.parseInt(parameters[3]);//todo find usage
        parent.addWorkspace(Workspace.builder().workspaceName(nameOfWorkSpace).port(port).
                address(parent.getAddress()).socketToServer(parent.getSocketToServer()).build());
        nameOfCreatedWorkspaces.add(nameOfWorkSpace);

    }

    private Client findClient(String phoneNumber, String password) {
        for (Client client : clients) {
            if (client.getPhoneNumber().equals(phoneNumber) && client.getPassword().equals(password)) {
                return client;
            }
        }
        return null;
    }

    private void sendCodeToHost(String hostAddress, int randomPort, String code) {


    }

    private boolean foundHostCreationProblem(String[] parameters,  Socket socketToHost) {
        if (Util.foundProblemInParameters(parameters.length, 4, socketToHost)) {
            return true;
        }

        int portStartRange = Integer.parseInt(parameters[2]);
        int portEndRange = Integer.parseInt(parameters[3]);
        int[] portRange = {portStartRange, portEndRange};

        if (portRangeConflict(portRange)) {
            sendSignal(socketToHost, "ERROR Port in use by another host");
            return true;
        }

        if (portStartRange < 10_000) {
            sendSignal(socketToHost, "ERROR Port number must be at least 10000");
            return true;
        }

        if ((portEndRange - portStartRange) > 1000) {
            sendSignal(socketToHost, "ERROR At most 1000 ports is allowed");
            return true;
        }

        String hostAddress = parameters[1];
        if (foundDuplicateAddresses(hostAddress)) {
            sendSignal(socketToHost, "Error this address is already in use");
            return true;
        }
        return false;
    }

    private boolean foundDuplicateAddresses(String address) {
        if(address.equals(SERVER_ADDRESS)){
            return true;
        }
        for (Host host : hosts) {
            if (host.getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    public boolean portRangeConflict(int[] portRange) {
        if (hosts.isEmpty()) {
            return false;
        }
        for (Host host : hosts) {
            int hostPortRangeStart = host.getPortRange()[0];
            int hostPortRangeEnd = host.getPortRange()[1];
            int portRangeStart = portRange[0];
            int portRangeEnd = portRange[1];
            if (hostPortRangeStart == portRangeStart && hostPortRangeEnd == portRangeEnd) {
                return true;
            }
        }
        return false;
    }

    public void addHost(Host host) {
        hosts.add(host);
    }


    public void addClient(Client client) {
        clients.add(client);
    }


}
