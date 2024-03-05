
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
// Client class

public class ZClient {

    static final int serverport = 3300;
    static final int buffersize = 4096;
    static String hostname = "localhost"; //127.0.0.1 (localhost)

    private Socket socket;

    private static String getFileName(String message) {
        return message.replace("FILE=", "");
    }

    private static long getFileSize(String message) {
        return Long.parseLong(message.replace("FILE_SIZE=", ""));
    }

    public static void main(String[] args) throws IOException {

        try {
            Scanner scn = new Scanner(System.in);

           InetAddress ip = InetAddress.getByName(hostname);

           
            Socket s = new Socket(ip, serverport);
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(ip, serverport + 1));;
          
            ObjectInputStream inputStream = new ObjectInputStream(s.getInputStream());
            String[] receivedArray = (String[]) inputStream.readObject();

            DataInputStream dis = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            while (true) {
                System.out.println("This is file list");
                for (int i = 0; i < receivedArray.length; i++) {
                    if (receivedArray[i] != null) {
                        System.out.println(i + "." + receivedArray[i]);
                    }
                }

                System.out.println("Enter file number to download: ");
                String tosend = scn.next();

                if (tosend.equalsIgnoreCase("Exit")) {
                    System.out.println("Closing this connection : " + s);
                    s.close();
                    socketChannel.close();
                    System.out.println("Connection closed");
                    break;
                }

                int tosendInt = Integer.parseInt(tosend);
                String tosendR = receivedArray[tosendInt];
                dos.writeUTF("D:\\" + tosendR); 

                System.out.println("Enter Mode choose 1 for TraditionalCopy or others for Zerocopy");
                String mode = scn.next();
                if (mode.equals("1")) {
                    dos.writeInt(1);
                } else {
                    dos.writeInt(2);
                }
                
                String serverResponse = dis.readUTF();    //อ่านไฟล์จาก server มาเป็น FILE=ชื่อ;FILE_SIZE=ขนาด
                System.out.println(serverResponse);
                
                if (serverResponse.contains("FILE")) {

                    String[] parts = serverResponse.split(";"); //ทำการเเยก ชื่อเเละขนาดออกจากกันโดย ชื่อ จะถูกเก็บในอาเรย์ช่องที่ 0 ขนาดจะเก็บในอาเรย์ช่องที่ 1

                    String fileName = getFileName(parts[0]);
                    long fileSize = getFileSize(parts[1]);
                    String path = "C:\\FORTEST\\";
                    String pathZ = "C:\\GAEBOLG\\";
                    String destinationPath = path + fileName;
                    String destinationZero = pathZ + fileName;
                    long start = System.currentTimeMillis();
                          System.out.println("DOWNLOADING NOW PLS WAIT............." );
                    if (mode.equals("1")) {
                        byte[] buffer = new byte[buffersize];
                        long totalsWrite = 0;
                        int bytesRead;
                        FileOutputStream outputStream = new FileOutputStream(destinationPath);
                        while (totalsWrite < fileSize) {
                            bytesRead = dis.read(buffer, 0, buffersize);
                            totalsWrite += bytesRead;

                     /*       System.out.println("Downloaded " + totalsWrite + " bytes."); */
                            outputStream.write(buffer, 0, bytesRead); 
                         }
                        System.out.println("Traditional Copy Success");
                        if (outputStream != null) {
                            outputStream.close();
                        }

                    } else {
                        FileChannel destination = null;
                       /* System.out.println("This is where the file collect " + destinationPath);*/
                        destination = new FileOutputStream(destinationZero).getChannel();
                        long currentRead = 0;
                        long read;
                        while (currentRead < fileSize /*&& (read = destination.transferFrom(socketChannel, currentRead, fileSize - currentRead)) != -1*/) {
                            read = destination.transferFrom(socketChannel, currentRead, fileSize - currentRead);
                            currentRead += read;
                        }
                        System.out.println("Zero Copy Success");
                        if (destination != null) {
                            destination.close();
                        }
                    }
                    long end = System.currentTimeMillis();
                    long timebro = end - start;
                    System.out.println("Time " + timebro + " ms");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
