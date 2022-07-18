package tech.sobhan.host;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static tech.sobhan.utils.DataGenerator.*;


public class HostProgram {
    public static void main(String[] args){
        Host host = createHost();
        host.run();
    }

    private static Host createHost() {
        Host host = Host.builder().build();
        int[] portRange = generatePortRange();
        if(!host.createHostAndRun("create-host " + generateAddress() + " "+portRange[0]+ " "+portRange[1])){
            sleep();
            return createHost();
        }
        return host;
    }

    private static final AtomicLong timeout = new AtomicLong(5000);
    private static void sleep(){
        try {
            Thread.sleep(timeout.getAndAdd(5000));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
