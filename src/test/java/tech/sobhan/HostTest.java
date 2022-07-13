package tech.sobhan;

import org.junit.Test;

public class HostTest {
    @Test
    public void toStringTest(){
        Host host = Host.builder().address("127.1.1.69").portRange(new int[]{10000,10100}).build();
        host.addWorkspace(new Workspace("company1",10001, host.getAddress(), host.getSocketToServer()));
        host.addWorkspace(new Workspace("company2",10062, host.getAddress(), host.getSocketToServer()));
        host.addWorkspace(new Workspace("company3",10043, host.getAddress(), host.getSocketToServer()));
        System.out.println(host);
    }
}
