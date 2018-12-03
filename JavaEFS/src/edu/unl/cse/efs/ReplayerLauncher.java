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
package edu.unl.cse.efs;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

//import org.kohsuke.args4j.CmdLineException;
//import org.kohsuke.args4j.CmdLineParser;

import java.util.concurrent.Semaphore;

import edu.umd.cs.guitar.model.data.TestCase;
import edu.umd.cs.guitar.replayer.GReplayerConfiguration;
import edu.unl.cse.efs.java.JavaLaunchApplication;
import edu.unl.cse.efs.replay.ApplicationMonitor;
import edu.unl.cse.efs.replay.ReplayerController;
import edu.unl.cse.efs.tools.ArrayTools;
import edu.umd.cs.guitar.model.data.Task;

public abstract class ReplayerLauncher extends Thread {
	
	/**
	 * Implement this class to launch the correct replayer for the application
	 * @author Amanda Swearngin
	 */
	protected LaunchApplication launcher; 
//	protected String gui; 
//	protected String output; 
//	protected String outputCGT;
//	protected String outputCSV;
//	protected String tcDirectory;
//	protected GReplayerConfiguration config;
	public abstract ApplicationMonitor getApplicationMonitor(); 
	public abstract GReplayerConfiguration getReplayerConfiguration();
	protected ApplicationData ad;
	protected LauncherData ld;
	protected ReplayerController replayerController; 
	protected TestCase testCase; 
	protected Task task;
	public static enum Type{ALL, MENU_EXTRACTION, BASED_ON_SELECTION};
	protected Type launchType;
	public Semaphore activated;
	public abstract void launchReplayer(); //launch the app specific replayer
	public final LinkedList<Integer> selected;
	protected boolean[] replayRange;
	/** How many test cases are we going to replay. **/
	protected int replayMax;
	protected boolean serverCaseOpen;
	
	public ReplayerLauncher(LaunchApplication launch, ApplicationData ad, LauncherData ld){
		this.launcher = launch;
		this.ad = ad;
		this.ld = ld;
		activated = new Semaphore(1);
		try {
			activated.acquire();
		} catch(InterruptedException e) {
			throw new RuntimeException("ReplayerLauncher: Replayer Sequence was interrupted during initialization.");
		}
		selected = new LinkedList<Integer>(Arrays.asList(new Integer[]{-1}));
		serverCaseOpen = false;
		this.launchType = Type.ALL;
		
		if(!ld.launchSelectionArguments.isEmpty()) {
			String selArg = ld.launchSelectionArguments;
			// end goal set the JRL's replay range. 
			boolean[][] inclusionMatrix = ArrayTools.getFilledArrayFor(selArg);
			switch(ArrayTools.bibleNotationType(selArg, false)) {
					case 0: setReplayRange(inclusionMatrix[0]);
			break;	case 1: setReplayRange(inclusionMatrix[0]);
			break;  case 2: setReplayRange(inclusionMatrix);
			}
		}
			
	}
	public ReplayerLauncher(JavaLaunchApplication javaLaunch, ApplicationData ad, LauncherData ld, Type replayerType)
	{
		this(javaLaunch, ad, ld);
		this.launchType = replayerType;
	}
	
	public void waitForReplayerFinish() throws InterruptedException
	{
		activated.acquire();
		activated.release();
		this.join();
	}

	/**
	 * Set the range of test cases in the test case files pointed to by tcDirectory's subdirectory structure
	 * to be replayed using a boolean inclusion array. The index i of cell containing a value of 'true' in replayOn,
	 * corresponds to a single test case in tcDirectory that will be replayed when this launcher is fired up, and
	 * the next cell following represents the next test case that can be found on the file system
	 * in alphabetical order within the tcDirectory directory.<br><br>
	 * 
	 * Preconditions: replayOn is non-null;
	 * @param replayOn
	 */
	public void setReplayRange(boolean[] replayOn)
	{
		selected.clear();
		replayMax = 0;
		
		int rangeSize = replayOn.length;
		replayRange = new boolean[rangeSize];
		for(int i = 0; i < rangeSize; i++) {
			replayRange[i] = replayOn[i];
			if(replayOn[i]) {
				selected.add(i + 1);
				replayMax++;
			}
		}
		launchType = Type.BASED_ON_SELECTION;
	}
	
	/**
	 * Set the range of test cases in the test case files pointed to by tcDirectory's subdirectory structure
	 * to be replayed using a two-dimensional boolean inclusion array.<br> 
	 * Both the range and depth of each subarray in the parameter 
	 * are used to calculate the number of files that we will search through 
	 * to find which ones to replay, i.e. this launcher's replayRange and replayMax.<br><br>
	 * 
	 * Preconditions: secondIntermediate is a non-null boolean 2D array. 
	 * @param secondIntermediate
	 */
	public void setReplayRange(boolean[][] secondIntermediate)
	{
		// count up the size of the full range.
		selected.clear();
		replayMax = 0;
		
		// need to find out how many elements are in the folders we're looking after
		int rangeSize = 0;
		int sISize = secondIntermediate.length;
		File dir = ad.getWorkingTestCaseDirectory();
		File[] tcDirs = dir.listFiles();
		Arrays.sort(tcDirs, new AlphaOrderComparator());
		for(int i = 0; i < sISize; i++) {
			File[] inside = tcDirs[i].listFiles(
					new FileFilter(){public boolean accept(File f) {
						return f.isFile() && f.getName().endsWith(".tst"); // is this a normal file created by a java application?}
					}});
			rangeSize += inside.length;
			// ensure that from now on we account for all the files in every directory we find
			// (up to and not beyond directories we need to match in second intermediate)
			secondIntermediate[i] = ArrayTools.extendRange(inside.length, secondIntermediate[i]);
		}
		// find out how many elements need to be replayed.
		replayRange = new boolean[rangeSize];
		int rangeNext = 0;
		for(boolean[] r : secondIntermediate) {
			for(int j = 0; j < r.length; j++) {
				
				replayRange[rangeNext + j] = r[j]; // true value in the matrix = true value in the array.
				if(r[j]) {
					selected.add(rangeNext + j + 1);
					replayMax++; // a true value indicates more to replay.
				}
			}
			rangeNext += r.length;
		}
		launchType = Type.BASED_ON_SELECTION;
	}
	/**
	 * From start to finish, this method launches the replayer initialization sequence, 
	 * and carries out the replay process.
	 * 
	 * Preconditions: Output directory tcDirectory exists. 
	 * Postconditions: the replayer launchReplayer method called by the subclass of this class is called.
	 */
	public abstract void launch();
	
	public static class AlphaOrderComparator implements Comparator<File>
	{
		public int compare(File a, File b) 
		{
			String name1 = a.getName(); 
			String name2 = b.getName();
			return name1.compareTo(name2);
		}
	}
}


