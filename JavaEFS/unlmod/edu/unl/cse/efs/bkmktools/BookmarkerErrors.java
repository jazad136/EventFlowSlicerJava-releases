/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
