package Database;

import java.sql.*;
import OrderManager.Order;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;

//TODO figure out how to make this abstract or an interface, but want the method to be static
public class Database {

    public static Connection dbConnect() {

        Connection con = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://localhost/less-than-3", "root", "");
            System.out.println("connected");
        }
        catch (CommunicationsException e) {
            System.err.println("Database not online or couldn't connect to database.");
        }
        catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return con;
    }

    public static void createTable(Connection con) {

        try {
            // create table less-than-3 if it doesn't already exist
            Statement st = con.createStatement();
            st.execute("CREATE table IF NOT EXISTS orders(orderId int, clientId int, instrument varchar(30), size int, price double)");
        }
        catch  (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void write(Order o) {

        Connection con = dbConnect();

        try {
            PreparedStatement pst = con.prepareStatement("INSERT INTO orders VALUES(?,?,?,?,?)");
             pst.setInt(1, o.id);
            pst.setInt(2, o.clientOrderID);
            pst.setString(3, o.instrument.toString());
            pst.setInt(4, o.sizeFilled());
            pst.setDouble(5, o.initialMarketPrice);

            int i = pst.executeUpdate();
            if(i>0){
                System.out.println(i+" record inserted with orderId = " + o.id);
            }
            else {
                System.out.println("Failed to insert record with orderId = " + o.id);
            }
        }
        catch (SQLException e) { e.printStackTrace();}

    }
}