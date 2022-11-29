package ChatRoom;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Scanner;

public class Server implements Runnable{
    // 所有的socket和SocketServer
    private java.util.List<Socket> socketList = new ArrayList<>();
    private java.util.List<ServerSocket> serverSocketList = new ArrayList<>();
    protected ConnectToClient connectToClient = new ConnectToClient(this);
    //是否监听
    private boolean isListenning = true;
    int waitPort = 31337;
    //用于和数据库进行连接
    private Database database;
    //之前发过来的消息
    public String preMsg = "";

    //不指定等待端口
    public Server() {
        this(31337);
    }
    //指定等待端口
    public Server(int waitPort) {
        this.database = new Database();
        this.waitPort = waitPort;
        new Thread(this).start();
    }
    public Database getDatabase() {
        return database;
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
    //将int类型格式的字符串转换成日期格式的字符串
    public static String int2Date(int nowTimeInt) {
        long now = Long.valueOf(nowTimeInt)*1000;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return  df.format(now);
    }
    //将当前的date转换为int
    public static int data2int() {
        return new Long(System.currentTimeMillis()/1000).intValue();
    }
    //将数据库格式的信息转换成终端格式的字符串
    private String database2show(String msg) {
        String result = "";
        try{
            String [] line = msg.split("\n");
            for (String s:line) {
                String[] col = s.split("\t");
                for (int i = 0; i < col.length; i++) {
                    System.out.println(i+"\n"+col[i]+"\n");
                }
                //name + time + message
                result += col[2] + "\t" + int2Date(Integer.parseInt(col[4])) + "\n" + col[3] + "\n";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }
    public void addPreMsg() {
        preMsg = database2show(database.selectAll());
    }
    //开始进行监听
    public void startCommunication() {
        addPreMsg();
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
                connectToClient.simpleMessage(socket,preMsg);
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
