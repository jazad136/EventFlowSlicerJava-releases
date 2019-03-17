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

import java.util.concurrent.Semaphore;

import edu.umd.cs.guitar.model.data.TaskList;

public abstract class CaptureLauncher {
	/**
	 * Launch the capture tool for an application
	 * @author Amanda Swearngin
	 * @author Jonathan Saddler
	 */
	 
	protected LaunchApplication launcher; 
	protected Semaphore activated;
	protected CaptureTestCase captureSupervisor;
	
	/**
	 * Instantiate the capture tool with only an application launcher instance. 
	 * @param launch
	 */
	public CaptureLauncher(LaunchApplication launch)
	{
		launcher = launch;
		activated = new Semaphore(1);
	}
	
	/**
	 * Wait for the capture launcher to finish its business before continuing. This method
	 * does not return until the capture has ended. 
	 * @throws InterruptedException
	 */
	public abstract void waitForCaptureFinish() throws InterruptedException;
	
	/**
	 * Ensure that a future call to the start method invokes a capture test case that communicates over RMI.  
	 */
	public abstract void useRMICaptureTaskList(String... rmiArgs);
	
	public abstract void dontUseRMICaptureTestCase();
	/**
	 * Launch capture
	 */
	public abstract void start(); 
	
	public abstract TaskList constructCapturedTaskList();
	
	/**
	 * End end
	 */
	public abstract void stop(); 
}
