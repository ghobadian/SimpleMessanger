package tech.sobhan.utils;

import java.security.SecureRandom;
import java.util.Arrays;

public class DataGenerator {
    private static int id = 1000;

    public static final SecureRandom RANDOM = new SecureRandom();

    private static int[] portRange = {10000,10100};

    private static String address = "127.0.0.0";

    public static int generateID(){
        return RANDOM.nextInt(1000,10000);//check for duplication
    }

    public static int[] generatePortRange(){
        int seed = RANDOM.nextInt(9);
        return new int[]{10000 + seed * 100, 10100 + seed * 100};
    }

    public static String generateAddress(){
        return address.substring(0,address.length()-1) + RANDOM.nextInt(10);
    }

    private static String moveOnePoint(String address) {
        int[] octets = Arrays.stream(address.split("\\.")).mapToInt(Integer::parseInt).toArray();
        for(int i=3;i>=0;i--){
            if(octets[i]<255){
                octets[i]++;
                return octets[0] + "." +octets[1] + "." +octets[2] + "." +octets[3];
            }
            octets[i] = 0;
        }
        System.out.println("all addresses are occupied");
        return null;
    }

    public static String createRandomCode(){
        StringBuilder output = new StringBuilder();
        for(int i=0;i<10;i++){
            output.append(RANDOM.nextInt(0,10));
        }
        return String.valueOf(output);
    }

    public static String generateToken(){
        StringBuilder output = new StringBuilder();
        for(int i=0;i<10;i++){
            output.append(RANDOM.nextBoolean() ? RANDOM.nextInt(10) :
                    String.valueOf((char) RANDOM.nextInt(97,123)));
        }
        return String.valueOf(output);
    }

    public static String generatePhoneNumber(){
        StringBuilder output = new StringBuilder("09");
        for(int i=0;i<9;i++){
            output.append(RANDOM.nextInt(10));
        }
        return output.toString();
    }

    public static int generateOneDigitNumber(){
        return RANDOM.nextInt(10);
    }
}
