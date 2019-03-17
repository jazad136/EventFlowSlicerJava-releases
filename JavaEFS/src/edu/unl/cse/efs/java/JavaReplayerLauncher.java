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
package edu.unl.cse.efs.java;

import static edu.unl.cse.efs.view.EventFlowSlicerErrors.errorOut;

import java.awt.AWTEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.guitar.replayer.*;
import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.LauncherData;
import edu.unl.cse.efs.ReplayerLauncher;
import edu.unl.cse.efs.commun.giveevents.NetCommunication;

public class JavaReplayerLauncher extends ReplayerLauncher implements NetCommunication
{
	/**
	 * Launch the replayer CTH driver for Java applications
	 * @author Amanda Swearngin, Jonathan Saddler
	 */
	private JavaLaunchApplication javaLaunch;
	private JavaReplayTestCase replaySupervisor;
	private JavaReplayerController javaRepController;
	private ApplicationData ad;
	private LauncherData ld;
	private JavaApplicationMonitor monitorActive;
	private JFCReplayerConfiguration repConfigActive;
	private boolean serverCaseOpen;

	/**
	 * Constructor for the JavaReplayerLauncher.
	 * Preconditions: 	testCaseDirectory is the directory to look for the test cases to rn
	 * 					outputXML points to the destination where the primary xml output from the replay should go
	 * 					outputCGT points to the destination where the CGT output from the replay should go or empty if CGT output is not desired.
	 * 					outputCSV points to the destination where the CSV output from the replay should go or empty if CSV output is not desired.
	 * 					CGT and CSV output parameters are both mutually nonempty, or mutually empty.
	 * 					guiFile points to the guiFile the GUITAR framework should utilize if any.
	 * 					appLauncher is a java application launcher that was properly initialized.
	 * Postconditions: 	Launcher is instantiated and ready to properly launch its replay thread.
	 * 					Replay back is set to off.
	 * 					RMI replay is according to the javaLaunch launch configuration.
	 */
	public JavaReplayerLauncher(JavaLaunchApplication javaLaunch, ApplicationData ad, LauncherData ld)
	{
		super(javaLaunch, ad, ld);
		this.javaLaunch = javaLaunch;
		this.ad = ad;
		this.ld = ld;
		monitorActive = getApplicationMonitor();
		repConfigActive = getReplayerConfiguration();
		replaySupervisor = new JavaReplayTestCase(monitorActive, javaLaunch,
				ad.getWorkingGUIFile().getAbsolutePath(),
				ad.getWorkingTestCaseDirectory().getAbsolutePath(),
				ad.getOutputDirectory().getAbsolutePath());
		replaySupervisor.setEFGFile(ad.getWorkingEFGFile().getAbsolutePath());

	}


	public void setEFGFile(String efgFilename)
	{
		replaySupervisor.setEFGFile(efgFilename);
	}

	class ReplayLaunchExceptions implements Thread.UncaughtExceptionHandler
	{
		public void uncaughtException(Thread t, Throwable e)
		{
			System.out.println("Caught exception in replayer launcher: " + e.getClass());
			if(javaLaunch.started)
				javaLaunch.closeApplicationInstances();
			errorOut(e);
		}
	}
	/**
	 * Instruct this test case to utilize RMI replay when running its replay thread.
	 * Preconditions: rmiArgs is a sequence of strings, containing at least one argument for the port to replay over.
	 * Postconditions: RMI replay is set to on.
	 */
	public JavaReplayTestCase useRMIReplayTestCase(String... rmiArgs)
	{
		if(rmiArgs.length > 0) {
			replaySupervisor = new JavaReplayTestCase(monitorActive, javaLaunch,
					ad.getWorkingGUIFile().getAbsolutePath(),
					ad.getWorkingTestCaseDirectory().getAbsolutePath(),
					ad.getOutputDirectory().getAbsolutePath(), rmiArgs);
			serverCaseOpen = true;
		}
		else
			throw new RuntimeException("ReplayerLauncher \"useRMIReplay\" was called without any references to rmi arguments.\n"
					+ "At least port argument is required to be passed via [rmiArgs].");
		return replaySupervisor;
	}

	public void dontUseRMIReplayTestCase()
	{
		replaySupervisor = new JavaReplayTestCase(monitorActive, javaLaunch,
				ad.getWorkingGUIFile().getAbsolutePath(),
				ad.getWorkingTestCaseDirectory().getAbsolutePath(),
				ad.getOutputDirectory().getAbsolutePath());
		serverCaseOpen = false;
	}

	/**
	 * Launch the java replay test case thread.
	 */
	public void run()
	{
		launch();
	}

	/**
	 * Return a string representation of items specified to this replayer launcher object as selected entities to be
	 * replayed.
	 */
	public String selectedString()
	{
		if(selected.isEmpty())
			return "";
		else if(selected.contains(-1))
			return "(none)";
		String toReturn = "(";
		Iterator<Integer> selIt = selected.iterator();
		toReturn += selIt.next();
		while(selIt.hasNext())
			toReturn += ", " + selIt.next();
		return toReturn + ")";
	}
	/**
	 * Start the ReplayerLauncher sequence. This method should only be called as a consequence
	 * of running this thread.
	 *
	 * Preconditions: 	This method was called from a thread running separately from the main thread.
	 * 					The thread running this code is not running alongside any other thread running the same code.
	 * Postconditions: 	The replayer startup banner will show in the console.
	 * 					The log from the replayer sequence will show in the console.
	 * 					The replayer sequence will complete if uninterrupted by the user or by bugs in the
	 * 					AUT program or the user's .tst file.
	 */



	public static String defaultMenuExtractionOutputFileName()
	{
		return "menu-extr";
	}


	public static String defaultFrameWidgetsOutputFileName()
	{
		return "frames";
	}

	public static String defaultDesignOutputFileName()
	{
		return "project";
	}

	public static class TestCaseFilter implements FileFilter
	{
		public boolean accept(File f)
		{
			// is this a normal file created by a java application?}
			return f.isFile() && f.getName().endsWith(".tst");
		}
	}

	public static class TaskDirectoryFilter implements FileFilter
	{
		public boolean accept(File f)
		{
			return f.isDirectory();
		}
	}
	public void launch()
	{
		// --- HEADER VARIABLE CONFIGURATION ---
		String tcDirectoryString = ad.getWorkingTestCaseDirectory().getPath();
		String outputDirectoryString = ad.getOutputDirectory().getPath();
		String appString = launcher.getAppName();
		// GUI FILE
		String guiString;

		if(!ad.hasWorkingGUIFile())
			guiString = "(no gui resource)";
		else
			guiString = ad.getWorkingGUIFile().getPath();
		String[] argsArr = launcher.getAppArguments();
		String argsString = "";
		if(argsArr.length > 0)
			argsString += Arrays.deepToString(argsArr);

		String bars = "===========================================";
		if(launchType == Type.ALL) {

			// -- FULL REPLAY: PRINT VARIABLES AND START --
			String repPrint = "";
			String titleString;
			if(serverCaseOpen)
				titleString = "EventFlowSlicer Remote Replayer Launcher: Full Procedure";
			else
				titleString = "EventFlowSlicer Replayer Launcher: Full Procedure";

			// PRINT THE REPLAYER INFORMATION
			repPrint += bars;
			repPrint += "\n" + titleString;

			repPrint += "\n  Testcase Directory:\t" + tcDirectoryString;
			repPrint += "\n  Application:\t\t" 		+ appString;
			repPrint += "\n  GUI File:\t\t" 		+ guiString;
			repPrint += !argsString.isEmpty() ? ("\n  Arguments:\t\t" 		+ argsString) : "";
			repPrint += "\n  Output Directory:\t" 	+ outputDirectoryString;
			repPrint += "\n" + bars;
			System.out.println(repPrint);
			if(serverCaseOpen) {
				// REPLAY STEP
				//obtain a list of all the test cases in the directory

				File dir = ad.getWorkingTestCaseDirectory();

				// first get a list of all subdirectories of dir.
				File[] children = dir.listFiles(new TaskDirectoryFilter());
				if(children==null) {
					System.err.println("Output directory cannot be read ....Exiting task analysis...");
					return;
				}
				ArrayList<File> childArr = new ArrayList<File>(Arrays.asList(children));
				// sort these into alphabetical order
				Collections.sort(childArr, new AlphaOrderComparator());
				// add dir at the beginning
				childArr.add(0, dir);
				// create an array of all these directories.
				children = childArr.toArray(new File[0]);

				// create a new replayer controller, and initialize the design.
				// jsaddler: at this point in this method:
				// 1) the application was started
				// 2) the model factory is loaded with a saved copy of the menu widgets
				// 3) the application is about to be restarted.
//				Arrays.sort(children, new AlphaOrderComparator());
				int taskCounter = 0;
				int fileCounter = 0;
				javaRepController = new JavaReplayerController(monitorActive);
				replayerController = javaRepController;
				replaySupervisor.setReplayerController(javaRepController);
				// for every child directory.
				int count = 0;
				Iterator<File> taskIt = childArr.iterator();

				while(taskIt.hasNext()) {
					File[] taskChildren = taskIt.next().listFiles(new TestCaseFilter());
					if(taskChildren==null)
						taskIt.remove();
					else
						count += taskChildren.length;
				}
				replayMax = count;
				children = childArr.toArray(new File[0]);
				for(int i=0; i<children.length; i++) {
					File[] taskChildren = children[i].listFiles(new TestCaseFilter());
					if(taskChildren==null)
						continue;

					Arrays.sort(taskChildren, new AlphaOrderComparator());
					javaRepController.setTaskName(children[i].getName());

					// for every valid child file.
					try {for(int j=0; j < taskChildren.length; j++, fileCounter++) {

						String taskOutput = bars;
						taskOutput += "\n" + titleString;
						taskOutput += "\n  Replaying Method " + (fileCounter+1) + " (# " + (taskCounter+1) + " of " + replayMax + ")";
						taskOutput += "\n  File: \t" + taskChildren[j].getName();
						taskOutput += "\n  Test Case Directory: \t"+ tcDirectoryString;
						taskOutput += "\n  Output Directory: \t" + outputDirectoryString;
						taskOutput += "\n" + bars;
						System.out.println(taskOutput);

						String[] currentArgs = javaLaunch.getRMIArguments();
						ArrayList<String> newArgs = new ArrayList<String>(Arrays.asList(currentArgs));
						int aIndex = newArgs.indexOf("-replay_sel");
						while(aIndex != -1) {
							newArgs.remove(aIndex+1);
							newArgs.remove(aIndex);
							aIndex = newArgs.indexOf("-replay_sel");
						}
						aIndex = newArgs.indexOf("-replay");
						while(aIndex != -1) {
							newArgs.remove(aIndex);
							aIndex = newArgs.indexOf("-replay");
						}
						// select the next task.
						newArgs.add("-replay_sel");
						newArgs.add(""+(fileCounter+1));
						JRTCSelectionOnly se = new JRTCSelectionOnly(replaySupervisor, fileCounter+1, newArgs.toArray(new String[0]));
						replaySupervisor = se;
						replaySupervisor.start();
						if(taskCounter == 0)
							activated.release();
						taskCounter++;
						waitForSubReplayerCompletion();
						// finish the thread before continuing.
						replaySupervisor.join();
					}} catch(InterruptedException e) {
						monitorActive.closeApp();
						activated.release();
						throw new RuntimeException("ReplayerLauncher: Replayer task was interrupted before completion.\n"
								+ fileCounter + " tasks were successfully completed.");
					}
				}// end search through folders loop.

				// uh oh, we didn't replay any test cases.
				if(taskCounter == 0) {
					System.out.println("No methods were replayed: perhaps you did not specify a task folder containing method subfolders?");
					activated.release();
				}
			} // end servercaseopen branch
			else {
				replaySupervisor.setReplayBackOnPort(ld.getSendbackRMIPort());
				replaySupervisor.start();
				activated.release();
			}
		}
		else if(launchType == Type.BASED_ON_SELECTION) {

			// SELECT TEST CASE REPLAY - PRINT AND START
			bars += "==="; // lengthen the standard bar for this special subcase
			String titleString;
			if(serverCaseOpen)
				titleString = "CogTool-Helper Remote Replayer Launcher: Select Test Cases\n";
			else
				titleString = "CogTool-Helper Replayer Launcher: Select Test Cases \n";

			String outputString = bars + "\n";
			outputString += titleString;

			outputString += "\n  Selected Methods:\t" + selectedString();
			outputString += "\n  Test Case Directory:\t"+ tcDirectoryString;
			outputString += "\n  Output Directory:\t" + outputDirectoryString;
			outputString += "\n  Application:\t\t" + appString;
			outputString += "\n  GUI File: \t\t" + guiString;
			outputString += "\n  Arguments string:\t" + argsString;
			outputString += "\n" + bars;
			System.out.println(outputString);

			//obtain a list of all the test cases in the directory
			File dir = ad.getWorkingTestCaseDirectory();
			File[] children = dir.listFiles();
			if(children==null) { // if output directory does not exist
				System.err.println("Output directory does not exist....Exiting task analysis...");
				return;
			}

			// create a new replayer controller, and initialize the design.
			ArrayList<File> childArr = new ArrayList<File>(Arrays.asList(children));
			// sort these into alphabetical order
			Collections.sort(childArr, new AlphaOrderComparator());
			// add dir at the beginning
			childArr.add(0, dir);
			// create an array of all these directories.
			children = childArr.toArray(new File[0]);

			int taskCounter = 0;
			int fileCounter = 0;
			javaRepController = new JavaReplayerController(monitorActive);
			replayerController = javaRepController;
			replaySupervisor.setReplayerController(javaRepController);
			for(int i=0; i<children.length && taskCounter < replayMax; i++){
				File[] taskChildren = children[i].listFiles(
						new FileFilter(){public boolean accept(File f) {
							return f.isFile() && f.getName().endsWith(".tst"); // is this a normal file created by a java application?}
						}});
				if(taskChildren==null)
					continue;

				Arrays.sort(taskChildren, new AlphaOrderComparator());
				javaRepController.setTaskName(children[i].getName());

				try {
					for(int j=0; j<taskChildren.length && taskCounter < replayMax; j++, fileCounter++)
						if(replayRange[fileCounter]) {
							if(serverCaseOpen)  {
								String taskOutput = bars;
								taskOutput += "\n" + titleString;
								taskOutput += "\n  Replaying Method " + (fileCounter+1) + " (# " + (taskCounter+1) + " of " + replayMax + ")";
								taskOutput += "\n  File: " + taskChildren[j].getName();
								taskOutput += "\n  Test Case Directory: \t"+ tcDirectoryString;
								taskOutput += "\n" + bars;
								System.out.println(taskOutput);

								String[] currentArgs = javaLaunch.getRMIArguments();
								ArrayList<String> newArgs = new ArrayList<String>(Arrays.asList(currentArgs));
								int aIndex = newArgs.indexOf("-replay_sel");
								while(aIndex != -1) {
									newArgs.remove(aIndex+1);
									newArgs.remove(aIndex);
									aIndex = newArgs.indexOf("-replay_sel");
								}
								aIndex = newArgs.indexOf("-replay");
								while(aIndex != -1) {
									newArgs.remove(aIndex);
									aIndex = newArgs.indexOf("-replay");
								}
								// select the next task
								newArgs.add("-replay_sel");
								newArgs.add(""+(fileCounter+1));
								JRTCSelectionOnly se = new JRTCSelectionOnly(replaySupervisor, fileCounter+1, newArgs.toArray(new String[0]));

								replaySupervisor = se;
								replaySupervisor.start();
								if(taskCounter == 0)
									activated.release();
								taskCounter++;
								waitForSubReplayerCompletion();
							}
							else {
								String taskOutput = bars;
								taskOutput += "\n" + titleString;
								taskOutput += "\n  Replaying Method " + (fileCounter+1) + " (# " + (taskCounter+1) + " of " + replayMax + ")";
								taskOutput += "\n  File: " + taskChildren[j].getName();
								taskOutput += "\n  Test Case Directory: \t"+ tcDirectoryString;
								taskOutput += "\n" + bars;
								System.out.println(taskOutput);
								if(taskCounter == 0 || !serverCaseOpen)
									javaRepController.initializeWindows();


								JRTCSelectionOnly se = new JRTCSelectionOnly(replaySupervisor, fileCounter);
								se.setReplayBackOnPort(ld.getSendbackRMIPort());
								replaySupervisor = se;
								replaySupervisor.start(); // start the next thread.
								if(taskCounter == 0)
									activated.release(); // release the activated semaphore if we've activated one replay
								taskCounter++; // increment the task counter
								replaySupervisor.join(); // wait for the last thread to end before starting a new thread.
							}
						}// end selected test case find file within folder loop.
				} catch(InterruptedException e) {
					monitorActive.closeApp();
					activated.release();
					replaySupervisor.activated.release();
					throw new RuntimeException("ReplayerLauncher: Replayer task was interrupted before completion.\n"
						+ fileCounter + " tasks were successfully completed.");
				}
			} // end test case replay loop
			if(taskCounter == 0) {// we didn't replay any test cases.
				System.out.println("No methods were replayed: perhaps you did not specify a task folder containing method subfolders?");
			}
			replaySupervisor.activated.release();
			activated.release();
		}
	}
	/**
	 * In the case of JavaReplayerLauncher, this method does nothing because
	 * this step is carried out in the JavaReplayTestCase object, invoked when the run method is invoked.
	 */
	public void launchReplayer()
	{
		// this method does nothing because this step is carried out in replaySupervisor.
	}

	/**
	 * Return a new application monitor instantiated using the launch application passed to this replayer launcher's constructor.
	 */
	public JavaApplicationMonitor getApplicationMonitor()
	{
		if(monitorActive == null)
			monitorActive = new JavaApplicationMonitor(javaLaunch);
		return monitorActive;
	}

	/**
	 * Returns a new Java Replayer configuration instance.
	 */
	public JFCReplayerConfiguration getReplayerConfiguration()
	{
		if(repConfigActive == null)
			repConfigActive = new JFCReplayerConfiguration();
		return repConfigActive;
	}

	/**
	 * Acquires a semaphore that doesn't return until this replayer has finished its replay sequence.
	 *
	 * Preconditions: 	When called, the replayer is progressing through a replay sequence.
	 * Postconditions: 	The replayer has finished an instance of a replay sequence.
	 */
	@Override
	public void waitForReplayerFinish() throws InterruptedException
	{
		activated.acquire();
		activated.release();
		replaySupervisor.join();
		this.join();
	}

	/**
	 * Do not wait for this thread to complete, but wait for a replayer that launched from this replayer to complete
	 * before returning.
	 */
	private void waitForSubReplayerCompletion() throws InterruptedException
	{
		replaySupervisor.activated.acquire();
		replaySupervisor.activated.release();
		replaySupervisor.join();
	}


	public JavaReplayerController getNewReplayerController()
	{
		return new JavaReplayerController(monitorActive);
	}

	public void gotEvent(AWTEvent nextEvent, String eventID, String windowName, String componentRoleName) throws java.rmi.RemoteException
	{

	}
	public void gotListEvent(String eventID, String windowName) throws java.rmi.RemoteException
	{

	}
	public void gotMenuItemEvent(String[][] components, String windowName) throws java.rmi.RemoteException
	{

	}
	public void gotComboSelectEvent(String eventID, String windowName, List<Integer> selection) throws java.rmi.RemoteException
	{

	}
	public void gotPageTabEvent(String eventID, String windowName, String tabData) throws java.rmi.RemoteException
	{

	}
	public void flushTextItems() throws java.rmi.RemoteException
	{

	}
	public void flushListItems(List<Integer> itemNumbers) throws java.rmi.RemoteException
	{

	}
	public void shutMeDown() throws java.rmi.RemoteException
	{
		try {
		replaySupervisor.shutMeDown();
		}catch(Exception e) {

		}
	}

	@Override
	public void gotKeyEvent(String[] keyData, char keyChar, String eventID, String windowName, String componentRoleName)
			throws RemoteException {
		// TODO Auto-generated method stub
	}
}
