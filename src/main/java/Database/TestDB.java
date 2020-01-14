package Database;

import Database.Database;

import java.sql.Connection;

public class TestDB {

    public static void main(String[] args) {
        Connection con = Database.dbConnect();
        Database.createTable(con);

    }

}