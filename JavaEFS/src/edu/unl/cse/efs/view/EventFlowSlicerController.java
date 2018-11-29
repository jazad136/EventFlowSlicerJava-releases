package edu.unl.cse.efs.view;


import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import edu.unl.cse.efs.java.JavaCaptureLauncher;
import edu.unl.cse.efs.java.JavaLaunchApplication;
import edu.unl.cse.efs.java.JavaReplayerLauncher;
import edu.unl.cse.efs.replay.JFCReplayerConfigurationEFS;
import edu.unl.cse.efs.ripper.JFCRipperConfigurationEFS;
import edu.unl.cse.efs.ripper.JFCRipperEFS;
import edu.unl.cse.efs.tools.LocationComparator;
import edu.unl.cse.efs.tools.PathConformance;
import edu.unl.cse.efs.tools.TaskListConformance;
import edu.unl.cse.efs.util.ReadArguments;
import edu.unl.cse.efs.view.ft.FittingTool;
import edu.unl.cse.efs.view.tcselect.IPSelectorDisplay;
import edu.unl.cse.efs.generate.TestCaseGenerate;
import edu.unl.cse.efs.guitarplugin.EFSEFGConverter;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.Required;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.model.data.Widget;
import edu.umd.cs.guitar.model.wrapper.GUIStructureWrapper;
import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.LauncherData;
import edu.unl.cse.efs.app.EventFlowSlicer;
import edu.unl.cse.efs.generate.DirectionalPack;
import edu.unl.cse.efs.generate.EFGPacifier;
import edu.unl.cse.efs.generate.FocusOnPack;
import edu.unl.cse.efs.generate.PEFGCreator;
import edu.unl.cse.efs.generate.SearchPack;
import edu.unl.cse.efs.generate.SelectorPack;
import edu.unl.cse.bmktools.EFGBookmarking;

public class EventFlowSlicerController
{
	private ApplicationData ad;
	private JavaLaunchApplication jLaunch;
	private JavaCaptureLauncher currentCapture;
	private JFCRipperEFS currentRip;
	private JavaReplayerLauncher currentReplay;
	private GUIStructure ripperArtifact;
	private TaskList workingTaskList;
	private EFG workingEventFlow;
	private PrintWriter logFileWriter;
	private SecondaryLoop ripLoop;
	private List<SelectorPack> facetInstructions;
	private List<DirectionalPack> facetDirectionals;
	private FocusOnPack facetFocus;
	private SearchPack facetSearches;
	private List<Integer> facetSearchesToModify;
	private boolean doLogging;


	public EventFlowSlicerController(ApplicationData appDataObject)
	{
		ad = appDataObject;
		ObjectFactory fact = new ObjectFactory();
		ripperArtifact = fact.createGUIStructure();
		workingTaskList = fact.createTaskList();
		facetInstructions = new ArrayList<SelectorPack>();
	}

	public void setWorkingEventFlow(EFG efg)
	{
		this.workingEventFlow = efg;
	}
	public void setWorkingGUIStructure(GUIStructure struct)
	{
		this.ripperArtifact = struct;
	}

	public void setWorkingTaskList(TaskList workingTL)
	{
		this.workingTaskList = workingTL;
	}

	/**
	 * Create a bookmarked EFG, an EFG that contains replayable labels for events.
	 * These replayable labels are also human-readable.
	 */
	public void bookmarkEFG()
	{
		System.out.println("Bookmarking the specified event flow graph...");
		EFGBookmarking bkmk = new EFGBookmarking(workingEventFlow, ripperArtifact);
		workingEventFlow = bkmk.getBookmarked(true);
		System.out.println("Done.");
	}

	/*
	 * * * * * * *
	 * CONSTRAINTS
	 * * * * * * *
	 */
	public void startFitting()
	{
		FittingTool.startAndReadTo(ad);
	}

	public void startFittingDialog(JFrame parentFrame)
	{
		FittingTool.startAndReadTo(ad, parentFrame);
	}


	public void startSelectorDialog(JFrame parentFrame)
	{
		facetInstructions = IPSelectorDisplay.startAndGetPacks(ad, parentFrame);
		facetDirectionals = IPSelectorDisplay.timDirPacksOutput;
		if(IPSelectorDisplay.timFocPacksOutput != null && !IPSelectorDisplay.timFocPacksOutput.isEmpty()) {
			facetFocus = IPSelectorDisplay.timFocPacksOutput.get(0);
		}
		if(IPSelectorDisplay.abbySearchPacksOutput != null && !IPSelectorDisplay.abbySearchPacksOutput.isEmpty()) {
			facetSearches = IPSelectorDisplay.abbySearchPacksOutput.get(0);
			facetSearchesToModify = new ArrayList<Integer>(IPSelectorDisplay.tooltipModify);
		}
		System.out.println("Modifying constraints...");
		workingTaskList.getWidget().clear();
		workingTaskList.getWidget().addAll(IPSelectorDisplay.outputScope);
		ObjectFactory fact = new ObjectFactory();
		List<Widget> focusList = IPSelectorDisplay.timFocPacksOutput.get(0).list;
		for(Widget w : focusList) {
			Required sample = fact.createRequired();
			sample.getWidget().add(w);
			// add the required rule necessary to get the generator running to the tasklist
			boolean foundR = false;
			for(Required r : workingTaskList.getRequired()) {
				if(sample.equals(r)) {
					foundR = true;
					break;
				}
			}
			if(!foundR)
				workingTaskList.getRequired().add(sample);
		}
		writeTaskListFile();
	}


	public int generateFacetedTCsFor(int directionInstruction) throws IOException
	{
		int testCases = 0;
		File newDDirectory = new File(ad.getOutputDirectory().getAbsoluteFile(), "D" + directionInstruction);

		TestCaseGenerate tcg = new TestCaseGenerate(
				workingEventFlow,
				ripperArtifact,
				newDDirectory.getAbsolutePath());
		// set up packs
		List<SelectorPack> packs = new ArrayList<SelectorPack>();
		if(facetDirectionals != null)
			packs.addAll(facetDirectionals);
		if(facetFocus != null)
			packs.add(facetFocus);
		if(facetSearches != null)
			packs.add(facetSearches);
		// ensure that search performers have updated values.
		for(SelectorPack testP : packs) {
			if(testP instanceof SearchPack) {
				SearchPack testSP = ((SearchPack)testP);
				List<Widget> newFullScope = TaskListConformance.checkAndSetWidgets(workingTaskList.getWidget(), ripperArtifact, workingEventFlow);
				for(int i : facetSearchesToModify) {
					// pull index from the tasklist.
					// reset the search pack performer.
					Widget newPerformer = newFullScope.get(i);
					testSP.setSearchPerformer(i, newPerformer);
				}
				testSP.resetList(newFullScope);
			}
			else {
				List<Widget> checkedWidgets = TaskListConformance.checkAndSetWidgets(testP.list, ripperArtifact, workingEventFlow);
				testP.resetList(checkedWidgets);
			}
		}
		// setup the generator
		tcg.setupSliceSets(workingTaskList, packs.toArray(new SelectorPack[0]));
		tcg.setupSliceIndexSets(workingTaskList);
		tcg.resetTimes();

		try {
			boolean hasMore = tcg.hasMoreSlices();
			while(hasMore) {
				testCases += tcg.runSliceAlgorithmAndWriteResultsToOutputDirectory(false);
				hasMore = tcg.advanceSplicer();
			}
			System.out.println("No more slices.");
		} catch(IOException e) {
			throw new IOException("Generator could not write files.");
		}
		EFG minimalEFG = tcg.postEFG;
		XMLHandler handler = new XMLHandler();
		String minimalFilename = ad.getOutputGenBaseFile() + "_minimal.EFG";
		System.out.println("Writing final minimal EFG to " + minimalFilename);
		long finalIOTime = System.currentTimeMillis();

		try { handler.writeObjToFile(minimalEFG, new FileOutputStream(minimalFilename)); }
		catch(IOException e) {
			finalIOTime = System.currentTimeMillis() - finalIOTime;
			// log time
			if(doLogging) {
				logFileWriter.append("gentc\t" + (tcg.algoDurationTime + tcg.ioHandlingTime + finalIOTime) + "\tms\n");
				logFileWriter.close();
			}
			throw new IOException("Generation was completed. (final EFG could not be written)");
		}
		finalIOTime = System.currentTimeMillis() - finalIOTime;
		// log time
		if(doLogging) {
			logFileWriter.append("gentc\t" + (tcg.algoDurationTime + tcg.ioHandlingTime + finalIOTime) + "\tms\n");
			logFileWriter.close();
		}
		System.out.println("Done.");
		return testCases;
	}
	public int startGeneratingTestCasesWithFacets(LauncherData ld) throws IOException
	{
		try {
			if(!ad.outputDirectoryExists()) {
				if(!ad.getOutputDirectory().mkdirs()) {
					throw new RuntimeException("Could not create results directory and write test cases:\n"
							+ ad.getOutputDirectory().getPath());
				}
			}
		} catch(Exception e) {
			throw new SecurityException(e.getMessage());
		}
		EFGPacifier pacifier = new EFGPacifier(workingEventFlow, workingTaskList, ad.getOutputGenBaseFile().getAbsolutePath());
		System.out.println("Using pacifier run type " + ld.getGeneratorRunningType() + ":");
		switch(ld.getGeneratorRunningType()) {
			case NOCHOICE:
			case RSECWO: workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev3(); break;
			case ECRSWO: workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev4(); break;
			case WORSEC: workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev6(); break;
			case NOREDS: // do nothing.
		}
		if(doLogging) {
			logFileWriter.append("1\t" + pacifier.times[EFGPacifier.RED1] + "\tms\n");
			logFileWriter.append("2\t" + pacifier.times[EFGPacifier.RED2] + "\tms\n");
			logFileWriter.append("3\t" + pacifier.times[EFGPacifier.RED3] + "\tms\n");
		}
		TestCaseGenerate tcg = new TestCaseGenerate(
				workingEventFlow,
				ripperArtifact,
				ad.getOutputDirectory().getAbsolutePath());
		int testCases = 0;
		// parameterize the events.
		PEFGCreator pefgc = new PEFGCreator(workingEventFlow, workingTaskList);
		workingEventFlow = pefgc.augmentEvents();
		if(!facetInstructions.isEmpty()) {
			for(int i = 0; i < facetDirectionals.size(); i++)
				testCases += generateFacetedTCsFor(i);
			return testCases;
		}

		tcg.setupIndexSets(workingTaskList);
		tcg.resetTimes();
		try{ testCases = tcg.runAlgorithmAndWriteResultsToOutputDirectory(false);}
		catch(IOException e) {
			throw new IOException("Generation Erorrs", e);
		}
		EFG minimalEFG = tcg.postEFG;
		XMLHandler handler = new XMLHandler();
		String minimalFilename = ad.getOutputGenBaseFile() + "_minimal.EFG";
		System.out.println("Writing final minimal EFG to " + minimalFilename);
		long finalIOTime = System.currentTimeMillis();

		try { handler.writeObjToFile(minimalEFG, new FileOutputStream(minimalFilename)); }
		catch(IOException e) {
			finalIOTime = System.currentTimeMillis() - finalIOTime;
			// log time
			if(doLogging) {
				logFileWriter.append("gentc\t" + (tcg.algoDurationTime + tcg.ioHandlingTime + finalIOTime) + "\tms\n");
				logFileWriter.close();
			}
			throw new IOException("Generation was completed. (final EFG could not be written)");
		}
		finalIOTime = System.currentTimeMillis() - finalIOTime;
		// log time
		if(doLogging) {
			logFileWriter.append("gentc\t" + (tcg.algoDurationTime + tcg.ioHandlingTime + finalIOTime) + "\tms\n");
			logFileWriter.close();
		}
		System.out.println("Done.");
		return testCases;
	}
	/*
	 *\\\\\\
	 *\\\\\GENERATION
	 *\\\\\\
	 */
	public int startGeneratingTestCases(LauncherData ld) throws IOException
	{
		try {
			if(!ad.outputDirectoryExists()) {
				if(!ad.getOutputDirectory().mkdirs())
					throw new RuntimeException("Could not create directory:\n"
							+ ad.getOutputDirectory().getPath());
			}
		} catch(Exception e) {
			throw new SecurityException("Could not create the results directory.", e);
		}

		for(EventType et : workingEventFlow.getEvents().getEvent())
			System.out.printf("[%s]\n", et.getEventId());

		EFGPacifier pacifier = new EFGPacifier(workingEventFlow, workingTaskList, ad.getOutputGenBaseFile().getAbsolutePath());
		System.out.println("Using pacifier run type " + ld.getGeneratorRunningType() + ":");
		switch(ld.getGeneratorRunningType()) {
			case NOCHOICE:
			case RSECWO: workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev3(); break;
			case ECRSWO: workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev4(); break;
			case WORSEC: workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev6(); break;
			case NOREDS: // do nothing.
		}
		if(doLogging) {
			logFileWriter.append("1\t" + pacifier.times[EFGPacifier.RED1] + "\tms\n");
			logFileWriter.append("2\t" + pacifier.times[EFGPacifier.RED2] + "\tms\n");
			logFileWriter.append("3\t" + pacifier.times[EFGPacifier.RED3] + "\tms\n");
		}
//		// add parameters to the events.
//		PEFGCreator pefgc = new PEFGCreator(workingEventFlow, workingTaskList);
//		workingEventFlow = pefgc.augmentEvents();

		TestCaseGenerate tcg = new TestCaseGenerate(
				workingEventFlow,
				ripperArtifact,
				ad.getOutputDirectory().getAbsolutePath());
		tcg.setupIndexSets(workingTaskList);
		tcg.resetTimes();
		int testCases = 0;
		try {
			testCases = tcg.runAlgorithmAndWriteResultsToOutputDirectory(false);
		} catch(IOException e) {
			throw new IOException("Generation");
		}
		EFG minimalEFG = tcg.postEFG;
		// write final constructed EFG to output directory:

		XMLHandler handler = new XMLHandler();
		String minimalFilename = ad.getOutputGenBaseFile().getPath() + "_minimal.EFG";
		System.out.println("Writing final minimal EFG to " + minimalFilename);
		long finalIOTime = System.currentTimeMillis();
		try {
			handler.writeObjToFile(minimalEFG, new FileOutputStream(minimalFilename));
		} catch(IOException e) {
			finalIOTime = System.currentTimeMillis() - finalIOTime;
			// log time
			if(doLogging) {
				logFileWriter.append("gentc\t" + (tcg.algoDurationTime + tcg.ioHandlingTime + finalIOTime) + "\tms\n");
				logFileWriter.close();
			}
			throw new IOException("Generation was completed. (final EFG could not be written)");
		}
		finalIOTime = System.currentTimeMillis() - finalIOTime;
		// log time
		if(doLogging) {
			logFileWriter.append("gentc\t" + (tcg.algoDurationTime + tcg.ioHandlingTime + finalIOTime) + "\tms\n");
			logFileWriter.close();
		}
		System.out.println("Done.");
		return testCases;
	}

	/**
	 * Check and make sure all the "widget"-events in the constraints tasklist have event ids attached to them.
	 * If one does not, append it to the ID.
	 * This action makes the tasklist generator-compatible.
	 * @param constraints
	 * @throws IllegalArgumentException
	 */
	public void relabelConstraintsWidgets()
	{
		workingTaskList = TaskListConformance.checkAndSetConstraintsWidgets(workingTaskList, ripperArtifact, workingEventFlow);
	}

	/*
	/*********
	 *********
	 **REPLAY
	 *********
	 *********
	 */
	/**
	 * This method will start the replayer launcher module using
	 * whatever files are set in the application data object of this controller,
	 * and whatever modes and settings are set in the launcher data provided.
	 */
	public void startReplay(LauncherData ld)
	{
		try {
			if(!ad.outputDirectoryExists()) {
				if(!ad.getOutputDirectory().mkdirs())
					throw new RuntimeException();
			}
		} catch(Exception e) {
			throw new SecurityException("Could not create the results directory.", e);
		}
		jLaunch = new JavaLaunchApplication(ad.getAppFile().getAbsolutePath(), new String[0]);
		if(!ad.getCustomMainClass().isEmpty())
			jLaunch.setCustomizedMainClass(ad.getCustomMainClass());
		currentReplay = new JavaReplayerLauncher(jLaunch, ad, ld);

		if(!ld.sendsToRMI()) {
			if(ad.hasArgumentsAppFile())
				jLaunch.saveAppArguments(ReadArguments.readAppArguments(ad.getArgumentsAppFile().getAbsolutePath()));
			if(ad.hasArgumentsVMFile())
				jLaunch.saveVMArguments(ReadArguments.readVMArguments(ad.getArgumentsVMFile().getAbsolutePath()));
		}
		else {
			String mainClass;
			String url;
			if(ad.hasCustomMainClass()) {
				mainClass = ad.getCustomMainClass();
				url = PathConformance.packageSensitiveApplicationLocation(ad.getAppFile(), mainClass);
			}
			else {
				mainClass = PathConformance.parseApplicationName(ad.getAppFile().getAbsolutePath());
				url = PathConformance.parseApplicationPath(ad.getAppFile().getAbsolutePath());
			}

			if(!JFCReplayerConfigurationEFS.URL_LIST.isEmpty())
				url += GUITARConstants.CMD_ARGUMENT_SEPARATOR + JFCReplayerConfigurationEFS.URL_LIST;
			ArrayList<String> rmiString = new ArrayList<String>();
			if(!url.isEmpty()) {
				rmiString.addAll(Arrays.asList(new String[]
				{"1099","-u", url,
						"-delay", "" + ApplicationData.openWaitTime,
						"-tcdir", ad.getWorkingTestCaseDirectory().getAbsolutePath(),
						"-resdir", ad.getOutputDirectory().getAbsolutePath(),
						"-noressubdir",
						"-replay"}));
			}
			else {
				rmiString.addAll(Arrays.asList(new String[]
						{"1099","-delay", "" + ApplicationData.openWaitTime,
								"-tcdir", ad.getWorkingTestCaseDirectory().getAbsolutePath(),
								"-resdir", ad.getOutputDirectory().getAbsolutePath(),
								"-noressubdir",
								"-replay"}));
			}
			if(ad.hasArgumentsAppFile()) {
				rmiString.add("-args");
				rmiString.add(ad.getArgumentsAppFile().getAbsolutePath());
			}
			if(ad.hasArgumentsVMFile()) {
				rmiString.add("-vm");
				rmiString.add(ad.getArgumentsVMFile().getAbsolutePath());
			}
			rmiString.add("-g");
			rmiString.add(ad.getWorkingGUIFile().getAbsolutePath());
			rmiString.add("-e");
			rmiString.add(ad.getWorkingEFGFile().getAbsolutePath());
			currentReplay.useRMIReplayTestCase(rmiString.toArray(new String[0]));
		}
		currentReplay.start();
	}

	//////////////
	//////////////
	///CAPTURE////
	//////////////
	//////////////
	/**
	 * jsaddler:
	 * This method will initialize one application monitor,
	 * will initialize this viewController's capture launcher variable,
	 * and will will start the capture launcher.
	 *
	 * The capture sequence can be stopped using the stopCapture method.
	 *
	 * Preconditions: 	none.
	 * Postconditions: 	the capture launcher is initialized
	 * 					the capture sequence has started.
	 */
	public void startCapture(LauncherData ld)
	{
		try {
			if(!ad.outputDirectoryExists()) {
				if(!ad.getOutputDirectory().mkdirs())
					throw new RuntimeException();
			}
		} catch(Exception e) {
			throw new SecurityException("Could not create the results directory at\n" + ad.getOutputDirectory());
		}
		//handle arguments
		jLaunch = new JavaLaunchApplication(ad.getAppFile().getAbsolutePath(), new String[0]);
		JavaCaptureLauncher jcl;
		// pre-configure the main class name if it is special.
		if(!ad.getCustomMainClass().isEmpty())
			jLaunch.setCustomizedMainClass(ad.getCustomMainClass());
		// choose rmi options.
		if(!ld.sendsToRMI()) {
			if(ld.sendsBackRMI())
				jcl = new JavaCaptureLauncher(jLaunch, ld.getSendbackRMIPort(), ApplicationData.openWaitTime);
			else
				jcl = new JavaCaptureLauncher(jLaunch, ApplicationData.openWaitTime);
			if(ad.hasArgumentsAppFile())
				jLaunch.saveAppArguments(ReadArguments.readAppArguments(ad.getArgumentsAppFile().getAbsolutePath()));
			if(ad.hasArgumentsVMFile())
				jLaunch.saveVMArguments(ReadArguments.readVMArguments(ad.getArgumentsVMFile().getAbsolutePath()));
			jcl.setResultsDirectory(ad.getOutputDirectory().getAbsolutePath());
		}
		else {
			if(ld.sendsBackRMI())
				jcl = new JavaCaptureLauncher(jLaunch, ld.getSendbackRMIPort(), ApplicationData.openWaitTime);
			else
				jcl = new JavaCaptureLauncher(jLaunch, ApplicationData.openWaitTime);
			ArrayList<String> rmiString = new ArrayList<String>();


			rmiString.addAll(Arrays.asList(new String[]
					{"1099",
					"-capture",
					"-resdir", ad.getOutputDirectory().getAbsolutePath(),
					"-noressubdir"}));
			if(ad.hasArgumentsAppFile()) {
				rmiString.add("-args");
				rmiString.add(ad.getArgumentsAppFile().getAbsolutePath());
			}
			if(ad.hasArgumentsVMFile()) {
				rmiString.add("-vm");
				rmiString.add(ad.getArgumentsVMFile().getAbsolutePath());
			}
			jcl.useRMICaptureTaskList(rmiString.toArray(new String[0]));
			jcl.setResultsDirectory(ad.getOutputDirectory().getAbsolutePath());
		}

		// start the capture procedures.
		jcl.start();
		currentCapture = jcl;
	}

	public void stopCapture() {
		if(currentCapture != null) {
			currentCapture.stop();
			workingTaskList = currentCapture.constructCapturedTaskList();
		}
	}

	/**
	 * Postconditions: Returns the name of the file written to the file system.
	 * @return
	 */
	public String writeTaskListFile()
	{
		XMLHandler handler = new XMLHandler();
		if(!ad.getOutputDirectory().exists())
			if(!ad.getOutputDirectory().mkdirs())
				throw new IllegalArgumentException("Tasklist Output directory could not be created.");
		handler.writeObjToFile(workingTaskList, ad.getWorkingTaskListFile().getPath());
		return ad.getWorkingTaskListFile().getPath();
	}


	/*
	 * ******
	 * RIPPING
	 * ******
	 */
	public void startRip()
	{
		try {
			if(!ad.outputDirectoryExists()) {
				if(!ad.getOutputDirectory().mkdirs())
					throw new RuntimeException();
			}
		} catch(Exception e) {
			throw new SecurityException("Could not create the results directory at\n" + ad.getOutputDirectory());
		}

		String appString = "Application Name\t: " + PathConformance.parseApplicationName(ad.getAppFile().getPath());
		String pathString = "Path to application\t: " + PathConformance.parseApplicationPath(ad.getAppFile().getPath());
		String constraintsString = "Constraints File\t: " + ad.getWorkingTaskListFile().getPath();
		String rdString = "Results Directory\t: " + ad.getOutputDirectory();

		String rcString;
		if(!ad.hasRipConfigurationFile())
			rcString = "Rip Configuration File: (none)";
		else
			rcString = "Rip Configuration File: " + ad.getRipConfigurationFile();
		String bars = "===========================================";
		System.out.println(bars);
		System.out.println("EventFlowSlicer Rip Launcher");
		System.out.println("  " + appString);
		System.out.println("  " + pathString);
		System.out.println("  " + constraintsString);
		System.out.println("  " + rdString);
		System.out.println("  " + rcString);
		System.out.println(bars);

		currentRip = new JFCRipperEFS();

		if(EventQueue.isDispatchThread()) {
			ripLoop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
			Thread ripThread = new Thread(
				new Runnable() {public void run() {
					// finish what needs to be finished.
//					EventFlowSlicer.initRipperVMChanges();
					ripperArtifact = currentRip.execute();
					// continue working.
					ripLoop.exit();
				}
			}
			);
			ripThread.start();
			ripLoop.enter();
		}
		else
			ripperArtifact = currentRip.execute();
	}


	/**
	 * Helper method to rip models from the app outside of the current java virtual machine,
	 * using a subprocess.
	 * @param waitLoop
	 */
	public void ripOutsideVM(final SecondaryLoop waitLoop)
	{
		// setup the process.

		final ProcessBuilder pb = setupRipProcess(); // Create the process that we need to fire off to begin ripping.
		// print some information

		String appString = "Application Name\t: " + PathConformance.parseApplicationName(ad.getAppFile().getPath());
		String pathString = "Path to application\t: " + PathConformance.parseApplicationPath(ad.getAppFile().getPath());
		String constraintsString = "Constraints File\t: " + ad.getWorkingTaskListFile().getPath();
		String rdString = "Results Directory\t: " + ad.getOutputDirectory();
		String rcString;
		if(!ad.hasRipConfigurationFile())
			rcString = "Rip Configuration File: (none)";
		else
			rcString = "Rip Configuration File: " + ad.getRipConfigurationFile();

		String bars = "===========================================";
		System.out.println(bars);
		System.out.println("EventFlowSlicer Remote Rip Launcher");
		System.out.println("  " + appString);
		System.out.println("  " + pathString);
		System.out.println("  " + constraintsString);
		System.out.println("  " + rdString);
		System.out.println("  " + rcString);
		System.out.println(bars);

		Thread ripThread = new Thread(new Runnable(){
			public void run() {
				try {
					Process p = pb.start(); // start the process
					BufferedReader appOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line;
					while((line = appOutput.readLine()) != null)
						System.out.println("STDOUT: " + line);
					// convert files to their proper format
					int exitCode = p.waitFor();
					endRipProcess(waitLoop);
					System.out.println("EVENTFLOWSLICER: The application under test was closed.");
					if(exitCode != 0)
						System.out.println("Application under test exited with code " + exitCode);

				}
				catch(IOException e) {
					System.err.println("There was an error starting the ripper.");
				}
				catch(InterruptedException e2) {
					System.err.println("The ripping process was interrupted.");
				}
			}
		});
		ripThread.start();
	}

	/**
	 * Using the input constraints file provided, run both the GUI ripper and EFG converter to generate the GUI
	 * and EFG files necessary to run test case generation.
	 *
	 * Preconditions: The results folder must be set prior to a call to this method.
	 * @param inputConstraintsFile
	 */
	public String setRipTasklist()
	{
		String message = "";
		try {
			ad.setDefaultWorkingTaskListFile();
			System.out.println("EVENTFLOWSLICER: Attempting to read post-rip TaskList...");
			XMLHandler handler = new XMLHandler();
			TaskList newConstraints = (TaskList)handler.readObjFromFile(ad.getWorkingTaskListFile(), TaskList.class);
			int oldWidgetCount = workingTaskList.getWidget().size();
			int newWidgetCount = newConstraints.getWidget().size();
			System.out.println("Done.");
			message = "The rip added (" + Math.abs(newWidgetCount - oldWidgetCount) + ") previously hidden events to the constraints file.\n";
			workingTaskList = newConstraints;
		} catch(ClassCastException | NullPointerException e) {
			message = "Rip was not successful: Working tasklist file was not found on file system.";
		}
		return message;
	}

	// create the process builder used for ripping outside the VM.
	private ProcessBuilder setupRipProcess()
	{
		if(!ad.getOutputDirectory().exists())
			if(!ad.getOutputDirectory().mkdirs())
				throw new IllegalArgumentException("Tasklist Output directory could not be created.");

		String mainClass;
		String url;
		if(ad.hasCustomMainClass()) {
			mainClass = ad.getCustomMainClass();
		}
		else {
			mainClass = ad.getAppFile().getAbsolutePath();
		}
		url = JFCRipperConfigurationEFS.URL_LIST;
		String[] finalAppArgs = new String[0];
		String[] vmFileArg = new String[0];
		ArrayList<String> ripperArgs;
		if(!url.isEmpty())
			ripperArgs = new ArrayList<String>(Arrays.asList(new String[]{
				mainClass, 	// class name
				"-normi",	// rip in VM
				"-delay", ""+ApplicationData.openWaitTime, 	// initial delay
				"-constfile", ad.getWorkingTaskListFile().getAbsolutePath(),     // constraints file
				"-u", url, // known classpath URL list
				"-resdir", ad.getOutputDirectory().getAbsolutePath(),
				"-noressubdir"
			}));
		else
			ripperArgs = new ArrayList<String>(Arrays.asList(new String[]{
				mainClass, 		// class name
				"-normi",		// rip in VM
				"-delay", ""+ApplicationData.openWaitTime, 										// initial delay
				"-constfile", ad.getWorkingTaskListFile().getAbsolutePath(),       // constraints file
				"-resdir", ad.getOutputDirectory().getAbsolutePath(),        // output destination
				"-noressubdir"
			}));
		if(ad.hasRipConfigurationFile()) {
			// configuration
			ripperArgs.add("-ripcon"); ripperArgs.add(ad.getRipConfigurationFile().getAbsolutePath());
		}
		if(ad.hasCustomMainClass()) {
			ripperArgs.add("-cmc");
			ripperArgs.add(ad.getCustomMainClass());
		}
		String colonArgs;
		colonArgs = "";
		if(ad.hasArgumentsAppFile()) {
			colonArgs = ReadArguments.colonDelimAppArgumentsFrom(ad.getArgumentsAppFile().getAbsolutePath());
			finalAppArgs = new String[]{"-a", colonArgs};
		}
		if(ad.hasArgumentsVMFile())
			vmFileArg = new String[]{"-vm", ad.getArgumentsVMFile().getAbsolutePath()};

		final ProcessBuilder pb = new ProcessBuilder();
		File currentLoc = new File(EventFlowSlicer.getRunLocation());
		if(currentLoc.isDirectory())
			currentLoc = new File(currentLoc, "efsjava.jar");
		String[] invoke = new String[]{"-jar", currentLoc.getAbsolutePath(), "-rip"};
		ArrayList<String> fullCommand = new ArrayList<String>();
		fullCommand.add(EventFlowSlicer.DEFAULT_JAVA_INVOKE_STRING);
		fullCommand.addAll(Arrays.asList(invoke));
		fullCommand.addAll(ripperArgs);
		fullCommand.addAll(Arrays.asList(finalAppArgs));
		fullCommand.addAll(Arrays.asList(vmFileArg));
		pb.command(fullCommand);
		pb.redirectErrorStream(true);
		pb.redirectError(Redirect.INHERIT);
		return pb;
	}
	/**
	 * After the rip is complete get the working rip tasklist and begin to write the widgets
	 * that were inferred from the rip to the workingTaskList used by this controller.
	 */
	public String modifyWorkingTasklistAfterRip()
	{
		// if we're inferring widgets requiring parameterized actions, modify the input tasklist with the widgets that were inferred.
		workingTaskList = currentRip.getRulesFilter().getTasklist();
		List<Widget> specialParameterized = currentRip.getRulesFilter().getParameterizedWidgets();
		String message = "";
		if(specialParameterized.isEmpty())
			message += "0 actionable widgets were inferred by the rip.";
		else {
			int addedInferred = 0;
			for(Widget w : specialParameterized)
				if(TaskListConformance.indexInTasklist(w, workingTaskList) == -1) {
					addedInferred++;
					workingTaskList.getWidget().add(w);
				}
			message += "(" + addedInferred + ") actionable widgets were added to the tasklist.";
		}
		return message;
	}


	/**
	 * This method is used to complete the rip process when the process is complete. The class that started the rip process
	 * should wait until the waitLoop provided has called the loop's exit method at the end of this procedure
	 * to allow for the ripper to compelete. This method is called only when the ripper is done.
	 * @param GUIStructureOutputLocation
	 * @param waitLoop
	 */
	private void endRipProcess(final SecondaryLoop waitLoop)
	{
		// fetch the GUI from the file system.
		XMLHandler handler = new XMLHandler();
		try {
			ripperArtifact = (GUIStructure) handler.readObjFromFile(ad.getWorkingGUIFile(), GUIStructure.class);
		}
		catch(ClassCastException e) {
			System.err.println("EVENTFLOWSLICER: Ripper artifact not available");
			waitLoop.exit();
			return;
		}
		// create the EFG file.
		EFG efgOutput = null;
		try {
			// use this code to rip the EFG.
			EFSEFGConverter converter = new EFSEFGConverter("JFC");
			try {efgOutput = (EFG)converter.generate(ripperArtifact);
			} catch(InstantiationException e) {
				System.err.println("EVENTFLOWSLICER: EFG was not converted: " + e.getMessage());
				return;
			}
			workingEventFlow = efgOutput;
		}
		catch(Exception e) {
			System.err.println("EFG Creation failed after ripping process was completed.");
		}
		if(workingEventFlow != null) {
			try{
				// bookmarking
				EFG bookmarkedEFG = EFGPacifier.copyOf(workingEventFlow);
				EFGBookmarking bkmk = new EFGBookmarking(bookmarkedEFG, ripperArtifact);
				bookmarkedEFG = bkmk.getBookmarked(true);
				modifyEFGInitials(bookmarkedEFG);
				System.out.println("EVENTFLOWSLICER: Writing completed EFG File to:\n\t" + ad.getWorkingEFGFile().getPath());
				handler.writeObjToFile(workingEventFlow, ad.getWorkingEFGFile().getAbsolutePath());
				System.out.println("EVENTFLOWSLICER: Done.");
			}
			catch(Exception e) {
				System.err.println("Failed Bookmarking test.");
			}
		}

		waitLoop.exit();
	}

	public List<DirectionalPack> getFacetDirectionals()
	{
		return facetDirectionals;
	}

	public void endRipProcess()
	{
		// fetch the GUI from the file system.

		XMLHandler handler = new XMLHandler();
		try {
			ripperArtifact = (GUIStructure) handler.readObjFromFile(ad.getWorkingGUIFile(), GUIStructure.class);
		}
		catch(ClassCastException e) {
			System.err.println("EVENTFLOWSLICER: Ripper artifact not available");
			return;
		}
		// create the EFG file.
		try {
			// use this code to rip the EFG.
			EFSEFGConverter converter = new EFSEFGConverter("JFC");
			EFG efgOutput;
			try {efgOutput = (EFG)converter.generate(ripperArtifact);
			} catch(InstantiationException e) {
				System.err.println("EVENTFLOWSLICER: EFG was not converted: " + e.getMessage());
				return;
			}
			workingEventFlow = efgOutput;

		}
		catch(Exception e) {
			System.err.println("EFG Creation failed after ripping process was completed.");
		}
		try{
			// bookmarking
			if(workingEventFlow != null) {
				EFG bookmarkedEFG = EFGPacifier.copyOf(workingEventFlow);
				EFGBookmarking bkmk = new EFGBookmarking(bookmarkedEFG, ripperArtifact);
				bookmarkedEFG = bkmk.getBookmarked(true);
				modifyEFGInitials(bookmarkedEFG);

				System.out.println("EVENTFLOWSLICER: Writing completed EFG File to:\n\t" + ad.getWorkingEFGFile().getPath());
				handler.writeObjToFile(workingEventFlow, ad.getWorkingEFGFile().getAbsolutePath());
				System.out.println("EVENTFLOWSLICER: Done.");
				System.out.println("EVENTFLOWSLICER: Bookmarking test successful.");
			}
		}
		catch(Exception e) {
			System.err.println("Failed Bookmarking test.");
		}
	}
	public void modifyEFGInitials(EFG bookmarkedEFG)
	{
		if(facetDirectionals != null && !facetDirectionals.isEmpty()) {
			LinkedList<Widget> firsts = new LinkedList<Widget>();
			for(DirectionalPack dp : facetDirectionals) {
				GUIStructureWrapper gsw = new GUIStructureWrapper(ripperArtifact);
				gsw.parseData();
				LocationComparator oc = new LocationComparator(gsw, workingTaskList.getWidget(), bookmarkedEFG.getEvents().getEvent());
				Collection<LinkedList<Widget>> mapped = oc.getMappedWidgets(dp.hovers, dp.isRightDirectional()).values();
				if(!mapped.isEmpty())
					firsts.addAll(mapped.iterator().next());
			}
			Iterator<EventType> bIt = bookmarkedEFG.getEvents().getEvent().iterator();
			List<Widget> newFirsts = TaskListConformance.checkAndSetWidgets(firsts, ripperArtifact, bookmarkedEFG);
			for(EventType et : workingEventFlow.getEvents().getEvent()) {
				EventType nextBE = bIt.next();
				boolean found = false;
				for(int i = 0; i < newFirsts.size() && !found; i++) {
					if(nextBE.getEventId().equals(newFirsts.get(i).getEventID())) {
						et.setInitial(true);
						found = true;
					}
				}
				if(!found)
					et.setInitial(false);
			}
		}

	}

	public String copyTaskListToRipDirectory() throws JAXBException
	{
		// open the file.
//		if(!ad.workingTaskListFileExists())
//			throw new IllegalArgumentException("Constraints tasklist file provided does not exist on the file system\n"
//					+ "\'" + ad.getWorkingTaskListFile() +"\'");
		if(!ad.getOutputDirectory().exists())
			if(!ad.getOutputDirectory().mkdirs())
				throw new IllegalArgumentException("Tasklist Output directory could not be created.");
		// check if this file contains TaskList data
		JAXBContext context = JAXBContext.newInstance("edu.umd.cs.guitar.model.data");
		Unmarshaller um = context.createUnmarshaller();
		Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingTaskListFile()));
		if(!(myFile instanceof TaskList))
			throw new IllegalArgumentException(""+myFile.getClass().getSimpleName());

		workingTaskList = (TaskList)myFile;
		// write the file to this new location
		File ripDir = ad.getOutputDirectory();
		File newTLFile = new File(ripDir, ad.getWorkingTaskListFile().getName());
		XMLHandler handler = new XMLHandler();
		handler.writeObjToFile(workingTaskList, newTLFile.getAbsolutePath());
		ad.setWorkingTaskListFile(newTLFile.getAbsolutePath());
		return newTLFile.getAbsolutePath();
	}

	// Retrieve Artifacts
	public GUIStructure getRipperArtifact()
	{
		return ripperArtifact;
	}

	public TaskList getCaptureArtifact()
	{
		return currentCapture.constructCapturedTaskList();
	}

	public PrintWriter setupGeneratorLogFile()
	{
		try {
			if(!ad.outputGenExtrasDirectoryExists()) {
				if(!ad.getOutputGenExtrasDirectory().mkdirs())
					throw new RuntimeException("Could not create directory:\n"
							+ ad.getOutputDirectory().getPath());
			}
		} catch(Exception e) {
			throw new SecurityException("Could not create the results directory.", e);
		}
		try {
			logFileWriter = new PrintWriter(new FileOutputStream(ad.getOutputExtrasTimeLogFile()), true);
			doLogging = true;
			return logFileWriter;
		} catch(FileNotFoundException e) {
			System.err.println("Log file will not be created in output directory due to file creation errors.");
			return null;
		}
	}

	public void waitForReplayerFinish() throws InterruptedException
	{
		currentReplay.waitForReplayerFinish();
	}
}
