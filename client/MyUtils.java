package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MyUtils {

    public MyUtils() {
    }

    public static void closeConnection(ServerSocket server, Socket socket, DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        if (server != null) {
            server.close();
        }
        if (socket != null) {
            socket.close();
        }
        if (inputStream != null) {
            inputStream.close();
        }
        if (outputStream != null) {
            outputStream.close();
        }
    }
}
