import java.io.*;
import java.net.*;

public class Sync {

    public static void main(String[] args) throws Exception {

        String hostIP;
        int portNumber;

        String directoryName;
        String pwd;

        Socket senderSocket;
        ServerSocket listenerSocket;

         if(args.length < 3) {
//           System.out.println("Usage: java Sync [IP] [Port] [DirectoryPath]")
             System.out.println("Not Enough args");
         }

        hostIP = (args[0] == null) ? "127.0.0.1" : args[0];
        portNumber = (args[1] == null) ? 9999 : Integer.parseInt(args[1]);
        directoryName = (args[2] == null) ? "test" : args[2];

        pwd = System.getProperty("user.dir");

        directoryName = pwd + "/" + directoryName;

        //Thread to listen and handle incoming data on port
        Thread portListener = new ListenerWorker(portNumber, directoryName);
        portListener.start();

        while (true) {
            //Establish Connection
            //Connct to remote server
            senderSocket = establishConnection(hostIP, portNumber);
            ////Send Direcotry
            if (senderSocket != null) {
                sendFiles(directoryName, senderSocket);
            }

            //while loop to wait for file changes

            //update bufferDirectory

        }
    }

    /**
     * Reads the directory and sends the data to the remote host socket
     *
     * @throws Exception
     */
    public static void sendFiles( String directoryName, Socket senderSocket) {
        try {
            File[] files = new File(directoryName).listFiles();

            BufferedOutputStream bufferOS = new BufferedOutputStream(senderSocket.getOutputStream());
            DataOutputStream dataOS = new DataOutputStream(bufferOS);

            //Write number of files
            dataOS.writeInt(files.length);
            for (File file : files) {
                //Write file length
                dataOS.writeLong(file.length());
                //Write file name
                dataOS.writeUTF(file.getName());

                FileInputStream fileIS = new FileInputStream(file);
                BufferedInputStream bufferIS = new BufferedInputStream(fileIS);

                //Write bytes from file input stream to the remote host port
                int dataByte = 0;
                while ((dataByte = bufferIS.read()) != -1) {
                    bufferOS.write(dataByte);
                }

                fileIS.close();
                bufferIS.close();
                // int size = 9022386;
                // byte[] data = new byte[size];
                // int fileEnd = bufferIS.read(data, 0, data.length);
                // bufferOS.write(data, 0, fileEnd);
                // bufferOS.flush();
            }
        } catch (Exception e) {
            System.out.println("Failed to send file: " + e);
        }
    }

    /**
     * Establishes connection with remote host
     *
     * @return Successful Socket connection to remote host
     */
    public static Socket establishConnection( String hostIP, Integer portNumber) {
        Socket connection = null;
        try {
            connection = new Socket(hostIP, portNumber);
        } catch (Exception e) {
            return null;
        }

        return connection;
    }
}

class ListenerWorker extends Thread {
    String dirName;
    int portNumber;
    Socket connectionSocket;
    ServerSocket serverSocket;

    public ListenerWorker(Integer portNumber, String dirName) {
        this.dirName = dirName;
        this.portNumber = portNumber;
    }

    public void run() {

        try {
        //Waits for data to come across port
            serverSocket = establishListener(portNumber);
            connectionSocket = serverSocket.accept();

        while (true) {

            //Triggers when data is received
            BufferedInputStream bufferIS = new BufferedInputStream(connectionSocket.getInputStream());
            DataInputStream dataIS = new DataInputStream(bufferIS);

            //Create directory if doesn't exist
            File directory = new File(dirName);
            directory.mkdir();

            //Create list of files equal to that being passed in
            int numberOfFiles = dataIS.readInt();
            File[] files = new File[numberOfFiles];

            //For every file in list
            for (int i = 0; i < numberOfFiles; i++) {
                long fileLength = dataIS.readLong();
                String fileName = dataIS.readUTF();

                //Create new file
                files[i] = new File(dirName + "/" + fileName);
                files[i].createNewFile();

                FileOutputStream fileOS = new FileOutputStream(files[i]);
                BufferedOutputStream bufferOS = new BufferedOutputStream(fileOS);

                //Write data bytes from port to file
                for (int j = 0; j < fileLength; j++) {
                    bufferOS.write(bufferIS.read());
                }
                bufferOS.close();
                fileOS.close();
            }
            bufferIS.close();
            dataIS.close();
        }
        } catch (Exception excep) {
            System.out.println("Failed to write directory:" + excep);
        }
    }

    /**
     * Establishes connection to local port
     * @return Successful Socket to local port
     */
    public static ServerSocket establishListener(int portNumber){
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(portNumber);
        } catch(Exception e) {
            System.out.println("Failed to connect to local port: "+portNumber);
        }
        return serverSocket;
    }

}
