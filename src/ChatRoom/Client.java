package ChatRoom;

import javax.swing.*;

public class Client extends JFrame {
    public static void main(String[] args) {
        new ChatRoomFrame("client1").startCommunication();
    }
}
