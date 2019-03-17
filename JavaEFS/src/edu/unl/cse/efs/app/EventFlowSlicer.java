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
package edu.unl.cse.efs.app;

import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.swing.JFrame;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCApplication2;
import edu.umd.cs.guitar.model.data.Configuration;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.ripper.JFCRipperConfiguration;
import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.LauncherData;
import edu.unl.cse.efs.LauncherData.GenType;
import edu.unl.cse.efs.commun.giveevents.NetCommunication;
import edu.unl.cse.efs.java.JavaCaptureTaskList;
import edu.unl.cse.efs.java.JavaLaunchApplication;
import edu.unl.cse.efs.replay.JFCReplayerConfigurationEFS;
import edu.unl.cse.efs.ripper.JFCRipperConfigurationEFS;
import edu.unl.cse.efs.tools.ArrayTools;
import edu.unl.cse.efs.tools.PathConformance;
import edu.unl.cse.efs.util.ReadArguments;
import edu.unl.cse.efs.view.EventFlowSlicerController;
import edu.unl.cse.efs.view.EventFlowSlicerErrors;
import edu.unl.cse.efs.view.EventFlowSlicerView;

public class EventFlowSlicer {
	private static SecondaryLoop waitLoop;
	private static LauncherData ld;
	private static ApplicationData ad;
	private static HashMap<String, String> poppedSystemProperties = new HashMap<String, String>();
	@SuppressWarnings("unused")
	private static boolean doGUI, doRip, doCapture, doGenerate, doConstraints, doBookmark, doReplay;
	public static final String DEFAULT_JAVA_INVOKE_STRING = System.getProperty("java.home")
			+ System.getProperty("file.separator") + "bin" + System.getProperty("file.separator") + "java";
	public static final String DEFAULT_JAVA_RMI_PORT = "1099";
	public static final String APP_INVOKE_STRING = getRunLocation();
	public static final PrintStream originalOut = System.out;
	public static final PrintStream originalErr = System.err;

	public static String getRunLocation() {
		try {
			String location = EventFlowSlicer.class.getProtectionDomain().getCodeSource()
					.getLocation().toURI().getPath();
			// location = System.getProperty("user.dir");
			return location;
		} catch (URISyntaxException uris) {
			return "";
		}
	}

	private static void reset() {
		ad = new ApplicationData();
		ld = new LauncherData(DEFAULT_JAVA_RMI_PORT);
		doGUI = doCapture = doConstraints = doRip = doGenerate = doBookmark = doReplay = false;
	}

	public static void main(String[] args) {
		reset();
		try {
			args = exhaustMainArgs(args);
			EventFlowSlicerController efc = new EventFlowSlicerController(ad);
			if (doGUI) {
				guiArgs(args);
				System.out.println("Opening Graphical Interface");

				JFrame frame = EventFlowSlicerView.setupFrame(EventFlowSlicerView.FRAME_BASE_SIZE_X,
						EventFlowSlicerView.FRAME_BASE_SIZE_Y);
				new EventFlowSlicerView.SetupView(frame, ad, ld);
				EventFlowSlicerView.show(frame);

				// update the GUI.
				if (ad.hasAppFile())
					EventFlowSlicerView.updateAppFile();
				if (ad.hasOutputDirectory())
					EventFlowSlicerView.updateOutputDirectory();
				if (ad.hasCustomMainClass())
					EventFlowSlicerView.updateCustomMainClass();
				if (ad.hasArgumentsAppFile())
					EventFlowSlicerView.updateArgsAppFile();
				if (ad.hasArgumentsVMFile())
					EventFlowSlicerView.updateArgsVMFile();
				if (ad.hasWorkingTaskListFile())
					EventFlowSlicerView.updateConstraintsFile();
				if (ad.hasWorkingGUIFile())
					EventFlowSlicerView.updateGUIFile();
				if (ad.hasWorkingEFGFile())
					EventFlowSlicerView.updateEFGFile();
				if (ad.hasWorkingTestCaseDirectory())
					EventFlowSlicerView.updateTestCaseDirectory();
				if (ad.hasRipConfigurationFile())
					EventFlowSlicerView.updateRipConfigurationFile();
				if (ld.hasLaunchSelectionArguments())
					EventFlowSlicerView.updateReplayTestCases();

			} else if (doCapture) {
				System.out.println("Doing capture");
				captureArgs(args);
				efc.startCapture(ld);
			} else if (doRip) {
				System.out.println("Doing rip");
				JFCRipperConfigurationEFS config = rippingArgs(args);
				if (!ld.sendsToRMI()) {
					efc.startRip(config);

					System.out.println(efc.modifyWorkingTasklistAfterRip());
					System.out.println("Done ripping.");
					ad.setDefaultWorkingTaskListFile();
					System.out.println("Writing TaskList file to...\n" + ad.getWorkingTaskListFile().getPath());
					efc.writeTaskListFile();
					System.out.println("Done.");
					System.exit(0); // we're done running the program. Exit now.
				} else {
					waitLoop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
					efc.ripOutsideVM(waitLoop);
					waitLoop.enter();
					System.out.println("EventFlowSlicer: Attempting to read rip TaskList...");
					String message = efc.setRipTasklist();
					System.out.println(message);
				}
			} else if (doConstraints) {
				System.out.println("Doing constraints");
				constraintsArgs(args, efc);
				// move the working constraints file to where it needs to be for
				// editing.
				ad.setDefaultWorkingTaskListFile();
				efc.writeTaskListFile();

				efc.startFitting();
			} else if (doGenerate) {
				System.out.println("Doing generate");
				generateArgs(args, efc);
				// the default running type is the original used in the thesis.
				if (ld.getGeneratorRunningType() == LauncherData.GenType.NOCHOICE)
					ld.setGeneratorRunningType(LauncherData.GenType.RSECWO);
				if (!ad.getOutputDirectory().exists())
					if (!ad.getOutputDirectory().mkdirs())
						throw new IllegalArgumentException("Generation output directory could not be created.");

				efc.bookmarkEFG(); // prepare the EFG
				efc.relabelConstraintsWidgets(); // prepare the constraints
				efc.setupGeneratorLogFile();
				efc.startGeneratingTestCases(ld);
			} else if (doReplay) {
				System.out.println("Doing replay");
				replayArgs(args, efc);
				efc.bookmarkEFG();
				efc.startReplay(ld);
				try {
					efc.waitForReplayerFinish();
				} catch (InterruptedException e) {
					System.out.println("Replayer task was interrupted before completion.");
				}
				System.out.println("Done");
				System.exit(0);
			}
		} catch (Exception e) {
			EventFlowSlicerErrors.errorOut(e);
		}
	}

	private static void generateArgs(String[] args, EventFlowSlicerController efc) {
		ArrayList<String> remaining = new ArrayList<String>();
		remaining.addAll(Arrays.asList(args));
		Iterator<String> argsIt = remaining.iterator();
		String target;
		boolean tFound, oFound, eFound, ebFound, rtFound, gFound;
		tFound = oFound = eFound = ebFound = rtFound = gFound = false;
		while (argsIt.hasNext()) {
			target = argsIt.next().toLowerCase();
			switch (target) {
			case "-resdir": {
				if (oFound)
					throw new IllegalArgumentException("-resdir parameter cannot be defined twice.");
				oFound = true;
				argsIt.remove();
				try {
					String filepath = argsIt.next();
					argsIt.remove();
					ad.setOutputDirectory(filepath);
					File parent = new File(ad.getOutputDirectoryProvided());
					if (filepath.isEmpty() || !parent.exists())
						throw new IllegalArgumentException("Output directory provided\n" + "\'" + filepath + "\'\n"
								+ "does not exist on the file system.");
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to -resdir parameter:\n"
							+ "Expected a path to a folder where results from generation can be stored.");
				}
			}
				break;
			case "-g": {
				if (gFound)
					throw new IllegalArgumentException("-g parameter cannot be defined twice.");
				gFound = true;
				argsIt.remove();
				try {
					String filepath = argsIt.next();
					argsIt.remove();
					ad.setWorkingGUIFile(filepath);
					if (!ad.workingGUIFileExists())
						throw new IllegalArgumentException("GUI Structure file provided\n" + "\'" + filepath + "\'\n"
								+ "does not exist on the file system.");

					// check if this file contains GUIStructure data
					JAXBContext context = JAXBContext.newInstance(GUIStructure.class);
					Unmarshaller um = context.createUnmarshaller();
					Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingGUIFile()));
					if (!(myFile instanceof GUIStructure))
						throw new IllegalArgumentException(
								"File provided is a " + myFile.getClass().getSimpleName() + " file");
					efc.setWorkingGUIStructure((GUIStructure) myFile);
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("Invalid argument passed to -g parameter:\n"
							+ "Expected a path to a GUI Structure file used for test case generation.");
				} catch (JAXBException e) {
					throw new IllegalArgumentException("Invalid argument passed to -g parameter:\n"
							+ "GUI structure model file provided could not be processed due to errors in XML syntax or structure.\n"
							+ e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage());
				}
			}
				break;
			case "-e":
			case "-eb": {
				if (target.equals("-e")) {
					if (eFound)
						throw new IllegalArgumentException("-e parameter cannot be defined twice.");
					if (ebFound)
						throw new IllegalArgumentException("-e and -eb are mutually exclusive parameters.");
					eFound = true;
					doBookmark = true;
				} else {
					if (ebFound)
						throw new IllegalArgumentException("-eb parameter cannot be defined twice.");
					if (eFound)
						throw new IllegalArgumentException("-e and -eb are mutually exclusive parameters.");
					ebFound = true;
				}
				try {
					argsIt.remove();
					String filepath = argsIt.next();
					argsIt.remove();
					ad.setWorkingEFGFile(filepath);
					if (!ad.workingEFGFileExists())
						throw new IllegalArgumentException("Event Flow Graph file provided\n" + "\'" + filepath + "\'\n"
								+ "does not exist on the file system.");
					// check if this file contains EFG data
					JAXBContext context = JAXBContext.newInstance(EFG.class);
					Unmarshaller um = context.createUnmarshaller();
					Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingEFGFile()));
					if (!(myFile instanceof EFG))
						throw new IllegalArgumentException();
					efc.setWorkingEventFlow((EFG) myFile);

				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("Invalid argument passed to -e parameter:\n"
							+ "Expected a path to an Event Flow Graph file used for test case generation.");
				} catch (JAXBException e) {
					throw new IllegalArgumentException("Invalid argument passed to -e parameter:\n"
							+ "EFG Model file provided could not be processed due to errors in XML syntax or structure.\n"
							+ e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage());
				}
			}
				break;
			case "-constfile": {
				if (tFound)
					throw new IllegalArgumentException("-constfile parameter cannot be defined twice.");
				tFound = true;
				argsIt.remove();
				try {
					String filepath = argsIt.next();
					argsIt.remove();
					ad.setWorkingTaskListFile(filepath);
					if (!ad.workingTaskListFileExists())
						throw new IllegalArgumentException("Constraints file provided\n" + "\'" + filepath + "\'\n"
								+ "does not exist on the file system.");
					// check if this file contains TaskList data
					JAXBContext context = JAXBContext.newInstance(TaskList.class);
					Unmarshaller um = context.createUnmarshaller();
					Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingTaskListFile()));
					if (!(myFile instanceof TaskList))
						throw new IllegalArgumentException(
								"File provided to -constfile parameter is a " + myFile.getClass() + " file.");
					efc.setWorkingTaskList((TaskList) myFile);
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("Invalid argument passed to -constfile parameter:\n"
							+ "Task list must be a valid Task List XML constraints file");
				} catch (JAXBException e) {
					throw new IllegalArgumentException("Invalid argument passed to -constfile parameter:\n"
							+ "Task List XML constraints file provided has errors in XML syntax or structure.\n"
							+ e.getLinkedException().getMessage());
				}
			}
				break;
			case "-rt": {
				if (rtFound)
					throw new IllegalArgumentException("-rt parameter cannot be defined twice.");
				rtFound = true;
				argsIt.remove();
				try {
					String arg = argsIt.next();
					argsIt.remove();
					switch (arg) {
					case "3":
					case "rsecwo":
						ld.setGeneratorRunningType(GenType.RSECWO);
						break;
					case "4":
					case "ecrswo":
						ld.setGeneratorRunningType(GenType.ECRSWO);
						break;
					case "5":
					case "noreds":
						ld.setGeneratorRunningType(GenType.NOREDS);
						break;
					case "6":
					case "worsec":
						ld.setGeneratorRunningType(GenType.WORSEC);
						break;
					default:
						throw new IllegalArgumentException();
					}
				} catch (NoSuchElementException | IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid argument passed to -rt parameter:\n"
							+ "Run type must be a valid run type id (an integer [3-6] or a runtype string)");
				}
			}
			}
		}
		// String[] left = remaining.toArray(new String[0]);
		// if(left.length < 1)
		// throw new IllegalArgumentException("No application file argument.");
		// ad.setAppFilePath(left[0]);
	}

	/**
	 * Accept arguments in the args provided that are relevant to the loading of
	 * the constraints tool.
	 */
	public static void constraintsArgs(String[] args, EventFlowSlicerController efc) throws FileNotFoundException {
		ArrayList<String> remaining = new ArrayList<String>();
		remaining.addAll(Arrays.asList(args));
		Iterator<String> argsIt = remaining.iterator();
		String target;
		boolean constfileFound, resdirFound;
		constfileFound = resdirFound = false;
		while (argsIt.hasNext()) {
			target = argsIt.next().toLowerCase();
			switch (target) {
			case "-resdir": {
				if (resdirFound)
					throw new IllegalArgumentException("-resdir parameter cannot be defined twice.");
				resdirFound = true;
				argsIt.remove();
				try {
					// check argument
					String resDir = argsIt.next();
					ad.setOutputDirectory(resDir);
					File parent = new File(resDir);
					if (resDir.isEmpty() || !parent.exists()) {
						throw new IllegalArgumentException("Output directory provided\n"
								+ ad.getOutputDirectory().getPath() + "\n" + "does not exist on the file system.");
					}
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to -resdir parameter:\n"
							+ "Expected a path to a folder where EventFlowSlicer results can be stored.");
				}
			}
				break;
			case "-constfile": {
				if (constfileFound)
					throw new IllegalArgumentException("-constfile parameter cannot be defined twice.");
				constfileFound = true;
				try {
					String filepath = argsIt.next();
					argsIt.remove();
					ad.setWorkingTaskListFile(filepath);
					if (!ad.workingTaskListFileExists())
						throw new IllegalArgumentException(
								"Constraints tasklist file provided does not exist on the file system\n" + "\'"
										+ filepath + "\'");

					// check if this file contains EFG data
					JAXBContext context = JAXBContext.newInstance(TaskList.class);
					Unmarshaller um = context.createUnmarshaller();
					Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingTaskListFile()));
					if (!(myFile instanceof TaskList))
						throw new IllegalArgumentException();
					efc.setWorkingTaskList((TaskList) myFile);
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("Invalid argument passed to -constfile parameter:\n"
							+ "Task list must be a valid Task List XML constraints file");
				} catch (JAXBException e) {
					throw new IllegalArgumentException("Invalid argument passed to -constfile parameter:\n"
							+ "Errors in XML syntax or structure.\n" + e.getLinkedException().getMessage());
				}
			}
				break;
			}
		}
		if (!constfileFound)
			throw new IllegalArgumentException(
					"When using the -constraints parameter, please specify a constraints file using the -constfile flag.");
		if (!resdirFound)
			throw new IllegalArgumentException(
					"When using the -constraints parameter, please specify an output directory using the -resdir flag.");
	}

	/**
	 * Accept arguments in the args provided that are relevant to the loading of
	 * the GUI interface.
	 *
	 * @param args
	 */
	public static void guiArgs(String[] args) {
		ArrayList<String> remaining = new ArrayList<String>();
		remaining.addAll(Arrays.asList(args));
		Iterator<String> argsIt = remaining.iterator();
		String target;
		boolean ripconFound, constfileFound, delayFound, cmcFound, resdirFound, argsFound, vmFound, tcdirFound, eFound,
				gFound;
		ripconFound = constfileFound = delayFound = cmcFound = resdirFound = argsFound = vmFound = tcdirFound = eFound = gFound = false;
		while (argsIt.hasNext()) {
			target = argsIt.next().toLowerCase();
			switch (target) {
			case "-ripcon": {
				if (ripconFound)
					throw new IllegalArgumentException("-ripcon parameter cannot be defined twice.");
				ripconFound = true;
				argsIt.remove();
				try {
					String filepath = argsIt.next();
					argsIt.remove();
					ad.setRipConfigurationFile(filepath);
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("Invalid argument passed to -ripcon parameter:\n"
							+ "Rip Configuration must be a valid Task List XML constraints file");
				}
			}
				break;
			case "-constfile": {
				if (constfileFound)
					throw new IllegalArgumentException("-constfile parameter cannot be defined twice.");
				constfileFound = true;
				argsIt.remove();
				try {
					String filepath = argsIt.next();
					argsIt.remove();
					ad.setWorkingTaskListFile(filepath);
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("Invalid argument passed to -constfile parameter:\n"
							+ "Task list must be a valid Task List XML constraints file");
				}
			}
				break;
			case "-delay": {
				if (delayFound)
					throw new IllegalArgumentException("-delay parameter cannot be defined twice.");
				delayFound = true;
				argsIt.remove();
				try {
					String arg = argsIt.next();
					int value = Integer.parseInt(arg);
					if (value < 0)
						throw new NumberFormatException();
					ApplicationData.openWaitTime = value;
					argsIt.remove();
				} catch (NoSuchElementException | NumberFormatException e) {
					throw new IllegalArgumentException("Invalid argument passed to -delay parameter:\n"
							+ "Application open delay must be a positive integer value");
				}
			}
				break;
			case "-cmc": {
				if (cmcFound)
					throw new IllegalArgumentException("-cmc parameter cannot be defined twice.");
				cmcFound = true;
				argsIt.remove();
				try {
					// check argument
					String customClass = argsIt.next();
					ad.setCustomMainClass(customClass);
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("Invalid argument passed to -usecogtool parameter:\n"
							+ "Expected a path to a custom runtime class that can be used to invoke the application under test.");
				}
			}
				break;
			case "-vm": {
				if (vmFound)
					throw new IllegalArgumentException("-vm parameter cannot be defined twice.");
				vmFound = true;
				argsIt.remove();
				try {
					String filepath = argsIt.next();
					ad.setArgumentsVMFile(filepath);
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to -vm parameter:\n"
							+ "Expected a path to the virtual machine arguments file containing a one-line arguments string.");
				}
			}
				break;
			case "-args": {
				if (argsFound)
					throw new IllegalArgumentException("-args parameter cannot be defined twice.");
				argsFound = true;
				argsIt.remove();
				try {
					String filepath = argsIt.next();
					ad.setArgumentsAppFile(filepath);
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to -args parameter:\n"
							+ "Expected a path to the arguments file containing a one-line arguments string.");
				}
			}
				break;
			case "-tcdir": {
				if (tcdirFound)
					throw new IllegalArgumentException("-tcdir parameter cannot be defined twice.");
				tcdirFound = true;
				argsIt.remove();
				try {
					// check argument
					String dir = argsIt.next();
					ad.setWorkingTestCaseDirectory(dir);
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to -tcdir parameter:\n"
							+ "Expected a path to a top level directory where input EventFlowSlicer test cases can be found.");
				}
			}
				break;
			case "-resdir": {
				if (resdirFound)
					throw new IllegalArgumentException("-resdir parameter cannot be defined twice.");
				resdirFound = true;
				argsIt.remove();
				try {
					// check argument
					String resDir = argsIt.next();
					ad.setOutputDirectory(resDir);
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to -resdir parameter:\n"
							+ "Expected a path to a folder where EventFlowSlicer results can be stored.");
				}
			}
				break;
			case "-e": {
				if (eFound)
					throw new IllegalArgumentException("-e parameter cannot be defined twice.");
				eFound = true;
				argsIt.remove();
				try {
					// check argument
					String filepath = argsIt.next();
					ad.setWorkingEFGFile(filepath);
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException(
							"No argument passed to -e parameter:\n" + "Expected a path to an Event Flow Graph file.");
				}
			}
				break;
			case "-g": {
				if (gFound)
					throw new IllegalArgumentException("-g parameter cannot be defined twice.");
				gFound = true;
				argsIt.remove();
				try {
					// check argument
					String filepath = argsIt.next();
					ad.setWorkingGUIFile(filepath);
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException(
							"No argument passed to -g parameter:\n" + "Expected a path to a GUI Structure file.");
				}
			}
				break;
			}
		}
		String[] left = remaining.toArray(new String[0]);
		if (left.length > 0)
			ad.setAppFilePath(left[0]);
	}

	private static JFCRipperConfigurationEFS rippingArgs(String[] args) {
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
			JFCRipperConfiguration.USE_JAR = true;
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


		String pathPath = PathConformance.parseApplicationPath(filepath);
		if(!pathPath.isEmpty()) {
			if(JFCRipperConfigurationEFS.URL_LIST.isEmpty()) // there is a path to append.
				JFCRipperConfigurationEFS.URL_LIST = pathPath;
			else
				JFCRipperConfigurationEFS.URL_LIST += GUITARConstants.CMD_ARGUMENT_SEPARATOR + pathPath;
		}
		filepath = JFCRipperConfigurationEFS.APP_ARGS_FILE;
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
		ad.setArgumentsVMFile(filepath);
		if (ad.hasArgumentsVMFile()) {
			if (!ad.argumentsVMFileExists())
				throw new IllegalArgumentException(
						"Java VM Arguments file provided does not exist on the file system.\n" + "\'" + filepath + "\'");
		}
		boolean useVMArgs = !JFCRipperConfigurationEFS.VM_ARGS_FILE.isEmpty();
		if(useVMArgs) {
			String[] jvmArgs = ReadArguments.readVMArguments(JFCRipperConfigurationEFS.VM_ARGS_FILE);
			String[] urlsList = new String[0];
			HashMap<String, String> sysProp = JavaLaunchApplication.imitateVMPropertyChanges(jvmArgs);
			urlsList = JavaLaunchApplication.getCPUrlsList(jvmArgs);
			for(String s : urlsList) {
				if(JFCRipperConfigurationEFS.URL_LIST.isEmpty())
					JFCRipperConfigurationEFS.URL_LIST += s;
				else
					JFCRipperConfigurationEFS.URL_LIST += GUITARConstants.CMD_ARGUMENT_SEPARATOR + s;
			}
			if(sysProp != null)
				poppedSystemProperties = sysProp;
		}
		if (!ripperAppExists())
			throw new IllegalArgumentException(
					"Application file provided:\n\'" + JFCRipperConfigurationEFS.LONG_PATH_TO_APP + "\'\n" + "cannot be found on the file system.");

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

	public static JFCReplayerConfigurationEFS replayArgs(String[] args, EventFlowSlicerController efc)
	{
		JFCReplayerConfigurationEFS configuration = new JFCReplayerConfigurationEFS();
		CmdLineParser parser = new CmdLineParser(configuration);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			throw new IllegalArgumentException(e);
		}
		String filepath;
		// check application file
		if (JFCReplayerConfigurationEFS.CMD_LINE_ARGS.isEmpty())
			throw new IllegalArgumentException(
					"Application file was not provided as an argument to the replayer arguments");
		filepath = JFCReplayerConfigurationEFS.CMD_LINE_ARGS.get(0);

		ad.setAppFilePath(filepath);
		if(JavaLaunchApplication.launchesJar(filepath))
			JFCReplayerConfigurationEFS.USE_JAR = true;
		if(JFCReplayerConfigurationEFS.MAIN_CLASS.isEmpty() && !filepath.isEmpty())
			JFCReplayerConfigurationEFS.MAIN_CLASS = ad.getAppFile().getAbsolutePath();
		JFCReplayerConfigurationEFS.LONG_PATH_TO_APP = JFCReplayerConfigurationEFS.MAIN_CLASS;

		// if there is a custom main class, use it as the path to the main class.
		if(!JFCReplayerConfigurationEFS.CUSTOM_MAIN_CLASS.isEmpty())
			JFCReplayerConfigurationEFS.MAIN_CLASS = JFCReplayerConfigurationEFS.CUSTOM_MAIN_CLASS;
		// if we're using a jar, then just use the filename as the main class path.
		else if(!JFCReplayerConfigurationEFS.USE_JAR)
			JFCReplayerConfigurationEFS.MAIN_CLASS = PathConformance.parseApplicationName(filepath);


		// set up the URL list further, if the input path to the app file is non
		// empty, add that path to the url lists, so that classpaths can be recognized from there.
		String inputPath = PathConformance.parseApplicationPath(filepath);
		if(!inputPath.isEmpty()) {
			if(JFCReplayerConfigurationEFS.URL_LIST.isEmpty())
				JFCReplayerConfigurationEFS.URL_LIST = inputPath;
			else
				JFCReplayerConfigurationEFS.URL_LIST += GUITARConstants.CMD_ARGUMENT_SEPARATOR + inputPath;
		}

		// check constraints.

		filepath = JFCReplayerConfigurationEFS.APP_ARGS_FILE;
		ad.setArgumentsAppFile(filepath);
		if (ad.hasArgumentsAppFile()) {
			if (!ad.argumentsAppFileExists())
				throw new IllegalArgumentException(
						"Application Arguments file provided does not exist on the file system.\n" + "\'" + filepath + "\'");
			else {
				String colonArgs = ReadArguments.colonDelimAppArgumentsFrom(ad.getArgumentsAppFile().getAbsolutePath());
				JFCReplayerConfigurationEFS.ARGUMENT_LIST = colonArgs;
			}
		}

		filepath = JFCReplayerConfigurationEFS.VM_ARGS_FILE;
		ad.setArgumentsVMFile(filepath);
		if (ad.hasArgumentsVMFile()) {
			if (!ad.argumentsVMFileExists())
				throw new IllegalArgumentException("Java VM Arguments file provided" + "\'" + filepath + "\'\n"
						+ "does not exist on the file system.");
		}
		boolean useVMArgs = !JFCReplayerConfigurationEFS.VM_ARGS_FILE.isEmpty();
		if(useVMArgs) {
			String[] jvmArgs = ReadArguments.readVMArguments(JFCReplayerConfigurationEFS.VM_ARGS_FILE);
			String[] urlsList = new String[0];
			HashMap<String, String> sysProp = JavaLaunchApplication.imitateVMPropertyChanges(jvmArgs);
			urlsList = JavaLaunchApplication.getCPUrlsList(jvmArgs);
			for(String s : urlsList) {
				if(JFCReplayerConfigurationEFS.URL_LIST.isEmpty())
					JFCReplayerConfigurationEFS.URL_LIST += s;
				else
					JFCReplayerConfigurationEFS.URL_LIST += GUITARConstants.CMD_ARGUMENT_SEPARATOR + s;
			}
			if(sysProp != null)
				poppedSystemProperties = sysProp;
		}
		if (!replayerAppExists())
			throw new IllegalArgumentException(
					"Application file provided:\n" + filepath + "\n" + "cannot be found on the file system.");

		// Results directory
		filepath = JFCReplayerConfigurationEFS.RESULTS_DIRECTORY;
		File parent = new File(filepath);
		ad.setOutputDirectory(filepath);
		if (filepath.isEmpty() || filepath == null)
			throw new IllegalArgumentException("An output directory was not provided via the -resdir argument.\n"
					+ "Output directory is required.");
		else if (!parent.exists())
			throw new IllegalArgumentException("Output directory provided:\n" + ad.getOutputDirectory().getPath() + "\n"
					+ "does not exist on the file system.");

		// Test Case Directory
		filepath = JFCReplayerConfigurationEFS.TESTCASE_DIRECTORY;
		parent = new File(filepath);
		ad.setWorkingTestCaseDirectory(filepath);
		if (filepath.isEmpty() || filepath == null)
			throw new IllegalArgumentException("A test case directory was not provided via the -tcdir argument."
					+ "Test case directory is required.");
		else if (!parent.exists())
			throw new IllegalArgumentException("Test case directory provided:\n"
					+ ad.getWorkingTestCaseDirectory().getPath() + "\n" + "does not exist on the file system.");

		// GUI file
		try {
			filepath = JFCReplayerConfigurationEFS.GUI_FILE;
			ad.setWorkingGUIFile(filepath);
			if (!ad.workingGUIFileExists())
				throw new IllegalArgumentException("GUI Structure file provided in -g parameter:\n" + "\'" + filepath
						+ "\'\n" + "does not exist on the file system\n");

			// check if this file contains TaskList data
			JAXBContext context = JAXBContext.newInstance(GUIStructure.class);
			Unmarshaller um = context.createUnmarshaller();
			Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingGUIFile()));
			if (!(myFile instanceof GUIStructure))
				throw new IllegalArgumentException(
						"File provided to -g parameter is a " + myFile.getClass() + " file.");
			efc.setWorkingGUIStructure((GUIStructure) myFile);
		} catch (JAXBException e) {
			throw new IllegalArgumentException(
					"Invalid argument passed to -g parameter:\n" + "Errors in XML syntax or structure.\n"
							+ e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage());
		}

		try {
			filepath = JFCReplayerConfigurationEFS.EFG_FILE;
			ad.setWorkingEFGFile(filepath);
			if (!ad.workingEFGFileExists())
				throw new IllegalArgumentException("Event flow graph file provided to -e  parameter:\n" + "\'"
						+ filepath + "\'\n" + "does not exist on the file system\n");

			// check if this file contains EFG data
			JAXBContext context = JAXBContext.newInstance(EFG.class);
			Unmarshaller um = context.createUnmarshaller();
			Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingEFGFile()));
			if (!(myFile instanceof EFG))
				throw new IllegalArgumentException(
						"File provided to -e parameter is a " + myFile.getClass() + " file.");
			efc.setWorkingEventFlow((EFG) myFile);
		} catch (JAXBException e) {
			throw new IllegalArgumentException(
					"Invalid argument passed to -e parameter:\n" + "Errors in XML syntax or structure.\n"
							+ e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage());
		}

		// get output files
		JFCReplayerConfigurationEFS.LOG_FILE = ad.getOutputDirectory().getAbsolutePath() + File.separator
				+ "Replay.log";
		ad.setReplayConfigurationFile(JFCReplayerConfigurationEFS.CONFIG_FILE);

		/**
		 * If application open delay is set and is valid
		 */
		if(JFCReplayerConfigurationEFS.APPLICATION_OPEN_DELAY < 0)
			JFCReplayerConfigurationEFS.INITIAL_WAITING_TIME = ApplicationData.openWaitTime;
		else {
			JFCReplayerConfigurationEFS.INITIAL_WAITING_TIME = JFCReplayerConfigurationEFS.APPLICATION_OPEN_DELAY;
			ApplicationData.openWaitTime = JFCReplayerConfigurationEFS.INITIAL_WAITING_TIME;
		}
		return configuration;
	}

	private static void captureArgs(String[] args) {
		ArrayList<String> remaining = new ArrayList<String>();
		remaining.addAll(Arrays.asList(args));
		Iterator<String> argsIt = remaining.iterator();
		String target;
		boolean delayFound, cmcFound, resdirFound, argsFound, vmFound;
		delayFound = cmcFound = resdirFound = argsFound = vmFound = false;
		while (argsIt.hasNext()) {
			target = argsIt.next();
			switch (target) {
			case "-delay": {
				if (delayFound)
					throw new IllegalArgumentException("-delay parameter cannot be defined twice.");
				delayFound = true;
				argsIt.remove();
				try {
					String arg = argsIt.next();
					int value = Integer.parseInt(arg);
					if (value < 0)
						throw new NumberFormatException();
					ApplicationData.openWaitTime = value;
					argsIt.remove();
				} catch (NoSuchElementException | NumberFormatException e) {
					throw new IllegalArgumentException("Invalid argument passed to -delay parameter:\n"
							+ "Application open delay must be a positive integer value");
				}
			}
				break;
			case "-cmc": {
				if (cmcFound)
					throw new IllegalArgumentException("-cmc parameter cannot be defined twice.");
				cmcFound = true;
				argsIt.remove();
				try {
					// check argument
					String customClass = argsIt.next();
					ad.setCustomMainClass(customClass);
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("Invalid argument passed to -cmc parameter:\n"
							+ "Expected a path to a custom runtime class that can be used to invoke the application under test.");
				}
			}
				break;
			case "-vm": {
				if (vmFound)
					throw new IllegalArgumentException("-vm parameter cannot be defined twice.");
				vmFound = true;
				argsIt.remove();
				try {
					String filepath = argsIt.next();
					ad.setArgumentsVMFile(filepath);
					if (!ad.argumentsVMFileExists())
						throw new IllegalArgumentException(
								"Java Virtual Machine arguments file provided to -vm parameter:\n" + "\'" + filepath
										+ "\'\n" + "does not exist on the file system");
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to -vm parameter:\n"
							+ "Expected a path to the virtual machine arguments file containing a one-line arguments string.");
				}
			}
				break;
			case "-args": {
				if (argsFound)
					throw new IllegalArgumentException("-args parameter cannot be defined twice.");
				argsFound = true;
				argsIt.remove();
				try {
					String filepath = argsIt.next();
					ad.setArgumentsAppFile(filepath);
					if (!ad.argumentsAppFileExists())
						throw new IllegalArgumentException("Application arguments file provided to -args parameter:\n"
								+ "\'" + filepath + "\'\n" + "does not exist on the file system");
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to -args parameter:\n"
							+ "Expected a path to the arguments file containing a one-line arguments string.");
				}
			}
				break;
			case "-resdir": {
				if (resdirFound)
					throw new IllegalArgumentException("-resdir parameter cannot be defined twice.");
				resdirFound = true;
				argsIt.remove();
				try {
					// check argument
					String resDir = argsIt.next();
					File parent = new File(resDir);
					ad.setOutputDirectory(resDir);
					if (resDir.isEmpty() || !parent.exists()) {
						throw new IllegalArgumentException("Output directory provided\n"
								+ ad.getOutputDirectory().getPath() + "\n" + "does not exist on the file system.");
					}
					argsIt.remove();
				} catch (NoSuchElementException e) {
					throw new IllegalArgumentException("No argument passed to -resdir parameter:\n"
							+ "Expected a path to a folder where results from running capture can be stored.");
				}
			}
				break;
			}
		}
		String[] left = remaining.toArray(new String[0]);
		if (left.length < 1)
			throw new IllegalArgumentException("No application file argument.");
		ad.setAppFilePath(left[0]);
	}

	private static String[] exhaustMainArgs(String[] args) {
		ArrayList<String> toReturn = new ArrayList<String>();
		toReturn.addAll(Arrays.asList(args));
		Iterator<String> argsIt = toReturn.iterator();
		String target;
		String allArguments = "-capture, -constraints, -rip, -generate, -replay, and -gui";
		boolean rmiFound, noRMIFound, sendbackFound, ripFound, genFound, captureFound, constFound, replayFound,
				rselFound, guiFound;
		rmiFound = noRMIFound = sendbackFound = ripFound = genFound = captureFound = constFound = replayFound = rselFound = guiFound = false;
		while (argsIt.hasNext()) {
			target = argsIt.next().toLowerCase();
			switch (target) {
			case "-noressubdir": {
				ad.unsetSubdirectoryFiller();
				argsIt.remove();
			}
				break;
			case "-rmi": {
				if (rmiFound)
					throw new IllegalArgumentException("-rmi parameter cannot be defined twice.");
				if (sendbackFound || noRMIFound)
					throw new IllegalArgumentException(
							"-sendback, -normi, and -rmi parameters cannot be defined together.");

				rmiFound = true;
				try {
					argsIt.remove();
					String portString = argsIt.next();
					Integer.parseInt(portString);
					ld.setSendtoRMIPort(portString);
					argsIt.remove();
				} catch (NoSuchElementException | NumberFormatException e) {
					throw new IllegalArgumentException("ERROR: Invalid arguments for -rmi/-sendback parameter:\n"
							+ "Usage: -rmi [-sendPrefs] <rmiPortNumber>,\n"
							+ "\twhere rmiPortNumber is a valid positive integer port number\n");
				}
			}
				break;
			case "-normi": {
				if (noRMIFound)
					throw new IllegalArgumentException("-normi parameter cannot be defined twice.");
				if (sendbackFound || rmiFound)
					throw new IllegalArgumentException(
							"-sendback, -normi, and -rmi parameters cannot be defined together.");
				noRMIFound = true;
				ld.setSendtoRMIPort("");
				argsIt.remove();
			}
				break;
			case "-sendback": {
				if (sendbackFound)
					throw new IllegalArgumentException("-sendback parameter cannot be defined twice.");
				if (rmiFound | noRMIFound)
					throw new IllegalArgumentException(
							"-sendback, -normi, and -rmi parameters cannot be defined together.");
				sendbackFound = true;
				try {
					argsIt.remove();
					String portString = argsIt.next();
					Integer.parseInt(portString);
					ld.setSendbackRMIPort(portString);
					argsIt.remove();
				} catch (NoSuchElementException | NumberFormatException e) {
					throw new IllegalArgumentException("ERROR: Invalid arguments for -sendback parameter:\n"
							+ "Usage: -sendback [-sendPrefs] <rmiPortNumber>,\n"
							+ "\twhere rmiPortNumber is a valid positive integer port number\n");
				}
			}
				break;
			case "-capture": {
				if (constFound | ripFound | genFound | replayFound | rselFound | guiFound)
					throw new IllegalArgumentException(allArguments + " are mutually exclusive parameters");
				if (captureFound)
					throw new IllegalArgumentException("-capture parameter cannot be defined twice.");
				captureFound = true;
				doCapture = true;
				argsIt.remove();
			}
				break;
			case "-constraints": {
				if (captureFound | ripFound | genFound | replayFound | rselFound | guiFound)
					throw new IllegalArgumentException(allArguments + " are mutually exclusive parameters");
				if (constFound)
					throw new IllegalArgumentException("-constraints parameter cannot be defined twice");
				constFound = true;
				doConstraints = true;
				argsIt.remove();
			}
				break;
			case "-rip": {
				if (captureFound | constFound | genFound | replayFound | rselFound | guiFound)
					throw new IllegalArgumentException(allArguments + " are mutually exclusive parameters");
				if (ripFound)
					throw new IllegalArgumentException("-rip parameter cannot be defined twice.");
				ripFound = true;
				doRip = true;
				argsIt.remove();
			}
				break;
			case "-generate": {
				if (captureFound | constFound | ripFound | guiFound | replayFound | rselFound)
					throw new IllegalArgumentException(allArguments + " are mutually exclusive parameters");
				if (genFound)
					throw new IllegalArgumentException("-generate parameter cannot be defined twice");
				genFound = true;
				doGenerate = true;
				argsIt.remove();
			}
				break;
			case "-replay": {
				if (captureFound | constFound | ripFound | guiFound | genFound)
					throw new IllegalArgumentException(allArguments + " are mutually exclusive parameters");
				if (rselFound)
					throw new IllegalArgumentException("-replay and -replay_sel cannot be defined together");
				if (replayFound)
					throw new IllegalArgumentException("-replay parameter cannot be defined twice");
				replayFound = true;
				doReplay = true;
				argsIt.remove();
			}
				break;
			case "-replay_sel": {
				if (captureFound | constFound | ripFound | genFound)
					throw new IllegalArgumentException(allArguments + " are mutually exclusive parameters");
				if (replayFound)
					throw new IllegalArgumentException("-replay and -replay_sel cannot be defined together");
				if (rselFound)
					throw new IllegalArgumentException("-replay_sel parameter cannot be defined twice");
				replayFound = true;
				doReplay = true;
				try {
					argsIt.remove();
					String selArgs = argsIt.next();
					if (ArrayTools.bibleNotationType(selArgs, false) == -1)
						throw new IllegalArgumentException();
					ld.setLaunchSelectionArguments(selArgs);
					argsIt.remove();
				} catch (NoSuchElementException | IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid argument passed to -replay_sel parameter:\n"
							+ "Replay selection parameter must be a replay-test-case specifier in \"bible notation,\" based on valid integers.");
				}
			}
				break;
			case "-gui": {
				if (ripFound | captureFound | genFound | constFound | replayFound)
					throw new IllegalArgumentException(allArguments + " are mutually exclusive parameters");
				if (guiFound)
					throw new IllegalArgumentException("-gui parameter cannot be defined twice");
				guiFound = true;
				doGUI = true;
				argsIt.remove();
			}
			}
		}
		if (!genFound && !captureFound && !ripFound && !constFound && !replayFound && !rselFound)
			doGUI = true;

		return toReturn.toArray(new String[0]);
	}

	/**
	 * Pull an RMI object of the name specified by stubName from the currently
	 * running Java JVM registry. This method assumes that a registry has
	 * already been instantiated for use by the current JVM.
	 *
	 * @param serverArgs
	 */
	public static NetCommunication beginRMISession(boolean captureSession, String port) {
		NetCommunication networkStub;
		Registry registry;

		String stubName;
		if (captureSession)
			stubName = "EFSCapture";
		else
			stubName = "EFSReplay";

		try {
			if (port.isEmpty())
				registry = LocateRegistry.getRegistry(Integer.parseInt(DEFAULT_JAVA_RMI_PORT));
			else
				registry = LocateRegistry.getRegistry(Integer.parseInt(port));
		} catch (RemoteException e) {
			throw new RuntimeException("JavaCaptureTestCase: Tried to locate registry but failed\n: ", e);
		}

		try {
			networkStub = (NetCommunication) registry.lookup(stubName);
		} catch (RemoteException | NotBoundException e) {
			throw new RuntimeException("JavaCaptureTestCase: Tried to lookup \"" + stubName
					+ "\" on JVM registry but failed:\n" + "Due to a " + e.getClass().getCanonicalName());
		}

		System.out.println("\n> JavaCaptureTestCase: Bind was successful.\n");
		return networkStub;
	}

	/**
	 * Remove a running session with a remote app from the registry. This method
	 * assumes that the registry has already been created.
	 *
	 * Preconditions: The registry has already been instantiated.
	 *
	 * @param captureSession
	 * @param port
	 * @return
	 */
	public static boolean endRMISession(boolean captureSession, String port) {
		String stubName;
		if (captureSession)
			stubName = "CTHCapture";
		else
			stubName = "CTHReplay";

		Registry registry;
		try {
			registry = LocateRegistry.getRegistry();
		} catch (RemoteException e) {
			throw new RuntimeException("Tried to locate registry but failed \n" + e);
		}

		try {
			registry.unbind(stubName);
		} catch (RemoteException e) {
			System.err.println(
					"Tried to unbind " + stubName + " from the registry but failed\n." + e.getLocalizedMessage());
			return false;
		} catch (NotBoundException e) {
			// the associated application never had a binding.
			// don't do anything.
		}
		return true;
	}

	/**
	 * This method instantiates the registry if it is not already created, turns
	 * the provided CTHCommunication into an object, passes the object to the
	 * registry, and returns the stub that we created.
	 *
	 * Preconditions: (none) Postconditions: networkStub is bound to the
	 * registry. An error message is printed out if the registry was already
	 * created.
	 */
	public static NetCommunication preloadRMISession(NetCommunication networkStub, String port) {
		String stubName;
		if (networkStub instanceof JavaCaptureTaskList)
			stubName = "EFSCapture";
		else
			stubName = "EFSReplay";

		Registry registry;

		// create registry, if one is not already created.
		try {
			LocateRegistry.createRegistry(Integer.parseInt(port));
		} catch (RemoteException | NumberFormatException e) {
			System.out.println(
					"Tried to create registry reference and failed. One registry reference might already exist from previous capture/replay operation.");
		}

		// create stub.
		try {
			networkStub = (NetCommunication) UnicastRemoteObject.exportObject(networkStub, Integer.parseInt(port));

		} catch (RemoteException | NumberFormatException e) {
			throw new RuntimeException("Tried to generate stub to put on registry but failed \n" + e);
		}

		// get registry again.
		try {
			registry = LocateRegistry.getRegistry();
		} catch (RemoteException e) {
			throw new RuntimeException("Tried to locate registry but failed \n" + e);
		}
		// bind stub to a name.
		try {
			registry.rebind(stubName, networkStub);
		} catch (RemoteException e) {
			throw new RuntimeException("Tried to bind the resource to a registry name but failed\n" + e);
		}
		return networkStub;
	}

	public static boolean replayerAppExists() {
		try {
			String[] URLs;
			if (!JFCReplayerConfigurationEFS.URL_LIST.isEmpty())
				URLs = JFCReplayerConfigurationEFS.URL_LIST.split(GUITARConstants.CMD_ARGUMENT_SEPARATOR);
			else
				URLs = new String[0];
			URLs = JFCApplication2.convertToURLStrings(URLs);
			new JFCApplication2(JFCReplayerConfigurationEFS.MAIN_CLASS, JFCReplayerConfigurationEFS.LONG_PATH_TO_APP, JFCReplayerConfigurationEFS.USE_JAR, URLs);
		} catch (Exception e) {
			System.err.println("ERROR: Class Not Found on File System:\n" + e.getMessage());
			return false;
		}
		return true;
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
