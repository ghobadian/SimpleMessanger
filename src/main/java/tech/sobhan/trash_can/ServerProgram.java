package tech.sobhan.trash_can;// echo server
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerProgram {
    public static void main(String args[]){
        Socket socket=null;
        ServerSocket serverSocket = null;
        System.out.println("Server Listening......");
        try{
            serverSocket = new ServerSocket(4445); // can also use static final PORT_NUM , when defined
        }
        catch(IOException e){
        }

        while(true){
            try{
                socket= serverSocket.accept();
                System.out.println("connection Established");
                ServerThread st = new ServerThread(socket);
                st.start();
                System.out.println("sth lola");
            }

            catch(Exception e){
                e.printStackTrace();
                System.out.println("Connection Error");

            }
        }

    }

}

