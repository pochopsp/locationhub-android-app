package it.unina.locationhub.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpClient {

    // Server IP address and port
    private final String SERVER_IP;
    private final int SERVER_PORT;

    // Message to send to the server
    private String mServerMessage;

    // Listens for responses from server
    private final TcpListener mMessageListener;

    // While this is true, this TcpClient will continue running
    private boolean tcpClientMustRun = false;

    // Used to send messages
    private PrintWriter mBufferOut;

    // Used to read messages from the server
    private BufferedReader mBufferIn;

    public TcpClient(TcpListener listener, String ip, int port) {
        mMessageListener = listener;
        SERVER_IP = ip;
        SERVER_PORT = port;
    }

    public interface TcpListener {
        void onMessageReceived(String message);
        void onConnectionEstablished();
    }

    // Used to send a message to the server
    public void sendMessage(final String message) {

        Runnable runnable = () -> {
            if (mBufferOut != null) {
                mBufferOut.println(message);
                mBufferOut.flush();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    // Close the connection and release the members
    public void stopClient() {

        tcpClientMustRun = false;

       if (mBufferOut != null) {
           mBufferOut.flush();
           mBufferOut.close();
       }
       mBufferIn = null;
       mBufferOut = null;
       mServerMessage = null;


    }

    public void run() throws ServerOrNetworkDisconnectedException, IOException {

        tcpClientMustRun = true;

        //Throws ConnectException if the network is off or the server is unreachable
        Socket currentSocket = new Socket();

        //Throws SocketTimeoutException after 1s if server is unreachable
        currentSocket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 1000);

        mMessageListener.onConnectionEstablished();

        try {

            //sends the message to the server
            mBufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(currentSocket.getOutputStream())), true);

            //receives the message which the server sends back
            mBufferIn = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));

            //in this while the client listens for the messages sent by the server
            while (tcpClientMustRun) {

                //Throws SocketException if network is off or application is closed
                try {
                    mServerMessage = mBufferIn.readLine();
                } catch (IOException e) {
                    currentSocket.close();
                    throw new ServerOrNetworkDisconnectedException();
                }

                if (mServerMessage == null) {
                    throw new ServerOrNetworkDisconnectedException();
                }

                //call the method messageReceived in UserRepository
                mMessageListener.onMessageReceived(mServerMessage);

            }

        } finally {
            //the socket must be closed. It is not possible to reconnect to this socket
            // after it is closed, which means a new socket instance has to be created.
            currentSocket.close();
        }


    }

}
