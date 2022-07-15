package tech.sobhan;

import java.io.*;
import java.util.ArrayList;

public class FileHandler {
    public static void loadFromFile(Server server) {
        try {
            loadHosts(server);
            loadClients(server);
        } catch (IOException e) {
            System.out.println("no previous files were found to load");
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    private static void loadClients(Server server) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("src/main/resources/server/clients.txt"));
        ArrayList<Client> clients = (ArrayList<Client>) in.readObject();
        for (Client client : clients) {
            server.addClient(client);
        }
        in.close();
    }

    private static void loadHosts(Server server) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream("src/main/resources/server/hosts.txt"));
        ArrayList<Host> hosts = (ArrayList<Host>) in.readObject();
        for (Host host : hosts) {
            server.addHost(host);
        }
        in.close();
    }

    public static void saveToFile(ArrayList<Host> hosts, ArrayList<Client> clients) {
        try {
            saveHosts(hosts);
            saveClients(clients);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveClients(ArrayList<Client> clients) throws IOException {
        FileOutputStream fout = new FileOutputStream("src/main/resources/server/clients.txt");
        ObjectOutputStream out=new ObjectOutputStream(fout);
        out.writeObject(clients);
        out.flush();
        out.close();
    }

    private static void saveHosts(ArrayList<Host> hosts) throws IOException {
        FileOutputStream fout = new FileOutputStream("src/main/resources/server/hosts.txt");
        ObjectOutputStream out=new ObjectOutputStream(fout);
        out.writeObject(hosts);
        out.flush();
        out.close();
    }
}
