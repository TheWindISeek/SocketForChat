package ChatRoom;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.*;
import java.util.List;

/*
 * 实现控制功能的类
 * 用于监听各种事件
 */
public class ChatRoomControl extends WindowAdapter implements ActionListener {
    private ChatRoomFrame chatRoomFrame;

    public ChatRoomControl(ChatRoomFrame chatRoomFrame) {
        this.chatRoomFrame = chatRoomFrame;
    }
    @Override
    public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        this.chatRoomFrame.closeAllStream();
        System.out.println("close all stream!");
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("请求连接")) {
            requestToLink();
        } else if (e.getActionCommand().equals("停止监听")) {
            this.chatRoomFrame.stopListenning();
        }
    }

    private void requestToLink() {
        String host;  //获得主机IP地址
        //如果当前已经被编辑过了 那么直接获取这个IP
        if (this.chatRoomFrame.comboBox.getSelectedIndex() == -1) {
            host = (String) this.chatRoomFrame.comboBox.getEditor().getItem();
        } else { // 如果当前没有被编辑 说明是选的给的东西
            host = (String) this.chatRoomFrame.comboBox.getSelectedItem();
        }
        try {
            int port = Integer.parseInt(this.chatRoomFrame.text_port.getText());//读取用户输入的端口
            if (port == this.chatRoomFrame.waitPort) {
                throw new ConnectException();
            }
            /*
                if(!isInLAN(host)) {
                    JOptionPane.showMessageDialog(jFrame, "所请求的IP和当前主机不在同一网段");
                    return;
                }
                */
            //如果是默认情况下 在socket构造的时候就去进行连接 那么默认的时间会达到20s 太长了 为其单独开一个线程过于麻烦
            //利用connect方法去限制了一下
            //针对这个问题别人还给出了一个方法 也就是利用getByAddr函数 看看能不能通过ip获得主机地址 这个函数快点
            //System.out.println("这是获得的host"+host);
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 2000);//如果IP地址不对 那么其实就会连接失败
            //this.chatRoomFrame.table.addTab(this.chatRoomFrame.name, TabPage(socket));
            this.chatRoomFrame.addTab(this.chatRoomFrame.name, socket);//tab添加新页
            this.chatRoomFrame.table.setSelectedIndex(this.chatRoomFrame.table.getTabCount() - 1); //tab指定新页为选中状态
        } catch (NumberFormatException ex) {    //端口格式异常
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "端口不是一个整数");
            System.out.println(ex.getClass().getName());
        } catch (UnknownHostException ex) {     //未知主机异常
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "主机IP地址错误。");
            System.out.println(ex.getClass().getName());
        } catch (ConnectException ex) { //连接异常
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "IP地址或端口错误，未建立TCP连接");
            System.out.println(ex.getClass().getName());
        } catch (SocketTimeoutException ex) {
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "连接超时 请检查IP地址");
        } catch (IOException ex) { // 传输的时候出现了问题
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "Socket连接时出现了异常");
            System.out.println(ex.getClass().getName());
        } catch (Exception ex) {
            System.out.println(ex.getClass().getName());
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "发生了未知错误");
        }
    }

    //计算子网内所有ip
    private String[] AllSubnetIP(String localIp, int netLength) {
        int hostLength = 32 - netLength;
        String[] allIp = new String[(int) (Math.pow(2, hostLength) - 2)];

        String[] splitedIp = null;
        int[] ipToInt = new int[4];

        splitedIp = localIp.split("\\."); // . 表示任意字符，要写成 " \\. "
        for (int i = 0; i < splitedIp.length; i++) {
            ipToInt[i] = Integer.parseInt(splitedIp[i]);//转化成int
            //System.out.println(ipToInt[i]);
        }
        String s = "";

        for (int i = 0; i < ipToInt.length; i++) {
            String str = "00000000";
            String temp;
            temp = str + Integer.toBinaryString(ipToInt[i]);
            //本机ip的二进制表示
            s = s + temp.substring(temp.length() - 8);

        }
        System.out.println("本机IP的二进制表示：" + s + "   " + s.length() + "\n");

        //截取出不变的网络号部分
        String netPart;
        netPart = s.substring(0, netLength);

        for (int i = 1; i < Math.pow(2, hostLength) - 1; i++) {
            String str = "00000000";
            String temp;
            temp = str + Integer.toBinaryString(i);
            allIp[i - 1] = netPart + temp.substring(temp.length() - 8);
            allIp[i - 1] = Integer.parseInt(
                    allIp[i - 1].substring(0, 8), 2) + "." +
                    Integer.parseInt(allIp[i - 1].substring(8, 16), 2) + "." +
                    Integer.parseInt(allIp[i - 1].substring(16, 24), 2) + "." +
                    Integer.parseInt(allIp[i - 1].substring(24), 2
                    );
        }
        return allIp;
    }

    private boolean isInLAN(String ip) throws UnknownHostException, SocketException {
        InetAddress localHost = InetAddress.getLocalHost();
        String hostAddress = localHost.getHostAddress();
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
        List<InterfaceAddress> list = networkInterface.getInterfaceAddresses();
        int netLength = list.get(0).getNetworkPrefixLength();
        String[] allIP = AllSubnetIP(hostAddress, netLength);
        for (String s : allIP) {
            if (s == ip) {
                return true;
            }
        }
        return false;
    }
}
