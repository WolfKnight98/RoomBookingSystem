// set up the application package 
package roombookingsystem;

// Import all needed libraries 
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * AppGUI is the main class in the Time Table application. 
 * This program allows users to book rooms. 
 * @author Dan
 */
public class AppGUI extends Application 
{

    /**
     * The main global variables that are used throughout. 
     */
    public static MySQL_Handler sql = new MySQL_Handler();
    private static HelperUtils util = new HelperUtils(); 
    private  int CURRENT_USER_ID; 
    private boolean BOOK_WINDOW_OPEN = false; 
    private boolean ADMIN_TOOLS_WINDOW_OPEN = false;
    private String CURRENT_ROOM = "";
    private LocalDate CURRENT_WEEK_DATE = null; 
    private LocalDate DATE_OF_OPEN = null; 
    
    /**
     * This function runs when the program is started. 
     * @param stage
     */
    @Override
    public void start( Stage stage ) 
    {
        // Anything we want to run before the login window pops up goes here 
        this.Login_Window( stage );
    }
    
    // The login window comes up when the application is first started.
    private void Login_Window( Stage stage )
    {
        // Set the title of the window
        stage.setTitle( "ICT Room Booker" );
        
        // We use a GridPane instead of just a Pane or manually placing objects, I found 
        // this is just easier as a GridPane also auto-adjusts. 
        //
        //  - Alignment is set to center, this way everything will have a good appearance.
        //  - The Hgap and Vgap are set to 10 to give objects a little bit of space.
        //  - A small amount of padding is added to the outside of the GridPane 
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        grid.setPadding( new Insets( 25, 25, 25, 25 ) );
        
        // Create the scene title, it acts like a header and sits at the top 
        Text scenetitle = new Text( "Welcome" );
        scenetitle.setFont( Font.font( "Tahoma", FontWeight.NORMAL, 20 ) );
        grid.add( scenetitle, 0, 0, 2, 1 );

        Label userName = new Label( "Username:" );
        grid.add( userName, 0, 1 );

        TextField userTextField = new TextField();
        grid.add( userTextField, 1, 1 );

        Label pw = new Label( "Password:" );
        grid.add( pw, 0, 2 );

        PasswordField pwBox = new PasswordField();
        grid.add( pwBox, 1, 2 );
        
        // Here we create a hyper-link that opens the create account window 
        Hyperlink getAccount = new Hyperlink();
        getAccount.setText( "Click here to register" );
        grid.add( getAccount, 0, 3, 2, 1 );
        getAccount.setOnAction( e -> {
            Stage accountStage = new Stage();
            this.CreateUserAccount( accountStage );
        });
        
        // Create the sign in button, parent the button to the HBox container
        Button btn = new Button( "Sign in" );
        HBox hbBtn = new HBox( 10 );
        hbBtn.setAlignment( Pos.BOTTOM_RIGHT );
        hbBtn.getChildren().add( btn );
        grid.add( hbBtn, 1, 4 );
        
        final Text actiontarget = new Text();
        grid.add( actiontarget, 0, 6, 2, 1 );

        // Create an event that is run when the user clicks the button 
        btn.setOnAction( e -> {
            // Get the username and password data from the textfields
            String username, password; 
            boolean valid, authorised;
            username = userTextField.getText();
            password = pwBox.getText();
            
            // Set the fill colour of the warning text
            actiontarget.setFill( Color.FIREBRICK );
            
            // Username and password verification
            if ( username.isEmpty() || password.isEmpty() ) {
                actiontarget.setText( "Username and password needed." );
            } else {
                actiontarget.setText( "Logging in ..." );
                
                // Call an SQL utility function to authenticate the username and password entered
                valid = sql.AuthenticateUser( username, password );

                // If the username and password is valid, then set the current user ID and load the timetable
                if ( valid ) {
                    CURRENT_USER_ID = sql.GetUserID( username );
                    
                    authorised = sql.IsAccountAuthorised( CURRENT_USER_ID );
                    
                    if ( authorised ) {
                        this.TimeTable( stage );
                    } else {
                        notify( "Your account has not been activated yet.", false );
                    }
                } else {
                    actiontarget.setText( "User details invalid." );
                }
            }
        } );

        Scene scene = new Scene( grid, 300, 275 );
        stage.setScene( scene );
        
        stage.show();
        stage.toFront();
    }
    
    // The main timetable function
    private void TimeTable( Stage stage )
    {
        double width, height; 

        width = 1000;
        height = 650;
        
        // Set the title of the window including the users name based on the user ID
        stage.setTitle( String.format( "ICT Room Booker - Logged in as %s", sql.GetFirstName( CURRENT_USER_ID ) ) );
        
        // Set up the grid pane
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setGridLinesVisible( false );    // DEBUG 
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        //grid.setPadding( new Insets( 25, 25, 25, 25 ) ); 
        
        // Add all 7 columns to the window, each with a size the same ( 1000 / 7 )
        // These will automatically adjust themselves depending on the width of the window
        for ( int col = 0; col < 7; col++ ) 
        {
            ColumnConstraints column = new ColumnConstraints( (width / 7) - 20 );
            grid.getColumnConstraints().addAll( column );
        }
        
        // Create the main scene for the window
        Scene scene = new Scene( grid, width, height );
        stage.setScene( scene );
        stage.setResizable( false );
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        stage.setOnCloseRequest( e -> {
            Platform.exit();
        } );
        
        // Call an SQL utility function to get all of the data from the rooms table in the database
        String[] rooms = sql.GatherTableUtilData( "rooms", "RoomName" );
        
        // Create a new combo box based on the data previously gathered
        ComboBox roomsList = util.NewComboBoxWithData( rooms );
        
        // Check to see if the global variable CURRENT_ROOM is empty and set its value
        if ( this.CURRENT_ROOM == "" ) {
            roomsList.setPromptText( rooms[0] );
            this.CURRENT_ROOM = rooms[0];
        } else {
            roomsList.setPromptText( this.CURRENT_ROOM );
        }
        
        // Set the action of the rooms combo box
        roomsList.setOnAction( e -> {
            this.CURRENT_ROOM = (String) roomsList.getValue();
            this.TimeTable( stage );
        } );
        
        // Add the combo box to the grid
        grid.add( roomsList, 0, 2, 6, 1 );
        
        // Only add the admin tools butotn if the user is an admin 
        if ( sql.IsAdmin( CURRENT_USER_ID ) ) 
        {
            // Create a new button in the top right hand corner
            Button adminTools = new Button( "Admin Tools" );
            HBox hbAdminTools = new HBox( 10 );
            hbAdminTools.setAlignment( Pos.CENTER );
            hbAdminTools.getChildren().add( adminTools );
            grid.add( hbAdminTools, 6, 0 );
            
            // Set the action of the button to open the admin tools window
            adminTools.setOnAction( e -> {
                if ( !this.ADMIN_TOOLS_WINDOW_OPEN ) {
                    this.ADMIN_TOOLS_WINDOW_OPEN = true; 
                    Stage adminStage = new Stage();
                    this.AdminTools( adminStage );
                } else {
                    notify( "The admin tools window is already open.", false ); 
                }
            } );
        }
        
        // Here we use the new Java SE 8 time library (java.time)
        LocalDate today = LocalDate.now();
        LocalDate monday, friday;
        
        // Check to see if the global variable CURRENT_WEEK_DATE is empty, if it is then 
        // set the date to the date the program is run. 
        if ( this.CURRENT_WEEK_DATE == null ) { 
            monday = util.GetMondayDate( today );
            friday = util.GetFridayDate( today );
            this.CURRENT_WEEK_DATE = monday; 
        } else {
            // Get the monday and friday date of the current week 
            monday = util.GetMondayDate( this.CURRENT_WEEK_DATE );
            friday = util.GetFridayDate( this.CURRENT_WEEK_DATE );
        }
        if ( this.DATE_OF_OPEN == null ) { this.DATE_OF_OPEN = monday; }
        
        // Display the current week date at the top center of the program 
        Label currentWeekDate = new Label( String.format( "Week of the %s.", this.CURRENT_WEEK_DATE ) );
        grid.add( currentWeekDate, 3, 1, 4, 1 );
        
        // Set up the data and button variables for the time table 
        ArrayList[][] bookingData = new ArrayList[ 5 ][ 3 ];
        Button[][] bookButtons = new Button[ 5 ][ 3 ]; 
        Button[][] deleteButtons = new Button[ 5 ][ 3 ]; 
        bookingData = sql.GetBookings( monday, friday, this.CURRENT_ROOM );
        LocalDate currentDate = this.CURRENT_WEEK_DATE;
        DayOfWeek dayName; 
        
        // Loop through a week's worth of time table slots with the data gathered from the
        // database. This whole for loop does the loading and displaying of time tabled rooms. 
        for ( int week = 0; week < 5; week++ )
        {
            // Get the day of the currentDate variable, this changes depending on the for loop's 
            // position. 
            dayName = currentDate.getDayOfWeek(); 
            
            // Create a label for the current name of the day
            HBox dayLabelContainer = new HBox( 10 );
            dayLabelContainer.setAlignment( Pos.CENTER );
            Text dayLabel = new Text( "" + util.Capitalise( dayName.toString() ) );
            dayLabel.setFont( Font.font( "Tahoma", FontWeight.NORMAL, 20 ) );
            dayLabelContainer.getChildren().add( dayLabel );
            
            // Loop through the lesson slots 
            for ( int slot = 0; slot < 3; slot++ )
            {
                // Set up the main VBox that holds all of the booking information 
                VBox vbox = new VBox();
                vbox.setAlignment( Pos.CENTER );
                vbox.setStyle( "-fx-border-color: #2e8b57; -fx-border-width: 2px; " );
                
                // Here we check to make sure the first item in the current row actually contains
                // a piece of data, if it doesn't this tells us that there isn't a booking for
                // this date.
                if ( bookingData[ week ][ slot ] != null ) 
                {
                    // Get the room name in String form 
                    String room = (String) bookingData[ week ][ slot ].get( 4 );
                    
                    // Make sure we only load bookings for the current room selected in the dropdown 
                    if ( room.equals( this.CURRENT_ROOM ) ) {
                        // Create the labels for the year group, subject and name 
                        int userid = (int) bookingData[ week ][ slot ].get( 6 );
                        Label yearLbl = new Label( "" + bookingData[ week ][ slot ].get( 2 ) );
                        Label subjLbl = new Label( "" + bookingData[ week ][ slot ].get( 5 ) );
                        Label nameLbl = new Label( "" + sql.GetBookName( userid ) );

                        // Add the labels to the VBox 
                        vbox.getChildren().add( nameLbl );
                        vbox.getChildren().add( subjLbl );
                        vbox.getChildren().add( yearLbl );
                        VBox.setMargin( nameLbl, new Insets( 5, 0, 0, 0 ) );
                        VBox.setMargin( yearLbl, new Insets( 0, 0, 5, 0 ) );

                        // Display the delete booking button if the current user booked it or is an admin
                        if ( ( sql.IsAdmin( CURRENT_USER_ID ) ) || ( (int) bookingData[ week ][ slot ].get( 6 ) ) == CURRENT_USER_ID ) {
                            deleteButtons[ week ][ slot ] = this.Admin_DeleteBooking( (byte) slot, currentDate, this.CURRENT_ROOM, stage );
                            vbox.getChildren().add( deleteButtons[ week ][ slot ] );
                        }
                    } else {
                        // Create the no booking label 
                        Label emptyLbl = new Label( "No booking." );

                        // Create a button for booking the slot 
                        bookButtons[ week ][ slot ] = this.BookingButton( (byte) (slot), currentDate, stage );

                        // Add it to the VBox
                        vbox.getChildren().add( emptyLbl );
                        vbox.getChildren().add( bookButtons[ week ][ slot ] );
                    }
                } else {
                    // Create the no booking label
                    Label emptyLbl = new Label( "No booking." );

                    // Create a button for booking the slot 
                    bookButtons[ week ][ slot ] = this.BookingButton( (byte) (slot), currentDate, stage );

                    // Add it to the VBox
                    vbox.getChildren().add( emptyLbl );
                    vbox.getChildren().add( bookButtons[ week ][ slot ] );
                }
                
                vbox.setMinHeight( 120.0 );
                
                grid.add( vbox, week + 1, slot + 8 );
            }
            
            grid.add( dayLabelContainer, week + 1, 7 );
            
            // Increase the currentDate variable by one day 
            currentDate = currentDate.plusDays( 1 );
        }
        
        // Create the main elements, including the title, usersname and welcome message  
        String lastname = sql.GetLastName( CURRENT_USER_ID );
        String title = sql.GetTitle( CURRENT_USER_ID );
        Text welcome = new Text( String.format( "Welcome, %s %s", title, lastname ) );
        welcome.setFont( Font.font( "Tahoma", FontWeight.NORMAL, 20 ) );
        grid.add( welcome, 0, 0 );
        
        // Display the current term
        Label currentTerm = new Label( String.format( "It is currently the %s term.", util.GetTerm( this.CURRENT_WEEK_DATE ) ) );
        grid.add( currentTerm, 0, 1, 4, 1 );
        
        // Create the buttons that allow the user to select the next or previous week 
        if ( !this.CURRENT_WEEK_DATE.equals( this.DATE_OF_OPEN ) ) {
            Button prevWeekButton = new Button( "Past week" );
            prevWeekButton.setOnAction( e -> {
                this.CURRENT_WEEK_DATE = this.CURRENT_WEEK_DATE.minusWeeks( 1 );
                this.TimeTable( stage );
            } );
            HBox prevWeekBtnCont = new HBox( 10 );
            prevWeekBtnCont.setAlignment( Pos.CENTER );
            prevWeekBtnCont.getChildren().addAll( prevWeekButton );
            grid.add( prevWeekBtnCont, 2, 11 );
        }
        
        Button nextWeekButton = new Button( "Next week" );
        nextWeekButton.setOnAction( e -> {
            this.CURRENT_WEEK_DATE = this.CURRENT_WEEK_DATE.plusWeeks( 1 );
            this.TimeTable( stage );
        } );
        HBox nextWeekBtnCont = new HBox( 10 );
        nextWeekBtnCont.setAlignment( Pos.CENTER );
        nextWeekBtnCont.getChildren().addAll( nextWeekButton );
        grid.add( nextWeekBtnCont, 4, 11 );
    }
    
    // This function creates a button that has a changed outcome depending on what is passed to it 
    private Button BookingButton( byte slot, LocalDate date, Stage timeTableStage )
    {
        // Create the button 
        Button btn = new Button( "Book" );
        
        // Set the action to open the booking window with variable data
        btn.setOnAction( e -> {
            if ( !this.BOOK_WINDOW_OPEN ) {
                this.BOOK_WINDOW_OPEN = true; 
                Stage bookStage = new Stage();
                this.BookWindow( bookStage, slot, date, timeTableStage );
            } else {
                notify( "The booking window is already open.", false );
            }
        } );
        
        return btn;
    }
    
    private void BookWindow( Stage stage, byte slot, LocalDate date, Stage timeTableStage ) 
    {
        double width, height;
        
        width = 250;
        height = 350;
        
        // Set yhe title 
        stage.setTitle( "Booking" );
        
        // Create the main grid handler 
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setGridLinesVisible( false );
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        grid.setPadding( new Insets( 25, 25, 25, 25 ) ); 
        
        // Set up the scene 
        Scene scene = new Scene( grid, width, height );
        stage.setScene( scene );
        stage.setResizable( false );
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        stage.setOnCloseRequest( eh -> {
            this.BOOK_WINDOW_OPEN = false; 
        });
        
        
        // Main elements 
        Text scenetitle = new Text( "Booking" );
        scenetitle.setFont( Font.font( "Tahoma", FontWeight.NORMAL, 20 ) );
        grid.add( scenetitle, 0, 0, 4, 1 );

        
        // Choose subject label for the dropdown 
        Label subject = new Label( "Choose subject" );
        grid.add( subject, 0, 2, 4, 1 );

        String[] subjects = sql.GatherTableUtilData( "subjects", "SubjectName" );
        ComboBox subjectList = util.NewComboBoxWithData( subjects );
        subjectList.setPromptText( "Select subject" );
        grid.add( subjectList, 0, 3, 4, 1 );
        
        
        // Year group label for the dropdown 
        Label form = new Label( "Year Group" );
        grid.add( form, 0, 5, 4, 1 );
        
        ObservableList<String> years = FXCollections.observableArrayList( "Year 7", "Year 8", "Year 9", "Year 10", "Year 11", "Year 12", "Year 13" ); 
        ComboBox yearList = new ComboBox( years ); 
        yearList.setPromptText( "Select year group" );
        grid.add( yearList, 0, 6, 4, 1 );
        
        Button btn = new Button( "Book" );
        HBox hbBtn = new HBox( 10 );
        hbBtn.setAlignment( Pos.BOTTOM_RIGHT );
        hbBtn.getChildren().add( btn );
        grid.add( hbBtn, 4, 10 );
        
        btn.setOnAction( e -> {            
            String yrGroup = (String) yearList.getValue(); 
            String subj = (String) subjectList.getValue();
            
            if ( sql.CheckBookingExists( date, slot, this.CURRENT_ROOM ) == false ) {
                if ( (yearList.getValue() == null) || (subjectList.getValue() == null) ) { 
                    notify( "You have some essential information missing that is required to make a booking, please check and try again.", true );
                } else {
                    sql.CreateBooking( date, yrGroup, slot, this.CURRENT_ROOM, subj, CURRENT_USER_ID );
                    notify( "Booking created.", false );
                    this.BOOK_WINDOW_OPEN = false; 
                    stage.close();
                    
                    this.TimeTable( timeTableStage );
                }
            } else {
                notify( "There is already a booking for the date and period you selected.", true );
            }
        } );
    }
    
    private void CreateUserAccount( Stage stage )
    {
        double width, height;

        width = 400;
        height = 500;
        
        // Set yhe title 
        stage.setTitle( "Create Account" );
        
        // Create the main grid handler 
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setGridLinesVisible( false );
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        grid.setPadding( new Insets( 25, 25, 25, 25 ) ); 
        
        // Set up the scene 
        Scene scene = new Scene( grid, width, height );
        stage.setScene( scene );
        stage.setResizable( false );
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        
        // Main elements 
        Text scenetitle = new Text( "Create Account" );
        scenetitle.setFont( Font.font( "Tahoma", FontWeight.NORMAL, 20 ) );
        grid.add( scenetitle, 0, 0, 4, 1 );
        
        Label firstNameLabel = new Label( "Enter forename:" );
        grid.add( firstNameLabel, 0, 2 );
        
        TextField firstNameField = new TextField();
        grid.add( firstNameField, 1, 2, 4, 1 );
        
        Label lastNameLabel = new Label( "Enter surname:" );
        grid.add( lastNameLabel, 0, 3 );
        
        TextField lastNameField = new TextField();
        grid.add( lastNameField, 1, 3, 4, 1 );
        
        Label usernameLabel = new Label( "Enter a username:" );
        grid.add( usernameLabel, 0, 4 );
        
        TextField usernameField = new TextField();
        grid.add( usernameField, 1, 4, 4, 1 );
        
        Label passwordLabel = new Label( "Enter a password:" );
        grid.add( passwordLabel, 0, 5 );
        
        PasswordField passwordField = new PasswordField();
        grid.add( passwordField, 1, 5, 4, 1 );
        
        Label pwordConfirmLabel = new Label( "Confirm password:" );
        grid.add( pwordConfirmLabel, 0, 6 );
        
        PasswordField pwordConfirmField = new PasswordField();
        grid.add( pwordConfirmField, 1, 6, 4, 1 );
        
        Label titleLabel = new Label( "Select title:" );
        grid.add( titleLabel, 0, 7 );
        
        ObservableList<String> titles = FXCollections.observableArrayList( "Mr", "Mrs", "Miss" ); 
        ComboBox titleList = new ComboBox( titles ); 
        titleList.setPromptText( "Select title" );
        grid.add( titleList, 1, 7, 4, 1 );
        
        Button submitButton = new Button( "Create account" );
        HBox submitButtonHB = new HBox( 10 );
        submitButtonHB.setAlignment( Pos.BOTTOM_RIGHT );
        submitButtonHB.getChildren().add( submitButton );
        grid.add( submitButtonHB, 1, 16, 4, 1 );
        
        submitButton.setOnAction( e -> {
            boolean missingInfo = false;  
            String title; 
            String[] userData = new String[6];
            
            userData[0] = firstNameField.getText();
            userData[1] = lastNameField.getText();
            userData[2] = usernameField.getText();
            userData[3] = passwordField.getText();
            userData[4] = pwordConfirmField.getText();
            title = (String) titleList.getValue();
            
            if ( util.StringsAreEmpty( userData ) ) {
                notify( "You have not entered any information.", true );
            } else {
                if ( util.ForenameIsValid( userData[0] ) && util.LastnameIsValid( userData[1] ) ) {
                    if ( !sql.CheckAccountExists( userData[2] ) && util.UsernameIsValid( userData[2] ) ) {
                        if ( util.PasswordIsValid( userData[3] ) && util.PasswordsMatch( userData[3], userData[4] ) ) {
                            if ( titleList.getValue() == null ) {
                                notify( "You have not selected a title.", true );
                            } else {
                                sql.CreateUserAccount( userData[2], userData[3], title, userData[0], userData[1], false );
                                notify( "Your account has been registered, an administrator will view your request and will either accept or deny it soon..", false );
                            }
                        } else if ( userData[3].isEmpty() || userData[4].isEmpty() ) {
                            notify( "Password/confirm password box is empty.", true );
                        } else {
                            notify( "Passwords do not match.", true );
                        }
                    } else if ( sql.CheckAccountExists( userData[2] ) ) {
                        notify( "An account with that username already exists.", true );
                    } else if ( userData[2].isEmpty() ) {
                        notify( "The username field is empty.", true );
                    } else {
                        notify( "Username must be between 5 and 32 characters.", true );
                    }
                } else if ( userData[0].isEmpty() || userData[1].isEmpty() ) { 
                    notify( "The forename/lastname field is empty.", true );
                } else {
                    notify( "The forename and/or lastname field contains invalid characters.", true );
                }
            }
        } );
    }
    
    private void AdminTools( Stage stage )
    {
        double width, height;

        width = 350;
        height = 250;
        
        // Set yhe title 
        stage.setTitle( "Booking" );
        
        // Create the main grid handler 
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setGridLinesVisible( false );
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        grid.setPadding( new Insets( 25, 25, 25, 25 ) ); 
        
        // Set up the scene 
        Scene scene = new Scene( grid, width, height );
        stage.setScene( scene );
        stage.setResizable( false );
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        stage.setOnCloseRequest( eh -> {
            this.ADMIN_TOOLS_WINDOW_OPEN = false; 
        });
        
        
        // Main elements 
        Text scenetitle = new Text( "Admin Tools" );
        scenetitle.setFont( Font.font( "Tahoma", FontWeight.NORMAL, 20 ) );
        grid.add( scenetitle, 0, 0, 4, 1 );
        
        
        // Add a room button
        Button addRoomBtn = new Button( "Add a room" );
        addRoomBtn.setMaxWidth( 125.0 );
        addRoomBtn.setOnAction( e -> {
            this.Admin_AddRoom( stage );
        } );
        grid.add( addRoomBtn, 0, 3 ); 
        
        
        // Delete a room button 
        Button deleteRoomBtn = new Button( "Delete a room" );
        deleteRoomBtn.setMaxWidth( 125.0 );
        deleteRoomBtn.setOnAction( e -> {
            this.Admin_DeleteRoom( stage );
        });
        grid.add( deleteRoomBtn, 0, 4 );
        
        
        // Add a subject button 
        Button addSubjectBtn = new Button( "Add a subject" );
        addSubjectBtn.setMaxWidth( 125.0 );
        addSubjectBtn.setOnAction( eh -> {
            this.Admin_AddSubject( stage );
        } );
        grid.add( addSubjectBtn, 1, 3 );
        

        // Delete a subject button
        Button deleteSubjectBtn = new Button( "Delete a subject" );
        deleteSubjectBtn.setMaxWidth( 125.0 );
        deleteSubjectBtn.setOnAction( eh -> {
            this.Admin_DeleteSubject( stage );
        } );
        grid.add( deleteSubjectBtn, 1, 4 );
        
        
        // User Management button
        Button userManagementBtn = new Button( "User management" );
        userManagementBtn.setMaxWidth( 250.0 );
        userManagementBtn.setOnAction( e -> {
            this.Admin_UserManager( stage );
        } );
        grid.add( userManagementBtn, 0, 5 );
    }
    
    private void Admin_AddRoom( Stage stage ) 
    {
        double width, height; 

        width = 200;
        height = 150;
        
        stage.setTitle( "Admin - Add Room" );
        
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setGridLinesVisible( false );    // DEBUG 
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        
        Scene scene = new Scene( grid, width, height );
        stage.setScene( scene );
        stage.setResizable( false );
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        stage.setOnCloseRequest( eh -> {
            this.ADMIN_TOOLS_WINDOW_OPEN = false; 
        });
        
        Label roomLabel = new Label( "Enter room name" );
        grid.add( roomLabel, 0, 0, 8, 1 );

        TextField roomName = new TextField(); 
        grid.add( roomName, 0, 1, 8, 1 );
        
        Button addBtn = new Button( "Add" );
        
        addBtn.setOnAction( e -> {
            String room = roomName.getText().toUpperCase();
            
            if ( ( !room.isEmpty() ) && ( sql.CheckRoomExists( room ) == false ) ) {
                if ( util.ValidString( room ) == true ) {
                    if ( util.ValidRoomName( room ) ) {
                        sql.AddRoom( room );
                        notify( "Room added.", false );
                        this.AdminTools( stage );
                    } else {
                        notify( "Room name exceeds 16 characters.", true );
                    }
                } else {
                    notify( "Only letters and numbers allowed.", true ); 
                }
            } else if ( room.isEmpty() ) {
                notify( "You didn't enter anything.", true );
            } else {
                notify( "That room name already exists, please enter a different room name.", true );
            }
            
            this.ADMIN_TOOLS_WINDOW_OPEN = false; 
        } );
        
        grid.add( addBtn, 1, 3 );
    }
    
    private void Admin_DeleteRoom( Stage stage )
    {
        double width, height; 

        width = 200;
        height = 150;
        
        stage.setTitle( "Admin - Delete Room" );
        
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setGridLinesVisible( false );    // DEBUG 
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        
        Scene scene = new Scene( grid, width, height );
        stage.setScene( scene );
        stage.setResizable( false );
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        stage.setOnCloseRequest( eh -> {
            this.ADMIN_TOOLS_WINDOW_OPEN = false; 
        });
        
        Label roomLabel = new Label( "Select room" );
        grid.add( roomLabel, 0, 0, 8, 1 );
        
        String[] rooms = sql.GatherTableUtilData( "rooms", "RoomName" );
        ComboBox roomList = util.NewComboBoxWithData( rooms );
        roomList.setPromptText( "Select room" );
        grid.add( roomList, 0, 1, 8, 1 );
        
        Button deleteBtn = new Button( "Delete" );
        
        deleteBtn.setOnAction(e -> {
            String roomName = (String) roomList.getValue(); 
            
            if ( roomName == null ) {
                notify( "You didn't select a room.", true );
            } else {
                sql.DeleteRoom( roomName );
                notify( "Room deleted.", false );
                //stage.close(); 
                this.AdminTools( stage );
            }
            
            this.ADMIN_TOOLS_WINDOW_OPEN = false; 
        });
        
        grid.add( deleteBtn, 1, 3 );
    }
    
    private void Admin_AddSubject( Stage stage ) 
    {
        double width, height; 

        width = 200;
        height = 150;
        
        stage.setTitle( "Admin - Add Subject" );
        
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setGridLinesVisible( false );
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        
        Scene scene = new Scene( grid, width, height );
        stage.setScene( scene );
        stage.setResizable( false );
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        stage.setOnCloseRequest( eh -> {
            this.ADMIN_TOOLS_WINDOW_OPEN = false; 
        });
        
        Label subjectLabel = new Label( "Enter subject name" );
        grid.add( subjectLabel, 0, 0, 8, 1 );

        TextField subjectName = new TextField(); 
        grid.add( subjectName, 0, 1, 8, 1 );
        
        Button addBtn = new Button( "Add" );
        
        addBtn.setOnAction( e -> {
            String subject = util.CapitaliseWords( subjectName.getText() );
            
            if ( ( !subject.isEmpty() ) && ( sql.CheckSubjectExists( subject ) == false ) ) {
                if ( util.ValidString( subject ) == true ) {
                    if ( util.ValidSubjectName( subject ) ) {
                        sql.AddSubject( subject );
                        notify( "Subject added.", false );
                        //stage.close();
                        this.AdminTools( stage );
                    } else {
                        notify( "Subject name exceeds 32 characters.", true );
                    }
                } else {
                    notify( "Only letters and numbers allowed.", true ); 
                }
            } else if ( subject.isEmpty() ) {
                notify( "You didn't enter anything.", true );
            } else {
                notify( "That subject name already exists, please enter a different subject name.", true );
            }
            
            this.ADMIN_TOOLS_WINDOW_OPEN = false; 
        } );
        
        grid.add( addBtn, 1, 3 );
    }
    
    private void Admin_DeleteSubject( Stage stage )
    {
        double width, height; 

        width = 200;
        height = 150;
        
        stage.setTitle( "Admin - Delete Subject" );
        
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setGridLinesVisible( false );
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        
        Scene scene = new Scene( grid, width, height );
        stage.setScene( scene );
        stage.setResizable( false );
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        stage.setOnCloseRequest( eh -> {
            this.ADMIN_TOOLS_WINDOW_OPEN = false; 
        });
        
        Label subjectLabel = new Label( "Select subject" );
        grid.add( subjectLabel, 0, 0, 8, 1 );
        
        String[] subjects = sql.GatherTableUtilData( "subjects", "SubjectName" );
        ComboBox subjectList = util.NewComboBoxWithData( subjects );
        subjectList.setPromptText( "Select subject" );
        grid.add( subjectList, 0, 1, 8, 1 );
        
        Button deleteBtn = new Button( "Delete" );
        
        deleteBtn.setOnAction(e -> {
            String subjectName = (String) subjectList.getValue(); 
            
            if ( subjectName == null ) {
                notify( "You didn't select a subject.", true );
            } else {
                sql.DeleteSubject( subjectName );
                notify( "Subject deleted.", false );
                //stage.close(); 
                this.AdminTools( stage );
            }
            
            this.ADMIN_TOOLS_WINDOW_OPEN = false; 
        });
        
        grid.add( deleteBtn, 1, 3 );
    }
    
    private void Admin_UserManager( Stage stage )
    {
        double width, height; 

        width = 900;
        height = 625;
        
        // Set the title of the window including the users name based on the user ID
        stage.setTitle( "User Management" );
        
        // Set up the grid pane
        GridPane grid = new GridPane();
        grid.setAlignment( Pos.CENTER );
        grid.setGridLinesVisible( false );
        grid.setHgap( 10 );
        grid.setVgap( 10 );
        
        // Create the main scene for the window
        Scene scene = new Scene( grid, width, height );
        stage.setScene( scene );
        stage.setResizable( false );
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        
        Text scenetitle = new Text( "User Management" );
        scenetitle.setFont( Font.font( "Tahoma", FontWeight.NORMAL, 20 ) );
        grid.add( scenetitle, 0, 0, 4, 1 );
        
        TableView table = new TableView();
        TableColumn idCol = new TableColumn( "ID" );
        TableColumn firstNameCol = new TableColumn( "First Name" );
        TableColumn lastNameCol = new TableColumn( "Last Name" );
        TableColumn userNameCol = new TableColumn( "Username" );
        TableColumn titleCol = new TableColumn( "Title" );
        TableColumn adminCol = new TableColumn( "Administrator" );
        TableColumn authCol = new TableColumn( "Authorised" );
        
        idCol.setCellValueFactory( new PropertyValueFactory<>( "userID" ) );
        firstNameCol.setCellValueFactory( new PropertyValueFactory<>( "firstName" ) );
        lastNameCol.setCellValueFactory( new PropertyValueFactory<>( "lastName" ) );
        userNameCol.setCellValueFactory( new PropertyValueFactory<>( "userName" ) );
        titleCol.setCellValueFactory( new PropertyValueFactory<>( "title" ) );
        adminCol.setCellValueFactory( new PropertyValueFactory<>( "admin" ) );
        authCol.setCellValueFactory( new PropertyValueFactory<>( "authorised" ) );
        
        ObservableList<UserRow> tableData = FXCollections.observableArrayList();
        
        String[][] users = sql.GetAllUsers(); 
        for ( String[] user : users ) {
            tableData.add( new UserRow( Integer.parseInt( user[0] ), user[1], user[2], user[3], user[4], user[5], user[6] ) );
        }
        
        table.setItems( tableData );
        table.getColumns().addAll( idCol, firstNameCol, lastNameCol, userNameCol, titleCol, adminCol, authCol );
        
        table.setRowFactory( tr -> {
            final TableRow<UserRow> row = new TableRow<>();
            final ContextMenu contextMenu = new ContextMenu();
            final MenuItem removeMenuItem = new MenuItem( "Delete user" );
            removeMenuItem.setOnAction( e -> { 
                int userid = row.getItem().GetUserID();
                
                if ( sql.IsAdmin( this.CURRENT_USER_ID ) ) 
                {
                    if ( this.CURRENT_USER_ID != userid ) {
                        sql.DeleteUserAccount( userid );
                        table.getItems().remove( row.getItem() ); 
                    } else {
                        notify( "You cannot delete your own account.", true );
                    }
                }
            } );
            
            contextMenu.getItems().add( removeMenuItem );
            row.contextMenuProperty().bind( Bindings.when( row.emptyProperty() ).then( (ContextMenu) null ).otherwise( contextMenu ) );
            return row;
        });
        
        grid.add( table, 0, 2 );
    }

    private Button Admin_DeleteBooking( byte slot, LocalDate date, String room, Stage timeTableStage )
    {
        Button btn = new Button( "Clear booking" );
        
        btn.setOnAction( e -> {
            sql.DeleteBooking( date, slot, room );            
            this.TimeTable( timeTableStage );
        } );
        
        return btn;
    }

    private void notify( String message, boolean is_error )
    {
        Alert alert = new Alert( is_error ? AlertType.ERROR : AlertType.INFORMATION ); 
        alert.setTitle( is_error ? "Error" : "Information" );
        alert.setContentText( message );
        alert.showAndWait(); 
    }
    
    /**
     *
     * @param args
     */
    public static void main( String[] args ) {
        launch( args );
    }
}