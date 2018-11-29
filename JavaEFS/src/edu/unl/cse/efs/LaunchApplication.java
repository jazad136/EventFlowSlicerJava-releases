package edu.unl.cse.efs;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import edu.unl.cse.efs.commun.giveevents.NetCommunication;

public abstract class LaunchApplication implements Runnable {
	/**
	 * Implement this class for launching and initializing preferences for each application type
	 * @author Amanda Swearngin
	 */
	protected String appName; 
	protected String path; 
	protected String[] applicationArguments;
	/** variable to indicate that the application has been started and is running, false if not running **/
	public boolean started;
	
	public boolean runInVM;
	protected String[] rmiArguments;
	protected NetCommunication networkStub;
	
	public LaunchApplication(String applicationPath, String applicationName)
	{
		appName = applicationName; 
		path = applicationPath;
		started = false;
		runInVM = true;
		rmiArguments = new String[0];
		applicationArguments = new String[0];
		networkStub = null;
	}
	
	public String[] getRMIArguments()
	{
		return rmiArguments;
	}
	
	/**
	 * Sets the arguments that will be passed to the java application this LaunchApplication refers to. 
	 * CogTool-Helper supports the passing of arguments to the java application currently instanted via the constructor. 
	 * Arguments will be passed in the order they are presented in argumentVector. Typically these arguments follow
	 * the invocation string of the application in question. 
	 * 
	 * Preconditions: 	none
	 * Postconditions: 	The arguments that will be passed in order after a call to the application's invocation string. 
	 */
	public void saveAppArguments(String[] argumentVector)
	{
		applicationArguments = new String[argumentVector.length];
		for(int i = 0; i < argumentVector.length; i++) 
			applicationArguments[i] = argumentVector[i];
	}
	
	/**
	 * Retrieve the list of the arguments that will be pased to the java application currently instantiated via the constructor.
	 * @return
	 */
	public String[] getAppArguments()
	{
		String[] toReturn = new String[applicationArguments.length];
		for(int i = 0; i < toReturn.length; i++)
			toReturn[i] = applicationArguments[i];
		return toReturn;
	}
	
	public String putProcessOnTheRegistry()
	{
		String name = "CTHCapture";
		String portArgument;
		if(rmiArguments.length > 0) 
			portArgument = rmiArguments[0];
		else
			portArgument = "1099";
		
		Registry registry;
		
		// create registry, if one is not already created.
		try {
			LocateRegistry.createRegistry(Integer.parseInt(portArgument));
		} catch(RemoteException e) {
			System.err.println("JavaCaptureTestCase: Tried to create registry reference and failed. A registry instance might already exist");
		}
		
		// create stub.
		try {networkStub = (NetCommunication)UnicastRemoteObject.exportObject(networkStub, Integer.parseInt(portArgument));} 
		catch(RemoteException e) {
			throw new RuntimeException("Tried to generate stub to put on registry but failed \n" + e);
		}
		
		// get registry again. 
		try {registry = LocateRegistry.getRegistry();}
		catch(RemoteException e) {
			throw new RuntimeException("Tried to locate registry but failed \n" + e);
		}
		// bind stub to a name. 
		try{registry.rebind(name, networkStub);}
		catch(RemoteException e) {
			throw new RuntimeException("Tried to bind the resource to a registry name but failed\n" + e);
		}
		return "\n> JCTCOpener service is live.\n";
	}
	
	/*
	 * Getters and Setters
	 */
	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}



	
	
	/**
	 * Launch the selected application in a new thread
	 */
	public abstract void run(); 

	/**
	 * Close all application instances
	 */
	public abstract void closeApplicationInstances(); 

	/**
	 * Restore the user profile
	 */
	public abstract void restoreProfile(); 


	/**
	 * Initialize the user profile
	 */
	public abstract void initProfile(); 

	/**
	 * Restart the application
	 */
	public void restart()
	{	
		closeApplicationInstances(); 
		restoreProfile(); 
		initProfile(); 
		run();
	}

}
