package edu.unl.cse.efs.java;

import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.umd.cs.guitar.model.GComponent;
import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.JFCXWindow;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.unl.cse.efs.replay.ApplicationMonitor;
import edu.unl.cse.efs.replay.ReplayerController;
import edu.unl.cse.guitarext.JavaTestInteractions;

public class JavaReplayerController extends ReplayerController{
	
	private JavaApplicationMonitor javaMonitor;
	private ArrayList<JavaTestInteractions> testInteractions;
	public JavaReplayerController(JavaApplicationMonitor monitor)
	{
		super(monitor); 
		javaMonitor = monitor; 		
	}
	
	/**
	 * Start the application, and a list of the windows that may have opened 
	 * when starting the application. 
	 * @return
	 */
	public List<GWindow> initializeWindows()
	{
		javaMonitor.startApp();	
		List<GWindow> extractionWindows = new LinkedList<GWindow>(javaMonitor.getAppRelatedGWindows());
		topWindows = new LinkedList<GWindow>(extractionWindows);
		return topWindows;
	}
	
	/**
	 * Return a list of JavaTestInteractions corresponding to windows in the java application representing
	 * actions that can be carried out on this replayerController's application. The list contains one module
	 * per window.  
	 * @return
	 */
	public List<JavaTestInteractions> getValidInteractions()
	{
		testInteractions = new ArrayList<>();
		JavaTestInteractions nextIModule;
		Set<Window> interactableWindows = javaMonitor.getAppRelatedAWTWindows();
		for(Window window : interactableWindows) {
			nextIModule = new JavaTestInteractions();
			nextIModule.scanWindowForInteractions(window);
			testInteractions.add(nextIModule);
		}
		return testInteractions;
	}
	
	/**
	 * Capture the screenshot of the interface before the step in the test case
	 */
	public boolean beforeStep(GComponent component, ComponentTypeWrapper compWrapper, 
			List<String> parameters, String aH)
	{
		//Get the currently active windows from test interactions
		this.topWindows = new LinkedList<GWindow>();
		for(int i = 0; i < testInteractions.size(); i++) 
			for(Window w : testInteractions.get(i).getWindowsScanned())
				this.topWindows.add(new JFCXWindow(w));
		
		System.out.println("-- Capturing the screen state. ---\n");
		JavaApplicationMonitor.captureScreenState(this.source, this.imgDirectory); 
		return false;
	}
	
	/**
	 * Set the application monitor used to open, close, and retrieve states from the application.
	 */
	public void setApplicationMonitor(ApplicationMonitor monitor)
	{
		super.setApplicationMonitor(monitor);
		javaMonitor = (JavaApplicationMonitor)monitor;
	}
	
	/**
	 * Get the application monitor used to open, close, and retrieve states from the application.
	 */
	public ApplicationMonitor getApplicationMonitor()
	{
		return javaMonitor;
	}
}
