
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ZServer {

    private static final int SERVER_PORT = 3300;
    private static final String folder = "D:\\";
    private static File[] fileList;
    private static String arr[];

    public int getserverport() {
        return SERVER_PORT;
    }

    public static void start() {
        int countarr = 0;
        fileList = new File(folder).listFiles();
        arr = new String[fileList.length];
        System.out.println("This range F " + fileList.length);
        for (int i = 0; i < fileList.length; i++) {

            if (fileList[i].isFile()) {
                System.out.println(fileList[i].getName());
                arr[countarr] = fileList[i].getName();
                countarr++;
            }
        }
    }
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(SERVER_PORT);
        ServerSocketChannel serverChannel;
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress("localhost", SERVER_PORT + 1));
        Scanner scan = new Scanner(System.in);
        start();
        while (true) {
            Socket s = null;
            SocketChannel socketChannel = null;
            try {
                System.out.println("Wating for connection...");
                s = ss.accept();
                socketChannel = serverChannel.accept();
                System.out.println("A new client is connected: " + s);
                ObjectOutputStream outputStream = new ObjectOutputStream(s.getOutputStream());
                outputStream.writeObject(arr);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(s.getInputStream()));
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                System.out.println("Assigning new thread for this client");
                Thread t = new ClientHandler(s, dis, dos, socketChannel);
                t.start();
            } catch (IOException e) {
                s.close();
                e.printStackTrace();
            }
        }
    }
}
class ClientHandler extends Thread {
    final DataInputStream dis;
    final DataOutputStream dos;
    final Socket s;
    final SocketChannel socketChannel;

    static final int BUFFER_SIZE = 4096;
    // Contructor

    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos, SocketChannel socketChannel) {
        this.s = s;
        this.dis = dis;
        this.dos = dos;
        this.socketChannel = socketChannel;
    }
    @Override
    public void run() {
        String received;
        while (true) {
            try {
                received = dis.readUTF();
                if (received.equals("D:\\Exit")) {
                    System.out.println("Client " + this.s + " sends exit...");
                    System.out.println("Closing this connection.");
                    this.s.close();
                    System.out.println("Connection closed");
                    break;
                }
                System.out.println("this is received" + received);
                System.out.println(received);
                int type = dis.readInt();
                File f = new File(received.trim());
                if (!f.exists()) {
                    dos.writeUTF("File does not exist. Please re-enter: ");
                } else {
                    long fileLength = f.length();
                    System.out.println("Sending " + f.getName());
                    dos.writeUTF("FILE=" + f.getName() + ";FILE_SIZE=" + fileLength);

                    if (type == 1) {
                        FileInputStream inputStream = new FileInputStream(received);
                        byte[] buffer = new byte[BUFFER_SIZE];
                        long totalBytesRead = 0;
                        int byteReads;
                        System.out.println("Sending By Mode TraditionalCopy.......");
                        while (totalBytesRead < fileLength) {
                            byteReads = inputStream.read(buffer, 0, BUFFER_SIZE);
                            dos.write(buffer, 0, byteReads);
                            totalBytesRead += byteReads;
                          /*  System.out.println("Sending " + byteReads + " bytes of data"); */
                        }
                        System.out.println("Sending completed ");
                    } else {
                        FileChannel source = null;
                        source = new FileInputStream(received).getChannel();
                        long totalRead = 0;
                        long read;
                        System.out.println("Sending By Mode ZeroCopy.......");
                        while (totalRead < fileLength /*&& (read = source.transferTo(currentRead, fileLength - currentRead, socketChannel)) != -1*/) {
                            read = source.transferTo(totalRead, fileLength - totalRead, socketChannel) ;
                            totalRead += read;
                        }
                         /*  System.out.println("Sending completed: " + totalBytesRead);*/
                            System.out.println("Sending completed ");
                    }
                }
            } catch (IOException e) {
                try {
                    System.out.println("Client " + this.s + " sends exit...");
                    System.out.println("Closing this connection.");
                 /* if (dis != null) {
                        dis.close();
                    }
                    if (dos != null) {
                        dos.close();
                    } */
                    if (s != null) {
                        s.close();
                    } 
                    if (socketChannel != null) {
                        socketChannel.close();
                    }
                    System.out.println("Connection closed");

                } catch (IOException ex) { }
                break;
            }
        }
    }
}
