package timetableapp;

import com.sun.xml.internal.ws.util.StringUtils;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

/**
 * Offers a collection of small utilities
 * @author Dan Hook
 */
public class HelperUtils 
{
    /**
     * Checks if the given string is valid by making sure it has only letters and numbers within.
     * @param text Text to check
     * @return true if the given text only contains letters or numbers
     */
    public boolean ValidString( String text )
    {
        if ( text.isEmpty() || this.IsJustWhitespace( text ) ) { return false; }
        
        char[] chars = text.toCharArray();
        
        for ( char c : chars ) 
        { 
            if( !Character.isLetterOrDigit( c ) || Character.isWhitespace( c ) )
            {
                return false;
            }
        }
        
        return true; 
    }
    
    /**
     * Checks if a room name is valid and does not contain more than 16 characters. 
     * @param name String name to check.
     * @return True if valid.
     */
    public boolean ValidRoomName( String name )
    {
        if ( name.length() >= 16 ) {
            return false; 
        } else {
            return true; 
        }
    }
    
    /**
     * Checks if the given subject name is valid. 
     * @param subject String name to check. 
     * @return True if valid. 
     */
    public boolean ValidSubjectName( String subject )
    {
        if ( subject.length() >= 32 ) {
            return false; 
        } else {
            return true; 
        }
    }
    
    /**
     * Capitalises a word or first word in a given string. 
     * @param word String to capitalise. 
     * @return Capitalised string. 
     */
    public String Capitalise( String word ) 
    {
        word = word.toLowerCase();
        return Character.toUpperCase( word.charAt(0) ) + word.substring(1);
    }
    
    /**
     * Capitalises a word or all words in a given string. 
     * @param sentence String to capitalise. 
     * @return Capitalised string.
     */
    public String CapitaliseWords( String sentence )
    {
        sentence = sentence.toLowerCase();
        return StringUtils.capitalize( sentence );
    }
    
    /**
     * Creates a new combo box populated with the given data. 
     * @param data String[] data to be added to the combo box.
     * @return ComboBox populated with given data. 
     */
    public ComboBox NewComboBoxWithData( String[] data )
    {
        ObservableList<String> boxData = FXCollections.observableArrayList();
        for ( int x = 0; x < data.length; x++ ) { boxData.add( data[x] ); }
        ComboBox box = new ComboBox( boxData ); 
        return box; 
    }
    
    /**
     * Checks the items of a String array to find a matching phrase. 
     * @param array String array to check
     * @param phrase String phrase to look for 
     * @return True if found 
     */
    public boolean ArrayContains( String[] array, String phrase )
    {
        for ( String index : array )
        {
            if ( index.equals( phrase ) ) {
                return true; 
            }
        }
        
        return false; 
    }
    
    /**
     * Checks to see if the given String array contains valid strings.
     * @param arr String array to check.
     * @return True if valid. 
     */
    public boolean ArrayContainsValidStrings( String[] arr )
    {
        for ( String s : arr )
        {
            if ( !this.ValidString( s ) ) { 
                return false; 
            }
        }
        
        return true; 
    }
    
    /**
     * Checks the given string for whitespace. 
     * @param text String to check. 
     * @return True if all characters are whitespace.
     */
    public boolean IsJustWhitespace( String text )
    {
        char[] chars = text.toCharArray();
        
        for ( char c : chars )
        {
            if ( !Character.isWhitespace( c ) ) {
                return false;
            }
        }
        
        return true; 
    }
    
    /**
     * Checks if the given strings are empty. 
     * @param array String array to check. 
     * @return True if it is empty. 
     */
    public boolean StringsAreEmpty( String[] array )
    {
        for ( String s : array ) 
        {
            return s.isEmpty() || this.IsJustWhitespace( s );
        }
        
        return true; 
//        return ( s.length() == 0 ) || ( s.equals( "" ) );
    }
    
    /**
     * Gets the current school term based off the date created when 
     * the function is called. 
     * @param date
     * @return Autumn, Spring or Summer depending on date. 
     */
    public String GetTerm( LocalDate date )
    {
        String monthString; 
        int month; 
        SimpleDateFormat sdf = new SimpleDateFormat( "M" );

        // Date dateNow = new Date();
        // monthString = sdf.format( date );
        // month = Integer.parseInt( monthString );
        month = date.getMonthValue();
        
        if ( (month >= 9) && (month <= 12) ) {
            return "Autumn";
        } else if ( (month >= 1) && (month <= 3) ) {
            return "Spring";
        } else if ( (month >= 4) && (month <= 7) ) {
            return "Summer";
        }
        
        return "Summer Holiday"; 
    }
    
    /**
     * Checks if the given date is a Monday. 
     * @param day Date to check. 
     * @return True if it's a Monday. 
     */
    public boolean IsMonday( LocalDate day )
    {
        if ( day.getDayOfWeek() != DayOfWeek.MONDAY )
        {
            return false; 
        }
        
        return true; 
    }
    
    /**
     * Checks if the given date is a Friday. 
     * @param day Date to check. 
     * @return True if it's a Friday. 
     */
    public boolean IsFriday( LocalDate day )
    {
        if ( day.getDayOfWeek() != DayOfWeek.FRIDAY )
        {
            return false; 
        }
        
        return true; 
    }
    
    /**
     * Gets the monday date of a week based on the date given. 
     * @param today Date to get monday from. 
     * @return Monday date. 
     */
    public LocalDate GetMondayDate( LocalDate today )
    {
        if ( this.IsMonday( today ) == false ) 
        {
            while ( today.getDayOfWeek() != DayOfWeek.MONDAY )
            {
                today = today.minusDays( 1 );
            }
            
            return today;
        }
        
        return today;
    }
    
    /**
     * Gets the Friday date of a week based on the date given. 
     * @param today Date to get Friday from. 
     * @return Monday date. 
     */
    public LocalDate GetFridayDate( LocalDate today )
    {
        if ( this.IsFriday( today ) == false ) 
        {
            while ( today.getDayOfWeek() != DayOfWeek.FRIDAY )
            {
                if ( (today.getDayOfWeek() == DayOfWeek.SATURDAY) || (today.getDayOfWeek() == DayOfWeek.SUNDAY) ) {
                    today = today.minusDays( 1 );
                } else {
                    today = today.plusDays( 1 );
                }
            }
            
            return today;
        }
        
        return today;
    }
    
    public boolean UsernameIsValid( String username )
    {
        if ( !username.isEmpty() && username.length() <= 32 && this.ValidString( username ) ) 
        {
            return true; 
        } 
        
        return false; 
    }
}