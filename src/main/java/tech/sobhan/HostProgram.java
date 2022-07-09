package tech.sobhan;


import java.util.Arrays;

import static tech.sobhan.DataGenerator.*;


public class HostProgram {
    public static void main(String[] args){
        String address = generateAddress();//todo
        int[] portRange = generatePortRange();
        Host host = new Host();
        System.out.println("address = " + address);
        System.out.println("portRange = " + Arrays.toString(portRange));
        if(host.requestCreatingHost("create-host " + address + " "+portRange[0]+ " "+portRange[1])){
            host.run();
        }
    }



    public void runHost(int number){

    }





}
