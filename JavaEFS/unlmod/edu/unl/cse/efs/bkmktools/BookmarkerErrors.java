package edu.unl.cse.efs.bkmktools;

import java.io.FileNotFoundException;

import edu.umd.cs.guitar.exception.GException;
import edu.unl.cse.efs.tools.ErrorTraceConformance;

public class BookmarkerErrors {

	private static final String usageString = 
			"Usage: java -jar bkmk.jar \n"
			+ "\t-testcase -e <efg_file> <test_case_1> [<test_case_2>...]";
	/**
     * Print an error message to the console, according to the nature of the throwable provided, and exit the system. 
     * This method never returns.
     * 
     * Preconditions: 	none
     * Postconditions: 	The system runtime has exited in an error state.
     * 					A String identifying the exception of type e has been printed to the terminal. 
     * @param e
     */
    public static void errorOut(Throwable e)
    {
    	if(e != null && e instanceof IllegalArgumentException) {
    		System.err.println("ERROR: Invalid arguments: " + e.getMessage());
    		System.err.println("\n" + usageString);
    		System.exit(1);
    	}
    	else {
    		if(e instanceof GException) { 
    			String message = "Guitar Exception: " + e.getClass().getSimpleName();
    			if(e.getMessage() != null && !e.getMessage().isEmpty())
    				message += ", Message: " + e.getMessage();
    			System.err.println(message);
    		}
    		else if(e instanceof FileNotFoundException) {
    			System.err.println("File specified:\n" + e.getMessage() + "\ncould not be located on the file system.");
    		}
    		else if(e != null) {
    			System.err.println(e.getClass().getSimpleName() + ", Message: " + e.getMessage());
    			System.err.println(ErrorTraceConformance.someOfStackTrace(e, 10));
    		}
    		else
    			System.err.print((String)null);
    		System.err.println(" Now Exiting...");
    		System.exit(1);
    	}
    }
    
    
}
