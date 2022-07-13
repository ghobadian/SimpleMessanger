package tech.sobhan;

import org.junit.Test;

import static tech.sobhan.FileHandler.loadFromFile;
import static tech.sobhan.FileHandler.saveToFile;

public class ServerTest {
    private static final Server server = new Server();
    @Test
    public void saveToFileTest(){
//        Host host1 = new Host("127.1.1.69", new int[]{10000,10100});
        Host host1 = Host.builder().address("127.1.1.69").portRange(new int[]{10000,10100}).build();
        host1.addWorkspace(new Workspace("company1",10001, host1.getAddress(), host1.getSocketToServer()));
        host1.addWorkspace(new Workspace("company2",10062, host1.getAddress(), host1.getSocketToServer()));
        host1.addWorkspace(new Workspace("company3",10043, host1.getAddress(), host1.getSocketToServer()));
        server.addHost(host1);

        server.addClient(Client.builder().phoneNumber("09031023519").password("fsdfhlahf").id(1546).build());
        server.addClient(Client.builder().phoneNumber("09031045859").password("vcxdaswe").id(6487).build());
        server.addClient(Client.builder().phoneNumber("09365468152").password("fasdgsxv").id(1235).build());
        saveToFile(server.getHosts(), server.getClients());
    }

    @Test
    public void loadFromFileTest(){
        loadFromFile(server);
//        for (Workspace workspace : server.getWorkspaces()) {
//            System.out.println(workspace);
//        }
    }
}
