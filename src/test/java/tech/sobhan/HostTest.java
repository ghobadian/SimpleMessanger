package tech.sobhan;

import org.junit.Test;
import tech.sobhan.host.Host;
import tech.sobhan.workspace.Workspace;

public class HostTest {
    @Test
    public void toStringTest(){
        Host host = Host.builder().address("127.1.1.69").portRange(new int[]{10000,10100}).build();
        host.addWorkspace(Workspace.builder().workspaceName("company1").port(10001)
                .address(host.getAddress()).socketToServer(host.getSocketToServer()).build());
        host.addWorkspace(Workspace.builder().workspaceName("company2").port(10062)
                .address(host.getAddress()).socketToServer(host.getSocketToServer()).build());
        host.addWorkspace(Workspace.builder().workspaceName("company3").port(10043)
                .address(host.getAddress()).socketToServer(host.getSocketToServer()).build());
        System.out.println(host);
    }
}
