package Database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    Connection testCon;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up DatabaseTest");
        testCon = Database.dbConnect();
    }

    @AfterEach
    void tearDown() {
        try {
            testCon.close();
        }
        catch(SQLException e) { e.printStackTrace(); }
    }


    @Test
    void createTableAndCheckColumns() {
        Database.createTable(testCon);
        try {
            Statement st = testCon.createStatement();
            ResultSet resultSet = st.executeQuery("SELECT * from orders");
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int numColumns = rsmd.getColumnCount();
            System.out.println(numColumns);
            ArrayList<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= numColumns; i++) {
               columnNames.add(rsmd.getColumnName(i));
            }
            ArrayList<String> expected = new ArrayList<>(Arrays.asList("orderId", "clientId", "instrument", "size", "price"));
            assertArrayEquals( expected.toArray(), columnNames.toArray());
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    void write() {
    }
}