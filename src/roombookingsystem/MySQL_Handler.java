/* 
 * Original MySQL handler code written by Dan Hook
 * Licensed under the Attribution 4.0 International license, anyone 
 * is free to modify this code in any way, shape or form, even for 
 * commercial use as long as the license is followed. 
 *
 * You may not apply legal terms or technological measures that 
 * legally restrict others from doing anything the license permits.
 * https://creativecommons.org/licenses/by/4.0/
*/

package roombookingsystem;

import com.mysql.jdbc.exceptions.jdbc4.MySQLNonTransientConnectionException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Alert;
 
/**
 * Allows Java programs to connect to an external MySQL database. Many tools 
 * have been added specifically for this application's purpose. 
 * @author Dan Hook
 */
public class MySQL_Handler
{
    // Use XAMPP (latest version) if the droplets become out of date 
    // or I run out of credit. 
    private final boolean DEBUG = false; 
    
    private static final String HOST = "localhost";
    private static final int PORT = 3306;
    private static final String DATABASE = "time_table_app";
    private static final String USERNAME = "root";
    private static final String PASSWORD = ""; 
    private Connection connection;
    
    private void DebugPrint( String text ) { if ( DEBUG == true ) System.out.println( "[SQL-DEBUG]: " + text ); }
 
    private void openConnection() throws SQLException, ClassNotFoundException, MySQLNonTransientConnectionException 
    {
        if ( connection != null && !connection.isClosed() ) return; 
 
        synchronized ( this ) 
        {
            if ( connection != null && !connection.isClosed() ) return;
            
            Class.forName( "com.mysql.jdbc.Driver" );
            DebugPrint( "Loaded MySQL driver" );
            DebugPrint( "Trying to connect..." );
            connection = DriverManager.getConnection( "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + "?autoReconnect=true&useSSL=false", USERNAME, PASSWORD );
            DebugPrint( "Connected." );
        }
    }
 
    /**
     * Runs the given SQL query against the database that is currently set up with the program. 
     * @param s The SQL query to run. 
     * @return Results from the query. 
     */
    public ResultSet query( String s ) 
    {
        if ( connection == null ) {
            DebugPrint( "No connection." );
            try {
                openConnection();
            } catch ( ClassNotFoundException e ) {
                errnotify( "The MySQL JDBC driver was not found or has been removed, contact an administrator." );
            } catch ( SQLException e ) {
                errnotify( "Could not establish a connection to the MySQL database." );
            }
        }
        
        try {
            Statement statement = connection.createStatement();
            return statement.executeQuery( s );
        } catch ( SQLException e ) {
            System.out.println( e.getMessage() );
            
            try {
                connection.close();
            } catch ( SQLException e1 ) {
                System.out.println( e1.getMessage() );
            }
            
            connection = null;
            
            try {
                openConnection();
            } catch ( ClassNotFoundException | SQLException e1 ) {
                System.out.println( e1.getMessage() );
            }
            
            try {
                Statement statement = connection.createStatement();
                return statement.executeQuery( s );
            } catch ( SQLException e1 ) {
                System.out.println( e1.getMessage() );
            }
        }
        
        return null;
    }
 
    /**
     * Executes an update on the current database from the given SQL statement.
     * @param string SQL query to update. 
     * @return -1 if there's an error.
     */
    public int executeUpdate( String string ) 
    {
        if ( connection == null ) {
            DebugPrint( "No connection." );
            try {
                openConnection();
            } catch ( ClassNotFoundException e ) {
                errnotify( "The MySQL JDBC driver was not found or has been removed, contact an administrator." );
            } catch ( SQLException e ) {
                errnotify( "Could not establish a connection to the MySQL database." );
            }
        }
        
        try {
            return connection.createStatement().executeUpdate( string );
        } catch ( SQLException e ) {
            System.out.println( e.getMessage() );
            
            try {
                connection.close();
            } catch ( SQLException e1 ) {
                System.out.println( e1.getMessage() );
            }
            
            connection = null;
            
            try {
                openConnection();
            } catch ( ClassNotFoundException | SQLException e1 ) {
                System.out.println( e1.getMessage() );
            }
            
            try {
                return connection.createStatement().executeUpdate( string );
            } catch ( SQLException e1 ) {
                System.out.println( e1.getMessage() );
            }
        }
        
        return -1;
    }
    
    /**
     * Checks to see if the given username already exists in the database. 
     * @param username Username to check.
     * @return true if it exists, false if it doesn't. 
     */
    public boolean CheckAccountExists( String username ) 
    {
        try {
            ResultSet rs = this.query( "SELECT UserName FROM users" );

            while ( rs.next() ) { if ( rs.getString( "UserName" ).equals( username ) ) { return true; } }
        } catch ( SQLException err ) {
            System.out.println( err.getMessage() );
        }
        
        return false;
    }
    
    /**
     * Creates a user account in the database. 
     * @param username Username that the user uses to login with. 
     * @param password Password that the user uses to login with. 
     * @param title Title of the user. 
     * @param firstname First name of the user. 
     * @param lastname Surname of the user. 
     */
    public void CreateUserAccount( String username, String password, String title, String firstname, String lastname, boolean authorised ) 
    {
        boolean exists = this.CheckAccountExists( username );
        byte auth = this.SQLBoolean( authorised );
        
        if ( exists ) {
            DebugPrint( "Account already exists, try a different username." );
        } else {
            String act = String.format( "INSERT INTO users (UserName, UserPassword, UserFirstName, UserLastName, UserRank, UserTitle, UserAuthorised) VALUES ('%s', MD5('%s'), '%s', '%s', '%d', '%s', '%d');", username, password, firstname, lastname, 0, title, auth ); 
            
            this.executeUpdate( act ); 
            
            DebugPrint( "Account created!" );
        }
    }
    
    /**
     * Checks to make sure that the given username and password make a valid account. The 
     * password is converted into an MD5 hash to keep it protected/encrypted, and unreadable 
     * by human eyes. 
     * @param username Username to check. 
     * @param password Password to check. 
     * @return true if the account is legit, false if it isn't. 
     */
    public boolean AuthenticateUser( String username, String password )
    {
        String hash, q, pw = "";
        ResultSet rs; 
        
        if ( this.CheckAccountExists( username ) ) {
            hash = this.GenerateHash( password );
            q = String.format( "SELECT * FROM users WHERE UserName = '%s';", username );
            rs = this.query( q );
            
            try {
                while ( rs.next() ) { 
                    pw = rs.getString( "UserPassword" );
                }

                if ( pw.equals( hash ) ) {
                    DebugPrint( "Username and password is correct!" );
                    return true; 
                } else {
                    DebugPrint( "Username or password is incorrect." );
                    return false; 
                }
            } catch ( SQLException err ) {
                System.out.println( err.getMessage() );
                return false;
            }
        } else {
            DebugPrint( "Sorry, that account doesn't exist." );
            return false;
        }
    }
    
    // Password notes - http://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords-in-java?rq=1
    private String GenerateHash( String password )
    {
        try {
            MessageDigest md = MessageDigest.getInstance( "MD5" );
            
            md.update( password.getBytes(), 0, password.length() );
            String hash = "" + new BigInteger( 1, md.digest() ).toString( 16 );
            
            return hash; 
        } catch ( NoSuchAlgorithmException err ) {
            System.out.println( err.getMessage() );
        }
        
        return "Error"; 
    }
    
    /**
     * Makes the user of the given ID an administrator. 
     * @param userid Unique ID
     */
    public void MakeAdmin( int userid ) 
    {
        int rank = this.GetRank( userid );
        
        if ( rank == 0 ) {
            this.executeUpdate( String.format( "UPDATE users SET UserRank = '1' WHERE UserID = '%d';", userid ) );
        }
    }
    
    /**
     * Internal method for collecting a set of data for a specific user from a column in the users table.
     * @param col_name The name of the column in the users table. 
     * @param userid User's unique ID.
     * @return String - result of the wanted data. 
     */
    private String GetUserColumnInfo( String col_name, int userid )
    {
        ResultSet column = this.query( String.format( "SELECT %s FROM users WHERE UserID = '%d';", col_name, userid ) );
        
        try {
            while ( column.next() ) {
                String info = column.getString( col_name );
                return info; 
            }
        } catch( SQLException err ) {
            System.out.println( err.getMessage() );
        }
        
        return "Error"; 
    }
    
    /**
     * Gets the userid linked to the given username. 
     * @param username Username to look for. 
     * @return Unique ID of the given username. 
     */
    public int GetUserID( String username ) 
    {
        ResultSet row = this.query( String.format( "SELECT * FROM users WHERE UserName = '%s';", username ) );
        
        try {
            while ( row.next() ) 
            { 
                if ( row.getString( "UserName" ).equals( username ) ) {
                    int id = row.getInt( "UserID" );
                    return id;
                }
            }
        } catch( SQLException err ) {
            System.out.println( err.getMessage() );
        }
        
        return -1;
    }
    
    /**
     * Used to find a username based on the given unique user id. 
     * @param userid Unique ID
     * @return Username linked to the given user id. 
     */
    public String GetUsername( int userid )
    {
        String user_name = this.GetUserColumnInfo( "UserName", userid );
        return user_name; 
    }
    
    /**
     * Used to find a first name based on the given unique user id. 
     * @param userid Unique ID 
     * @return First name linked to the given user id. 
     */
    public String GetFirstName( int userid )
    {        
        String first_name = this.GetUserColumnInfo( "UserFirstName", userid );
        return first_name;
    }
    
    /**
     * Used to find a surname based on the given unique user id. 
     * @param userid Unique ID
     * @return Surname linked to the given user id. 
     */
    public String GetLastName( int userid ) 
    {
        String last_name = this.GetUserColumnInfo( "UserLastName", userid );
        return last_name;
    }
    
    /**
     * Used to find the level of access for a specific user. 
     * @param userid Unique ID
     * @return Level of access, 0 being a standard user and 1 being an administrator. 
     */
    public int GetRank( int userid )
    {
        int rank = Integer.parseInt( this.GetUserColumnInfo( "UserRank", userid ) );
        return rank; 
    }
    
    /**
     * Works out if the given user id has an administrator level. 
     * @param userid Unique ID
     * @return True if admin, false if standard user. 
     */
    public boolean IsAdmin( int userid ) 
    {
        int rank = this.GetRank( userid );
        
        return rank == 1; 
    }

    /**
     * Used to find the title linked to the given unique user id. 
     * @param userid Unique ID
     * @return Title linked to the given user id. 
     */
    public String GetTitle( int userid )
    {
        String title = this.GetUserColumnInfo( "UserTitle", userid );
        return title;
    }
    
    /**
     * Used to get a user's name formatted in B Smith instead of Bob Smith
     * @param userid Unique ID
     * @return Formatted name 
     */
    public String GetBookName( int userid ) 
    {
        String firstname = this.GetFirstName( userid );
        String lastname = this.GetLastName( userid ); 
        
        String[] firstInitial = firstname.split( "" );
        
        return firstInitial[0] + " " + lastname; 
    }
    
    /**
     * Checks if there is already a booking on the given date and slot. 
     * @param date Date to check for (must be in English form, e.g. 03/12/2016).
     * @param slot Slot to check, 1 = 1st period, 2 = 2nd period and 3 = 3rd period. 
     * @param room 
     * @return True if it exists, false if it doesn't. 
     */
    public boolean CheckBookingExists( LocalDate date, byte slot, String room )
    {
        String dateS = this.ConvertDate( date );
        
        ResultSet bookings = this.query( "SELECT * FROM bookings;");
        
        try {
            while ( bookings.next() ) {
                String dateDB, roomDB;
                int slotDB; 
                
                dateDB = bookings.getString( "BookingDate" );
                slotDB = bookings.getInt( "BookingSlot");
                roomDB = bookings.getString( "BookingRoom" );
                
                if ( ( dateDB.equals( dateS ) ) & ( slotDB == slot ) & ( roomDB.equals( room ) ) )
                {
                    DebugPrint( "Booking already exists." );
                    return true; 
                }
            }
        } catch ( SQLException err ) {
            System.out.println( err.getMessage() );
        }
        
        return false; 
    }
    
    /**
     * Creates a booking in the database with the given arguments. 
     * @param date Date of the booking. 
     * @param year_group Year group of lesson 
     * @param slot Slot of the booking, 1, 2 or 3. 
     * @param room Room of the booking. 
     * @param subject Subject that takes place in the room. 
     * @param userid User ID of who booked it. 
     */
    public void CreateBooking( LocalDate date, String year_group, byte slot, String room, String subject, int userid ) 
    {
        if ( this.CheckBookingExists( date, slot, room ) == false ) {
            String dateS = this.ConvertDate( date );
            String q = String.format( "INSERT INTO bookings (BookingDate, BookingYearGroup, BookingSlot, BookingRoom, BookingSubject, UserID) VALUES ('%s', '%s', '%d', '%s', '%s', '%d');", dateS, year_group, slot, room, subject, userid );
            this.executeUpdate( q );
        }
    }
    
    /**
     * Deletes a booking based on the data passed to the function. 
     * @param date Date to delete from. 
     * @param slot Slot to delete from. 
     * @param room Room to delete from. 
     */
    public void DeleteBooking( LocalDate date, byte slot, String room )
    {
        if ( this.CheckBookingExists( date, slot, room ) == true )
        {
            String q = String.format( "DELETE FROM bookings WHERE BookingDate = '%s' AND BookingSlot = '%d' AND BookingRoom = '%s';", date, slot, room );
            this.executeUpdate( q );
        }
    }
    
    /**
     * Gets a set of bookings between the dates given. 
     * @param startDate Start date of range. 
     * @param endDate End date of range. 
     * @param room Room to check for bookings.
     * @return ArrayList[][] of bookings. 
     */
    public ArrayList[][] GetBookings( LocalDate startDate, LocalDate endDate, String room )
    {
        String q = String.format( "SELECT * FROM bookings WHERE BookingRoom = '%s' AND BookingDate >= '%s' AND BookingDate <= '%s' ORDER BY BookingDate;", room, startDate, endDate );
        ResultSet rs = this.query( q );
        ArrayList[][] bookingData = new ArrayList[5][3];
        int index = 0; 
        LocalDate currentDate = startDate; 
        
        try {
            while ( rs.next() )
            {
                int id, uid; 
                LocalDate date; 
                String year, roomDB, subject;
                byte slot; 
                
                id = rs.getInt( "BookingID" );
                date = rs.getDate( "BookingDate" ).toLocalDate();
                year = rs.getString( "BookingYearGroup" );
                slot = rs.getByte( "BookingSlot" );
                roomDB = rs.getString( "BookingRoom" );
                subject = rs.getString( "BookingSubject" );
                uid = rs.getInt( "UserID" );
                
                if ( !currentDate.equals( date ) ) {
                    while ( !currentDate.equals( date ) ) {
                        currentDate = currentDate.plusDays( 1 );
                        index++;
                    }
                }
                
                bookingData[ index ][ slot ] = new ArrayList();
                    
                bookingData[ index ][ slot ].add( id ); 
                bookingData[ index ][ slot ].add( date ); 
                bookingData[ index ][ slot ].add( year ); 
                bookingData[ index ][ slot ].add( slot ); 
                bookingData[ index ][ slot ].add( roomDB ); 
                bookingData[ index ][ slot ].add( subject ); 
                bookingData[ index ][ slot ].add( uid ); 
            }
            
            return bookingData;
        } catch ( SQLException err ) {
            errnotify( err.toString() );
        }
        
        return null;
    }
    
//    /**
//     *
//     * @param week
//     * @param slot
//     * @return
//     */
//    public boolean CheckLessonExists( byte week, byte slot )
//    {
//        ResultSet lessons = this.query( "SELECT * FROM lessons;" );
//        
//        try {
//            while ( lessons.next() ) {
//                if ( (lessons.getByte( "LessonWeek" ) == week) && (lessons.getByte( "LessonSlot" ) == slot ) ) {
//                    DebugPrint( "Week lesson already exists." );
//                    return true; 
//                }
//            }
//        } catch ( SQLException err ) {
//            System.out.println( err.getMessage() );
//        }
//        
//        return false; 
//    }
//    
//    /**
//     *
//     * @param week
//     * @param slot
//     * @param subject
//     * @param yearGroup
//     * @param room
//     */
//    public void CreateLesson( byte week, byte slot, String subject, String yearGroup, String room )
//    {
//        if ( this.CheckLessonExists( week, slot ) == false ) {
//            String q = String.format( "INSERT INTO lessons (LessonWeek, LessonSlot, LessonSubject, LessonYearGroup, LessonRoom) VALUEs ('%d', '%d', '%s', '%s', '%s');", week, slot, subject, yearGroup, room );
//            this.executeUpdate( q ); 
//        }
//    }
    
    /**
     * Gathers data from the specified table and specified column. 
     * @param tableName Table to get data from. 
     * @param columnName Column to get data from. 
     * @return String[] data from the database. 
     */
    public String[] GatherTableUtilData( String tableName, String columnName )
    {
        String q = String.format( "SELECT * FROM %s;", tableName );
        ResultSet rs = this.query( q );
        List<String> dataList = new ArrayList<String>();
        
        try {
            while ( rs.next() )
            {
                dataList.add( rs.getString( columnName ) );
            }
            
            String[] dataArray = dataList.toArray( new String[0] );
            return dataArray;
        } catch ( SQLException err ) {
            System.out.println( err );
        }
        
        return null; 
    }
    
    /**
     * Checks if the specified room exists.
     * @param room Room to check for. 
     * @return True if the room exists, false if it doesn't.
     */
    public boolean CheckRoomExists( String room )
    {
        ResultSet rooms = this.query( "SELECT * FROM rooms;" );
        
        try {
            while ( rooms.next() ) {
                if ( rooms.getString( "RoomName" ).equals( room ) ) {
                    DebugPrint( "Room already exists." );
                    return true; 
                }
            }
        } catch ( SQLException err ) {
            System.out.println( err.getMessage() );
        }
        
        return false; 
    }
    
    /**
     * Adds a room to the database. 
     * @param room Room name to add. 
     */
    public void AddRoom( String room )
    {
        if ( this.CheckRoomExists( room ) == false ) {
            String q = String.format( "INSERT INTO rooms (RoomName) VALUES ('%s');", room );
            this.executeUpdate( q ); 
        }
    }
        
    /**
     * Deletes a room from the database. 
     * @param room Room to delete. 
     */
    public void DeleteRoom( String room ) 
    {
        if ( this.CheckRoomExists( room ) == true ) {
            String q = String.format( "DELETE FROM rooms WHERE RoomName = '%s';", room );
            this.executeUpdate( q ); 
        }
    }
    
    /**
     * Checks if the given subject exists in the database. 
     * @param subject Subject name to check for. 
     * @return True if it exists, false if it doesn't. 
     */
    public boolean CheckSubjectExists( String subject )
    {
        ResultSet subjects = this.query( "SELECT * FROM subjects;" );
        
        try {
            while ( subjects.next() ) {
                if ( subjects.getString( "SubjectName" ).equals( subject ) ) {
                    DebugPrint( "Subject already exists." );
                    return true; 
                }
            }
        } catch ( SQLException err ) {
            System.out.println( err.getMessage() );
        }
        
        return false; 
    }
    
    /**
     * Adds the given subject to the database.
     * @param subject Subject name to add. 
     */
    public void AddSubject( String subject )
    {
        if ( this.CheckSubjectExists( subject ) == false ) {
            String q = String.format( "INSERT INTO subjects (SubjectName) VALUES ('%s');", subject );
            this.executeUpdate( q ); 
        }
    }
        
    /**
     * Deletes a subject from the database. 
     * @param subject Subject to delete. 
     */
    public void DeleteSubject( String subject ) 
    {
        if ( this.CheckSubjectExists( subject ) == true ) {
            String q = String.format( "DELETE FROM subjects WHERE SubjectName = '%s';", subject );
            this.executeUpdate( q ); 
        }
    }
    
    /**
     * Converts a Java Date into the format used by MySQL
     * @param date Date to convert. 
     * @return Converted date. 
     */
    public String ConvertDate( LocalDate date )
    {
        DateTimeFormatter dt = DateTimeFormatter.ofPattern( "yyyy-MM-dd" );
        
        return date.format( dt ); 
    }
    
    private byte SQLBoolean( boolean bool )
    {
        if ( bool ) {
            return 1;
        } else {
            return 0; 
        }
    }
    
    private void errnotify( String message )
    {
        Alert alert = new Alert( Alert.AlertType.ERROR ); 
        alert.setTitle( "Error" );
        alert.setContentText( message );
        alert.showAndWait(); 
    }
}