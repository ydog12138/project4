

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class ChatServer {
    private static int uniqueId = 0;
    private final List<ClientThread> clients = new ArrayList<>();
    private final int port;


    private ChatServer(int port) {
        this.port = port;
    }

    /*
     * This is what starts the ChatServer.
     * Right now it just creates the socketServer and adds a new ClientThread to a list to be handled
     */
    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while(true) {
                Socket socket = serverSocket.accept();
                Runnable r = new ClientThread(socket, uniqueId++);
                Thread t = new Thread(r);
                clients.add((ClientThread) r);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        int port_number = 1500;
        if (args.length == 1)
            port_number = Integer.parseInt(args[0]);
        ChatServer server = new ChatServer(port_number);
        server.start();
    }


    private synchronized void broadcast(String message) throws IOException{
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:MM:SS");
        String date = dateFormat.format(new Date());
        for (ClientThread i : clients) {
            i.writeMessage(message + " " + date);
        }
        System.out.println(message + " " + date);
    }

    private synchronized void remove(int id) {
        for (ClientThread i : clients) {
            if (i.id == id) {
                i.close();
                clients.remove(i);
                break;
            }
        }
    }
    /*
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     */
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;

        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
            // Read the username sent to you by client
            try {
                cm = (ChatMessage) sInput.readObject();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            //System.out.println(username + ": Ping");


            // Send message back to the client
            try {
                sOutput.writeObject(username + ": " + cm.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean writeMessage(String msg) throws IOException {
            if (!socket.isConnected())
                return false;
            else {
                sOutput.writeObject(msg);
                return true;
            }
        }

        private void close() {
            try {
                sOutput.close();
                sInput.close();
                socket.close();
            } catch (IOException e) {
                System.out.println(username + " has logged out.");
            }
        }
    }
}
