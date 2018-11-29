package edu.unl.cse.efs.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.umd.cs.guitar.graph.converter.EFG2GraphvizFixString;
import edu.umd.cs.guitar.graph.converter.TST2GraphvizFixString;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.unl.cse.bmktools.Bookmarker;
import edu.unl.cse.bmktools.EFGBookmarking;
import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.guitarplugin.EFSEFGConverter;
import edu.unl.cse.efs.guitarplugin.GUIStructure2EFGConverterEFS;
import edu.unl.cse.efs.tools.PathConformance;
import edu.unl.cse.efs.view.EventFlowSlicerErrors;
import edu.unl.cse.guitarext.GUITARDataFiles;
import edu.unl.cse.guitarext.GUITARDataFiles.WrongFileTypeException;

public class VisToolchain {
	private static GUIStructure2EFGConverterEFS guic;
	private static GUIStructure guis;
	private static EFGBookmarking bkmk;
	private static EFG eventFlow, bkmkEventFlow;
	private static List<TestCase> sequences;
	private static String newFileBase = "";
	private static XMLHandler handler = new XMLHandler();
	private static ApplicationData ad;

	// not going to handle bookmarked EFGs
	private static boolean termEFG, termTST, termTXT;

	public static boolean SIMPLIFY = false;

	public static void main(String[] args)
	{
		guic = new GUIStructure2EFGConverterEFS();
		CmdLineParser parser = new CmdLineParser(guic);
		try {
			parser.parseArgument(args);
			part1();
			part2(eventFlow, guis, newFileBase);
			part3(bkmkEventFlow, newFileBase);
		}
		catch(CmdLineException e) {
			System.err.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}

	public static void part3T(TestCase testCase, String newFileBase)
	{
		StringBuffer result = TST2GraphvizFixString.toGraphviz(testCase);
		if(newFileBase.isEmpty())
			newFileBase = guic.OUTPUT_DIRECTORY + File.separator;

		String OUT_DOT_FILE = newFileBase +
				PathConformance.parseApplicationName(guic.GUI_FILE) + "_BKMK.DOT";
		try {
			System.out.println("\n\nWriting DOT file to:\n\t" + OUT_DOT_FILE);
			// Create file
			FileWriter fstream = new FileWriter(OUT_DOT_FILE);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(result.toString());
			// Close the output stream
			out.close();
			System.out.println("DONE");
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}
	/**
	 * Takes an EFG, constructs a DOT file.
	 * @param eventFlow
	 * @param newFileBase
	 */
	public static void part3(EFG eventFlow, String newFileBase)
	{
		StringBuffer result = EFG2GraphvizFixString.toGraphviz(eventFlow);
		if(newFileBase.isEmpty())
			newFileBase = guic.OUTPUT_DIRECTORY + File.separator;

		String OUT_DOT_FILE = newFileBase +
				PathConformance.parseApplicationName(guic.GUI_FILE) + "_BKMK.DOT";
		try {
			System.out.println("\n\nWriting DOT file to:\n\t" + OUT_DOT_FILE);
			// Create file
			FileWriter fstream = new FileWriter(OUT_DOT_FILE);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(result.toString());
			// Close the output stream
			out.close();
			System.out.println("DONE");
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}
	/**
	 * Takes a raw EFG, constructs a bookmarked EFG
	 * @param eventFlow
	 * @param guis
	 * @param newFileBase
	 */
	public static void part2A(EFG eventFlow, GUIStructure guis, String newFileBase)
	{
		bkmk = new EFGBookmarking(eventFlow, guis);
		String outFile = newFileBase + PathConformance.parseApplicationName(guic.GUI_FILE) + "_BKMK.EFG";
		bkmkEventFlow = bkmk.getBookmarked(true);
		try {
			System.out.println("\n\nWriting bookmarked EFG file to:\n\t" + outFile);
			handler.writeObjToFile(bkmkEventFlow, outFile);
			System.out.println("DONE");
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}
	public static void part2(EFG eventFlow, GUIStructure guis, String newFileBase)
	{
		bkmk = new EFGBookmarking(eventFlow, guis);
		String outFile = newFileBase + PathConformance.parseApplicationName(guic.GUI_FILE) + "_BKMK.EFG";
		bkmkEventFlow = bkmk.getBookmarked(true);
		try {
			System.out.println("\n\nWriting bookmarked EFG file to:\n\t" + outFile);
			handler.writeObjToFile(bkmkEventFlow, outFile);
			System.out.println("DONE");
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
			System.exit(0);
		}
	}
	/**
	 * Takes a GUIStructure, constructs a raw EFG.
	 * @param guic
	 */
	public static EFG part1A(GUIStructure guis)
	{
		EFG toReturn = null;
		try {
			EFSEFGConverter plugin = new EFSEFGConverter();
			if(SIMPLIFY)
				plugin.handleHoverEvents = false;
			toReturn = plugin.generate(guis);
			if(ad.hasWorkingGUIFile() && !ad.hasOutputDirectory()) {
				// make EFG file string if it is not defined
				newFileBase = ad.getOutputDirectory() + File.separator;
				ad.setWorkingGUIFile(newFileBase + PathConformance.parseApplicationName(ad.getWorkingGUIFile().getAbsolutePath()) + ".EFG");
			}
			handler.writeObjToFile(eventFlow, ad.getWorkingGUIFile().getAbsolutePath());

		} catch (Exception e) {
			EventFlowSlicerErrors.errorOut(e);
		}

		String bars = "===========================================";
		System.out.println(bars);
		System.out.println("\tInput GUI: \t" + guic.GUI_FILE);
		System.out.println("\tOutput EFG: \t" + guic.EFG_FILE);
		System.out.println(bars);
		return toReturn;
	}

	public static ArrayList<String> exhaustHiLevArgs(String[] argsArr)
	{
		ArrayList<String> args = new ArrayList<String>(Arrays.asList(argsArr));
		boolean g2eFound, g2tFound, g2xFound, e2tFound, e2xFound, t2xFound;
		g2eFound = g2tFound = g2xFound = e2tFound = e2xFound = t2xFound = false;
		Iterator<String> argsIt = args.iterator();
		String target;
		while(argsIt.hasNext()) {
			target = argsIt.next();
		}
		return args;
	}
	private static ArrayList<String> exhaustArgs(String[] argsArr)
	{
		ArrayList<String> args = new ArrayList<String>(Arrays.asList(argsArr));
		boolean gFound, eFound, mFound, oFound;
		gFound = eFound = mFound = oFound = false;
		Iterator<String> argsIt = args.iterator();
		String target;
		String filepath;
		while(argsIt.hasNext()) {
			target = argsIt.next();
			switch(target) {
			case "-o" : {
				argsIt.remove();
				if(oFound)
					throw new ArgsDoubleDef(target);
				oFound = true;
				try {
					filepath = argsIt.next();
					argsIt.remove();
					handleInputFile(target, filepath);
				} catch(NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to " + target + " parameter:\n"
							+ "Expected an existing location on the file system to store all output of the current operation.");
				}
			} break;
			case "-g" : {
				argsIt.remove();
				if(gFound)
					throw new ArgsDoubleDef(target);
				gFound = true;
				try {
					filepath = argsIt.next();
					argsIt.remove();
					handleInputFile(target, filepath);
				}
				catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to " + target + " parameter:\n"
							+ "Expected a path to a GUI Structure file used for test case generation.");
				}
			} break;
			case "-e" : {
				argsIt.remove();
				if(eFound)
					throw new ArgsDoubleDef(target);
				eFound = true;
				try {
					filepath = argsIt.next();
					argsIt.remove();
					handleInputFile(target, filepath);
				}
				catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to " + target + " parameter:\n"
							+ "Expected a path to an Event Flow Graph file used for test case generation.");
				}
			} break;
			case "-m" : {
				argsIt.remove();
				if(mFound)
					throw new ArgsDoubleDef(target);
				mFound = true;
				try {
					filepath = argsIt.next();
					argsIt.remove();
					handleInputFile(target, filepath);
				}
				catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to " + target + " parameter:\n"
							+ "Expected a path to a Test Case file outputted from the test case generation procedure.");
				}
			} break;

			}

		}
		return args;
	}

	private static void handleInputFile(String paramFlag, String filepath)
	{
		try {
			switch(paramFlag) {
			case "-g"  : {
				ad.setWorkingGUIFile(filepath);
				if (!ad.workingGUIFileExists())
					throw new IllegalArgumentException("GUI Structure file provided to \'" + paramFlag + "\' parameter"
							+ "\n" + "\'" + filepath + "\'\n"
							+ "does not exist on the file system.");
				guis = GUITARDataFiles.getGUIFile(ad.getWorkingGUIFile());
			} break;
			case "-e"  : {
				ad.setWorkingEFGFile(filepath);
				if (!ad.workingEFGFileExists())
					throw new IllegalArgumentException("EFG file provided to \'" + paramFlag + "\' parameter" +
							"\n" + "\'" + filepath + "\'\n"
							+ "does not exist on the file system.");
				eventFlow = GUITARDataFiles.getEFGFile(ad.getWorkingEFGFile());
			} break;
			case "-o"  : {
				ad.setOutputDirectory(filepath);
				if(!ad.outputDirectoryExists()) {
					throw new IllegalArgumentException("Output directory provided to \'" + paramFlag + "\' parameter" +
							"\n" + "\'" + filepath + "\'\n"
							+ "is not a directory on the file system.");
				}
			} break;
			case "-m"  : {
				File target = new File(filepath);
				if (!ApplicationData.standardizedFileExists(target))
					throw new IllegalArgumentException("TestCase file provided to \'" + paramFlag + "\' parameter" +
							"\n" + "\'" + filepath + "\'\n"
							+ "does not exist on the file system.");
				sequences.add(GUITARDataFiles.getTestCaseFile(target));
			} break;

			}
		}
		catch (JAXBException e) {
			throw new IllegalArgumentException("Invalid argument passed to " + paramFlag + " parameter:\n"
					+ "File provided could not be processed due to errors in XML syntax or structure.\n"
					+ e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage());
		}
		catch (WrongFileTypeException e) {
			throw new IllegalArgumentException("Invalid argument passed to " + paramFlag + " parameter:\n"
					+ "File provided is a " + e.read + " file, but the argument expects a " + e.expected + " file");
		}
	}
	public static void part1()
	{

		try {
			EFSEFGConverter plugin = new EFSEFGConverter();

			if(guic.SIMPLIFY)
				plugin.handleHoverEvents = false;
			guis = (GUIStructure) handler.readObjFromFile(guic.GUI_FILE, GUIStructure.class);
			eventFlow = plugin.generate(guis);
			if(guic.EFG_FILE.isEmpty() && !guic.OUTPUT_DIRECTORY.isEmpty()) {
				// make EFG file string if it is not defined
				newFileBase = guic.OUTPUT_DIRECTORY + File.separator;
				guic.EFG_FILE = newFileBase + PathConformance.parseApplicationName(guic.GUI_FILE) + ".EFG";
			}
			handler.writeObjToFile(eventFlow, guic.EFG_FILE);

		} catch (Exception e) {
			EventFlowSlicerErrors.errorOut(e);
		}

		String bars = "===========================================";
		System.out.println(bars);
		System.out.println("\tInput GUI: \t" + guic.GUI_FILE);
		System.out.println("\tOutput EFG: \t" + guic.EFG_FILE);
		System.out.println(bars);
	}
//	private static class UnexpectedMissing extends RuntimeException
//	{
//		public UnexpectedMissing()
//		{
//
//		}
//	}
	private static class ArgsDoubleDef extends RuntimeException
	{
		public final String target;
		public ArgsDoubleDef(String repeatedParam)
		{
			target = repeatedParam;
		}
		public String getMessage() { return "" + target + " cannot be defined twice."; }
	}
}
