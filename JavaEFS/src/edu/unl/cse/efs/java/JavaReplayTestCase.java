package edu.unl.cse.efs.java;

import static edu.unl.cse.efs.view.EventFlowSlicerErrors.errorOut;

import java.awt.AWTEvent;
import java.io.File;
import java.io.FileFilter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.Task;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.umd.cs.guitar.replayer.JFCReplayerConfiguration;
import edu.unl.cse.efs.ReplayerLauncher.AlphaOrderComparator;
import edu.unl.cse.efs.app.EventFlowSlicer;
import edu.unl.cse.efs.commun.giveevents.NetCommunication;
import edu.unl.cse.efs.replay.JFCReplayerEFS;

public class JavaReplayTestCase extends Thread implements NetCommunication
{
	public boolean runAsServer, captureInVM;
	protected final String tcDirectory;
	protected final String outputDirectory;
	protected final String guiFile;

	protected final JavaLaunchApplication guiLauncher;
	protected final JavaApplicationMonitor monitor;
//	protected final JavaCogToolModelFactory factory;

	protected JavaReplayerController repController;
	protected String taskName;
	protected String efgFile;
	protected String replayBackPort;
	protected Task currentTask;
	protected boolean replayBack;
	protected boolean inferMethodsOn;
	// used by other classes
	NetCommunication networkStub;
	final Semaphore activated;
	// used by superclasses only
//	private String stateCompareFile;

	public JavaReplayTestCase(JavaApplicationMonitor jmon,  JavaLaunchApplication guiLauncher,
			String guiFile, String tcDirectory, String outputDirectory, String... rmiArguments)
	{
		this.guiLauncher = guiLauncher;
		this.captureInVM = guiLauncher.runInVM;
		this.runAsServer = !captureInVM;
		this.monitor = jmon;
		this.outputDirectory = outputDirectory;
		this.tcDirectory = tcDirectory;
		this.guiFile = guiFile;

		if(rmiArguments.length != 0) {
			runAsServer = true;
			guiLauncher.useRMI(rmiArguments, this);
		}
		else {
			runAsServer = false;
			guiLauncher.dontUseRMI();
		}
		replayBack = false;
		inferMethodsOn = false;
		replayBackPort = "";
		activated = new Semaphore(1);
		try {activated.acquire();}
		catch(InterruptedException e) {
			throw new RuntimeException("ReplayTestCase: Replayer Sequence was interrupted during initialization.");
		}
//		stateCompareFile = "";
	}
	public void setEFGFile(String efgFilename)
	{
		this.efgFile = efgFilename;
	}
//	public void setStateComparisonConfiguration(String stateConfigurationFile)
//	{
//		this.stateCompareFile = stateConfigurationFile;
//	}
//	/**
//	 * Turn on the method inference functionality for this replay test case object.
//	 * By default method inference is turned off.
//	 * @param on
//	 */
//	public void setInferMethods(boolean on)
//	{
//		inferMethodsOn = on;
//	}

	class ReplayExceptions implements Thread.UncaughtExceptionHandler
	{
		public void uncaughtException(Thread t, Throwable e)
		{
			System.out.println();
			initiateCoolDown();
			errorOut(e);
		}
	}

	/**
	 * Set a variable that determines whether this test case will send its results over the network, using
	 * the number represented in portArgument as the port to use.
	 * Also specify whether we want to receive a preferences file over the connection
	 * using the specified capture back port. If no port argument is specified,
	 * the getPreferences parameter is not used.
	 *
	 * Preconditions: 	portArgument is not null.
	 * Postconditions: 	All replayBack related parameters are properly set.
	 * @param on
	 */
	public void setReplayBackOnPort(String portArgument)
	{
		if(!portArgument.isEmpty()) {
			replayBack = true;
			replayBackPort = portArgument;
		}
		else {
			replayBack = false;
			replayBackPort = "";
		}
	}

	public void launchReplayer(int testNumber)
	{
		System.out.println("JavaReplayTestCase: Launching the Java Replayer. Begin Test Case " + testNumber);
		JFCReplayerEFS replayer = new JFCReplayerEFS();

		// set main class, guiFile, output file, and task name.

		String gui = "resources" + File.separator + "gui" + File.separator + "GUITAR-Default-" + guiLauncher.getAppName() + ".GUI";
		String efg = "resources" + File.separator + "gui" + File.separator + "GUITAR-Default-" + guiLauncher.getAppName() + ".EFG";
		boolean runWithoutGUI;
		if(guiFile == null || guiFile.isEmpty()
		|| efgFile == null || efgFile.isEmpty())
			runWithoutGUI = true;
		else {
			 runWithoutGUI = false;
			 gui = guiFile;
			 efg = efgFile;
		}

		if(guiLauncher.launchesClass())
			JFCReplayerConfiguration.setBasicAttributes(guiLauncher.getAppName(), gui, efg, taskName);
		else {
			String entrance = guiLauncher.getPath() + guiLauncher.getAppName() + guiLauncher.getExtension();
			JFCReplayerConfiguration.setBasicAttributes(entrance, gui, efg, taskName);
		}

		boolean usePauseMonitor = false;


		JFCReplayerConfiguration.setAppDefaults(guiLauncher.getAppURLs(), guiLauncher.launchesJar(), usePauseMonitor, runWithoutGUI);
		JFCReplayerConfiguration.setWaitTimes(7000, 250);
		replayer.setCgHelper(repController);
		try {
			replayer.execute();
		} catch (Exception e) {
			errorOut(e);
		}
		System.out.println("\n---\n"
				+ "JavaReplayTestCase: Replayer has finished replaying \n"
				+ "Test Case " + testNumber + ".\n---");
	}

	public void run()
	{
		if(runAsServer) {
			Thread appThread = new Thread(guiLauncher);
			appThread.setUncaughtExceptionHandler(new ReplayExceptions());
			appThread.start();
			System.out.println("\n>\t Now starting the application, please wait up to 10 seconds for application to settle...");
		}
		else {
			Thread.currentThread().setUncaughtExceptionHandler(new ReplayExceptions());
			if(replayBack)
				networkStub = EventFlowSlicer.beginRMISession(false, replayBackPort);

			//obtain a list of all the test cases in the directory

//			File dir = new File(this.tcDirectory);
//			File[] children = dir.listFiles();
//			// if output directory does not exist
//			if(children==null) {
//				System.err.println("Output directory does not exist....Exiting task analysis...");
//				return;
//			}

			File dir = new File(this.tcDirectory);
			File[] children = dir.listFiles();
			ArrayList<File> childArr = new ArrayList<File>(Arrays.asList(children));
			// sort these into alphabetical order
			Collections.sort(childArr, new AlphaOrderComparator());
			// add dir at the beginning
			childArr.add(0, dir);
			// create an array of all these directories.
			children = childArr.toArray(new File[0]);
			// if output directory does not exist
			if(children==null) {
				System.err.println("Output directory does not exist....Exiting task analysis...");
				return;
			}
			else{
				// jsaddler: at this point in this method:
				// 1) the application was started
				// 2) the model factory is loaded with a saved copy of the menu widgets
				// 3) the application is about to be restarted.
//
//			OLD
//			ArrayList<File> childArr = new ArrayList<File>(Arrays.asList(children));
//			// sort these into alphabetical order
//			Collections.sort(childArr, new AlphaOrderComparator());
//			// add dir at the beginning
//			childArr.add(0, dir);
//			// create an array of all these directories.
//			children = childArr.toArray(new File[0]);
//			// create a new replayer controller. This starts a new instance of the application.
			// NEW
				Arrays.sort(children, new AlphaOrderComparator());
				int taskCounter = 1;
				File[] taskChildren = null;
				int i;
				for(i = 0; i<children.length; i++) {
					if(!children[i].isDirectory())
						continue;
					taskChildren = children[i].listFiles(
						new FileFilter(){public boolean accept(File f) {return f.isFile() && f.getName().endsWith(".tst") // is this a normal file created by a java application?}
					;}});
					if(taskChildren==null || taskChildren.length == 0)
						continue;

					Arrays.sort(taskChildren, new AlphaOrderComparator());
					for(int j = 0; j < taskChildren.length; j++) {
						replayTask(children[i], taskCounter, taskChildren[j], j+1);
						activated.release();
						initiateCoolDown();
					}
				}
				// end multiple task files in one folder loop
				//infer additional methods
			}// end multiple task folders in demonstration loop
			if(inferMethodsOn)
				System.out.println("Running Inferred methods algorithm...");

		}// end run in vm
	}

public void replayTask(File parentTask, int realTaskNumber, File childMethod, int realMethodNumber)
{
	String fileName = this.tcDirectory +  File.separator + parentTask.getName() + File.separator + childMethod.getName();
	System.out.println("JavaReplayTestCase: Replaying file: " + childMethod.getName());
	taskName = fileName;
	//extract the file extension (make sure it is  a tst file)
	int mid= fileName.lastIndexOf(".");
	String ext=fileName.substring(mid+1,fileName.length());

	// get the test case from this test case directory
	if(ext.equals("tst")){
		//create a directory for the image results
//		File resultsDir = new File(fileName + "_images");
		String parentDir = outputDirectory + File.separator + parentTask.getName() + "_images";
		File resultsDir = new File(parentDir, childMethod.getName() + "_images");
		resultsDir.mkdirs();
		repController = new JavaReplayerController(monitor);
		repController.setApplicationMonitor(monitor);

//		String results = fileName + "_images" + File.separator;
		repController.setImageDir(parentDir + File.separator + childMethod.getName() + "_images" + File.separator);
		// reads a the task file and test case file from the results folder.
		// to their global variables.
		Object taskObj = null;
		XMLHandler handler= new XMLHandler();
		try{
			taskObj = handler.readObjFromFile(fileName, Task.class);
			if(taskObj instanceof TestCase) {
				TestCase ourTC = (TestCase)taskObj;
				taskObj = JFCReplayerEFS.mapTestCaseToTask(ourTC);
			}
			currentTask = (Task)taskObj;
		}
		catch(Exception e){
			System.err.println("\nTask file ould not be loaded to task object. \n" +
					e.getClass() + ":" + e.getMessage());
		}
		repController.initializeWindows();
		launchReplayer(realTaskNumber);

	}// end detect .tst
}
	/**
	 * Close the instances of the application still open. This method is meant to be called after the replayer
	 * finishes its final task.
	 */
	public void initiateCoolDown()
	{
		// delegate the parent process to shut this one down.
		if(replayBack && networkStub != null) {
			try {
				networkStub.shutMeDown();
			} catch(RemoteException e) {
				throw new RuntimeException("JavaReplayTestCase: Tried to initiate shutdown over RMI but failed.");
			}
		}
		else {
			if(monitor != null) {
				monitor.closeApp();
			}
		}
	}

	public void setReplayerController(JavaReplayerController newReplayerController)
	{
		repController = newReplayerController;
	}
	@Override
	public void gotEvent(AWTEvent nextEvent, String eventID, String windowName, String componentRoleName)
			throws RemoteException
	{
		// does nothing in a replayTestCase
	}

	@Override
	public void gotKeyEvent(String[] keyData, char keyChar, String eventID, String windowName, String componentRoleName)
			throws RemoteException {
		// does nothing in a replayTestCase (for now)

	}

	@Override
	public void gotListEvent(String eventID, String windowName)
			throws RemoteException
	{
		// does nothing in a replayTestCase
	}

	@Override
	public void gotMenuItemEvent(String[][] components, String windowName)
			throws RemoteException
	{
		// does nothing in a replayTestCase
	}

	@Override
	public void gotComboSelectEvent(String eventID, String windowName, List<Integer> selection)
			throws RemoteException
	{
		// does nothing in a replayTestCase
	}

	@Override
	public void flushTextItems() throws RemoteException
	{
		// does nothing in a replayTestCase
	}

	@Override
	public void flushListItems(List<Integer> itemNumbers) throws RemoteException
	{
		// does nothing in a replayTestCase
	}

	@Override
	public void gotPageTabEvent(String eventID, String windowName, String tabData)
	{
		// does nothing in a replayTestCase
	}
	@Override
	public void shutMeDown() throws RemoteException
	{
		System.out.println("Shutting down other application...");
		initiateCoolDown();
		boolean ended = EventFlowSlicer.endRMISession(false, guiLauncher.getRMIRegistryPort());
		Thread.interrupted();
		if(!ended)
			System.err.println("JavaReplayTestCase: App Replay may still be in session.");
		else
			System.out.println("JavaReplayTestCase: Other app shutdown is fully complete.");
	}
	@Override
	public void gotHoverEvent(String componentID, String componentRoleName, String windowName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void gotWindowCloseEvent(String componentID, String windowName) throws RemoteException {
		// TODO Auto-generated method stub

	}

}
