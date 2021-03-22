package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main{
    static String address = "127.0.0.1";
    static int port = 23456;

    private static final String OK = "200";

    private static final String PUT_REQ = "PUT";
    private static final String GET_REQ = "GET";
    private static final String DELETE_REQ = "DELETE";

    private static final String ENTER_ID = "Enter id: ";
    private static final String ENTER_NAME = "Enter name of the file: ";
    static private final String path =  System.getProperty("user.dir") + "/File Server/task/src/client/data/";

    static Socket socket;
    static DataInputStream input;
    static DataOutputStream output;
    static Scanner sc = new Scanner(System.in);
    static final String SENT_MESSAGE = "The request was sent.";

    static {
        try {
            socket = new Socket(InetAddress.getByName(address), port);
            output = new DataOutputStream(socket.getOutputStream());
            input = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException {
        startAction();
    }


    private static void startAction() throws IOException {
        System.out.println("Client started!");

        System.out.print("Enter action (1 - get a file, 2 - save a file, 3 - delete a file): ");
        String action = sc.nextLine();

        switch (action) {
            case "exit":
                stopServer();
                break;
            case "1":
                getFile();
                break;
            case "2":
                putFile();
                break;
            case "3":
                deleteFile();
                break;
            default:
                System.out.println("Wrong action!");
        }
        sc.close();
        MyUtils.closeConnection(null, socket, input, output);
    }


    private static void stopServer() throws IOException {
        System.out.println(SENT_MESSAGE);
        output.writeUTF("exit");
    }


    private static String getFileName() {
        System.out.print("Enter filename: ");
        return sc.nextLine();
    }

    private static int howToGetDelete(String action) {
        System.out.printf("Do you want to %s the file by name or by id (1 - name, 2 - id): ", action);
        return Integer.parseInt(sc.nextLine());
    }

    private static void sentUTF(String httpAction, String fileName, Boolean deleteGetByName) throws IOException {
        if (deleteGetByName != null) {
            output.writeUTF(httpAction + "/" + fileName + "/" + deleteGetByName);
        } else {
            output.writeUTF(httpAction + "/" + fileName);
        }
        System.out.println(SENT_MESSAGE);
    }


    private static void getFile() throws IOException {
        int howToGetDelete = howToGetDelete("get");
        if (howToGetDelete == 1) {
            System.out.print(ENTER_NAME);
            String fileName = sc.nextLine();
            sentUTF(GET_REQ, fileName, false);
        } else if (howToGetDelete == 2) {
            System.out.print(ENTER_ID);
            String fileId = sc.nextLine();
            sentUTF(GET_REQ, fileId, true);
        }

        String response = input.readUTF();
        if (OK.equals(response)) {
            System.out.print("The file was downloaded! Specify a name for it: ");
            String fileName = sc.nextLine();
            File file = new File(path + fileName);
            System.out.println("FILE PATH: " + file.getPath());
            int length = input.readInt();
            byte[] message = new byte[length];
            input.readFully(message, 0, message.length);
            Files.write(Paths.get(file.getPath()), message);
            System.out.println("File saved on the hard drive!");
        } else {
            System.out.println("The response says that creating the file was forbidden!");
        }
    }


    private static void putFile() throws IOException {
        System.out.print("Enter name of the file: ");
        String fileName = sc.nextLine();
        System.out.print("Enter name of the file to be saved on server: ");
        String fileNameOnTheServer = sc.nextLine();
        String getPath = path + fileName;
        if (new File(path + fileName).exists()) {
            byte[] message = Files.readAllBytes(new File(getPath).toPath());
            String messageUTF = PUT_REQ + "/" + fileNameOnTheServer + "." + fileName.split("\\.")[1];
            output.writeUTF(messageUTF);
            output.writeInt(message.length);
            output.write(message);

            String response = input.readUTF();
            String status = response.split("/")[0];
            if (OK.equals(status)) {
                String id = response.split("/")[1];
                System.out.printf("Response says that file is saved! ID = %s\n", id);
            } else {
                System.out.println("The response says that the file was not found!");
            }
        }
    }


    private static void deleteFile() throws IOException {
        int howToGetDelete = howToGetDelete("delete");

        if(howToGetDelete == 1) {
            System.out.print(ENTER_NAME);
            String fileName = sc.nextLine();
            sentUTF(DELETE_REQ, fileName, true);
        } else if(howToGetDelete == 2) {
            System.out.print(ENTER_ID);
            String fileId = sc.nextLine();
            sentUTF(DELETE_REQ, fileId, false);
        }

        String response = input.readUTF();
        if (OK.equals(response)) {
            System.out.println("The response says that the file was successfully deleted!");
        } else {
            System.out.println("The response says that the file was not found!");
        }
    }
}
