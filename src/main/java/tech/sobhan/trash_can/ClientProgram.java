package tech.sobhan.trash_can;// A simple Client Server Protocol .. Client for Echo Server

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ClientProgram {

    public static void main(String args[]) throws IOException{
        try {
            InetAddress address = InetAddress.getLocalHost();
            Socket s1=new Socket(address, 4445); // You can use static final constant PORT_NUM
            Scanner scanner = new Scanner(System.in);
            DataInputStream in = new DataInputStream(s1.getInputStream());
            DataOutputStream out = new DataOutputStream(s1.getOutputStream());
            System.out.println("Client Address : " + address);
            System.out.println("Enter Data to echo Server ( Enter QUIT to end):");

            String response;
            String line;
            line=scanner.nextLine();
            while(line.compareTo("QUIT")!=0){
                out.writeUTF(line);
                response=in.readUTF();
                System.out.println("Server Response : "+response);
                line = scanner.nextLine();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
//        finally{
//            in.close();os.close();out.close();s1.close();
//            System.out.println("Connection Closed");
//        }

    }
}