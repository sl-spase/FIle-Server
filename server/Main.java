package server;

import client.MyUtils;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {

    static String address = "127.0.0.1";
    static int port = 23456;

    private static final String PUT_REQ = "PUT";
    private static final String GET_REQ = "GET";
    private static final String DELETE_REQ = "DELETE";
    private static final String EXIT_REQ = "exit";

    private static final String OK = "200";
    private static final String FORBIDDEN = "403";
    private static final String NOT_FOUND = "404";

    static ServerSocket server;
    static Map<Integer, String> mapFile = new HashMap<>();
    static private final String path =  System.getProperty("user.dir") + "/File Server/task/src/server/data/";
    static ExecutorService executorService = Executors.newCachedThreadPool();
    public static void main(String[] args) throws IOException {

        try {
            loadMapping();
            server = new ServerSocket(port, 50, InetAddress.getByName(address));
            System.out.println("Server started!");
            while (true) {
                Socket socket = server.accept();
                executorService.submit(() -> {
                    try {
                        handleClientRequest(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (SocketException ignored) {
            System.out.println("Ignored");
        } catch (IOException e) {
            e.printStackTrace();
        }

        executorService.shutdownNow();

        saveMapping();
        System.out.println("CONNECTION HAS BEEN CLOSED");
        System.exit(0);
    }


    private static void getFileContent(String fileName, boolean getById, DataOutputStream dataOutputStream) throws IOException {
        if (getById) {
            fileName = mapFile.get(Integer.parseInt(fileName));
        }
        try {
            System.out.println("The file was got");
            byte[] message = Files.readAllBytes(new File(path + fileName).toPath());
            dataOutputStream.writeUTF(OK);
            dataOutputStream.writeInt(message.length);
            dataOutputStream.write(message);

        } catch (IOException e) {
            dataOutputStream.writeUTF(NOT_FOUND);
        }
    }

    private static void deleteFile(String fileName, boolean byName, DataOutputStream dataOutputStream) throws IOException {
        if (!byName) {
            fileName = mapFile.get(Integer.parseInt(fileName));
        }
        File file = new File(path + fileName);
        if (file.exists() && file.delete()) {
            mapFile.values().remove(fileName);
            dataOutputStream.writeUTF(OK);
        } else {
            dataOutputStream.writeUTF(NOT_FOUND);
        }
    }

    private static void loadMapping() {
        File file = new File(path + "mapping.txt");
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNext()) {
                String[] line = scanner.nextLine().split(":");
                mapFile.put(Integer.parseInt(line[0].trim()), line[1].trim());
            }
        } catch (FileNotFoundException e) {
            System.out.println("No file found: " + file);
        }
    }

    private static void saveMapping() throws IOException {
        File file = new File(path + "mapping.txt");
        FileWriter fileWriter = new FileWriter(file);
        mapFile.forEach((key, value) -> {
            try {
                fileWriter.write(key  + ":" + value + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        fileWriter.close();
    }

    private static void handleClientRequest(Socket socket) throws IOException {
        DataInputStream input = null;
        DataOutputStream output = null;

        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            String received = input.readUTF();
            System.out.println("Input data stream: " + received);
            String typeOfRequest = received.split("/")[0];
            if (EXIT_REQ.equals(typeOfRequest)) {
                executorService.shutdownNow();
                System.exit(0);
            }

            String fileName = received.split("/")[1];

            if(PUT_REQ.equals(typeOfRequest)) {
                saveFileOnServer(fileName, input, output);
            } else if(GET_REQ.equals(typeOfRequest)) {
                boolean getById = received.split("/")[2].equals("true");
                getFileContent(fileName, getById, output);
            } else if(DELETE_REQ.equals(typeOfRequest)) {
                boolean deleteByName = received.split("/")[2].equals("true");
                deleteFile(fileName, deleteByName, output);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MyUtils.closeConnection(null, socket, input, output);
        }
    }


    private static void saveFileOnServer(String fileName, DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        if (fileName.startsWith(".")) {
            fileName = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss").format(new Date()) + fileName;
            System.out.println(fileName);
        }
        try {
            File file = new File(path + fileName);
            if (!file.exists()) {
                int length = dataInputStream.readInt();
                byte[] message = new byte[length];
                dataInputStream.readFully(message, 0, message.length);
                Files.write(Paths.get(file.getPath()), message);
                int fileId = Collections.max(mapFile.keySet()) + 1;
                mapFile.put(fileId, fileName);
                dataOutputStream.writeUTF(OK + "/" + fileId);
            } else {
                throw new IOException();
            }
        } catch (IOException e) {
            dataOutputStream.writeUTF(FORBIDDEN);
        }
    }
}

