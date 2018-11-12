package edu.unl.cse.efs.bkmktools;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.unl.cse.efs.tools.PathConformance;
import edu.unl.cse.efs.tools.WildcardFiles;
import edu.unl.cse.efs.tools.StringTools;

public class Bookmarker {
	
	public static boolean tstBookmark, efgBookmark, reverse;
	public static File inputEFGFile, inputGUIFile;
	public static File outputDirectory;
	public static File[] inputTSTFiles;
	public static File[] inputEFGFiles;
	
	public static void main(String[] args)
	{
		tstBookmark = efgBookmark = false;
		reverse = false;
		outputDirectory = null;
		startup(args);
	}
	
	public static String[] parseArguments(String[] args)
	{
		ArrayList<String> allArgs = new ArrayList<String>(Arrays.asList(args));
		Iterator<String> argsIt = allArgs.iterator();
		while(argsIt.hasNext()) {
			String target = argsIt.next();
			if(target.equals("-testcase")) {
				tstBookmark = true;
				argsIt.remove();
			}
			else if(target.equals("-event")) {
				efgBookmark = true;
				argsIt.remove();
			}
			else if(target.equals("-reverse")) {
				reverse = true;
			}
		}
		
		return allArgs.toArray(new String[0]);
	}
	public static void startup(String[] args)
	{
		try {
			String[] leftoverArgs = parseArguments(args);
			if(tstBookmark) {
				tstBookmarkingArgs(leftoverArgs);
				for(File theTST : inputTSTFiles) {
					if(!reverse) 
						TSTBookmarking.doBookmark(outputDirectory.getPath() + File.separator, inputEFGFile.getAbsolutePath(), theTST.getAbsolutePath());
					else 
						TSTBookmarking.doUnBookmark(outputDirectory.getPath() + File.separator, theTST.getAbsolutePath());
				}
			}
			if(efgBookmark) {
				efgBookmarkingArgs(leftoverArgs);
				EFG builtEFG;
				String newFileDir = outputDirectory.getAbsolutePath() + File.separator;
				for(File theEFG : inputEFGFiles) {
					if(!reverse) {
						EFGBookmarking marker = new EFGBookmarking(theEFG.getAbsolutePath(), inputGUIFile.getAbsolutePath());
						builtEFG = marker.getBookmarked(true);
					}
					else {
						EFGBookmarking.EFGUnBookmarking unmarker = new EFGBookmarking.EFGUnBookmarking(theEFG.getAbsolutePath());
						builtEFG = unmarker.getUnBookmarked();
					}
					String base = PathConformance.parseApplicationName(theEFG.getAbsolutePath()) + "_BKMK.EFG";
					writeEFG(newFileDir + base, builtEFG);
					System.out.println("Done.");
				}
			}
			
		}
		catch(FileNotFoundException | IllegalArgumentException e) {
			BookmarkerErrors.errorOut(e);
		}
	}
	
	public static void writeEFG(String filename, EFG theEFG)
	{
		XMLHandler handler = new XMLHandler();
		System.out.println("Writing file to \n\"" + filename + "\"");
		handler.writeObjToFile(theEFG, filename);
	}
	public static String[] efgBookmarkingArgs(String[] args) throws FileNotFoundException
	{
		ArrayList<String> allArgs;
		ArrayList<File> level2Args;
		
		allArgs = new ArrayList<String>(Arrays.asList(args));
		level2Args = new ArrayList<File>();
		Iterator<String> argsIt = allArgs.iterator();
		boolean eFound, gFound, oFound;
		eFound = gFound = oFound = false;
		
		String target;
		
		while(argsIt.hasNext()) {
			target = argsIt.next();
			if(target.equals("-g")) {
				if(gFound) 
					throw new IllegalArgumentException("-g parameter cannot be defined twice.");
				try {
					argsIt.remove();
					String filename = argsIt.next();
					argsIt.remove();
					File newFile = new File(filename);
					if(!newFile.exists()) throw new FileNotFoundException(filename);
					inputGUIFile = newFile;
					gFound = true;
				} catch(NoSuchElementException e) {
						throw new IllegalArgumentException("Invalid argument passed to -g parameter:\n"
								+ "Argument passed to -g must be a valid GUI model file.");
				}
			}
			else if(target.equals("-o")) {
				if(oFound)
					throw new IllegalArgumentException("-o parameter cannot be defined twice.");
				try {
					argsIt.remove();
					String filename = argsIt.next();
					argsIt.remove();
					File newFile = new File(filename);
					if(!newFile.exists()) throw new FileNotFoundException("Directory :\"" + filename + "\"");
					if(!newFile.isDirectory()) throw new IllegalArgumentException();
					outputDirectory = newFile;
					oFound = true;
				}
				catch(NoSuchElementException | IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid argument passed to -o parameter.\n"
							+ "Argument passed to -o must be a valid output directory.");
				}
			}
			else {// any other argument is an EFG file argument.
				String filename = target;
				argsIt.remove();
				if(filename.contains("*")) {
					if(StringTools.charactersIn(filename, '*') >= 2) 
						throw new IllegalArgumentException("Invalid wildcard argument passed to the program.\n"
								+ "Input argument containing multiple asterisks is invalid.");
					int starPos = filename.indexOf('*');
					String prePosString = filename.substring(0, starPos);
					String postPosString = filename.substring(starPos+1);
					File currentFile = null;
					try {
						JAXBContext context = JAXBContext.newInstance(EFG.class);
						ArrayList<File> retrieved = new ArrayList<File>(WildcardFiles.findFiles(prePosString, postPosString));
						for(File iF : retrieved) {
							currentFile = iF;
							// test to see if the file is a valid TST	
				    		if(!iF.exists()) 
				    			continue; // skip invalid files
				    		else if(iF.isDirectory())
				    			continue; // skip directories found.
				    		Unmarshaller um = context.createUnmarshaller();
				    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(iF));
				    		if(!(myFile instanceof EFG)) 
				    			continue;
				    		// passed all the tests, add the file.
				    		level2Args.add(iF);
						}
						if(!level2Args.isEmpty())
							eFound = true;
					}
					catch(IOException e) {
						throw new IllegalArgumentException("An access violation occurred while parsing input file arguments:\n" 
								+ e.getMessage());
					}
					catch(JAXBException e) {
						String fileString =  currentFile != null ? currentFile.getName() : ""; 
						throw new IllegalArgumentException("An XML parsing violation occurred while parsing input file arguments:\n" 
								+ fileString + ": " + e.getLinkedException().getMessage());
					}
				}
				else try {
					File newFile = new File(filename);
					
					// check if the file is valid and contains data
					if(!newFile.exists()) throw new FileNotFoundException(filename);
					else if(newFile.isDirectory()) throw new IllegalArgumentException();
					
					// check if this file contains EFG data
					JAXBContext context = JAXBContext.newInstance(EFG.class);
					Unmarshaller um = context.createUnmarshaller();
		    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(newFile));
		    		if(!(myFile instanceof EFG)) throw new IllegalArgumentException();
		    		// add the file. 
		    		level2Args.add(newFile);
		    		eFound = true;
				}
				catch(JAXBException e) {
					throw new IllegalArgumentException("An XML parsing violation occurred while parsing input file arguments:\n" 
							+ e.getMessage());
				}
				
				
				
			}
		}
		if(!eFound) throw new IllegalArgumentException("Event flow graph parameter(s) were not specified.");
		if(!gFound) throw new IllegalArgumentException("-g parameter was not specified.");
		
		inputEFGFiles = level2Args.toArray(new File[0]);
		
		if(outputDirectory == null)
			outputDirectory = new File(System.getProperty("user.dir"));
		return allArgs.toArray(new String[0]);
	}
	
	public static String[] tstBookmarkingArgs(String[] args) throws FileNotFoundException
	{
		
		ArrayList<String> allArgs = new ArrayList<String>(Arrays.asList(args));
		ArrayList<File> level2Args = new ArrayList<File>();
		Iterator<String> argsIt = allArgs.iterator();
		boolean efgFound, tst1Found, oFound;
		efgFound = tst1Found = oFound = false;
		while(argsIt.hasNext()) {
			String target = argsIt.next();
			// efg file
			if(target.equals("-e")) {
				if(efgFound) 
					throw new IllegalArgumentException("-e parameter cannot be defined twice.");
				try {
					argsIt.remove();
					File newFile = new File(argsIt.next());
					argsIt.remove();
					if(!newFile.exists()) throw new FileNotFoundException("Directory: \"" + newFile.getAbsolutePath() + "\"");
					inputEFGFile = newFile;
					efgFound = true;
				} catch(NoSuchElementException e) {
						throw new IllegalArgumentException("Invalid argument passed to -e parameter:\n"
								+ "First argument must be a valid EFG model file");
				}
			}
			else if(target.equals("-o")) {
				if(oFound)
					throw new IllegalArgumentException("-o parameter cannot be defined twice.");
				try {
					argsIt.remove();
					String filename = argsIt.next();
					argsIt.remove();
					File newFile = new File(filename);
					if(!newFile.exists()) throw new FileNotFoundException(filename);
					if(!newFile.isDirectory()) throw new IllegalArgumentException();
					outputDirectory = newFile;
					oFound = true;
				}
				catch(NoSuchElementException | IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid argument passed to -o parameter.\n"
							+ "Argument passed to -o must be a valid output directory.");
				}
			}
			else { // any other argument is a TST file argument.
				String filename = target;
				argsIt.remove();
				if(filename.contains("*")) {
					if(StringTools.charactersIn(filename, '*') >= 2) 
						throw new IllegalArgumentException("Invalid wildcard argument passed to the program.\n"
								+ "Input argument containing multiple asterisks is invalid.");
					int starPos = filename.indexOf('*');
					String prePosString = filename.substring(0, starPos);
					String postPosString = filename.substring(starPos+1);
					File currentFile = null;
					try {
						JAXBContext context = JAXBContext.newInstance(TestCase.class);
						Unmarshaller um = context.createUnmarshaller();
						ArrayList<File> retrieved = new ArrayList<File>(WildcardFiles.findFiles(prePosString, postPosString));
						for(File iF : retrieved) {
							currentFile = iF;
							// test to see if the file is a valid TST
							if(!iF.exists()) 
				    			continue; // skip invalid files
				    		else if(iF.isDirectory())
				    			continue; // skip directories found.
				    		
				    		Object myFile;
				    		try { myFile = JAXBIntrospector.getValue(um.unmarshal(iF));} 
				    		catch(JAXBException e) {
				    			continue;  // this in particular is not a problem, just an invalid file
				    		}
				    		if(!(myFile instanceof TestCase)) 
				    			continue;
				    		// passed all the tests, add the file.
				    		level2Args.add(iF);
						}
						if(!level2Args.isEmpty())
							tst1Found = true;
						else {
							System.err.println("WARNING: Arguments specified by " + filename + " contained no valid input TestCase files");
						}
					}
					catch(IOException e) {
						throw new IllegalArgumentException("An access violation occurred while parsing input file arguments:\n" 
								+ e.getMessage());
					}
					catch(JAXBException e) {
						String fileString =  currentFile != null ? currentFile.getName() : ""; 
						throw new IllegalArgumentException("An XML parsing violation occurred while parsing input file arguments:\n" 
								+ fileString + ": " + e.getLinkedException().getMessage());
					}
				}
				else 
					try {
						File newFile = new File(filename);
						// check if the file is valid and contains data
						if(!newFile.exists()) throw new FileNotFoundException(filename);
						else if(newFile.isDirectory()) throw new IllegalArgumentException();
						
						// check if this file contains EFG data
						JAXBContext context = JAXBContext.newInstance(EFG.class);
						Unmarshaller um = context.createUnmarshaller();
			    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(newFile));
			    		if(!(myFile instanceof EFG)) throw new IllegalArgumentException();
			    		// add the file. 
			    		level2Args.add(newFile);
			    		tst1Found = true;
					}
					catch(JAXBException e) {
						throw new IllegalArgumentException("An XML parsing violation occurred while parsing input file arguments:\n" 
							+ e.getMessage());
					}		
					catch(IllegalArgumentException e) {
						System.err.println("Argument specified by " + filename + " is not a valid input TST file.");
					}
				
			}
		}
		inputTSTFiles = level2Args.toArray(new File[0]);
		
		if(!efgFound)
			throw new IllegalArgumentException("Please specify event flow graph parameter with -e flag.");
		if(!tst1Found)
			throw new IllegalArgumentException("Please specify at least one tst parameter to be bookmarked.");
		if(outputDirectory == null)
			outputDirectory = new File(System.getProperty("user.dir"));
		
		return allArgs.toArray(new String[0]);
	}
}
