/*******************************************************************************
 *    Copyright (c) 2018 Jonathan A. Saddler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    Contributors:
 *     Jonathan A. Saddler - initial API and implementation
 *******************************************************************************/
package edu.unl.cse.efs.view;

import java.io.FileNotFoundException;

import org.kohsuke.args4j.CmdLineException;

import edu.umd.cs.guitar.exception.GException;


public class EventFlowSlicerErrors {
	public static enum ArgType {APPLICATION, APPARGS, VMARGS, RESULTS};
	private static final String usageString = 
			"Usage: java -jar efsjava.jar"
			+ "\n\t-capture <application_file> -resdir <output_directory> "
			+ "[ -args <arguments_string_file> ] [ -vm <virtual_machine_arguments_string_file> ]"
			+ "\n\t-constraints -constfile <constraints_file>"
			+ ""
			+ "\n\t-rip <application_file> -resdir <output_directory> -constfile <consraints_file>  "
			+ "[ -args <arguments_string_file> ] [ -vm <java_virtual_machine_arguments_string_file> ] [ -cmc <special_main_class> ] [ -ripcon <rip_configuration_file> ]"
			+ "\n\t-generate <application_file> -g <input_gui_structure_file> <-e <input_GUITAR_efg_file> | -eb <input_human_readable_efg_file> > -constfile <consraints_file> "
			+ "[ -args <arguments_string_file> ] [ -vm <java_virtual_machine_arguments_string_file> ] [ -cmc <special_main_class> ] [ -rt <generator_algorithm> ]"
			+ "\n\t(-replay | -rpy_sel <test_case_string>> <application_file>) -g <output_gui_structure_file> -e <input__efg_file> -tcdir <input_test_case_directory> [ -args <arguments_string_file> ] [ -vm <java_virtual_machine_arguments_string_file> ] [ -cmc <special_main_class> ] [ -noressubdir ]"
			+ "\n\t(-gui [gui_args...])";
	
	/*
	 * java -jar efsjava.jar -capture [-args <arguments_string_file>] [-vm <-constfile>]

       java -jar efsjava.jar -constraints -constfile <constraints_file>

       java  -jar  efsjava.jar  -rip  -c <application_file> -g <output_gui_structure_file> -constfile <consraints_file> [ -args <argu-
       ments_string_file> ] [ -vm <java_virtual_machine_arguments_string_file> ] [ -cmc <special_main_class> ] [ -ripcon <rip_configu-
       ration_file> ]

       java   -jar   efsjava.jar   -generate  <application_file>  -g  <input_gui_structure_file>  <-e  <input_GUITAR_efg_file>  |  -eb
       <input_human_readable_efg_file>  >  -constfile  <consraints_file>  [  -args  <arguments_string_file>   ]   [   -vm   <java_vir-
       tual_machine_arguments_string_file> ] [ -cmc <special_main_class> ] [ -rt <generator_algorithm> ]

       java   -jar   efsjava.jar   <-replay  |  -rpy_sel  <test_case_string>>  <application_file>  -g  <output_gui_structure_file>  -e
       <input__efg_file> -tcdir <input_test_case_directory>  [  -args  <arguments_string_file>  ]  [  -vm  <java_virtual_machine_argu-
       ments_string_file> ] [ -cmc <special_main_class> ] [ -noressubdir ]
	 */
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
    	if((e != null && e instanceof IllegalArgumentException)
    	|| (e.getCause() != null && e.getCause() instanceof CmdLineException)) {
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
    		else if(e instanceof FileNotFoundException)  
    			System.err.println("File specified:\n" + e.getMessage() + "\ncould not be located on the file system.");
    		
    		else if(e != null) {
    			System.err.println(e.getClass().getSimpleName() + ", Message: " + e.getMessage());
    			System.err.println(someOfStackTrace(e, 10));
    		}
    		else
    			System.err.print((String)null);
    		System.err.println(" Now Exiting...");
    		System.exit(1);
    	}
    }
    
    public static String someOfStackTrace(Throwable e, int lines)
    {
    	String toReturn = "";
    	StackTraceElement[] st = e.getStackTrace();
    	String last = "";
    	int dupLines = 0;
    	for(int i = st.length-1; i >= 0 && i > st.length-lines; i--) {
    		if(last.equals(st[i].toString())) {
    			lines++;
    			dupLines++;
    		}
    		else {
    			if(dupLines > 0)
    				toReturn += dupLines + " more.\n";
    			last = st[i].toString();
    			
    			toReturn += st[i] + "\n";
    			dupLines = 0;
    		}
    	}
    	return toReturn;
    }
    
    /**
     * Source for the PreferencesProblem class. The preferences problem is a CogTool-Helper exception thrown
     * when there is a problem with variables specified in the preferences file. The problem is always related
     * to one of the arguments passed to a specific preference in the preferences file being invald.  
     * @author jsaddle
     */
    public static class PreferencesProblem extends Exception {
    	
    	public final ArgType prefName;
    	public String expected;
    	
    	public PreferencesProblem(ArgType prefName)
    	{
    		this.prefName = prefName;
    	}
    	
    	public PreferencesProblem(ArgType prefName, String expected)
    	{
    		this.prefName = prefName;
    		this.expected = expected;
    	}
    	
    	public String getLocalizedMessage()
    	{
    		String toReturn = "";
    		switch(prefName) {
			case APPLICATION:break;
			case RESULTS:break;
			case VMARGS:break;
			default:break;
    		}
    		return toReturn;
    	}
    }
}
