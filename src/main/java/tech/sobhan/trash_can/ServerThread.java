package tech.sobhan.trash_can;

import java.io.*;
import java.net.Socket;

public class ServerThread extends Thread {

    String line = null;
    DataInputStream in = null;
    DataOutputStream out = null;
    Socket socket = null;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            line = in.readUTF();
            while (line.compareTo("QUIT") != 0) {
                out.writeUTF(line);
                System.out.println("Response to Client  :  " + line);
                line = in.readUTF();
            }
        } catch (IOException e) {
            System.out.println("IO error in server thread");
        }
        finally {
            try {
                System.out.println("Connection Closing..");
                if (in != null) {
                    in.close();
                    System.out.println(" Socket Input Stream Closed");
                }

                if (out != null) {
                    out.close();
                    System.out.println("Socket Out Closed");
                }
                if (socket != null) {
                    socket.close();
                    System.out.println("Socket Closed");
                }

            } catch (IOException ie) {
                System.out.println("Socket Close Error");
            }
        }
    }
}
