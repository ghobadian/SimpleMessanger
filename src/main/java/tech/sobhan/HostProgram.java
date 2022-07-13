package tech.sobhan;

import static tech.sobhan.DataGenerator.*;


public class HostProgram {
    public static void main(String[] args){
        Host host = Host.builder().build();
        int[] portRange = generatePortRange();
        host.handleCommand("create-host " + generateAddress() + " "+portRange[0]+ " "+portRange[1]);
        host.run();
    }
}
