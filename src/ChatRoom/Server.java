package ChatRoom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server implements Runnable{
    // 所有的socket和SocketServer
    private java.util.List<Socket> socketList = new ArrayList<>();
    private java.util.List<ServerSocket> serverSocketList = new ArrayList<>();
    protected ConnectToClient connectToClient = new ConnectToClient();
    //是否监听
    private boolean isListenning = true;
    int waitPort = 31337;
    //不指定等待端口
    public Server() {
        //用于打开命令行
        new Thread(this).start();
    }
    //指定等待端口
    public Server(int waitPort) {
        this.waitPort = waitPort;
        new Thread(this).start();
    }
    //应该关闭所有的流 成功返回true 失败返回false
    boolean closeAllStream() {
        try {
            for (Socket socket : socketList) {
                if(!socket.isClosed())
                    socket.close();
            }
            for (ServerSocket serverSocket : serverSocketList) {
                if(!serverSocket.isClosed())
                    serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //开始进行监听
    public void startCommunication() {
        try{
            ServerSocket serverSocket = new ServerSocket(waitPort);
            //等待TCP连接建立 一个建立完成后 继续循环
            while (isListenning) {
                System.out.println("这是服务器端等待连接的端口"+waitPort);
                Socket socket = serverSocket.accept();
                //用于释放
                socketList.add(socket);
                serverSocketList.add(serverSocket);
                //新建一个线程去让这些人访问
                connectToClient.addNewClient(socket);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }finally {
            closeAllStream();
        }
    }
    void stopListenning() {
        isListenning = false;
    }

        //做一个小型命令行
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String readString;
        while (true) {
            readString = scanner.nextLine();
            if(readString.equals("close")) {
                stopListenning();
                closeAllStream();
                System.out.println("关闭了所有流");
            } else if (readString.equals("start")) {
                startCommunication();
                System.out.println("开始监听");
            } else if(readString.equals("stop")) {
                stopListenning();
                System.out.println("停止监听");
            } else if(readString.equals("quit")) {
                stopListenning();
                closeAllStream();
                break;
            }
        }
        System.out.println("退出线程");
    }
}
