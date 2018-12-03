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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.Task;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.unl.cse.efs.ReplayerLauncher.AlphaOrderComparator;
import edu.unl.cse.efs.app.EventFlowSlicer;
import edu.unl.cse.efs.replay.JFCReplayerEFS;

public class JRTCSelectionOnly extends JavaReplayTestCase 
{
	private final int selectedTask;
	/**
	 * If you have a previous replayer launcher object on hand, you can use it to initialize a JRTCSelection only object.<br><br> 
	 * 
	 * Preconditions: usedController is non-null and is pre-initialized with the main-frame menu widgets to be used during the replay to 
	 * recognize menus in the frames.   
	 */
	public JRTCSelectionOnly(JavaReplayerLauncher repLauncher, JavaLaunchApplication guiLauncher, 
			JavaReplayerController usedController,
			String guiFile, String tcDirectory, String outputDirectory, String[] rmiArguments, int selectedTask) 
	{
		super(repLauncher.getApplicationMonitor(), guiLauncher, guiFile, tcDirectory, outputDirectory, rmiArguments);
		this.selectedTask = selectedTask;
		this.repController = usedController;
	}
	
	/**
	 * If you don't have a previous launcher object, you can use a previous test case oubject instead.<br><br>
	 * 
	 * Preconditions:  usedController is non-null and is pre-initialized with the main-frame menu widgets to be used during the replay process
	 * 				   to recognize menus in the frames. 
	 */
	public JRTCSelectionOnly(JavaReplayTestCase previous, int selectedTask, String... newRMIArguments) 
	{
		super(previous.monitor, previous.guiLauncher, 
				previous.guiFile, previous.tcDirectory, previous.outputDirectory,  
				newRMIArguments.length > 0 ? newRMIArguments : previous.guiLauncher.getRMIArguments()); 
		// decide on rmi arguments depending on whether rmi arguments string is/isnot empty. 
		this.efgFile = previous.efgFile;
		this.selectedTask = selectedTask;
		this.repController = previous.repController;
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
				
				Arrays.sort(children, new AlphaOrderComparator());
				int arraySpot = selectedTask;
				int taskCounter = 1;
				File[] taskChildren = null;
				int i = 0;
				for(i = 0; i<children.length; i++){
					if(!children[i].isDirectory())
						continue;
					taskChildren = children[i].listFiles(
						new FileFilter(){public boolean accept(File f) {return f.isFile() && f.getName().endsWith(".tst") // is this a normal file created by a java application?}
					;}});
					if(taskChildren==null)
						continue;
					if(arraySpot >= taskChildren.length) {
						// the task has to be in a future folder
						// decrease the counter by the children we skipped.
						arraySpot -= taskChildren.length;
						continue;
					}
					break;
				}
				if(i < children.length) {
					Arrays.sort(taskChildren, new AlphaOrderComparator());
					replayTask(children[i], taskCounter, taskChildren[arraySpot], arraySpot+1);
				}
				activated.release();
				System.out.println("Finished replaying all testcases\n "
						+ "for subdirectories of test case directory:\n"
						+  tcDirectory);
				initiateCoolDown();
			}// end loop through children of folder else
		}// end run in vm
	}
	
	public void replayTask(File parentTask, int realTaskNumber, File childMethod, int realMethodNumber)
	{
		String fileName = this.tcDirectory +  File.separator + parentTask.getName() + File.separator + childMethod.getName();
		System.out.println("JavaReplayTestCase: Replaying file: " + fileName);
		taskName = fileName;
		//extract the file extension (make sure it is  a tst file)
		int mid= fileName.lastIndexOf(".");
		String ext=fileName.substring(mid+1,fileName.length());

		// get the test case from this test case directory
		if(ext.equals("tst")){
			//create a directory for the image results
//			File resultsDir = new File(fileName + "_images");
			String parentDir = outputDirectory + File.separator + parentTask.getName() + "_images";
			File resultsDir = new File(parentDir, childMethod.getName() + "_images");
			resultsDir.mkdirs();
			
			repController.setApplicationMonitor(monitor); 

//			String results = fileName + "_images" + File.separator; 
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
			
			// set the replayer controller to point its image directory to the results folder. 
//			repController.setImageDir(results);
			// LAUNCH THE REPLAYER
			launchReplayer(realTaskNumber);
			
		}// end detect .tst
	}
	
	public class AlphaOrderComparator implements Comparator<File>
	{
		public int compare(File a, File b) 
		{
			String name1 = a.getName(); 
			String name2 = b.getName();
			return name1.compareTo(name2);
		}
	}
}
