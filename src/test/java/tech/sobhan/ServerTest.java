package tech.sobhan;

import org.junit.Test;
import tech.sobhan.client.Client;
import tech.sobhan.host.Host;
import tech.sobhan.server.Server;
import tech.sobhan.workspace.Workspace;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static tech.sobhan.utils.FileHandler.loadFromFile;
import static tech.sobhan.utils.FileHandler.saveToFile;

public class ServerTest {
    private static final Server server = new Server();
    @Test
    public void saveToFileTest(){
//        Host host1 = new Host("127.1.1.69", new int[]{10000,10100});
        Host host1 = Host.builder().address("127.1.1.69").portRange(new int[]{10000,10100}).build();
        host1.addWorkspace(Workspace.builder().workspaceName("company1").port(10001)
                .address(host1.getAddress()).socketToServer(host1.getSocketToServer()).build());
        host1.addWorkspace(Workspace.builder().workspaceName("company2").port(10062)
                .address(host1.getAddress()).socketToServer(host1.getSocketToServer()).build());
        host1.addWorkspace(Workspace.builder().workspaceName("company3").port(10043)
                .address(host1.getAddress()).socketToServer(host1.getSocketToServer()).build());
        server.addHost(host1);

        server.addClient(Client.builder().phoneNumber("09031023519").password("fsdfhlahf").id(1546).build());
        server.addClient(Client.builder().phoneNumber("09031045859").password("vcxdaswe").id(6487).build());
        server.addClient(Client.builder().phoneNumber("09365468152").password("fasdgsxv").id(1235).build());
        saveToFile(server.getHosts(), server.getClients());
    }

    @Test
    public void loadFromFileTest(){
        loadFromFile(server);
    }

    @Test
    public void chooseARandomHostTest(){
        loadFromFile(server);
        assertNull(server.chooseARandomHost());
        Host host = Host.builder().address("1.2.3.4").portRange(new int[]{4,5}).socketToServer(new Socket()).build();
        server.addHost(host);
        assertEquals(server.chooseARandomHost(), host);
    }
}
