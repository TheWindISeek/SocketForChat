package ChatRoom;

import org.apache.commons.io.file.StandardDeleteOption;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

class TabPage extends JPanel implements Runnable, ActionListener {
    private ChatRoomFrame chatRoomFrame;

    JTextArea text_receiver;     //显示对话内容的文本区
    JTextField text_sender;     //输入发送内容的文本行
    JButton[] buttons;         //发送、离线、删除页按钮
    PrintWriter cout;        //格式化字符输出流
    Socket socket;          //和当前页相关联的用于聊天的TCP socket对象
    String[] strs = {"发送", "离线", "删除页", "发送文件"}; // 数组

    //接收文件
    FileTrans fileTrans = new FileTrans();
    int udpPort;
    Semaphore order = new Semaphore(0);


    TabPage(ChatRoomFrame chatRoomFrame,Socket socket)             //为每个socket构造一个tab页
    {
        super(new BorderLayout());//采用边界布局
        //给显示消息的区域添加了滚动框
        this.add(new JScrollPane(this.text_receiver = new JTextArea()));//添加了一个滚动框
        this.text_receiver.setEditable(false);//不允许对值进行任何修改

        //以下创建工具栏，输入内容，添加发送等命令按钮
        JToolBar toolbar = new JToolBar();
        this.add(toolbar, "South");//把工具栏放在最下面
        toolbar.add(this.text_sender = new JTextField(16));//长度为16
        this.text_sender.addActionListener(this);//用当前类对其进行监听
        //生成三个功能按钮

        this.buttons = new JButton[strs.length];
        for (int i = 0; i < this.buttons.length; i++) {
            this.buttons[i] = new JButton(strs[i]);
            toolbar.add(buttons[i]);
            this.buttons[i].addActionListener(this);
        }

        this.buttons[2].setEnabled(false);   //删除页按钮无效

        //绑定到客户端的socket上
        this.socket = socket;
        this.chatRoomFrame = chatRoomFrame;
        //开一个线程 让其能够同时接受多人消息
        (new Thread(this)).start();
        new Thread(fileTrans).start();//接收文件的线程
    }

    public void run()       //线程运行方法，接收对方信息，将对方发来的字符串添加到文本区
    {
        BufferedReader bufreader;
        Reader reader;
        try {   //下句从Socket获得字节输出流，再创建格式化字符输出流，立即flush
            //使用默认的字符集编码 得到对应socket的输出字符流
            this.cout = new PrintWriter(this.socket.getOutputStream(), true);
            //发送自己网名给对方，访问外部类.this.name
            this.cout.println(this.chatRoomFrame.name);
            this.cout.println(fileTrans.port);
            System.out.println("这是发过去的端口号"+fileTrans.port);

            //以下两句将Socket的字节输入流转换成字符流，默认GBK字符集，再创建缓冲字符输入流
            reader = new InputStreamReader(this.socket.getInputStream());
            bufreader = new BufferedReader(reader);
            //接收对方网名
            String line = bufreader.readLine();
            System.out.println(line);
            /*
            //选择是否接收和该用户建立连接
//                if(JOptionPane.showConfirmDialog(jFrame, "是否与"+line+"建立连接","是否聊天",JOptionPane.YES_NO_OPTION)
//                    == 1) {
//                    System.out.println("this is 1");
//                } else {
//                    System.out.println("this is 2");
//                }
            */
            int index = this.chatRoomFrame.table.getSelectedIndex();//当前选中的页 最开始一个页都没有 所以是-1
            System.out.println(index);//当前页在tab中的序号，外部类.this.tab

            if (index >= 0) {//当前页在tab中的序号，外部类.this.tab
                this.chatRoomFrame.table.setTitleAt(index, line);//如果别人来消息了 那么就提前显示
            } else {
                //否则 新建一个tab0
                this.chatRoomFrame.table.setTitleAt(++index, line);//将对方网名设置为当前页标题
            }

            line = bufreader.readLine();
            System.out.println("这是对方发过来的UDP端口号"+line);
            udpPort = Integer.parseInt(line);
            order.release();//可以开始等待接收了

            //不断的去接收
            while ((line = bufreader.readLine()) != null && !line.equals(" ")) {//如果此时接收到的消息是null则退出
                this.chatRoomFrame.table.setSelectedIndex(index);           //收到对方信息时，显示该页
                this.text_receiver.append(line + "\r\n");
            }

            //关闭所有已经打开的流
            bufreader.close();
            reader.close();
            this.cout.close();
            this.socket.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "在信息传输过程中出现了问题");
        }
            catch (Exception ex) {
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "发送了一些错误");
        }
        finally {
            this.buttons[0].setEnabled(false);         //发送按钮无效
            this.buttons[1].setEnabled(false);         //离线按钮无效
            this.buttons[2].setEnabled(true);          //删除页按钮有效
            this.buttons[3].setEnabled(false);          //发送文件按钮无效
            //this.buttons[4].setEnabled(false);
        }
    }

    public void actionPerformed(ActionEvent event) {//单击tab页上的"发送"等按钮
        try {
            if (event.getSource() == this.buttons[0]) { // 发送
                sendMessage();
            } else if (event.getSource() == buttons[1]) { // 离线
                offLine();
            } else if (event.getSource() == this.buttons[2]) {  // 删除当前页
                deletePage();
            } else if (event.getSource() == this.buttons[3]) {  //传输文件
                transferFile();
            }
            /*
            else if(event.getSource() == this.buttons[4]) { // 接收文件
                receiveFile();
            }*/
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //发送
    private void sendMessage() {
        if(isMessageInvalid(this.text_sender.getText())) {//没有消息 无法发送
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "发送消息不能为空");
            return;
        }
        this.cout.print(this.chatRoomFrame.name + "  \t" +
                getCurrentDate() + "\n\t" + this.text_sender.getText() + "\n");
        this.cout.flush();
        //使用println 可以正常接受别人的信息
        //使用print 只能正常显示自己的信息 果然是flush的问题 println会默认刷新缓冲区 但是print不会 需要手动刷新缓冲区
        this.text_receiver.append(this.chatRoomFrame.name + "  \t" +
                getCurrentDate() + "\n\t" + this.text_sender.getText() + "\n");
        this.text_sender.setText("");//重置输入
    }
    //离线
    private void offLine() {
        this.text_receiver.append("我离线\n");
        //告诉对方我离线了
        this.cout.println(this.chatRoomFrame.name + "离线\n" + " ");
    }
    //删除当前页
    private void deletePage() {
        this.chatRoomFrame.table.remove(this);//tab.getSelectedIndex());//删除tab当前页
    }

    //传输文件
    private void transferFile() throws IOException {
        try{
            JFileChooser fileChooser = new JFileChooser("D:\\");//默认在javaProject文件打开
            if(UIManager.getLookAndFeel().isSupportedLookAndFeel()) {
                final String platform = UIManager.getSystemLookAndFeelClassName();
                if(!UIManager.getLookAndFeel().getName().equals(platform)) {
                    UIManager.setLookAndFeel(platform);
                }
            }

            if(fileChooser.showOpenDialog(this.chatRoomFrame.jFrame) == JFileChooser.APPROVE_OPTION) {
                //String filePath = fileChooser.getSelectedFile().getPath();
                System.out.println(fileChooser.getSelectedFile().getPath());//看看文件路径是不是选的那个
                //System.out.println(fileChooser.getSelectedFile().getName());
                byte[] files = (fileChooser.getSelectedFile().getName() + "\n" +
                        String.valueOf(fileChooser.getSelectedFile().length()))
                        .getBytes(StandardCharsets.UTF_8);
                //发送UDP包 请求传输
                //order.acquire();//等待udpport被写入
                (new Thread(
                        new FileTrans(fileChooser.getSelectedFile().getPath(),files)
                )).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("你已经渡过了困难");
        }
        // String filePath = Cmd.command("cmd /c start explorer.exe /select D:\\");
       // System.out.println(filePath);

        //String s = JOptionPane.showInputDialog(this.chatRoomFrame.jFrame, "请输入文件路径", "选择传输文件路径", JOptionPane.YES_NO_CANCEL_OPTION);
        //解析文件路径
        //将文件读入到缓冲数组中
        //将缓冲数组写入到输入流当中去
       // System.out.println(s);
    }
    /*将一个按照网络字节顺序编码的字节数组变成一个IP地址*/
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
    //order in network
    int bytes2Int(byte[] bytes) {
        int i = 0;
        for (int j = bytes.length-1; j >= 0; --j) {
            i |= bytes[j] << (bytes.length -j -1);
        }
        return i;
    }
    //接收文件
    private void receiveFile() {
        System.out.println("接收文件");
    }

    enum FileOps {
        RECEIVE,
        SEND
    }

    private class FileTrans implements Runnable{

        DatagramSocket ds;//用于接收和发送UDP报文的ds
        int port;//用于指示接收UDP报文的端口
        byte[] recBuf = new byte[1024];//发送报文的字节缓冲区
        DatagramPacket packet = new DatagramPacket(recBuf, recBuf.length);//具体的数据报
/*
       // String ipPattern = "([0-9]{1,3}.){3}[0-9]{1,3}";

       // String portPattern = "[0-9]{1,5}";

        Pattern sendPattern = Pattern.compile("send " + ipPattern + " "
                + portPattern + " .*");*/
        FileOps fileOps;//不想再做一个类开线程了 就用这个在函数里面进行区分
        String filePath;//要发送的文件路径
        //构造函数们
        public FileTrans() {
            try {
                init(FileOps.RECEIVE, -1);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        public FileTrans(FileOps fileOps) {
            try {
                init(fileOps,-1);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        public FileTrans(int port) {
            try {
                init(FileOps.RECEIVE, port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public FileTrans(DatagramSocket ds, DatagramPacket send) {
            init(ds, send);
        }
        public FileTrans(byte[] files) {
            init(files);
        }
        public FileTrans(String filePath, byte[] files) {
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
            System.out.println("init:UDP数据包的目标IP"+bytes2IP(socket.getInetAddress().getAddress()));
            System.out.println(udpPort);
            packet = new DatagramPacket(
                    files, files.length,
                    new InetSocketAddress(bytes2IP(socket.getInetAddress().getAddress()),udpPort)
            );
                System.out.println("构造数据包完毕 address"+packet.getAddress());
            //UDP 发送包也需要通过端口 不指定的话 默认分配
            ds = new DatagramSocket();
            fileOps = FileOps.SEND;
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        private void init(FileOps fileOps, int port) throws Exception {
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
            fileOps = FileOps.SEND;
        }

        @Override
        public void run() {
            try{
                //ServerSocket serverSocket = new ServerSocket(0);
                //Socket socket = serverSocket.accept();
                //只有有人请求连接的时候 才去让文件按钮可以读
                //buttons[4].setEnabled(true);
                //BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
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

            }catch (NumberFormatException ex) {
              JOptionPane.showMessageDialog(chatRoomFrame.jFrame, "接收报文格式错误");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        //发送文件
        private void send() {
            try {

                System.out.println("这是发送的数据"+packet.getData());
                //发送请求 连接的包
                ds.send(packet);
                //接收缓冲区不能和发送缓冲区一致 否则会读到之前的
//                byte[] buf = new byte[32];
//                packet = new DatagramPacket(
//                        buf, buf.length
//                );
                //等待接收回复UDP包
                ds.receive(packet);
                //如果允许 则再发送文件
                System.out.println("这是收到的字符"+new String(packet.getData(), 0, packet.getLength(),StandardCharsets.UTF_8));
                //可能是因为缓冲区的问题
                //是因为之前和现在共用缓冲区 同时getData方法得到的字节数组还是之前的长度 所以进行转换的时候就超过了范围了
                int datas = Integer.parseInt(new String(packet.getData(), 0, packet.getLength(),StandardCharsets.UTF_8));
                /*System.out.println("字符串长度"+datas.length());
                for (int i = 0; i < datas.length(); i++) {
                    System.out.println(datas.charAt(i));
                    System.out.println((int)datas.charAt(i));
                }
                 */
                System.out.println("这是接收方回送回来的报文"+datas);
                // 0 端口无法使用 拒绝接收文件
                if(datas == 0) {
                    return;
                } else {
                    // 其他端口正常使用 需要建立TCP连接
                    Socket sendFileSocket = new Socket();
                    //连接上对应IP的主机的对应端口 2s超时
                    sendFileSocket.connect(new InetSocketAddress(packet.getAddress(), datas), 2000);
                    //提示连接重构信息
                    JOptionPane.showMessageDialog(chatRoomFrame.jFrame,"连接成功");
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
                    JOptionPane.showMessageDialog(chatRoomFrame.jFrame, filePath+"发送成功");
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
                buttons[3].setEnabled(true);
                System.out.println("这是每次等待时ds的本地端口"+ds.getLocalPort());
                //等待UDP连接到来
                ds.receive(packet);
                //请求连接后不能再让你发了
                //发送按钮失效
                buttons[3].setEnabled(false);
                System.out.println("这个时候已经接收到了发过来的数据"+ds.getLocalPort());
                //拆包 发送的文件名/文件大小
                String data = new String(packet.getData());
                String[] files = data.split("\n");
                //int size = Integer.parseInt(files[1]);
                //询问是否开始文件传输
                System.out.println("这是接收到的UDP数据报文"+data);
                System.out.println(files[0] + files[1]);
                int choice =
                        JOptionPane.showConfirmDialog(chatRoomFrame.jFrame,
                                "文件名"+files[0]+"文件大小(B)"+files[1], "是否接收文件", JOptionPane.OK_CANCEL_OPTION);
                //如果不要的话 继续等待
                if(choice == JOptionPane.CANCEL_OPTION) {
                    byte[] cancel = "0".getBytes(StandardCharsets.UTF_8);
                    packet = new DatagramPacket(cancel,cancel.length,packet.getSocketAddress());
                    ds.send(packet);
                    continue;//不接收 直接继续等待
                } else if(choice == JOptionPane.YES_OPTION) {
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
                    JOptionPane.showMessageDialog(chatRoomFrame.jFrame, files[0]+"接收成功");
                    //关闭所有的流 再次循环
                    fileStream.close();
                    fileSocket.getInputStream().close();
                    fileSocket.close();
                    fileServer.close();
                }
            }
        }
    }

    // 以下皆为功能函数 实现小型功能
    private boolean isMessageInvalid(String msg) {
        //System.out.println(msg);
        for(int i = 0; i < msg.length(); ++i) {
            if(msg.charAt(i) != ' ') {//如果都是空格 则消息为空 不让发送
                return false;
            }
        }
        return true;
    }
    private String getCurrentDate() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }
}
