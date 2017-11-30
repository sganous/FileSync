# FileSync

**Task**

Assignment Description. The goal of this assignment is to become familiar with peer to peer (P2P) networks and having a client act as a server. You may work on this project in pairs.
Design a program which allows two or more computers to synchronize files across a local area network (LAN). Each computer should run its own local instance of the client software, and the files which will be synchronized should be able to be synchronized multi-directionally.

**Introduction**

Many core Java packages can be used to create useful applications. Our application uses java.net package to establish connection between two systems residing on the same network.  This point is important, because it will not work for arbitrary networks (having no connection between them). Systems on same network means that they are connected through the same router, or directly connected to each other via LAN Wire.

When we connect to a network (a router), we get an IP from the router, which is usually of the form xxx.xxx.x.x, where x is a no. between 0 to 255 like `192.168.1.5` or `192.168.0.5`. Usually the first address in the series is of the router itself, i.e. `192.168.1.1`. In networking, there is a simple concept of a socket which is the combination of IP Address and Port number. Port number is a is a number between 0 to 65535. Out of this, 0 to 1023 are reserved for specific services and 1024 to 65536 can be used for our code. In java, there are two classes which help in communication via these ports. These are namely, `java.net.Socket` and `java.net.ServerSocket`. Socket acts as a client and ServerSocket acts as a server. In our case, we used both in the same program.

**How it Works**

An instance of our application will run on system A and perform setup operations. This includes getting user input (IP of other system, mutual port, working directory) and saving the file timestamps in a HashTable structure. The setup also involves starting a listener worker (thread) who’s sole job is to detect any incoming data and write it to the system’s directory. The main method will continue and await a connection from another system on the network. Once the program runs on system B, a connection condition is met within the code and B will start sending existing files in its directory to be intercepted by the worker’s blocking call. The directories on both system could be empty initially, meaning the workers will not write anything. Both programs are now looping to monitor for file changes by the user by comparing its original file timestamps with new timestamps. Say a user modifies a file on system A, the program’s inner while loop will break due to a timestamp difference and it will send the new contents. Timestamps are also updated in the process. B’s worker will intercept and update B’s directory. The cycle repeats to emulate a synchronized directory on both systems.

**Usage**

* Compile the program:
```sh
javac src/Sync.java
```

* Run the program
```sh
java Sync [remoteHostIP] [port#] [directory]
```
