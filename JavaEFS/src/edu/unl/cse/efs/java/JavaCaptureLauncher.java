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
import java.util.Arrays;
import java.util.Collection;

import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.unl.cse.efs.CaptureLauncher;
import edu.unl.cse.efs.capture.java.JavaStepType;

/**
 * This class is responsible for launching the monitor that
 * will capture the actions executed on the specified java
 * application. It acts as a controller for the capture test case that launches
 * and captures information from java capture, and a mediator between the CaptureTestCase objects and other
 * interested parties.
 * 
 * Methods here start new capture test cases, stop old ones, and finally retrieve the methods
 * that they generate.
 * 
 * @author Jonathan Saddler
 */
public class JavaCaptureLauncher extends CaptureLauncher{
 
	
	private JavaLaunchApplication javaLaunch;
	private boolean serverCaseOpen;
	private int openDelay;
	private String resultsDirectoryPath;
	
	/**
	 * Constructor accepting an application, and open delay. Capture back is set to off. RMI capture
	 * set to off. 
	 */
	public JavaCaptureLauncher(JavaLaunchApplication launch, int openDelay) {
		super(launch);
		this.openDelay = openDelay;
		javaLaunch = launch;
		captureSupervisor = new JavaCaptureTaskList(javaLaunch, openDelay);
		captureSupervisor.setCaptureBackOnPort("");
		resultsDirectoryPath = "";
	}
	
	
	/**
	 * Constructor accepting an application, a captureBack port, and an openDelay. Capture back is set to on if
	 * captureBackPort is non-empty and non-null, rmi capture is set to off.  
	 */
	public JavaCaptureLauncher(JavaLaunchApplication launch, String captureBackPort, int openDelay)
	{
		super(launch);
		javaLaunch = launch;
		this.openDelay = openDelay;
		captureSupervisor = new JavaCaptureTaskList(javaLaunch, openDelay);
		captureSupervisor.setCaptureBackOnPort(captureBackPort);
		resultsDirectoryPath = "";
	}
	
	/**
	 * 
	 * @param resultsDirectoryPath
	 */
	public void setResultsDirectory(String resultsDirectoryPath)
	{
		if(resultsDirectoryPath != null)
			this.resultsDirectoryPath = resultsDirectoryPath;	
	}
	/**
	 * Set the capture launcher to capture over RMI, using arguments specified by rmiArgs. 
	 * 
	 * Preconditions: constructor was called with a valid java launch application
	 * Postconditions: Capture launcher will launch capture in RMI mode. 
	 */
	public void useRMICaptureTaskList(String... rmiArgs)
	{
		if(rmiArgs.length > 0) {
			captureSupervisor = new JavaCaptureTaskList(javaLaunch, openDelay, rmiArgs);
			serverCaseOpen = true;
		}
		else 
			throw new RuntimeException("Capture launcher \"useRMICapture\" was called without any references to rmi arguments.\n"
					+ "At least the port argument is required");
	}
	
	/**
	 * Set the capture launcher to not capture over RMI.  
	 * Preconditions:	constructor was called with a valid java launch application
	 * Postconditions:  Capture launcher will launch capture in non-RMI (host VM) mode.
	 */
	public void dontUseRMICaptureTestCase()
	{
		captureSupervisor = new JavaCaptureTaskList(javaLaunch, openDelay);
		serverCaseOpen = false;
	}
	
	/** 
	 * Initialize and start the testCase based on whether a previous RMI instance has been instantiated or not.
	 * If it has, start the instance.
	 */
	@Override
	public void start()
	{	
		// print information;
		String bars = "===========================================";
		System.out.println(bars);
		String title;
		if(serverCaseOpen) 
			title = "EventFlowSlicer Remote Capture Launcher\n";
		else
			title = "EventFlowSlicer Capture Launcher\n";
		String rdString = "Results Directory\t: " + resultsDirectoryPath;
		String appString = "Application Name\t: " + javaLaunch.getAppName();
		String pathString = "Path to application\t: " + javaLaunch.getPath();
		String typeString = "Type\t\t\t: Undefined";
		if(javaLaunch.launchesClass()) typeString = "Type\t\t\t: Class file";
		else if(javaLaunch.launchesJar()) typeString = "Type\t\t\t: Jar file";
		
		System.out.println(title);
		System.out.println("  " + rdString);
		System.out.println("  " + appString);
		System.out.println("  " + pathString);
		System.out.println("  " + typeString);
		// detect jar or class file
				
		System.out.print("  Arguments: \t\t");
		String[] args = javaLaunch.getAppArguments();
		if(args.length == 0) 
			System.out.println("(none)");
		else
			System.out.println(Arrays.deepToString(args));
		System.out.println(bars);	
		captureSupervisor.start();
	}

	/**
	 * Stop the captureSupervisor thread asynchronously, and return the method containing task
	 * steps collected by this captureSupervisor object.  
	 */
	@Override
	public void stop() 
	{
		captureSupervisor.interrupt();
		captureSupervisor.endAndGetTaskList();
	}
	
	/**
	 * Acquires a semaphore that doesn't return until this capture sequence has been terminated.
	 * 
	 * Preconditions: 	When called, the replayer is progressing through a replay sequence.
	 * Postconditions: 	The replayer has finished an instance of a replay sequence. 
	 */
	public void waitForCaptureFinish() throws InterruptedException
	{
		((JavaCaptureTaskList)captureSupervisor).activated.acquire();
		((JavaCaptureTaskList)captureSupervisor).activated.release();
		captureSupervisor.join();
	}
	
	public TaskList constructCapturedTaskList()
	{
		Collection<JavaStepType> allSaved = ((JavaCaptureTaskList)captureSupervisor).getSavedSteps();
		if(allSaved.isEmpty())
			return (new ObjectFactory()).createTaskList();
		return JavaCaptureUtils.getTranslatedTaskList(allSaved);
	}
}
