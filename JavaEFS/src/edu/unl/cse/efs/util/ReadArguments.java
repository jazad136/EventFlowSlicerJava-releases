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
package edu.unl.cse.efs.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static edu.unl.cse.efs.view.EventFlowSlicerErrors.*;
public class ReadArguments {
	
	public static String colonDelimAppArgumentsFrom(String argumentsFile)
	{
		String[] args = readAppArguments(argumentsFile);
		if(args.length == 0)
			return "";
		
		String toReturn = args[0];
		for(int i = 1; i < args.length; i++) 
			toReturn += ":" + args[i];
		return toReturn;
	}
	
	public static String colonDelimVMArgumentsFrom(String argumentsFile)
	{
		String[] args = readVMArguments(argumentsFile);
		if(args.length == 0)
			return "";
		
		String toReturn = args[0];
		for(int i = 1; i < args.length; i++) 
			toReturn += ":" + args[i];
		return toReturn;
	}
	/**
	 * Read application arguments from a file, and handle any exceptions properly.
	 * @return
	 */
	public static String[] readAppArguments(String argumentsFile)
	{
		String[] argumentVector = new String[0];
		if(!argumentsFile.isEmpty()) {
			String appArgs = "";
			try (Scanner reader = new Scanner(new FileInputStream(argumentsFile)))
			{
				appArgs = reader.nextLine();
			}
			catch(FileNotFoundException e) {
				errorOut(new FileNotFoundException("ERROR: Application arguments file provided cannot be found."));
			}
			catch(NoSuchElementException e) {
				errorOut(new PreferencesProblem(ArgType.VMARGS, "No Lines were found in the Application arguments file provided."));
			}
			catch(IllegalStateException e) {
				errorOut(new IllegalStateException("ERROR: Could not access file system to open Application arguments file.", e));	
			}
			argumentVector = argumentsVector(appArgs);
		}
		return argumentVector;
	}
	
	/**
	 * Take a space-delimited string of arguments and parses it into a String vector,
	 * each cell containing one member of the arguments string. If an encoded space is
	 * found it, the two strings surrounding it remain joined as one member.  
	 * @return
	 */
	public static String[] argumentsVector(String spaceDelimArgs)
	{
		ArrayList<String> argsVector = new ArrayList<String>();
		int lastVStart = 0;
		char[] asChars = spaceDelimArgs.toCharArray();
		for(int i = 0; i < asChars.length; i++) {
			if(asChars[i] == ' ') {
				if(i != 0 && asChars[i-1] == '\\') 
					continue;
				else {
					argsVector.add(spaceDelimArgs.substring(lastVStart, i));
					lastVStart = i+1;
				}
			}
		}
		
		if(!spaceDelimArgs.substring(lastVStart).isEmpty())
			argsVector.add(spaceDelimArgs.substring(lastVStart));
		return argsVector.toArray(new String[0]);
	}
	
	/**
	 * Read virtual machine arguments from a file, and handle any exceptions properly.
	 * @return
	 */
	public static String[] readVMArguments(String vmArgumentsFile)
	{
		String[] argumentVector = new String[0];
		if(!vmArgumentsFile.isEmpty()) {
			String appArgs = "";
			try (Scanner reader = new Scanner(new FileInputStream(vmArgumentsFile)))
			{
				appArgs = reader.nextLine();
			}
			catch(FileNotFoundException e) {
				errorOut(new FileNotFoundException("Virtual Machine arguments file provided cannot be found."));
			}
			catch(NoSuchElementException e) {
				errorOut(new PreferencesProblem(ArgType.APPARGS, "No Lines were provided in the Virtual Machine arguments file provided."));
			}
			catch(IllegalStateException e) {
				errorOut(new IllegalStateException("ERROR: Could not access file system to open Virtual Machine arguments file.", e));
			}
			argumentVector = argumentsVector(appArgs);
		}
		return argumentVector;
	}
}
