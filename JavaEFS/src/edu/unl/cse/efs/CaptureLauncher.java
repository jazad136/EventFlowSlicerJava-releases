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
