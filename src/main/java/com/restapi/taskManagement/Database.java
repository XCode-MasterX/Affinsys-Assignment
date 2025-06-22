package com.restapi.taskManagement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;

// Connections: Work
// Getting Data: Works
// Updating data: Works
// Inserting data: Works

/* Delete Syntax:
        delete from <table name> where <conditions>;
    Getting Syntax:
        select <col name> from <table name> where <conditions>;
    Insertion Syntax:
        insert into <table name> <col. names (optional)> values(<values of mentioned cols. or all cols.>);
    Update Syntax:
        update <table name> set <col name> = <new value> where <conditions>;
*/
public class Database {
    private static Database instance;
    private Connection conn;
    private PreparedStatement statement = null;
    private ResultSet result;
    private ResultSetMetaData metaData;

    private final String DEFAULT_LINK = "jdbc:mysql://127.0.0.1:3306/world";
    private final String DEFAULT_USER = "root";
    private final String DEFAULT_PASSWORD = "123456789";

    private Database() {
        try { 
            conn = DriverManager.getConnection(DEFAULT_LINK, DEFAULT_USER, DEFAULT_PASSWORD);
        }
        catch(SQLException exception) {
            System.out.println("State: " + exception.getSQLState());
            exception.printStackTrace();
        }
    }

    private Database(String url, String user, String password) {
        try { 
            conn = DriverManager.getConnection(url, user, password);
        }
        catch(SQLException exception) {
            System.out.println("State: " + exception.getSQLState());
            exception.printStackTrace();
        }
    }

    public static Database getInstance() {
        if(instance == null) instance = new Database();
        return instance;
    }

    public void populateStatement(Object... things) {
        try{
            for(int i = 0; i < things.length; i++) {
                if(things[i] instanceof String str)
                    statement.setString(i + 1, str);
                else if(things[i] instanceof Integer in)
                    statement.setInt(i + 1, in);
                else if(things[i] instanceof Number num)
                    statement.setDouble(i + 1, num.doubleValue());
                else if(things[i] instanceof Boolean bool)
                    statement.setBoolean(i + 1, bool);
                else if(things[i] instanceof LocalDateTime date)
                    statement.setTimestamp(i + 1, java.sql.Timestamp.valueOf(date));
                else if(things[i] instanceof Instant instant)
                    statement.setTimestamp(i + 1, Timestamp.from(instant));
            }
        }
        catch(SQLException e) {
            System.out.println(e.getSQLState());
            e.printStackTrace();
        }
    }

    public String[] getQuery(String query, Object... things) {
        ArrayList<String> allRows = new ArrayList<>();
        StringBuilder json = new StringBuilder();

        try {
            statement = createPrep(query);
            populateStatement(things);

            result = statement.executeQuery();
            metaData = result.getMetaData();
            final int colCount = result.getMetaData().getColumnCount();

            while(result.next()) {
                json.delete(0, json.length());

                json.append("{\n");
                    for(int i = 0; i < colCount; i++) {
                        String colName = metaData.getColumnName(i + 1);
                        Object ret = result.getObject(colName);

                        // PLEASE GSON JUST SHUT THE HELL UP ABOUT TIMEZONES.
                        if(colName.equals("timestamp"))
                        {
                            json.append(String.format("\"%s\" : \"%sZ\",\n", colName, ret));
                            continue;
                        }

                        json.append(String.format("\"%s\" : \"%s\",\n", colName, ret));                        
                    }
                json.append("}");

                allRows.add(json.deleteCharAt(json.length() - 3).toString());
            }
            statement.close();
            result.close();
        }
        catch(SQLException exception) {
            System.out.println("State: " + exception.getSQLState());
            exception.printStackTrace();
            return null;
        }
        finally {
            result = null;
            metaData = null;
        }

        String ret[] = new String[allRows.size()];
        ret = allRows.toArray(ret);
        return ret;
    }

    public int updateQuery(String query, Object... things) {
        int updated = -1;

        try {
            statement = createPrep(query);
            populateStatement(things);

            updated = statement.executeUpdate();
            statement.close();
        }
        catch(SQLException exception) {
            System.out.println("State: " + exception.getSQLState());
            exception.printStackTrace();
        }

        return updated;
    }

    public int deleteQuery(String query, Object... things) {
        int updated = 0;

        try {
            statement = createPrep(query);
            populateStatement(things);
            updated = statement.executeUpdate();
            statement.close();
        }
        catch(SQLException exception) {
            updated = 0;
            System.out.println("State: " + exception.getSQLState());
            exception.printStackTrace();
        }

        return updated;
    }

    public PreparedStatement createPrep(String query) {
        try{
            return conn.prepareStatement(query);
        }
        catch(SQLException e) {
            System.out.println(e.getSQLState());
            e.printStackTrace();
        }
        return null;
    }

    public void terminate() {
        try{
            statement.close();
            result.close();
            if (conn != null && !conn.isClosed())
                conn.close();
        }
        catch(SQLException e) {
            System.out.println(e.getSQLState());
            e.printStackTrace();
        }
    }

    // THIS METHOD WAS ONLY USED TO MANUALLY TEST THE QUERIES OF THIS PROGRAM.
    public static void main(String args[]) {
        java.util.Scanner in = new java.util.Scanner(System.in);

        System.out.println("Enter the connection url: ");
        Database ins = getInstance();

        System.out.println("Enter query: ");
        String query = in.nextLine();
        String start = query.split(" ")[0].toLowerCase();

        if(start.equals("select"))
            System.out.println(java.util.Arrays.toString(ins.getQuery(query)));
        else if(start.equals("update") || start.equals("insert"))
            System.out.println(ins.updateQuery(query) + " rows updated.");
        else
            System.out.println(ins.deleteQuery(query));

        in.close();
    }
}