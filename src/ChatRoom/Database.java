package ChatRoom;

import java.sql.*;

public class Database {
    private Connection con;
    private String url = "jdbc:sqlserver://localhost:1433;DatabaseName=Server";
    private Connection getConnection() {
        if(con == null) {
            try{
                con = DriverManager.getConnection(url,"sa","1lucifer");//申请与数据库的连接url 分别是地址 用户名 密码
                System.out.println("连接成功");
            }catch (SQLException e){
                System.out.println("连接失败");
                e.printStackTrace();
            }
        }
        return con;
    }

    public Database() {
        //连接上数据库
        this.con = getConnection();
    }
    //增加一条消息 发送者ip 发送者端口 发送者网名 发送的信息 发送的时间
    public boolean insert(String ip, String port, String name, String message, int time) {
        try {
            if(con == null) {
                con = getConnection();
            }
            Statement st = con.createStatement();
            String sql = "insert GroupChat(ip,port,name,message,time)" +
                    "values('"+ip+"','"+port+"','"+name+"','"+message+"',"+time+");";
            System.out.println("这是查询语句"+sql);
            st.execute(sql);
        }catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    //删除指定ip的所有消息
    public boolean delete(String ip) {
        try {
            if(con == null) {
                con = getConnection();
            }
            Statement st = con.createStatement();
            st.executeQuery("delete from GroupChat" +
                        "where ip = '" + ip+"'");
        }catch (Exception ex) {
            ex.printStackTrace();
            return  false;
        }
        return true;
    }

    //修改 对应ip在某个时间发送的某条消息为新的message
    public boolean update(String ip, String message, int time) {
        try {
            if(con == null) {
                con = getConnection();
            }
            Statement st = con.createStatement();
            st.executeQuery("update GroupChat " +
                    "set message = '"+message+"'" +
                    "where ip = '"+ip+"' and time = " + time);
        }catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
    //查询所有的信息
    public String selectAll() {
        String s = "";
        try {
            if(con == null) {
                con = getConnection();
            }
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("select * from GroupChat");
            while (rs.next()) {
                s += rs.getString(1);//ip
                s += '\t';
                s += rs.getString(2);//port
                s += '\t';
                s += rs.getString(3);//name
                s += '\t';
                s += rs.getString(4);//message
                s += '\t';
                s += rs.getString(5);//time
                s += '\n';
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }
    //查询指定ip的所有信息
    public String selectIP(String ip) {
        String s = "";
        try {
            if(con == null) {
                con = getConnection();
            }
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("select * from GroupChat where ip = '" + ip + "'");
            while (rs.next()) {
                s += rs.getString(1);//ip
                s += '\t';
                s += rs.getString(2);//port
                s += '\t';
                s += rs.getString(3);//name
                s += '\t';
                s += rs.getString(4);//message
                s += '\t';
                s += rs.getString(5);//time
                s += '\n';
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return s;
    }

    public static void main(String[] args) {
        String select = new Database().selectAll();
        System.out.println(select);
    }
}
