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
package edu.unl.cse.efs.replay;

import java.util.List;

import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.replayer.GReplayerMonitor;
import edu.umd.cs.guitar.replayer.monitor.GTestMonitor;
import edu.unl.cse.efs.LaunchApplication;

public abstract class ApplicationMonitor {
	/**
	 * Abstract class to provide functions for obtaining information from the application, and opening up new 
	 * windows and closing them
	 * @author Amanda Swearngin
	 */
	protected LaunchApplication launcher; 
	protected GReplayerMonitor replayerMonitor; 
	protected GTestMonitor testMonitor; 
	
	public ApplicationMonitor(LaunchApplication launch){
		launcher = launch;
	}
	
	/**
	 * Get all of the top level windows
	 * @return - List<GWindow>
	 */
	public abstract List<GWindow> getAllTopWindows();
	
	/**
	 * Get all of the root windows 
	 * @return List<GWindow>
	 */
	public abstract List<GWindow> getRootWindows();
	
	/**
	 * REturn the current GUIStructure state of the application
	 * @return GUIStructure
	 */
	public abstract GUIStructure getCurrentState(); 


	/**
	 * Load a new instance of the application
	 */
	public abstract void startApp();

	/**
	 * Close the most recently opened instance of the application
	 */
	public abstract void closeApp();
	
	/**
	 * Restart the application monitor
	 */
	public abstract void restart();

	/**
	 * Get the replayer monitor
	 * @return GReplayerMonitor
	 */
	public abstract GReplayerMonitor getReplayerMonitor();

	public static void captureScreenState(int captureNumber,String path) {
		//Sleep  
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// Ignore.
		}

		//Take the screenshot
		CaptureScreenImage capture = new CaptureScreenImage();
		try {
			capture.captureScreen(path + Integer.toString(captureNumber) + ".jpg");
		} catch (Exception e) {
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   
	}
}
