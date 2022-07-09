package tech.sobhan;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Workspace {
    private String name;
    private int port;
    private Host parent;
    private final HashMap<String,Socket> usernameAndSocket;
    private final HashMap<String,JSONArray> usernameAndChats;

    private Workspace(){
        usernameAndSocket = new HashMap<>();
        usernameAndChats = new HashMap<>();
    }

    public Workspace(String name, int port, Host parent) {
        this();
        this.name = name;
        this.port = port;
        this.parent = parent;
    }

    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("workspace "+ name + " started");
            while(true){//todo disconnect
                System.out.println("====================");
                System.out.println("Waiting for a Client ...");
                Socket socketFromClient = serverSocket.accept();
                System.out.println("Client accepted");
                WorkspaceThread workspaceThread = new WorkspaceThread(socketFromClient,this);
                workspaceThread.start();
                System.out.println("socketFromClient = " + socketFromClient);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket findSocketFromUsername(String username) {
        return usernameAndSocket.get(username);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Host getParent() {
        return parent;
    }

    public void setParent(Host parent) {
        this.parent = parent;
    }

    public void save(String usernameOfClient, Socket socketFromClient) {
        usernameAndSocket.put(usernameOfClient, socketFromClient);
    }

    public void save(String username, JSONObject chat) {
        if(usernameAndChats.get(username)==null){
            usernameAndChats.put(username, new JSONArray());
        }
        usernameAndChats.get(username).add(chat);
    }

    public JSONArray findChats(String username) {
        return usernameAndChats.get(username);
    }
}
