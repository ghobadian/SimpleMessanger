package tech.sobhan;

import java.util.Scanner;

public class ScannerWrapper {
    private static final Scanner scanner = new Scanner(System.in);
    public static String nextLine(){
        return scanner.nextLine();
    }
}
