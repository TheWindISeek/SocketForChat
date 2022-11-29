package ChatRoom;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class ConnectToClient implements Runnable{

    List<Socket> clients = new ArrayList<>();
    List<ListenClient> listenClients = new ArrayList<>();
    Server server;

    public ConnectToClient(Server server) {
        this.server = server;
        System.out.println("初始化ConnectToClient");
    }

    //添加一个新的客户端socket连接
    public boolean addNewClient(Socket clientSocket) {
        clients.add(clientSocket);
        System.out.println("服务器端添加"+clientSocket.getInetAddress());
        //对于这些客户端 都需要新建一个进程 让ListenClient的实例专门处理这个socket的用于接收从他们这里得到的数据
        listenClients.add(new ListenClient(clientSocket));
        return true;
    }

    /*
    * 向当前的所有客户端广播msg
    * */
    public void boardcastMessage(String msg) throws IOException{
        //直接向所有的发
        for (ListenClient client: listenClients) {
            client.sendMessage(msg);
        }
    }
    //向指定的socket发送信息
    public void simpleMessage(Socket socket,String msg) throws IOException {
        for (ListenClient client: listenClients) {
            //向指定的socket发送信息
            if(client.client == socket) {
                System.out.println("你已成功发送消息\n"+msg);
                client.sendMessage(msg);
                return;
            }
        }
    }
    class ListenClient implements Runnable{
        CleanFileTrans fileTrans = new CleanFileTrans();//文件传输的东西
        Socket client;//用于发送聊天的socket
        PrintWriter printSocket;
        int udpPort;//udp发送端口
        String name; //客户机的名字

        String srcIP;
        String srcPort;

        public ListenClient(Socket client) {
            try{
                this.client = client;
                srcIP = bytes2IP(client.getInetAddress().getAddress());
                srcPort = String.valueOf(client.getLocalPort());
                printSocket = new PrintWriter(client.getOutputStream(),true);
            }catch (Exception ex) {
                ex.printStackTrace();
            }
            //开启线程 用于处理相关连接
            (new Thread(this)).start();
        }

        //这个进程用于监听每个客户端的消息
        //最简单的方式就是接收到消息后 直接就在这里向广播
        @Override
        public void run() {
            try{
                receiveMessage();
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        //向当前已连接的socket的对象发送消息msg
        void sendMessage(String msg) throws IOException{
            printSocket.println(msg);
            printSocket.flush();
        }
        //不断接收着流
        void receiveMessage() {
            try {
                //缓冲读 读取
                Reader reader = new InputStreamReader(this.client.getInputStream());
                BufferedReader bufreader = new BufferedReader(reader);
                //接收对方网名
                String line = bufreader.readLine();
                System.out.println("这是对方发过来的网名");
                System.out.println(line);
                this.name = line;
                line = bufreader.readLine();
                System.out.println("这是对方发过来的UDP端口号"+line);
                this.udpPort = Integer.parseInt(line);

                int cnt = 0;
                //不断的去接收
                while ((line = bufreader.readLine()) != null && !line.equals(" ")) {//如果此时接收到的消息是null则退出
                    System.out.println(this.name+line+"\n");
                    //读取到消息后 直接广播
                    boardcastMessage(line+"\n");
                    //模二加
                    cnt = cnt ^ 1;
                    if(cnt == 0) {
                        //接收到客户端的消息 再进行插入操作
                        server.getDatabase().insert(srcIP, srcPort, name, line.replace('\t', ' ').trim(), Server.data2int());
                        //直接更新字符串
                        server.addPreMsg();
                    }
                }
                System.out.println(name+"停止通信");
                //关闭所有已经打开的流
                bufreader.close();
                reader.close();
                this.closeAllStream();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        //关闭所有已经打开的流
        void closeAllStream() {
            try {
                client.close();
                printSocket.close();
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        //用于开始文件传输的类
        private class CleanFileTrans implements Runnable{

            DatagramSocket ds;//用于接收和发送UDP报文的ds
            int port;//用于指示接收UDP报文的端口
            byte[] recBuf = new byte[1024];//发送报文的字节缓冲区
            DatagramPacket packet = new DatagramPacket(recBuf, recBuf.length);//具体的数据报
            TabPage.FileOps fileOps;//不想再做一个类开线程了 就用这个在函数里面进行区分
            String filePath;//要发送的文件路径
            //构造函数们
            public CleanFileTrans() {
                try {
                    init(TabPage.FileOps.RECEIVE, -1);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
            public CleanFileTrans(TabPage.FileOps fileOps) {
                try {
                    init(fileOps,-1);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
            public CleanFileTrans(int port) {
                try {
                    init(TabPage.FileOps.RECEIVE, port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            public CleanFileTrans(DatagramSocket ds, DatagramPacket send) {
                init(ds, send);
            }
            public CleanFileTrans(byte[] files) {
                init(files);
            }
            public CleanFileTrans(String filePath, byte[] files) {
                this.filePath = filePath;
                init(files);
            }
            private void init(byte[] files) {
                try {
                /*
                socket.getLocalSocketAddress();
                socket.getInetAddress();
                socket.getRemoteSocketAddress();
                socket.getChannel();
                socket.getLocalAddress();
                 */
                    System.out.println("init:UDP数据包的目标IP"+bytes2IP(client.getInetAddress().getAddress()));
                    System.out.println(udpPort);
                    packet = new DatagramPacket(
                            files, files.length,
                            new InetSocketAddress(bytes2IP(client.getInetAddress().getAddress()),udpPort)
                    );
                    System.out.println("构造数据包完毕 address"+packet.getAddress());
                    //UDP 发送包也需要通过端口 不指定的话 默认分配
                    ds = new DatagramSocket();
                    fileOps = TabPage.FileOps.SEND;
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
            private void init(TabPage.FileOps fileOps, int port) throws Exception {
                //如果端口号不合理 就让系统自动分配
                if(port < 1024 || port > 65535) {
                    ds = new DatagramSocket();
                } else {
                    ds = new DatagramSocket(port);
                }
                this.fileOps = fileOps;
                this.port = ds.getLocalPort();
                System.out.println("这是实际获得的端口号" + ds.getLocalPort());
            }
            private void init(DatagramSocket ds, DatagramPacket send) {
                this.ds = ds;
                this.packet = send;
                fileOps = TabPage.FileOps.SEND;
            }

            //FileOps = send则转为运行send函数 否则运行receive函数
            @Override
            public void run() {
                try{
                    switch (fileOps) {
                        case SEND: {
                            send();
                            break;
                        }
                        case RECEIVE : {
                            receive();
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            //发送文件
            private void send() {
                try {
                    System.out.println("这是发送的数据"+packet.getData());
                    ds.send(packet);                    //发送请求 连接的包
                    ds.receive(packet);//等待接收回复UDP包
                    //如果允许 则再发送文件
                    System.out.println("这是收到的字符"+new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8));
                    int datas = Integer.parseInt(new String(packet.getData(), 0, packet.getLength(),StandardCharsets.UTF_8));
                    System.out.println("这是接收方回送回来的报文"+datas);
                    if(datas == 0) {// 0 端口无法使用 拒绝接收文件
                        return;
                    } else {
                        // 其他端口正常使用 需要建立TCP连接
                        Socket sendFileSocket = new Socket();
                        //连接上对应IP的主机的对应端口 2s超时
                        sendFileSocket.connect(new InetSocketAddress(packet.getAddress(), datas), 2000);
                        //准备文件的输入流 socket的输出流
                        BufferedInputStream fileStream = new BufferedInputStream(
                                new FileInputStream(filePath)
                        );
                        BufferedOutputStream bos = new BufferedOutputStream(
                                sendFileSocket.getOutputStream()
                        );
                        byte[] buf = new byte[1024];//1KB
                        // -1 即到文件结尾了
                        int len;
                        while((len = fileStream.read(buf, 0, buf.length)) != -1) {
                            //len 是文件读取的大小 buf是数组 我把这些数据再写入到 socket的输出流里面去
                            //不一次性读完是为了内存考虑 总不能有多大的文件 我就要多大的内存吧
                            System.out.println("本次发送的文件长度为"+len);
                            bos.write(buf, 0, len);
                            bos.flush();//手动刷新
                        }
                        //标记已经发送完毕
                        sendFileSocket.shutdownOutput();
                        System.out.println("已关闭输入流");
                        //如果运行至此 那么文件已经发送完毕
                        //关闭已建立的流
                        fileStream.close();
                        bos.close();
                        //sendFileSocket.getOutputStream().close();
                        sendFileSocket.close();
                    }
                } catch(IOException ex) {
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            //接收可能到达的文件 未完成
            private void receive() throws Exception {
                while (true) {
                    //下一次等待的时候 发送按钮生效
                    packet = new DatagramPacket(recBuf, recBuf.length);
                    System.out.println("这是每次等待时ds的本地端口"+ds.getLocalPort());
                    //等待UDP连接到来
                    ds.receive(packet);
                    //请求连接后不能再让你发了
                    //发送按钮失效
                    System.out.println("这个时候已经接收到了发过来的数据"+ds.getLocalPort());
                    //拆包 发送的文件名/文件大小
                    String data = new String(packet.getData());
                    String[] files = data.split("\n");
                    //int size = Integer.parseInt(files[1]);
                    //询问是否开始文件传输
                    System.out.println("这是接收到的UDP数据报文"+data);
                    System.out.println(files[0] + files[1]);
                    {
                        //不然的话 等待TCP连接 并将当前等待的端口作为报文内容发回去
                        ServerSocket fileServer = new ServerSocket(0);

                        System.out.println("这是用于文件传输的端口"+fileServer.getLocalPort());
                        byte[] bport = String.valueOf(fileServer.getLocalPort()).getBytes(StandardCharsets.UTF_8);

                        packet = new DatagramPacket(bport, bport.length, packet.getSocketAddress());
                        System.out.println("这是转换为UTF8后回送的报文"+new String(packet.getData()));
                        ds.send(packet);
                        //等待TCP连接
                        Socket fileSocket = fileServer.accept();
                        //建立TCP连接
                        //开始传输 建立文件输出流 socket输入流
                        //就到当前路径创建文件输出流
                        BufferedOutputStream fileStream = new BufferedOutputStream(
                                new FileOutputStream(files[0])
                        );
                        BufferedInputStream bis = new BufferedInputStream(
                                fileSocket.getInputStream()
                        );
                        byte[] buf = new byte[1024];
                        int len;
                        System.out.println("等待接收数据中");
                        while ((len = bis.read(buf, 0, buf.length)) != -1) {
                            System.out.println("本次文件读取的长度为"+len);
                            fileStream.write(buf, 0, len);
                        }
                        //传输结束
                        System.out.println("传输结束");
                        //关闭所有的流 再次循环
                        fileStream.close();
                        fileSocket.getInputStream().close();
                        fileSocket.close();
                        fileServer.close();
                    }
                }
            }
        }
    }

    /*这个是Connect To Client 函数的线程函数*/
    @Override
    public void run() {
        System.out.println("Connect To Client Thread");
    }

    String bytes2IP(byte[] bytes) {
        String ip = "";
        for (int i = 0; i < bytes.length; i++) {
            //注意IP地址应该是无符号数 别用int强转 不然就错了
            System.out.println(String.valueOf(bytes[i] & 0x0FF));
            ip += String.valueOf(bytes[i] & 0x0FF);
            if(i + 1 == bytes.length)
                break;
            ip += ".";
        }
        return ip;
    }
    protected String getCurrentDate() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }
}
