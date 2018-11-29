package edu.unl.cse.efs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCApplication2;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.Configuration;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PartialConfiguration;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.ripper.JFCRipperConfiguration;
import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.LauncherData;
import edu.unl.cse.efs.app.EventFlowSlicer;
import edu.unl.cse.efs.java.JavaLaunchApplication;
import edu.unl.cse.efs.ripper.JFCRipperConfigurationEFS;
import edu.unl.cse.efs.tools.PathConformance;
import edu.unl.cse.efs.view.EventFlowSlicerView;

import static edu.unl.cse.efs.util.ReadArguments.ConfigProperty.*;
import static edu.unl.cse.efs.view.EventFlowSlicerErrors.*;
public class ReadArguments
{

	public static final String DEFAULT_CONFIGFILENAME = "epreferences.xml";
	public static java.io.File currentConfigIODirectory;
	public static String currentConfigIOFilename = DEFAULT_CONFIGFILENAME;
	public static enum ConfigProperty {
		all,
		runchoice,
		app,
		resdir,
		cfile,
		aafile,
		vafile,
		custclass,
		ripconfile,
		gfile,
		efile,
		tcdir,
		repconfile,
		algorithm,
		rmichoice,
		tcsel,
		opendelay,
		iwaitTime,
		usejar,
	}
	static ConfigProperty[] ripperConfigTypes = new ConfigProperty[]{
			app, opendelay, resdir, aafile, vafile, cfile, ripconfile};



	public static JFCRipperConfigurationEFS ripperConfiguration(String[] args, ApplicationData ad)
	{
		JFCRipperConfigurationEFS configuration = new JFCRipperConfigurationEFS();
		CmdLineParser parser = new CmdLineParser(configuration);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			throw new IllegalArgumentException(e);
		}
		String filepath;
		// check application
		if (JFCRipperConfigurationEFS.CMD_LINE_ARGS.isEmpty())
			throw new IllegalArgumentException(
					"Application file was not provided as an argument to the ripper arguments");
		filepath = JFCRipperConfigurationEFS.CMD_LINE_ARGS.get(0);
		ad.setAppFilePath(filepath);
		if (JavaLaunchApplication.launchesJar(filepath))
			JFCRipperConfigurationEFS.USE_JAR = true;
		if (JFCRipperConfigurationEFS.MAIN_CLASS.isEmpty() && !filepath.isEmpty())
			JFCRipperConfigurationEFS.MAIN_CLASS = ad.getAppFile().getAbsolutePath();

		if (JFCRipperConfigurationEFS.CUSTOM_MAIN_CLASS.isEmpty()) {
			JFCRipperConfigurationEFS.LONG_PATH_TO_APP = JFCRipperConfigurationEFS.MAIN_CLASS;
			if (!JFCRipperConfigurationEFS.USE_JAR)
				JFCRipperConfigurationEFS.MAIN_CLASS = PathConformance.parseApplicationName(filepath);
		} else {
			JFCRipperConfigurationEFS.LONG_PATH_TO_APP = JFCRipperConfigurationEFS.MAIN_CLASS;
			JFCRipperConfigurationEFS.MAIN_CLASS = JFCRipperConfigurationEFS.CUSTOM_MAIN_CLASS;
		}

		// Edit URL List
		String pathPath = PathConformance.parseApplicationPath(filepath);
		if(!pathPath.isEmpty()) {
			if(JFCRipperConfigurationEFS.URL_LIST.isEmpty()) // there is a path to append.
				JFCRipperConfigurationEFS.URL_LIST = pathPath;
			else
				JFCRipperConfigurationEFS.URL_LIST += GUITARConstants.CMD_ARGUMENT_SEPARATOR + pathPath;
		}
		if(JFCRipperConfigurationEFS.USE_JAR) {
			if(JFCRipperConfigurationEFS.URL_LIST.isEmpty()) // there is a path to append.
				JFCRipperConfigurationEFS.URL_LIST = JFCRipperConfigurationEFS.LONG_PATH_TO_APP;
			else
				JFCRipperConfigurationEFS.URL_LIST += GUITARConstants.CMD_ARGUMENT_SEPARATOR + JFCRipperConfigurationEFS.LONG_PATH_TO_APP;
		}
		filepath = JFCRipperConfigurationEFS.APP_ARGS_FILE;
		// arguments app file
		ad.setArgumentsAppFile(filepath);
		if (ad.hasArgumentsAppFile()) {
			if (!ad.argumentsAppFileExists())
				throw new IllegalArgumentException(
						"Application Arguments file provided does not exist on the file system.\n" + "\'" + filepath + "\'");
			else {
				String colonArgs = ReadArguments.colonDelimAppArgumentsFrom(ad.getArgumentsAppFile().getAbsolutePath());
				JFCRipperConfigurationEFS.ARGUMENT_LIST = colonArgs;
			}
		}
		filepath = JFCRipperConfigurationEFS.VM_ARGS_FILE;

		// arguments vm file
		ad.setArgumentsVMFile(filepath);
		if (ad.hasArgumentsVMFile()) {
			if (!ad.argumentsVMFileExists())
				throw new IllegalArgumentException(
						"Java VM Arguments file provided does not exist on the file system.\n" + "\'" + filepath + "\'");
		}
		EventFlowSlicer.initRipperVMChanges();
		if (!ripperAppExists())
			throw new IllegalArgumentException(
					"Application file provided:\n\'" + JFCRipperConfigurationEFS.LONG_PATH_TO_APP + "\'\n" + "cannot be loaded from the file system.");

		// results directory
		filepath = JFCRipperConfigurationEFS.RESULTS_DIRECTORY;
		File parent = new File(filepath).getAbsoluteFile().getParentFile();
		ad.setOutputDirectory(filepath);
		if (filepath.isEmpty() || filepath == null)
			throw new IllegalArgumentException("An output directory was not provided via the -resdir argument");
		else if (!parent.exists())
			throw new IllegalArgumentException("Output directory provided\n" + ad.getOutputDirectory().getPath() + "\n"
					+ "does not exist on the file system.");

		// get output files

		ad.setDefaultWorkingGUIFile();
		JFCRipperConfigurationEFS.GUI_FILE = ad.getWorkingGUIFile().getAbsolutePath();
		ad.setDefaultWorkingEFGFile();
		JFCRipperConfiguration.LOG_FILE = ad.getOutputDirectory().getAbsolutePath() + File.separator + "Rip.log";

		// main class
		ad.setCustomMainClass(JFCRipperConfigurationEFS.CUSTOM_MAIN_CLASS);

		// check constraints.
		try {
			filepath = JFCRipperConfigurationEFS.RULES_FILE;
			ad.setWorkingTaskListFile(filepath);
			if (!ad.workingTaskListFileExists())
				throw new IllegalArgumentException(
						"Constraints tasklist file provided does not exist on the file system\n" + "\'" + filepath
								+ "\'");

			// check if this file contains TaskList data
			JAXBContext context = JAXBContext.newInstance(TaskList.class);
			Unmarshaller um = context.createUnmarshaller();
			Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingTaskListFile()));
			if (!(myFile instanceof TaskList))
				throw new IllegalArgumentException(
						"File provided to -constfile parameter is a " + myFile.getClass() + " file.");
		} catch (JAXBException e) {
			throw new IllegalArgumentException("Invalid argument passed to -constfile parameter:\n"
					+ "Errors in XML syntax or structure.\n" + e.getLinkedException().getMessage());
		}



		// check configuration file.
		filepath = JFCRipperConfigurationEFS.CONFIG_FILE;
		ad.setRipConfigurationFile(filepath);
		if (ad.hasRipConfigurationFile()) {
			if (!ad.ripConfigurationFileExists())
				throw new IllegalArgumentException(
						"Ripper Configuration file provided does not exist on the file system.\n" + "\'" + filepath
								+ "\'");
			// check if this file contains Configuration data
			try {
				JAXBContext context = JAXBContext.newInstance(Configuration.class);
				Unmarshaller um = context.createUnmarshaller();
				Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getRipConfigurationFile()));
				if (!(myFile instanceof Configuration))
					throw new IllegalArgumentException(
							"File provided to -ripcon parameter is a " + myFile.getClass() + " file.");
			} catch (JAXBException e) {
				throw new IllegalArgumentException("Invalid argument passed to -constfile parameter:\n"
						+ "Errors in XML syntax or structure.\n" + e.getLinkedException().getMessage());
			}
		}

		// properly and robustly inherit features from the input arguments, and overwrite the initial waiting time.
		if(JFCRipperConfigurationEFS.APPLICATION_OPEN_DELAY < 0)
			JFCRipperConfigurationEFS.INITIAL_WAITING_TIME = ApplicationData.openWaitTime;
		else {
			JFCRipperConfigurationEFS.INITIAL_WAITING_TIME = JFCRipperConfigurationEFS.APPLICATION_OPEN_DELAY;
			ApplicationData.openWaitTime = JFCRipperConfigurationEFS.INITIAL_WAITING_TIME;
		}
		return configuration;
	}
	public static String[] asRipperArgsArray(ApplicationData ad, LauncherData ld)
	{
		/*
		 * Example:
		 * -rip
		 * -normi
		 * /Users/jsaddle/Desktop/ResearchFileSystems/AppRun/jedit/jedit_src_class/jedit.jar
		 * -args /Users/jsaddle/Desktop/ResearchFileSystems/JEditInput/jedit_appl_Sort.txt
		 * -vm /Users/jsaddle/Desktop/ResearchFileSystems/JEditInput/jedit_vm.txt
		 * -resdir /Users/jsaddle/Desktop/ResearchFileSystems/JEditResults
		 * -constfile /Users/jsaddle/Desktop/ResearchFileSystems/JEditSaved/Rules/EFSnew/JEditLS/Rules_JEditLS_Tim_done
		 * -ripcon /Users/jsaddle/Desktop/ResearchFileSystems/JEditSaved/RipConfigs/EFSnew/JEditLS/ConfigurationJEditLS
		 */
		ArrayList<String> args = new ArrayList<String>();

		for(ConfigProperty type : ripperConfigTypes) {
			switch(type) {
			case app		:
				args.add(ad.getAppFile().getAbsolutePath());
				break;
			case opendelay  :
				args.addAll(Arrays.asList(new String[]{"-delay", ""+ApplicationData.openWaitTime}));
				break;
			case resdir		:
				args.addAll(Arrays.asList(new String[]{"-resdir", ad.getOutputDirectory().getAbsolutePath()}));
			case aafile		:
				if(ad.hasArgumentsAppFile())
					args.addAll(Arrays.asList(new String[]{"-args", ad.getArgumentsAppFile().getAbsolutePath()}));
				break;
			case vafile		:
				if(ad.hasArgumentsVMFile())
					args.addAll(Arrays.asList(new String[]{"-vm", ad.getArgumentsVMFile().getAbsolutePath()}));
				break;
			case cfile		:
				args.addAll(Arrays.asList(new String[]{"-constfile", ad.getWorkingTaskListFile().getAbsolutePath()}));
				break;
			case ripconfile	:
				if(ad.hasRipConfigurationFile())
					args.addAll(Arrays.asList(new String[]{"-ripcon", ad.getRipConfigurationFile().getAbsolutePath()}));
	break;	default	: {}
			}
		}
		String[] toReturn = args.toArray(new String[0]);
		return toReturn;
	}
	private static ObjectFactory fact = new ObjectFactory();
	private static PartialConfiguration pc;

	public static boolean configurationIsSet()
	{
		return pc != null;
	}
	public static void loadNewConfiguration()
	{
		pc = fact.createPartialConfiguration();
	}
	public static void setConfigurationIOFile(String directory, String filename)
    {
    	if(directory != null)
    		currentConfigIODirectory = new File(directory);
    	if(filename != null && !filename.isEmpty())
    		currentConfigIOFilename = filename;
    }

	public static void setupPreferences(String prefDirectory, String prefFilename, boolean loadNow)
			throws JAXBException, IllegalArgumentException
	{
		if(loadNow){
			try {
				loadConfiguration(new File(prefDirectory), prefFilename);
			}
			catch(FileNotFoundException e) {
				System.out.println("Preferences file was not present on file system. Continuing...");
				loadNewConfiguration();
			}
			catch(JAXBException e) {
				System.out.println("Preferences file provided could not be processed due to errors in XML syntax or structure. Continuing...");
				loadNewConfiguration();
			}
			catch(IllegalArgumentException e) {
				System.out.println(e.getLocalizedMessage());
				loadNewConfiguration();
			}
		}
	}
	public static void loadConfiguration(File configIODirectory, String configIOFilename) throws JAXBException, IllegalArgumentException, FileNotFoundException
	{
		try {
			XMLHandler handler = new XMLHandler();
			File loadFile = new File(configIODirectory, configIOFilename);
			if(!loadFile.exists())
				throw new FileNotFoundException("Preferences File.");
			boolean foundContent = false;
			try {
				List<String> lines = Files.readAllLines(loadFile.toPath());
				for(int i = 0; i < 2 && !foundContent; i++)
					if(!lines.get(i).trim().isEmpty())
						foundContent = true;
			} catch(IOException e) {
				// jump to statement below.
			}
			if(!foundContent)
				throw new IllegalArgumentException("Preferences file provided to preferences parameter contains no content.");
			Object prefs = handler.readObjFromFileThrowExceptions(loadFile, PartialConfiguration.class);
			if (!(prefs instanceof PartialConfiguration)) {
				pc = fact.createPartialConfiguration();
				throw new IllegalArgumentException("File provided to preferences parameter is a " + prefs.getClass() + " file.");
			}
			pc = (PartialConfiguration)prefs;
		}
		catch(JAXBException e) {
			pc = fact.createPartialConfiguration();
			throw e;
		}
	}

	/**
	 * Handle all the checking necessary to store a preferences file.
	 * If the directory does not exist, use the current directory.
	 * If the filename does not exist, use it.
	 * If the directory specified does not exist, print that we cannot write the preferences file.
	 */
	public static void storeFile()
	{
		XMLHandler handler = new XMLHandler();
		if(currentConfigIODirectory == null
		|| currentConfigIODirectory.getName().isEmpty())
			currentConfigIODirectory = new File(System.getProperty("user.dir"));

		if(currentConfigIOFilename == null
		|| currentConfigIOFilename.isEmpty())
			currentConfigIOFilename = DEFAULT_CONFIGFILENAME;

		if(!currentConfigIODirectory.exists())
			System.out.println("Preferences output directory not found on file system. Cannot write preferences");

		File loadFile = new File(currentConfigIODirectory, currentConfigIOFilename);

		if(loadFile.exists() && !loadFile.canWrite())
			System.out.println("EventFlowSlicer does not have permission to write preferences to \n" + loadFile.getAbsolutePath());

		try {
			handler.writeObjToFileThrowExceptions(pc, loadFile.getAbsolutePath());
		}
		catch(FileNotFoundException e) {
			System.out.println("Could not write preferences file to file system. \nDirectory specified:\n" + currentConfigIODirectory + "\ncould not be found");
		}
		catch(JAXBException e) {
			System.out.println("Preferences file could not be written to disk due to errors in XML syntax or structure.\n\n" + e.getLinkedException().getLocalizedMessage());
		}
		catch(IOException e) {
			System.out.println("IOException: " + e.getLocalizedMessage());
		}
	}

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
	public static void setPreferencesFromView(ConfigProperty... whichP)
	{
		if(EventFlowSlicerView.vars != null) { // if the view has been opened previously.
			if(whichP.length == 1 && whichP[0] == ConfigProperty.all)
				whichP = ConfigProperty.values();

			for(ConfigProperty p : whichP) {
				switch(p) {
				case app 		:
					if(!EventFlowSlicerView.vars.appFile().isEmpty())
						pc.setApp(EventFlowSlicerView.vars.appFile());
					else
						pc.setApp(null);
					break;
				case resdir		:
					if(!EventFlowSlicerView.vars.outputDirectory().isEmpty())
						pc.setResDir(EventFlowSlicerView.vars.outputDirectory());
					else
						pc.setResDir(null);
					break;
				case cfile		:
					if(!EventFlowSlicerView.vars.constraintsFile().isEmpty())
						pc.setCFile(EventFlowSlicerView.vars.constraintsFile());
					else
						pc.setCFile(null);
					break;
				case aafile		:
					if(EventFlowSlicerView.vars.argsAppFile().isEmpty())
						pc.setAAFile(EventFlowSlicerView.vars.argsAppFile());
					else
						pc.setAAFile(null);
					break;
				case vafile		:
					if(EventFlowSlicerView.vars.argsVMFile().isEmpty())
						pc.setVAFile(EventFlowSlicerView.vars.argsVMFile());
					else
						pc.setVAFile(null);
					break;
				case custclass	:
					if(EventFlowSlicerView.vars.customMainClass().isEmpty())
						pc.setCustClass(EventFlowSlicerView.vars.customMainClass());
					else
						pc.setCustClass(null);
					break;
				case ripconfile	:
					if(EventFlowSlicerView.vars.ripConfigurationFile().isEmpty())
						pc.setRipconFile(EventFlowSlicerView.vars.ripConfigurationFile());
					else
						pc.setRipconFile(null);
					break;
				case gfile		:
					if(EventFlowSlicerView.vars.guiStructureFile().isEmpty())
						pc.setGFile(EventFlowSlicerView.vars.guiStructureFile());
					else
						pc.setGFile(null);
					break;
				case efile		:
					if(EventFlowSlicerView.vars.eventFlowGraphFile().isEmpty())
						pc.setEFile(EventFlowSlicerView.vars.eventFlowGraphFile());
					else
						pc.setEFile(null);
					break;
				case tcdir		:
					if(EventFlowSlicerView.vars.testCaseDirectory().isEmpty())
						pc.setTCDir(EventFlowSlicerView.vars.testCaseDirectory());
					else
						pc.setTCDir(null);
					break;
				case tcsel 		:
					if(EventFlowSlicerView.vars.testCaseSelection().isEmpty())
						pc.setTCSel(EventFlowSlicerView.vars.testCaseSelection());
					else
						pc.setTCSel(null);
					break;
				case algorithm	:
					pc.setAlgorithm(LauncherData.GenType.RSECWO.name()); break; // hard coded
				default			: break;
	//			case repconfile : pc.setRepconFile(theString);
				}
			}
		}
	}

	/**
	 * Read preferences from the loaded prefrences file of this ReadArguments object
	 *
	 * If a preference is not found, do nothing
	 */
	public static void searchPreferences(ApplicationData ad, LauncherData ld, ConfigProperty... preferences)
	{
		// if first parameter is "all" iterate through them all
		if(preferences.length == 1 && preferences[0] == ConfigProperty.all)
			preferences = ConfigProperty.values();

		for(ConfigProperty s : preferences)
			switch(s) {
				case app		:
					if(pc.getApp() != null && !pc.getApp().isEmpty())
						ad.setAppFilePath(pc.getAAFile());
					break;
				case resdir		:
					if(pc.getResDir() != null && !pc.getResDir().isEmpty())
						ad.setOutputDirectory(pc.getResDir());
					break;
				case cfile		:
					if(pc.getCFile() != null && !pc.getCFile().isEmpty())
						ad.setWorkingTaskListFile(pc.getCFile());
					break;
				case aafile		:
					if(pc.getAAFile() != null && !pc.getAAFile().isEmpty())
						ad.setArgumentsAppFile(pc.getAAFile());
					break;
				case vafile		:
					if(pc.getVAFile() != null && !pc.getVAFile().isEmpty())
						ad.setArgumentsVMFile(pc.getVAFile());
					break;
				case custclass	:
					if(pc.getCustClass() != null && !pc.getCustClass().isEmpty())
						ad.setCustomMainClass(pc.getCustClass());
					break;
				case ripconfile	:
					if(pc.getRipconFile() != null && !pc.getRipconFile().isEmpty())
						ad.setRipConfigurationFile(pc.getRipconFile());
					break;
				case gfile		:
					if(pc.getGFile() != null && !pc.getGFile().isEmpty())
						ad.setWorkingGUIFile(pc.getGFile());
					break;
				case efile		:
					if(pc.getEFile() != null && !pc.getEFile().isEmpty())
						ad.setWorkingEFGFile(pc.getEFile());
					break;
				case tcdir		:
					if(pc.getTCDir() != null && !pc.getTCDir().isEmpty())
						ad.setWorkingTestCaseDirectory(pc.getTCDir()); break;
				case tcsel		:
					if(pc.getTCSel() != null && !pc.getTCSel().isEmpty())
						ld.setLaunchSelectionArguments(pc.getTCSel());
					break;
//				case repconfile : // not handled.
//					ad.setReplayConfigurationFile(pc.getRepconFile()); break;
//				case algorithm	: { // not handled.
//					GenType ret;
//					try {
//						ret = GenType.valueOf(pc.getAlgorithm());
//						if(ret == null)
//							break;
//						if(ret == GenType.NOCHOICE)
//							break;
//					} catch(Exception e) {
//						break;
//					}
//					ld.setGeneratorRunningType(ret);
//				}
//				break;
				case rmichoice 	: {
					String theChoice = pc.getRMIChoice();
					if(theChoice == null)
						break;
					String[] split = theChoice.split(":");
					if(split.length == 2) {
						if(split[0].equals("RMIOn"))
							ld.setSendtoRMIPort(split[1]);
						else if(split[0].equals("RMISendbackOn"))
							ld.setSendbackRMIPort(split[1]);
						else
							ld.unsetRMI();
					}
				}
				break;
				default:
			}
	}

	public static boolean ripperAppExists() {
		try {
			String[] URLs;
			if (!JFCRipperConfigurationEFS.URL_LIST.isEmpty())
				URLs = JFCRipperConfigurationEFS.URL_LIST.split(GUITARConstants.CMD_ARGUMENT_SEPARATOR);
			else
				URLs = new String[0];
			URLs = JFCApplication2.convertToURLStrings(URLs);
			new JFCApplication2(JFCRipperConfigurationEFS.MAIN_CLASS, JFCRipperConfigurationEFS.LONG_PATH_TO_APP, JFCRipperConfigurationEFS.USE_JAR, URLs);
		} catch (Exception e) {
			System.err.println("ERROR: Class Not Found on File System:\n" + e.getMessage());
			return false;
		}
		return true;
	}

}
