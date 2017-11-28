import java.io.*;
import java.net.*;

public class Sync {

  public static void main (String[] args) throws Exception {

    String hostname;
    int portNumber;

    String directory;

    Socket senderSocket;
    ServerSocket listenerSocket;

    if(args.length = 0) {
      System.out.println("Usage: java Sync [IP] [Port] [DirectoryPath]")
    }

    hostIP = (args[0] == null) ? "127.0.0.1" : args[0];
    portNumber = (args[1] == null) ? 9999 : Integer.parseInt(args[1]);
    directory = (args[2] == null) ? "~/test" : args[3];

    File transferFile = new File("~/test.txt");
    File outputFile = new File("/home/sergeyg/output.txt");

    BufferedOutputStream bos = null;
    DataOutputStream dos = null;

    //Establish Connections
    senderSocket = establishConnection();
    listenerSocket = establishListener();

    if(senderSocket != null) {
      sendFiles();
    }
    receiveFiles();



    //        Socket sendSocket = null;
    //        InputStream dis = null;
    //        OutputStream dos = null;
    //
    //        InputStream fileIn = null;
    //        OutputStream fileOut = null;
    //
    //        boolean done = false;
    //        int count;
    //
    //        try{
    //            //Setup listener on given port
    //            socketListener = new ServerSocket(portNumber);
    //            servSocket = socketListener.accept();
    //            dis = servSocket.getInputStream();
    //
    //
    //            //Setup sender on remote port
    //            sendSocket = new Socket( hostname, portNumber);
    //            dos = sendSocket.getOutputStream();
    //
    //
    //            //Read the file and send it over
    //            byte[] bytes = new byte[16 * 1024];
    //
    ////            count = 0;
    ////            if(transferFile.exists()) {
    ////                fileIn = new FileInputStream(transferFile);
    ////                while ((count = fileIn.read(bytes)) > 0) {
    ////                    dos.write(bytes, 0, count);
    ////                }
    ////            }
    //
    //
    //            //Establish Listener and listen
    //            fileOut = new FileOutputStream(outputFile);
    //            int count1;
    //            while((count1 = dis.read(bytes))>0){
    //                fileOut.write(bytes, 0, count1);
    //            }
    //
    //        }catch (Exception e){
    //            System.out.println("Failed to do whatever in the try block");
    //        }

  }

  /**
   * Establishes connection with remote host
   * @return Successful Socket connection to remote host
   */
  public static Socket establishConnection(){
    Socket connection = null;
    try{
      connection = new Socket(hostname, portNumber);
    } catch (Exception e) {
      return null;
    }

    return connection;
  }

  /**
   * Establishes connection to local port
   * @return Successful Socket to local port
   */
  public static ServerSocket establishListener(){
    ServerSocket serverSocket = null;
    try{
      serverSocket = new ServerSocket(portNumber);
    } catch(Exception e) {
      System.out.println("Failed to connect to local port: "+portNumber);
    }
    return serverSocket;
  }

  /**
   * Reads the directory and sends the data to the remote host socket
   * @throws Exception
   */
  public static void sendFiles() throws Exception{
    File[] files = new File(directory).listFiles();

    BufferedOutputStream bos = new BufferedOutputStream(senderSocket.getOutputStream());
    DataOutputStream dos = new DataOutputStream(bos);

    //Write number of files
    dos.writeInt(files.length);
    for(File file : files)
    {
      //Write file length
      dos.writeLong(file.length());
      //Write file name
      dos.writeUTF(file.getName());

      FileInputStream fis = new FileInputStream(file);
      BufferedInputStream bis = new BufferedInputStream(fis);

      //Write bytes from file input stream to the remote host port
      int dataByte = 0;
      while((dataByte = bis.read()) != -1) bos.write(dataByte);

      bis.close();
    }
    dos.close();
  }

  /**
   * Listens to the local socket.
   * When data comes in, it reads the files and writes them locally.
   * @throws Exception
   */
  public static void receiveFiles() throws Exception{
    //Waits for data to come across port
    Socket socket = listenerSocket.accept();

    //Triggers when data is received
    BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
    DataInputStream dis = new DataInputStream(bis);

    //Create directory if doesn't exist
    File dir = new File(directory);
    dir.mkdir();

    //Create list of equal to that being passed in
    int filesCount = dis.readInt();
    File[] files = new File[filesCount];

    //For every file in list
    for(int i = 0; 1 < filesCount; i++)
    {
      long fileLength = dis.readLong();
      String fileName = dis.readUTF();

      //Create new file
      files[i] = new File(directory+"/"+fileName);
      files[i].createNewFile();

      FileOutputStream fos = new FileOutputStream(files[i]);
      BufferedOutputStream bos = new BufferedOutputStream(fos);

      //Write data bytes from port to file
      for( int j = 0; j < fileLength; j++ )
      {
        bos.write(bis.read());
      }
      bos.close();
    }
    dis.close();
  }


}
