package ChatRoom;
//TCP

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ChatRoomFrame  {
    //聊天室标题
    private String title = "聊天室";
    //下拉框 可显示的IP
    private String[] ip = {"127.0.0.1", "192.168.2.220", "192.168.2.241"};
    //名字 和 等待的端口
    String name = "";
    int waitPort;
    //框架和面板
    JFrame jFrame = new JFrame();
    private JPanel guide = new JPanel();
    private JPanel chat = new JPanel();
    //最上面的工具栏
    private JToolBar toolBar = new JToolBar();
    private JTextField text_name = new JTextField("");//网名
    private JTextField wait_port = new JTextField("");//本机当前的等待端口
    JComboBox<String> comboBox = new JComboBox<String>(ip);//可选电脑IP的连接
    JTextField text_port = new JTextField("", 5);//连接电脑的端口
    private JButton connect = new JButton("请求连接");//连接按钮
    private JButton stopListen = new JButton("停止监听");//结束循环按钮
    //下方的聊天窗口
    JTabbedPane table = new JTabbedPane();
    // 所有的socket和SocketServer
    private java.util.List<Socket> socketList = new ArrayList<>();
    private java.util.List<ServerSocket> serverSocketList = new ArrayList<>();

    //监听类
    private ChatRoomControl chatRoomControl = new ChatRoomControl(this);
    //是否监听
    private boolean isListenning = true;
    /*
    * 构造函数 用于指定网名 等待端口 和 标题
    * */
    public ChatRoomFrame(String name, int waitPort, String title) {
        init(name, waitPort, title);
    }
    //用户名字 和 将要等待的端口
    public ChatRoomFrame(String name, int waitPort) {
        init(name, waitPort, "聊天室");
    }

    public ChatRoomFrame(String name) {
        init(name, 0, "聊天室");
    }

    private void init(String name, int waitPort, String title) {
        try {
            //整体框架的位置 大小 布局
            jFrame.setLayout(new BorderLayout());
            jFrame.setLocation(700, 300);
            jFrame.setMinimumSize(new Dimension(700, 400));
            jFrame.setTitle(title + "  " + InetAddress.getLocalHost().toString());
            //可视 按关闭直接关闭
            jFrame.add(guide, "North");
            jFrame.add(chat, BorderLayout.CENTER);
            jFrame.pack();
            jFrame.setVisible(true);
            jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);   //该关的时候关
            //让别人自己输入网名
            name = JOptionPane.showInputDialog(jFrame,"请输入你的网名",JOptionPane.OK_CANCEL_OPTION);
            this.title = title;
            this.name = name;
            this.waitPort = waitPort;
            //上部面板
            guide.setLayout(new BorderLayout());
            guide.add(toolBar, "North");
            //网名
            toolBar.add(new JLabel("网名"));
            this.name = name;
            text_name.setText(name);
            text_name.setEditable(false);//无法修改网名
            toolBar.add(text_name);
            //接受消息用的端口
            toolBar.add(new JLabel("等待端口"));
            wait_port.setText("" + waitPort);
            toolBar.add(wait_port);
            wait_port.setEditable(false);      //无法自行修改
            wait_port.setColumns(5);
            //可供选择的IP
            toolBar.add(new JLabel("IP"));
            toolBar.add(comboBox);
            toolBar.add(new JLabel("端口"));
            toolBar.add(text_port);
            toolBar.add(connect);
            toolBar.add(stopListen);
            //可自行输入IP
            comboBox.setEditable(true);
            //聊天面板
            chat.setLayout(new BorderLayout());
            chat.add(table);
            chat.setVisible(true);
            //添加监听程序
            connect.addActionListener(chatRoomControl);
            stopListen.addActionListener(chatRoomControl);
            jFrame.addWindowListener(chatRoomControl);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    //应该关闭所有的流 成功返回true 失败返回false
    boolean closeAllStream() {
        try {
            for (Socket socket : socketList) {
                socket.close();
            }
            for (ServerSocket serverSocket : serverSocketList) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        System.out.println(name + "关闭了所有流");
        return true;
    }

    //开始进行监听
    public void startCommunication() {
        try{
            //等待TCP连接建立 一个建立完成后 继续循环
            while (isListenning) {
                System.out.println(waitPort);
                ServerSocket serverSocket = new ServerSocket(waitPort);
                waitPort = serverSocket.getLocalPort();
                System.out.println(waitPort);
                wait_port.setText("" + waitPort);

                Socket socket = serverSocket.accept();
                //用于释放
                socketList.add(socket);
                serverSocketList.add(serverSocket);
                //新页面
                table.addTab("", new TabPage(this, socket));
                table.setSelectedIndex(table.getTabCount() - 1);
                //等待端口后移
                waitPort = 0;//等于0的时候 让系统自动分配空闲端口 尽可能避免端口的冲突
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(jFrame, ex.getMessage());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(jFrame, ex.getMessage());
        }finally {
            closeAllStream();
        }
    }
    void stopListenning() {
        isListenning = false;
        wait_port.setText("停止监听");
        System.out.println(this.name + "停止了监听");
    }
    //tab 添加新页
    void addTab(String name, Socket socket) {
        table.addTab(name, new TabPage(this, socket)); //tab添加新页
    }
}
