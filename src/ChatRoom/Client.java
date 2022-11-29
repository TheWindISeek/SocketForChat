package ChatRoom;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client extends TabPage {
    InputStreamReader reader;
    BufferedReader bufferedReader;
    //客户端直接继承自TabPage
    Client(ChatRoomFrame chatRoomFrame, Socket socket) {
        super(chatRoomFrame, socket);
        this.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                //按下enter 就发送数据
                if(e.getKeyCode() == KeyEvent.VK_UP) {
                    System.out.println("你按了上键");
                }
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                    System.out.println("你按下了enter键");
                }
            }
            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println(e.getKeyCode());
                //按下enter 就发送数据
                if(e.getKeyCode() == KeyEvent.VK_UP) {
                    System.out.println("你按了上键");
                }
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                    System.out.println("你按下了enter键");
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                //按下enter 就发送数据
                if(e.getKeyCode() == KeyEvent.VK_UP) {
                    System.out.println("你按了上键");
                }
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                    System.out.println("你按下了enter键");
                }
            }
        });
    }

    //与指定的服务器进行通信
    //这个线程用于接收接收数据
    @Override
    public void run() {
        try{
            //先互相通信
            this.cout.println(this.chatRoomFrame.name);
            this.cout.println(udpPort);
            this.cout.flush();
            //开始读取
            reader = new InputStreamReader(this.socket.getInputStream());
            bufferedReader = new BufferedReader(
                    reader
            );
            System.out.println("client"+this.chatRoomFrame.name+"已经在这里等着了");
            String line;
            while ((line = bufferedReader.readLine()) != null && !line.equals(" ")) {
                //收到了消息显示上就行了
                System.out.println(chatRoomFrame.name+line);
                this.text_receiver.append(line+"\n");
            }

            System.out.println("客户端接收进程已退出");
            closeAllStream();
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    //关闭所有流
    protected void closeAllStream() throws Exception {
        this.bufferedReader.close();
        this.reader.close();
        this.cout.close();
        this.socket.close();
    }
    //客户端向服务器发送数据
    @Override
    void sendMessage() {
        if(isMessageInvalid(this.text_sender.getText())) {//没有消息 无法发送
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "发送消息不能为空");
            return;
        }
        if(this.text_sender.getText().length() > 32) {
            JOptionPane.showMessageDialog(this.chatRoomFrame.jFrame, "发送消息过长");
            return;
        }
        //发送数据
        this.cout.print(this.chatRoomFrame.name + "  \t" +
                getCurrentDate() + "\n" + this.text_sender.getText() + "\n");
        this.cout.flush();
        this.text_sender.setText("");//重置输入
    }

    @Override
    void deletePage() {
        try {
            super.deletePage();
            this.closeAllStream();
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
