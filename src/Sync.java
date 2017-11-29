import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Sync {

    public static void main(String[] args) throws Exception {

        String hostIP;
        int portNumber;

        String directoryName;
        String pwd;

        Socket senderSocket;
        ServerSocket listenerSocket;

        ReentrantLock lock = new ReentrantLock();

        HashMap<String, Long> fileTimestampsOld = null;
        HashMap<String, Long> fileTimestampsNew = null;

        hostIP = (args[0] == null) ? "127.0.0.1" : args[0];
        portNumber = (args[1] == null) ? 9999 : Integer.parseInt(args[1]);
        directoryName = (args[2] == null) ? "test" : args[2];

        pwd = System.getProperty("user.dir");

        directoryName = pwd + "/" + directoryName;

        //Thread to listen and handle incoming data on port
        Thread portListener = new ListenerWorker(portNumber, directoryName, lock);
        portListener.start();


        //Store a copy of current directory timestamps
        fileTimestampsOld = generateFileTimestamps(directoryName);

        while (true) {
            //Establish Connection
            senderSocket = establishConnection(hostIP, portNumber);
            //Send Direcotry if successfull connection
            if (senderSocket != null) {
                sendFiles(directoryName, senderSocket);
            }

            //while loop to wait for file changes
            boolean filesNotChanged = true;
            while(filesNotChanged) {
                lock.lock();
                try {
                    fileTimestampsNew = generateFileTimestamps(directoryName);
                    //Check for more or less files
                    if (fileTimestampsOld.size() != fileTimestampsNew.size()) {
                        filesNotChanged = false;
                    }
                    for (String key : fileTimestampsOld.keySet()) {
                        //Checks if old file still in new direcotry
                        if (fileTimestampsNew.containsKey(key)) {
                            //Checks if file has been updated
                            if (fileTimestampsOld.get(key) != fileTimestampsNew.get(key)) {
                                filesNotChanged = false;
                                break;
                            }
                        } else {
                            filesNotChanged = false;
                            break;
                        }
                    }
                } finally {
                    lock.unlock();
                    Thread.sleep(5000);
                }
            }
            //update bufferDirectory
            fileTimestampsOld = fileTimestampsNew;
        }
    }

    /**
    * Reads files from a directory and sends the data to a remote host socket
    *
    * @throws IOException
    * @throws FileNotFoundException
    */
    public static void sendFiles( String directoryName, Socket senderSocket) {
        BufferedOutputStream bufferOS = null;
        DataOutputStream dataOS = null;
        try {
            File[] files = new File(directoryName).listFiles();

            bufferOS = new BufferedOutputStream(senderSocket.getOutputStream());
            dataOS = new DataOutputStream(bufferOS);

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
            }
            dataOS.close();
            bufferOS.close();
        } catch (FileNotFoundException e) {
            System.err.println("Failed to open directory: " + directoryName);
        } catch (IOException e) {
            System.err.println("Failed to handle file: " + e.getMessage());
        }
    }

    /**
    * Establishes connection with remote host.
    *
    * @return Socket connection to remote host.
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

    /**
    * Creates a hash table of file names and their last modified dates in a given directory.
    *
    * @param directoryName Directory path.
    * @return HashMap<String, Long> of fileName and timestamp.
    */
    public static HashMap generateFileTimestamps( String directoryName ) {
        HashMap<String, Long> fileTimestamps = new HashMap();
        File[] files = new File(directoryName).listFiles();
        for ( File file : files ) {
            fileTimestamps.put(file.getName(), file.lastModified());
        }
        return fileTimestamps;
    }
}

/**
* Thread that handles listening to a local socket and handling incoming data.
*/
class ListenerWorker extends Thread {
    String dirName;
    int portNumber;
    Socket connectionSocket;
    ServerSocket serverSocket;
    Lock lock;

    /**
    * @param portNumber current connected port.
    * @param dirName working directory.
    * @param lock Global lock on for concurrency on reading file timestamps.
    */
    public ListenerWorker(Integer portNumber, String dirName, Lock lock) {
        this.dirName = dirName;
        this.portNumber = portNumber;
        this.lock = lock;
    }

    /**
    * Establishes a listener on portNumber and writes the incoming file data
    * to dirName.
    */
    public void run() {
        try {
            //Waits for data to come across port
            serverSocket = establishListener();
            while (true) {
                //Accept connection and lock critial section
                connectionSocket = serverSocket.accept();
                lock.lock();
                //Triggers when data is received
                BufferedInputStream bufferIS = new BufferedInputStream(connectionSocket.getInputStream());
                DataInputStream dataIS = new DataInputStream(bufferIS);

                //Create directory if doesn't exist and cleanse
                File directory = new File(dirName);
                directory.mkdir();
                for(File file: directory.listFiles()){
                    if (!file.isDirectory()){
                        file.delete();
                    }
                }

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
                lock.unlock();
            }
        } catch (FileNotFoundException e) {
            System.err.println("Failed to write directory: " + dirName);
        } catch (IOException e) {
            System.err.println("Failed to open IO streams: " + e.getMessage());
        }
    }

    /**
    * Establishes connection to local server port.
    * @throws IOException
    * @return Socket to local server port.
    */
    public ServerSocket establishListener() {
        ServerSocket serverSocket = null;
        try{
            serverSocket = new ServerSocket(portNumber);
        } catch(IOException e) {
            System.err.println("Failed to connect to local port: " + portNumber);
        }
        return serverSocket;
    }
}
