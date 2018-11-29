package edu.unl.cse.efs.java;

import java.util.*;
import java.util.List;
import java.io.*;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.JFCApplication2;
import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.LaunchApplication;
import edu.unl.cse.efs.app.EventFlowSlicer;
import edu.unl.cse.efs.util.ReadArguments;
import edu.unl.cse.efs.view.EventFlowSlicerView;
import edu.unl.cse.jontools.paths.PathConformance;
import edu.unl.cse.efs.commun.giveevents.NetCommunication;

/**
 * This class that can readily be used to launch a Java application on the fly.
 * The process maintained by this class has to be maintained asynchronously, i.e.
 * The process will not close on its own. It must be closed by a call to 
 * closeApplicationInstances(). This class has the added ability to return all the 
 * windows related to this application, and keeps track of windows opened before
 * this one. 
 * 
 * JavaLaunchApplication will manage the connection from the CogTool-Helper 
 * control application to the RMI-connected application.
 * 
 * @author Amanda Swearngin, Jonathan Saddler
 */

public class JavaLaunchApplication extends LaunchApplication implements Runnable {
	
	public Process myProcess;
	private String extensionString;
	private String customizedMainClass;
	private String invocationMainClass; 
	private String[] classpathURLs;
	private Set<Window> unrelatedWindows;
	private JFCApplication2 targetJavaApp;
	private String[] javaVirtualMachineArguments;
	private boolean registryObjectLive;
	private String rmiRegistryPort;
	private String javaCommand;
	private HashMap<String, String> oldSystemProperties;
	
	
	/**
	 * Default constructor. Run the application specified in the VM upon a call to this Launcher's thread, using the arguments specified. 
	 */
	public JavaLaunchApplication(String applicationPath, String applicationName, String extension, String[] applicationArgs)
	{
		super(applicationPath, applicationName);
		extensionString = extension;
		unrelatedWindows = new HashSet<Window>();
		started = false;
		runInVM = true;
		saveAppArguments(applicationArgs);
		saveVMArguments(new String[0]);
		registryObjectLive = false;
		javaCommand = EventFlowSlicer.DEFAULT_JAVA_INVOKE_STRING;
//		setJavaTerminalCommand("/System/Library/Frameworks/JavaVM.framework/Versions/1.5/Commands/java");
		oldSystemProperties = new HashMap<>();
		customizedMainClass = "";
		invocationMainClass = "";
	}
	
	/**
	 * Convenience constructor. Utilize the pieces of the raw path provided for the path to the application, the name of the
	 * object, and the extension string defining how to treat the program. Run the application specified in the VM 
	 * upon instantiation of this Launcher's thread, using the arguments provided. 
	 */
	public JavaLaunchApplication(String rawPathToApp, String[] applicationArgs)
	{
		this(PathConformance.parseApplicationPath(rawPathToApp), 
			PathConformance.parseApplicationName(rawPathToApp),
			PathConformance.parseApplicationExtension(rawPathToApp), 
			applicationArgs);
	}
	
	/**
	 * Setup the JavaLaunchApplication to use a customized main class name. 
	 * It is always interpreted as a class name that respects the package
	 * structure of the place where the class can be found on the file system.
	 * Rather than just calling "java appName", we may need to call java "thepackage.appName."
	 * to invoke the main class.  
	 * @param mainClassName
	 */
	public void setCustomizedMainClass(String mainClassName)
	{
		this.customizedMainClass = mainClassName;
	}
	
	/**
	 * Public method to change the command used to invoke the java runtime environment. 
	 * @param invokeCommand
	 */
	public void setJavaTerminalCommand(String invokeCommand)
	{
		javaCommand = invokeCommand;
	}
	
	/**
	 * Public method to reset the command used to invoke the java runtime environment to its
	 * default value of DEFAULT_JAVA_INVOKE_STRING.
	 */
	public void setJavaTerminalCommandToDefault()
	{
		javaCommand = EventFlowSlicer.DEFAULT_JAVA_INVOKE_STRING;
	}
	
	/**
	 * Sets the arguments that will be passed to the java application this LaunchApplication refers to. 
	 * CogTool-Helper supports the passing of arguments to the java application currently instanted via the constructor. 
	 * Arguments will be passed in the order they are presented in argumentVector. Typically these arguments FOLLOW
	 * the invocation string of the application in question. 
	 * 
	 * Preconditions: none
	 * Postconditions: 	The arguments that will be passed in order after a call to the application's invocation string. 
	 * 					 
	 * @param argumentVector
	 */
	public void saveVMArguments(String[] argumentVector)
	{
		javaVirtualMachineArguments = new String[argumentVector.length];
		for(int i = 0; i < argumentVector.length; i++)
			javaVirtualMachineArguments[i] = argumentVector[i];
	}
	
	
	/**
	 * A call to ensure that, when launching this thread, we attempt to fork a child process and run the application under test
	 * run via RMI. 
	 * @param rmiArgs
	 * @param netStub
	 */
	public void useRMI(String[] rmiArgs, NetCommunication netStub)
	{
		runInVM = false;
		this.rmiArguments = Arrays.copyOf(rmiArgs, rmiArgs.length);
		this.networkStub = netStub;
	}
	
	/**
	 * A call to ensure that, when launching this thread, we attempt to run the application under test 
	 * in the current virtual machine.
	 */
	public void dontUseRMI()
	{
		runInVM = true;
		this.rmiArguments = new String[0];
		this.networkStub = null;
	}
	
	public String[] getAppURLs()
	{
		return classpathURLs;
	}
	/**
	 * Return the extension string of this application.
	 * 
	 * Preconditions:	none.
	 * Postconditions: 	the extension string of this java file is returned. 
	 * @return
	 */
	public String getExtension()
	{
		return extensionString;
	}
	
	/**
	 * return the JFCApplication associated with this JavaLaunchApplication. 
	 * @return
	 */
	public JFCApplication2 getAppAbstraction()
	{
		return targetJavaApp;
	}
	
	/**
	 * Return the string representing the absolute path to this application fully reconstructed from its components. 
	 */
	public String fullAppFilename()
	{
		return path + appName + extensionString;
	}
	
	/**
	 * Return the fullyQualifiedApplicationName of the application that this
	 * JavaLaunchApplication will launch by. 
	 * Searches through the list of classpathLocations searching for the one path
	 * that is a substring of the application path. 
	 * When we find the substring, take the fullClassName, and remove the overlap
	 * between the fullClassName and this special classpath location. 
	 * Return the resulting substring
	 * @param classpathLocations
	 * @return
	 */
//	public String packageQualifiedApplication
	public String packageQualifiedApplicationName(List<String> classpathLocations)
	{
		String packageClassName = "";
		String fullClassName = path + appName;
		for(int i = 0; i < classpathLocations.size(); i++)
			if(fullClassName.contains(classpathLocations.get(i))) {
				packageClassName = classpathLocations.get(i);
				break;
			}
		
		// if the package className is empty;
		if(packageClassName.isEmpty())
			return appName;
		String pClassName = fullClassName.substring(packageClassName.length()); // trim off the beginning of the full class string. 
		if(pClassName.startsWith(File.separator)) // trim off a hanging separator if present. 
			pClassName = pClassName.substring(1);
		return pClassName;
	}
	
	public static String packageQualifiedApplicationName(String path, String appName, List<String> classpathLocations)
	{
		String packageClassName = "";
		String fullClassName = path + appName;
		for(int i = 0; i < classpathLocations.size(); i++)
			if(fullClassName.contains(classpathLocations.get(i))) {
				packageClassName = classpathLocations.get(i);
				break;
			}
		
		// if the package className is empty;
		if(packageClassName.isEmpty())
			return appName;
		String pClassName = fullClassName.substring(packageClassName.length()); // trim off the beginning of the full class string. 
		if(pClassName.startsWith(File.separator)) // trim off a hanging separator if present. 
			pClassName = pClassName.substring(1);
		return pClassName;
	}
	
	public static void undoImitatedVMPropertyChanges(HashMap<String, String> oldSystemProperties)
	{
		for(Map.Entry<String, String> me : oldSystemProperties.entrySet()) 
			System.setProperty(me.getKey(), me.getValue());
	}
	public void undoImitatedVMPropertyChanges()
	{
		for(Map.Entry<String, String> me : oldSystemProperties.entrySet()) 
			System.setProperty(me.getKey(), me.getValue());
	}
	public static HashMap<String, String> imitateVMPropertyChanges(String[] javaVirtualMachineArguments)
	{
		HashMap<String, String> oldSystemProperties = new HashMap<String, String>();
		for(int i = 0; i < javaVirtualMachineArguments.length; i++) {
			if(javaVirtualMachineArguments[i].equalsIgnoreCase("-cp")) 
			try {
				String currentCP = System.getProperty("java.class.path");
				ArrayList<String> currentCPParts = new ArrayList<String>(Arrays.asList(currentCP.split(File.pathSeparator)));
				int addIndex = currentCPParts.size();
				try {
					String target = javaVirtualMachineArguments[i+1];
					String[] parts = target.split(File.pathSeparator);
					for(String s : parts) {
						boolean add = true;
						for(String cps : currentCPParts) 
							if(s.equals(cps))
								add = false;
						if(add)
							currentCPParts.add(s);
					}
				} catch(Exception e) {
					System.err.println("An Invalid Java System Property string "
							+ "was provided as a java virtual machine argument to the current java application.");
					continue;
				}
			
				// construct the new classpath from added parts. 
				for(int j = addIndex; j < currentCPParts.size(); j++) 
					currentCP += File.pathSeparator + currentCPParts.get(j);
				
				System.setProperty("java.class.path", currentCP);
			} catch(Exception e) {
				System.err.println("The System class path property java.class.path could not be retrieved"
						+ " or could not be changed.");
			}
			
			else if(javaVirtualMachineArguments[i].startsWith("-D")) {
				String target = javaVirtualMachineArguments[i], name = "", property = "";
				try {
					String[] parts = target.split("=");
					name = parts[0].substring(2);
					property = parts[1];
				} catch(Exception e) {
					System.err.println("An Invalid Java System Property string "
							+ "was provided as a java virtual machine argument to the current java application.");
				}
				try {
					boolean oldPropertyWasPresent = System.getProperty(name) != null;
					System.setProperty(name, property);
					if(oldPropertyWasPresent)
						oldSystemProperties.put(name, property);
				} 
				catch(Exception e) {
					System.err.println("The VM property \"" + target + "\" specified property could not be set");
				}
			}
		}
		return oldSystemProperties;
	}
	/**
	 * Changes system properties depending on the arguments passed to this JavaLaunchApplication 
	 * for arguments that begin with "-D".  
	 */
	public void imitateVMPropertyChanges()
	{
		for(int i = 0; i < javaVirtualMachineArguments.length; i++) {
			if(javaVirtualMachineArguments[i].equalsIgnoreCase("-cp")) 
			try {
				String currentCP = System.getProperty("java.class.path");
				ArrayList<String> currentCPParts = new ArrayList<String>(Arrays.asList(currentCP.split(File.pathSeparator)));
				int addIndex = currentCPParts.size();
				try {
					String target = javaVirtualMachineArguments[i+1];
					String[] parts = target.split(File.pathSeparator);
					for(String s : parts) {
						boolean add = true;
						for(String cps : currentCPParts) 
							if(s.equals(cps))
								add = false;
						if(add)
							currentCPParts.add(s);
					}
				} catch(Exception e) {
					System.err.println("An Invalid Java System Property string "
							+ "was provided as a java virtual machine argument to the current java application.");
					continue;
				}
			
				// construct the new classpath from added parts. 
				for(int j = addIndex; j < currentCPParts.size(); j++) 
					currentCP += File.pathSeparator + currentCPParts.get(j);
				
				System.setProperty("java.class.path", currentCP);
			} catch(Exception e) {
				System.err.println("The System class path property java.class.path could not be retrieved"
						+ " or could not be changed.");
			}
			
			else if(javaVirtualMachineArguments[i].startsWith("-D")) {
				String target = javaVirtualMachineArguments[i], name = "", property = "";
				try {
					String[] parts = target.split("=");
					name = parts[0].substring(2);
					property = parts[1];
				} catch(Exception e) {
					System.err.println("An Invalid Java System Property string "
							+ "was provided as a java virtual machine argument to the current java application.");
				}
				try {
					boolean oldPropertyWasPresent = System.getProperty(name) != null;
					System.setProperty(name, property);
					if(oldPropertyWasPresent)
						oldSystemProperties.put(name, property);
				} 
				catch(Exception e) {
					System.err.println("The VM property \"" + target + "\" specified property could not be set");
				}
			}
		}
	}
	public static String[] getCPUrlsList(String[] javaVirtualMachineArguments)
	{
		int cpArg = argsContains("-cp", javaVirtualMachineArguments);
		if(cpArg != -1) {
			String[] cpArguments = javaVirtualMachineArguments[cpArg+1].split(File.pathSeparator);
			return cpArguments;
		}
		return new String[0];
	}
	/**
	 * Called when we wish to run the application in the currently running virtual machine. 
	 */
	public void runAppInHostVM()
	{
		// save currently open windows as "unrelated" windows.
		saveCurrentWindowsAsUnrelated();
		// start a new java application using the given file name and type.
		LinkedList<String> places = new LinkedList<String>();
	
		if(launchesJar()) {
			places.add(path);
			places.add(fullAppFilename());
		}
		else if(launchesClass()) {
			places.add(path);
		}
		
		// utilize javaVirtualMachine arguments to manipulate the classpath. 
		int cpArg = argsContains("-cp", javaVirtualMachineArguments);
		if(cpArg != -1) {
			places.clear();
			String[] cpArguments = javaVirtualMachineArguments[cpArg+1].split(File.pathSeparator);
			places.addAll(Arrays.asList(cpArguments));
		}
		imitateVMPropertyChanges();
		
		if(!customizedMainClass.isEmpty())
			invocationMainClass = customizedMainClass;
		else if(launchesClass())
			invocationMainClass = packageQualifiedApplicationName(places);
		String[] URLs;
		
		try {
			URLs = JFCApplication2.convertToURLStrings(places.toArray(new String[0]));
		} catch(MalformedURLException e) {
			throw new RuntimeException("ERROR when launching Java Application. String was provided in paths \n"
					+ "that could not be parsed.");
		}
		classpathURLs = URLs;
		
		if(launchesJar()) {
			try {
				String entrance = path + appName + extensionString;
				if(!invocationMainClass.isEmpty()) {
//					targetJavaApp = new JFCApplication2(path + appName + extensionString, path + appName + extensionString, true, URLs);
					targetJavaApp = new JFCApplication2(invocationMainClass, entrance, true, URLs);
				}
				else {
//					targetJavaApp = new JFCApplication2(path + appName + extensionString, path + appName + extensionString, true, URLs);
					targetJavaApp = new JFCApplication2("", entrance, true, URLs);
				}
				System.out.println("LOCATION: " + targetJavaApp.getAppPathViaCodeSource());
				targetJavaApp.connect(applicationArguments);
			} 
			catch(ClassNotFoundException e) {
				throw new RuntimeException("ERROR when launching Java Application:\n"
						+ "Could not derive main class to run within jar file provided.", e);
			}
			catch(IOException e) {
				throw new RuntimeException("ERROR when launching Java Application:\n"
						+ "Could not read target jar file.", e);
			}
		}
		else if(launchesClass()) {
			try {
				String entrance;
				entrance = path + appName;
				if(!invocationMainClass.isEmpty()) 
					targetJavaApp = new JFCApplication2(invocationMainClass, entrance, false, URLs);
				else {
					invocationMainClass = entrance;
					targetJavaApp = new JFCApplication2(entrance, entrance, false, URLs);
				}
				
				System.out.println("LOCATION: " + targetJavaApp.getAppPathViaCodeSource());
				targetJavaApp.connect(applicationArguments);
			}
			catch(Exception e) {
				throw new RuntimeException("ERROR when launching Java Application:\n"
						+ "Could not launch class file.", e);			
			}
		}
		System.setOut(EventFlowSlicer.originalOut);
		System.setErr(EventFlowSlicer.originalErr);
	}
	
	public static int argsContains(String argument, String[] argumentsList)
	{
		for(int i = 0; i < argumentsList.length; i++) 
			if(argumentsList[i].equals(argument))
				return i;
		return -1;
	}
	public void runAppViaRMI()
	{
		String message = putProcessOnTheRegistry();
		System.out.println(message);
		
		ProcessBuilder pBuilder = new ProcessBuilder();
//		String currentDir = System.getProperty("user.dir") + File.separator;
		File currentLoc = new File(EventFlowSlicer.getRunLocation());
		if(currentLoc.isDirectory())
			currentLoc = new File(currentLoc, "efsjava.jar");
		InputStream appErrors = null;
		BufferedReader appOutput = null;
		
		// redirect the error stream from the sub application. 
		pBuilder.redirectErrorStream(true);
		pBuilder.redirectError(Redirect.INHERIT);
		
		// set up the file path. 
		String invoke;
		if(!launchesJar() && !customizedMainClass.isEmpty()) {
			pBuilder = pBuilder.directory(null);
			invoke = path + appName + extensionString;
		}
		else {
			pBuilder = pBuilder.directory(null);
			invoke = path + appName + extensionString;	
		}

		ArrayList<String> realRMIArgs = new ArrayList<String>();
		realRMIArgs.add("-sendback");
		String vmArgsFile = "";
		for(int i = 0; i < rmiArguments.length; i++) {
			if(rmiArguments[i].equals("-vm")) { // this argument here will override the currently specified vm arguments
				realRMIArgs.add(rmiArguments[i]);
				i++;
				vmArgsFile = rmiArguments[i]; // the next argument should be the rmi args file. 
				javaVirtualMachineArguments = ReadArguments.readVMArguments(vmArgsFile);
				realRMIArgs.add(rmiArguments[i]);
			}
			else
				realRMIArgs.add(rmiArguments[i]);
		}
		if(!customizedMainClass.isEmpty()) {
			realRMIArgs.add("-cmc");
			realRMIArgs.add(customizedMainClass);
		}
		
		// combine all arguments into one command starting with the command string.
		
		// first 4 components. 
		String[] jarArgumentString = new String[]{"-jar", currentLoc.getPath(), invoke};
		ArrayList<String> vmCommands = new ArrayList<String>(Arrays.asList(javaVirtualMachineArguments));
		
		
		// combine all arguments into one long command. 
		ArrayList<String> allArgs = new ArrayList<String>();
		
		allArgs.add(javaCommand);
		allArgs.addAll(vmCommands);
		allArgs.addAll(Arrays.asList(jarArgumentString));
		allArgs.addAll(realRMIArgs);
		pBuilder.command(allArgs.toArray(new String[0]));
		
		try {
			myProcess = pBuilder.start();
			appErrors = myProcess.getErrorStream();
			appOutput = new BufferedReader(new InputStreamReader(myProcess.getInputStream()));
		}
		catch(IOException e) {
			throw new RuntimeException("ERROR: Could not launch application from file provided.");
		}
		try {
			String errors = "";
			String line;
			while((line = appOutput.readLine()) != null) 
				System.out.println("STDOUT: " + line);
			while(true) {
				int c = appErrors.read();
				if(c == -1)
					break;
				else
					errors += (char)c;
			}
			myProcess.waitFor();
			System.err.println(errors);
		} catch(InterruptedException | IOException e) {
		}
	}
	
	/**
	 * VERY SENSITIVE METHOD.
	 *   
	 * @return
	 */
	public NetCommunication retrieveProcessFromTheRegistry()
	{
		String name = "CTHCapture";
		Registry registry;
		try {registry = LocateRegistry.getRegistry();}
		catch(RemoteException e) {
			throw new RuntimeException("Tried to locate registry but failed \n" + e);
		}
		
		try {return (NetCommunication)registry.lookup(name);} 
		catch(RemoteException | NotBoundException e) {
			throw new RuntimeException("JavaCaptureTestCase: Tried to lookup " + name + " but failed\n" + e);
		} 
	}
	
	public String putProcessOnTheRegistry()
	{
		String portArgument;
		
		if(rmiArguments.length > 0) {
			if(rmiArguments[0].equals("-sendprefs")) 
				portArgument = rmiArguments[1];	
			else
				portArgument = rmiArguments[0];
		}
		else
			portArgument = "1099";
		
		networkStub = EventFlowSlicer.preloadRMISession(networkStub, portArgument);
		rmiRegistryPort = portArgument;
		registryObjectLive = true;
		return ">\t EFSCapture service is live.\n";
	}
	
	public String getRMIRegistryPort()
	{
		if(!registryObjectLive)
			return "";
		else
			return rmiRegistryPort;
	}

	
	
	public void run()
	{
		started = true;
		if(runInVM) 
			runAppInHostVM();
		else 
			runAppViaRMI();	
	}
	
	public static boolean launchesJar(String rawPath)
	{
		String ext = PathConformance.parseApplicationExtension(rawPath);
		return ext.equals(".jar");
	}
	
	
	public boolean launchesJar()
	{
		return extensionString.equals(".jar");
	}
	
	public boolean launchesClass()
	{
		return extensionString.equals(".class") || extensionString.isEmpty();
	}
	
	/**
	 * Returns a set of all the windows pertaining to this one by filtering out all
	 * windows created by this application that are either unrelated to the 
	 * open application, or invalid, or hidden windows. 
	 * @return
	 */
	public Set<Window> getAppRelatedAWTWindows()
	{
		if(!started)
			return new HashSet<Window>();
		
		Window[] windows = Window.getWindows();
		Set<Window> toReturn = new HashSet<Window>();
		
		for(Window target : windows) {
			String name = target.getAccessibleContext().getAccessibleName();
			// if the window name is empty, skip it.
			if(unrelatedWindows.contains(target) || name == null || name.isEmpty())
				continue;
			else if(target.getAccessibleContext().getAccessibleName().equals("CogTool Helper"))
				continue;
			// if the window can't be displayed, skip it.
			else if(!target.isDisplayable())	
				continue;
			else
				// the window passes the bar. Return it. 
				toReturn.add(target); 
		}	
		return toReturn;
	}
	
	private boolean isRelatedAWTWindow(Window target)
	{
		String name = target.getAccessibleContext().getAccessibleName();
		// if the window name is empty, skip it.
		if(unrelatedWindows.contains(target) || name == null || name.isEmpty())
			return false;
		else if(target.getAccessibleContext().getAccessibleName().equals("CogTool Helper"))
			return false;
		// if the window can't be displayed, skip it.
		else if(!target.isDisplayable())
			return false;
		else
			// the window passes the bar. Return it. 
			return true;
	}
	/**
	 * Returns a set containing all GWindows related to this window. 
	 * No windows containing the title "CogTool Helper" (the title of the cogtool helper frame)
	 * are returned in this collection. 
	 * @return
	 */
	public Set<GWindow> getAppRelatedGWindows()
	{
		if(!started)
			return new HashSet<GWindow>();
		
		// get the windows that would have been returned by getAllWindow
		if(targetJavaApp == null)
			return new HashSet<GWindow>();
		Set<GWindow> toReturn = targetJavaApp.getAllWindow();
		Iterator<GWindow> removalIterator = toReturn.iterator();
		GWindow target;
		
		// remove any reference to CogTool Helper. 
		while(removalIterator.hasNext()) {
			target = removalIterator.next();
			if(target.getTitle().equals("CogTool Helper"))
				removalIterator.remove();
		}
		
		// return the remaining windows
		return toReturn;
	}
	
	
	/**
	 * Close any windows belonging application instances tied to the running java application. 
	 * If the title of the window doesn't match the name of the cogtoolhelper window, and if the window wasn't listed
	 * as tied to a second application, we haven't closed yet, dispose of it. 
	 */
	@Override
	public void closeApplicationInstances() 
	{
		if(runInVM) {
			System.out.print("Ensuring the java application is closed...  ");
			if(targetJavaApp != null) {
				if(!targetJavaApp.disconnect()) {
					// don't do anything special, it just means the class is not completely unloaded.
					@SuppressWarnings("unused")
					boolean notUnloaded = true;
				}
			}
			// dispose all windows. 
			if(EventQueue.isDispatchThread()) {
				Collection<Window> myWindows = getAppRelatedAWTWindows();
				for(Window w : myWindows)	
					if(w instanceof JFrame) {
						String title = ((JFrame)w).getTitle();
						if(title.contains(appName) || !title.equals("CogTool Helper")) 
							w.dispose();
					}
					else
						w.dispose();
				undoImitatedVMPropertyChanges();
				started = false;	
			}
			else {
				try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run()
					{	
						Collection<Window> myWindows = getAppRelatedAWTWindows();
						for(Window w : myWindows)	
							if(w instanceof JFrame) {
								String title = ((JFrame)w).getTitle();
								if(title.contains(appName) || !title.equals("CogTool Helper")) 
									w.dispose();
							}
							else
								w.dispose();
						undoImitatedVMPropertyChanges();
						started = false;
					}
				});
				} 
				catch(InterruptedException | InvocationTargetException e) {
					throw new RuntimeException("JavaLaunchApplication: Thread was interrupted in the midst of attempt to dispose of app windows.");
				}
			}
		}
		else {
			System.out.println(">\tEnsuring the java application is closed...");
			myProcess.destroy();
			int exitSuccess = 0;
			int exitValue = 0;
			Thread.interrupted();
			try {
				exitValue = myProcess.waitFor();
			} catch(InterruptedException e) {
				throw new RuntimeException("JavaLaunchApplication: Subprocess was interrupted while exiting.");
			}
			if(exitValue != exitSuccess) 
				System.out.println(">\tSubprocess exited with exit value: " + exitValue);
			started = false;
		}
		
		// reset the look and feel now that the java app is done. 
		// (otherwise the look and feel carries over to CogToolHelper, which we don't want!)
		try {
			UIManager.setLookAndFeel(EventFlowSlicer.lookAndFeel);
		} catch(UnsupportedLookAndFeelException e) {
			System.err.println("JavaLaunchApplication: Could not reset look and feel. Buttons may appear to "
					+ "look strange in resulting interface.");
		}
	}
	
	@Override
	public void restoreProfile() 
	{
		if(appName.toUpperCase().contains("RACHOTA")) {
			File rachotaConfig = new File(System.getProperty("user.home") + "/.rachota");
		
			System.out.println("Deleting rachota configuration folder if present:\n" 
					+ rachotaConfig.getAbsolutePath());
			if(rachotaConfig.isDirectory()) 
				rachotaConfig.delete();
		}
	}

	/**
	 * Set up all necessary preliminaries to get this application running correctly. 
	 */
	@Override
	public void initProfile() 
	{
		// this method does nothing for java launch applications. 
	}
	
	/**
	 * Java applications have to restart in a special way. 
	 */
	@Override
	public void restart()
	{
		// reset application environment
		try {
			SwingUtilities.invokeAndWait(new Runnable() { public void run() {
				closeApplicationInstances();
			}});
		} catch(InterruptedException | InvocationTargetException e) {
			throw new RuntimeException("JavaLaunchApplication: Tried to close app windows, but thread was interrupted.", e);
		}
		restoreProfile();
		Thread newLaunch = new Thread(this);
		newLaunch.run();
		System.out.println("JavaLaunchApplication: Application Restarted.");
		//maximize windows
		try {
			Thread.sleep(ApplicationData.maximizeDelay);
		} catch(InterruptedException e) {
			System.err.println(e.getCause() + "\n" + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("JavaLaunchApplication: Now maximizing windows.");
		maximizeAppWindows();
		
		// wait to release program control.
		try {
			System.out.println("\nJava Launch Application: Waiting " + ApplicationData.openWaitTime/1000 + " seconds for java app to settle.\n");
			Thread.sleep(ApplicationData.openWaitTime);
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void saveCurrentWindowsAsUnrelated()
	{
		unrelatedWindows.clear();
		Window[] previousWindows = Window.getWindows();
		unrelatedWindows.addAll(Arrays.asList(previousWindows));
	}
	
	public boolean shouldMaximize(Frame f)
	{
		if(isRelatedAWTWindow(f) && 
			!f.getType().equals(Window.Type.POPUP) &&
			!f.getType().equals(Window.Type.UTILITY))
			return true;
		return false;
	}
	
	public void maximizeAppWindows()
	{
		Frame[] frames = Frame.getFrames();
		Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		for(int i = 0; i < frames.length; i++) 
			if(isRelatedAWTWindow(frames[i])) {
				frames[i].setMaximizedBounds(maxBounds);
				frames[i].setExtendedState(frames[i].getExtendedState() | JFrame.MAXIMIZED_BOTH);
			}
	}
	
	@SuppressWarnings("unused")
	private class NotInUse
	{
		private void maximizeAppWindows2()
		{
			Set<Window> windows = getAppRelatedAWTWindows();
			Stack<Frame> toMaximize = new Stack<Frame>();
			final Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			
			
			for(Frame f : Frame.getFrames()) 
				if(shouldMaximize(f))
					toMaximize.push(f);
			
			for(Frame f : toMaximize) {
				final Frame resizeFrame = f; 
				if(EventQueue.isDispatchThread()) {

					resizeFrame.setVisible(true);
					resizeFrame.requestFocus();
					resizeFrame.setMaximizedBounds(maxBounds);
					System.out.println("maxframe1");
					resizeFrame.setExtendedState(resizeFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
					System.out.println("maxFrame2");
				}
				else
					SwingUtilities.invokeLater(new Runnable() {public void run() {
						resizeFrame.requestFocus();
						resizeFrame.setMaximizedBounds(maxBounds);
						System.out.println("maxframe1");
						resizeFrame.setExtendedState(resizeFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
						System.out.println("maxFrame2");
					}});
			}
		}
	}
}
