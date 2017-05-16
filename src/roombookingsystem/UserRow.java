package roombookingsystem;

import javafx.beans.property.SimpleStringProperty;

public class UserRow 
{
    private final int USER_ID;
    private final SimpleStringProperty userID, firstName, lastName, userName, title, admin, authorised; 
    
    public UserRow( int id, String firstName, String lastName, String userName, String title, String admin, String authorised )
    {
        this.USER_ID = id; 
        this.userID = new SimpleStringProperty( Integer.toString( id ) ); 
        this.firstName = new SimpleStringProperty( firstName );
        this.lastName = new SimpleStringProperty( lastName );
        this.userName = new SimpleStringProperty( userName );
        this.title = new SimpleStringProperty( title );
        this.admin = new SimpleStringProperty( SQLBooleanToJava( admin ) ? "Yes" : "No" );
        this.authorised = new SimpleStringProperty( SQLBooleanToJava( authorised ) ? "Yes" : "No" );
    }
    
    public String getUserID() { return userID.get(); }
    public String getFirstName() { return firstName.get(); }
    public String getLastName() { return lastName.get(); }
    public String getUserName() { return userName.get(); }
    public String getTitle() { return title.get(); }
    public String getAdmin() { return admin.get(); }
    public String getAuthorised() { return authorised.get(); }
    
    
    public int GetUserID()
    {
        return this.USER_ID;
    }
    
    private boolean SQLBooleanToJava( String bool )
    {
        if ( bool.equals( "1" ) ) {
            return true; 
        } else {
            return false; 
        }
    }
}
