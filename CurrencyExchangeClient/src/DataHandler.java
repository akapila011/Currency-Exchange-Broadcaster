
import java.sql.*;
import java.util.ArrayList;

public class DataHandler {

    public Connection conn = null;

    public DataHandler(){
        try{    // Create or open currencyexchange.db
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:currencyexchange.db");
            conn.setAutoCommit(false);
            //createTable();
        }
        catch (ClassNotFoundException cEx){
            cEx.printStackTrace();
            //System.out.println("Could not find the JDBC Class");
        }
        catch (SQLException sqlEx){
            System.out.println("Could not find currencyexchange.db");
        }
    }

    public void createTable(){
        Statement stmt;
        try{
            stmt = conn.createStatement();
            // create table if not exists TableName (col1 typ1, ..., colN typN)
            String sql = "CREATE TABLE IF NOT EXISTS rates " +
                    "(id INT PRIMARY KEY NOT NULL, " +
                    "exchange_name CHAR(10) NOT NULL, " +
                    "exchange_rate CHAR(8) NOT NULL, " +
                    "date CHAR(12), " +
                    "time CHAR(6));";
            stmt.execute(sql);
            stmt.close();
        }
        catch (SQLException sqlEx){
            System.out.println("could not create 'rates' table");
        }
    }

    public boolean insertRate(String[] record){
        // record is name,rate,date,time
        try{
            Statement stmt = conn.createStatement();
            String sql = "INSERT INTO rates (exchange_name,exchange_rate,date,time)" +
                    "VALUES ('" + record[0] + "', '" + record[1] + "', '" + record[2] + "', '" + record[3] + "');";
            stmt.execute(sql);
            stmt.close();
            conn.commit();
            return true;
        }
        catch (SQLException sqlEx){
            return false;
        }
    }

    public ArrayList<String[]> findRateHistory(String rateName){
        ArrayList<String[]> history = new ArrayList<String[]>();
        try{
            Statement stmt = conn.createStatement();
            ResultSet results = stmt.executeQuery("SELECT exchange_name, exchange_rate, date, time FROM rates");
            while (results.next()){
                String[] record = new String[4];
                record[0] = results.getString("exchange_name");
                record[1] = results.getString("exchange_rate");
                record[2] = results.getString("date");
                record[3] = results.getString("time");
                history.add(record);
            }
            results.close();
            stmt.close();
            return history;
        }
        catch (SQLException sqlEx){
            System.out.println("Problem in finding exchange rates history for " + rateName);
            return history;
        }
    }
}
