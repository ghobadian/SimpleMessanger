package tech.sobhan;

import static tech.sobhan.DataGenerator.*;


public class HostProgram {
    public static void main(String[] args){
        Host host = createHost();
        host.run();
    }

    private static Host createHost() {
        Host host = Host.builder().build();
        int[] portRange = generatePortRange();
        if(!host.createHostAndRun("create-host " + generateAddress() + " "+portRange[0]+ " "+portRange[1])){
            return createHost();
        }
        return host;
    }
}
