/**
 * Create a plugin that can be used to execute actions 
 * before and after the execution of replay steps. 
 */
package edu.unl.cse.efs.replay;

import java.util.List;


import edu.umd.cs.guitar.model.GComponent;
import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;

public class ReplayerController {

	private int replayerRuns; 
	protected String imgDirectory; 
	String designID; 
//	protected String previousComponent;
//	protected String previousActionType; 
	protected static final int openWaitTime = 10000;
	protected int source;
	boolean prevFound = true;
	String taskName = ""; 
	
	//new
//	CogToolModelFactory factory; 
	protected ApplicationMonitor applicationMonitor;
	protected List<GWindow> topWindows;

	public ReplayerController(ApplicationMonitor monitor){
		applicationMonitor = monitor; 
		this.imgDirectory = "";
		this.designID = ""; 
		replayerRuns = 0;
		source = 0;
	}
	
	/**
	 * Reset before replaying the next test case
	 */
	public void reset()
	{
		setReplayerRuns(replayerRuns+1);
//		factory.reset(); 
//		this.prevFound = true;
//		this.previousComponent = ""; 
//		this.previousActionType = ""; 
	}

	/**
	 * jsaddler: 
	 * Start the application, initialize the demonstration using the current designID, and 
	 * perform an initial capture of the state of the application if 
	 * the replayer had no previous runs. Should be called by an active replayer
	 * before the replayer begins it's replay operations. 
	 * 
	 * Preconditions: 	This class has been properly initialized.
	 * 					Design ID has been set. 
	 * Postconditions: 	The demonstration of the cogtoolModelFactory of this controller has been initialized
	 * 					If there were no replayer runs previous to a call to this method,
	 * 					the destination frame of the cogtoolmodelfactory is set to 2, and the state of the application is captured and stored
	 * 					to the factory as the previous state.
	 */
	public void beforeReplay()
	{
		System.out.println("ReplayerController: Initializing replay.\n");

		//load a new document
//		applicationMonitor.startApp(); 

//		this.documentBounds = CogToolHelperConstants.DEFAULT_BOUNDS; 

		//Start building CogTool demonstration
//		factory.initializeDemonstration(designID); 


		//get teh currrent starting state for the frame, this is the first replayer run so we assume
		//the rest of the frames will have the same start state, we only need extract this once
//		this.sourceFrame = 1; 
//		factory.setSourceFrame(this.sourceFrame); 
//		if(this.replayerRuns==0){
//			this.destFrame = 2;
//			factory.setDestFrame(this.destFrame);
//			//Capture the current state of the interface
//			System.out.println("Capturing the initial state. \n");
//			this.prevState = applicationMonitor.getCurrentState(); 
//			factory.setPrevState(this.prevState); 
//		}
	}


	

	/**
	 * Before executing a step in the test case, 
	 * save the information on the windows currently open, 
	 * and save an image of the screen.
	 */
	public boolean beforeStep(GComponent component, ComponentTypeWrapper compWrapper, List<String> parameters, String aH)
	{
		//Get the currently active windows
		this.topWindows = applicationMonitor.getRootWindows();

		//Construct a new instance of BuildFrames with either a guitar GComponent, or ComponentTypeWrapper (this widget could not be found on the interface currently, 
		//but we would like to use a keyboard shortcut for it)

		System.out.println("-- Capturing the screen state. ---\n");
		ApplicationMonitor.captureScreenState(this.source, this.imgDirectory); 
		return false;
	}

	/**
	 * Capture the design after the step in the test case, and increment the frame number. 
	 */
	public void afterStep(){
		try {
			System.out.println("\nSleeping 3 seconds...\n");
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		incrementSource();
	}

	/**
	 * jsaddler: 
	 * Reset the frame builder of the cogtoolmodelfactory using null values, add a final frame 
	 * to the factory, and add one more demonstration to the cogtoolmodelfactory before closing the
	 * application, resetting this replayer controller, and incrementing the number of replayer runs by one.
	 * Should be called by an active replayer after the replayer has finished its replay operations. 
	 * 
	 * Preconditions: 	The application referenced by the application monitor should be open. 
	 * Postconditions: 	The application referenced by the application monitor is closed. 
	 * 					Replayer runs is incremented by one. 
	 * 					The frame builder of the cogtoolmodelfactory is reset with null values.
	 * 				 	A "last" frame has been added to the demonstration list for the cogtoolmodelfactory. 
	 */
	public void afterReplay(){
		//take the final screenshot
//		System.out.println("Capturing final frame. \n");

		//capture final screenshot
//		GComponent finComp = null;
//		factory.resetFrameBuilder(null, finComp, null, this.imgDirectory, this.designID, null, applicationMonitor); 
//		factory.addLastFrame(this.imgDirectory); 
//		factory.addDemonstration(); 
		ApplicationMonitor.captureScreenState(this.source, this.imgDirectory);
		System.out.println("ReplayerController: Closing the application.");
		applicationMonitor.closeApp(); //close the currently opened application instance
		reset(); //increment number of runs and reset before replaying the next test case
	}
	
	public void setTaskName(String name) 
	{
		this.taskName = name;
	}

	public void setImageDir(String img) 
	{
		this.imgDirectory = img;
	}

	public void setDesignID(String designIDVal) 
	{
		this.designID  = designIDVal;
	}

	protected void setReplayerRuns(int replayerRunsValue) 
	{
		this.replayerRuns = replayerRunsValue;
	}
	
	protected void incrementSource()
	{
		source++;
	}
	
	public void setApplicationMonitor(ApplicationMonitor monitor) 
	{
		this.applicationMonitor = monitor; 
	}
	
	public ApplicationMonitor getApplicationMonitor() {
		return this.applicationMonitor;
	}
}
