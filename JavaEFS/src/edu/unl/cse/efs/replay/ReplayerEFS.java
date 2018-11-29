package edu.unl.cse.efs.replay;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.accessibility.AccessibleRole;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.event.GEvent;
import edu.umd.cs.guitar.exception.ComponentDisabled;
import edu.umd.cs.guitar.exception.ComponentNotFound;
import edu.umd.cs.guitar.exception.GException;
import edu.umd.cs.guitar.exception.ReplayerStateException;
import edu.umd.cs.guitar.exception.WindowNotFound;
import edu.umd.cs.guitar.model.GComponent;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.JFCXWindow;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.data.Step;
import edu.umd.cs.guitar.model.data.StepType;
import edu.umd.cs.guitar.model.data.Task;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.GUIStructureWrapper;
import edu.umd.cs.guitar.model.wrapper.PropertyTypeWrapper;
import edu.umd.cs.guitar.replayer.Replayer;
import edu.umd.cs.guitar.replayer.monitor.GTestMonitor;
import edu.umd.cs.guitar.util.GUITARLog;
import edu.unl.cse.efs.bkmktools.EFGBookmarking;
import edu.unl.cse.efs.java.JavaReplayerController;
import edu.unl.cse.efs.tools.StringTools;
import edu.unl.cse.guitarext.GUIEventModel;
import edu.unl.cse.guitarext.HeadTable;
import edu.unl.cse.guitarext.JavaTestInteractions;

public class ReplayerEFS extends Replayer {

	/**
	 * Test case data
	 */

	/**Task data*/
	private Task tsk;
	// private Document docEFG;
	//flags added by Amanda
	private boolean runWithoutGUI = false;
	private boolean replaysJavaApp;
	private ReplayerController replayerController;
	private List<JavaTestInteractions> javaInteractions;

	public ReplayerEFS(Task task, boolean replaysJavaApp)
	{
		this.tsk = task;
		this.runWithoutGUI = true;
		this.replaysJavaApp = replaysJavaApp;
	}

	public ReplayerEFS(TestCase tc, GUIStructure guiJAXB, EFG efgJAXB, String guiFilename, boolean replaysJavaApp)
			throws ParserConfigurationException, SAXException, IOException
	{
		super();
		this.guiStructureAdapter = new GUIStructureWrapper(guiJAXB);
		this.efg = efgJAXB;

		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder;
		builder = domFactory.newDocumentBuilder();
		docGUI = builder.parse(guiFilename);

		sDataPath = null;
		useImage = false;

		this.runWithoutGUI = false;
		this.replaysJavaApp = replaysJavaApp;
	}
	/**
	 * Author: jsaddle, adapted from code in Guitar: edu.umd.cs.guitar.replayer.Replayer
	 */
	public ReplayerEFS(TestCase tc, String guiFilename, String efgFilename, boolean replaysJavaApp)
			throws ParserConfigurationException, SAXException, IOException {
		super();

		this.tc = tc;
		this.sGUIFfile = guiFilename;
		this.sEFGFfile = efgFilename;
		XMLHandler handler = new XMLHandler();
		// Initialize GUI structure object
		GUIStructure gui = (GUIStructure) handler.readObjFromFile(guiFilename,
				GUIStructure.class);
		this.guiStructureAdapter = new GUIStructureWrapper(gui);

		// Initialize EFG object
		this.efg = (EFG) handler.readObjFromFile(efgFilename, EFG.class);
		EFGBookmarking bkmk = new EFGBookmarking(efg, gui);
		efg = bkmk.getBookmarked(true);

		// Initialize GUI XML file
		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder;
		builder = domFactory.newDocumentBuilder();
		docGUI = builder.parse(guiFilename);

		sDataPath = null;
		useImage = false;

		this.runWithoutGUI = false;
		this.replaysJavaApp = replaysJavaApp;
	}
	public void execute() throws ComponentNotFound, IOException
	{
		try {
			monitor.setUp();

			// Modified to always perform replayerController.captureBeforePlay()
			// regardless of tsk and tc state - wdm - Feb 1, 2013
			//CogTool-Helper - Amanda - 11/6/2011
			replayerController.beforeReplay();

			int nStep = 0;
			List<Step> tSteps = null;
			List<StepType> lSteps = null;
			if(runWithoutGUI){
				tSteps = tsk.getStep();
				nStep = tSteps.size();

				for (int i = 0; i < nStep; i++) {
					Step step = tSteps.get(i);
					// START STEP
					executeStep(step);
					// END STEP
				}

			}
			else{
				lSteps = tc.getStep();
				nStep = lSteps.size();

				for (int i = 0; i < nStep; i++) {
					StepType stepType = lSteps.get(i);
					// START STEP
					executeStep(stepType);
					// END STEP
				}
			}

			monitor.cleanUp();

			//CogTool-Helper - Amanda 11/6/2011
			replayerController.afterReplay();
		}
		catch (GException e) {
			for (GTestMonitor monitor : lTestMonitor)
				monitor.exceptionHandler(e);

			// END STEP
			// END TESTCASE
			throw e;
		}
	}

	public void executeStep(StepType step)
			throws ComponentNotFound, ReplayerStateException, IOException
	{

		// get important information from the TestCase Step.
		if(replaysJavaApp) {
			javaInteractions = ((JavaReplayerController)replayerController).getValidInteractions();
			System.out.println("Replayer: Valid java app interactions saved. " +
					javaInteractions.size() + " window(s) were scanned.\n");
			HeadTable.allInteractions = javaInteractions;
		}
		String sWindowTitle = "";
		String sWidgetID = "";
		String sAction = "";

		String sEventID = step.getEventId();
		if(sWidgetID.contains(GUITARConstants.NAME_VERSION_SEPARATOR)) {
			int indexOfNVS = sWidgetID.indexOf(GUITARConstants.NAME_VERSION_SEPARATOR);
			sWidgetID = sWidgetID.substring(indexOfNVS+1);
		}
		int[] seps = StringTools.findNCharactersIn(sWidgetID, GUITARConstants.NAME_SEPARATOR.charAt(0), 2);
		if(seps[1] != -1)
			sWidgetID = sWidgetID.substring(0, seps[1]); // remove the action string from this event ID.


		System.out.println("Executing Step EventID = " + sEventID);
		// Learn the events that are actionable on the step.
		// Get widget ID and actions

		sWidgetID = null;
		sAction = null;
		if(efg == null) System.err.println("Null EFG during replay, eventID:"+sEventID);
		List<EventType> lEvents = efg.getEvents().getEvent();

		for (EventType event : lEvents) {
			String eventID = event.getEventId();
			if (sEventID.equals(eventID)) {
				sWidgetID = event.getWidgetId();
				sAction = event.getAction();
				break;
			}
		}
		// Locate step event in EFG
		if (sWidgetID == null) {
			System.err.println("Step Event ID = " + sEventID + ". Not found in EFG.");
			throw new ReplayerStateException();
		} else if (sAction == null) {
			System.err.println("Step Event ID = " + sEventID + ". Action not found in EFG.");
			throw new ReplayerStateException();
		}

		sWindowTitle = getWindowName(sWidgetID);
		if (sWindowTitle == null) {
			GUITARLog.log.error("Step Event ID = " + sEventID
					+ ". Unable to locate window for widget");

			throw new ReplayerStateException();
		}

		System.out.println("Waiting for window:");
		System.out.println(" + Window Title = " + sWindowTitle);
		System.out.println(" + Widget ID    = " + sWidgetID);

		// Wait for expected window to appear
		GWindow gWindow = monitor.getWindow(sWindowTitle);

		if (gWindow == null) {
			System.err.println("Expected window did not appear");
			throw new ComponentNotFound();
		}
		GComponent container = gWindow.getContainer();
		if(container==null)
			throw new WindowNotFound();

		System.out.println("FOUND window");
		System.out.println(" + Window Title = " + gWindow.getTitle() + "\n");

		ComponentTypeWrapper comp = guiStructureAdapter.getComponentFromID(sWidgetID);

		// TEST
		if (comp == null) {
			System.err.println("Component not found in GUI state.");
			throw new ReplayerStateException();
		}

		System.out.println("Searching for widget:");
		System.out.println(" + Widget ID = " + sWidgetID);

		/*
		 * Once the window has been identified, search the window (gWindow)
		 * for the widget to click (sWidgetID).
		 */
		JavaTestInteractions myJTI = HeadTable.getInteractionsForWindowName(sWindowTitle);
		if(myJTI.isEmpty())
			throw new ComponentNotFound();
		List<PropertyTypeWrapper> IDAdapter = new ArrayList<PropertyTypeWrapper>();

		ComponentTypeWrapper compTypeWrapper = guiStructureAdapter.getComponentFromID(sWidgetID);
		PropertyType pt = compTypeWrapper.getFirstPropertyByName(GUITARConstants.CTH_EVENT_ID_NAME);
		IDAdapter.add(new PropertyTypeWrapper(pt));

		GComponent gComponent;
		if(compareWindow(pt, gWindow))
			gComponent = container;
		else
			gComponent = container.getFirstChild(IDAdapter);
		// TEST
		if (gComponent == null) {
			// Matching widget was not found
		   // Bail out with exception
			System.err.println("gComponent == null. ComponentNotFound exception.");
			throw new ComponentNotFound();
		}


		// Matching widget was found
		System.out.println("FOUND widget");
		System.out.println(" + Widget Title = " + gComponent.getTitle());
		// TEST

		// Execute action on matched widget
		GEvent gEvent = monitor.getAction(sAction);
		List<String> parameters = step.getParameter();
		System.out.println(" + Action: *" + sAction + "\n");

		// Set Optional data
		AttributesType optional = comp.getDComponentType().getOptional();
		Hashtable<String, List<String>> optionalValues = null;

		if (optional != null) {
			optionalValues = new Hashtable<String, List<String>>();
			for (PropertyType property : optional.getProperty())
				optionalValues.put(property.getName(), property.getValue());
		}

		boolean skip = replayerController.beforeStep(gComponent, comp, parameters, gEvent.getClass().getSimpleName());
		if(skip)
			return;

		// perform the action
		if (parameters == null)
			gEvent.perform(gComponent, optionalValues);
		else if (parameters.size() == 0)
			gEvent.perform(gComponent, optionalValues);
		else
			gEvent.perform(gComponent, parameters, optionalValues);

		if(replaysJavaApp) {
			JFCXComponent javaComp = (JFCXComponent)gComponent;
			AccessibleRole jRole = javaComp.getAccessibleContext().getAccessibleRole();
			boolean mustCloseMenus = javaComp.getComponent() instanceof MenuElement && !jRole.equals(AccessibleRole.MENU);
			boolean mustCloseCombos = jRole.equals(AccessibleRole.COMBO_BOX) && !sAction.equals(ActionClass.ACTION.actionName);

			if(mustCloseMenus || mustCloseCombos) try{
				SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					System.out.println("Replayer: Closing Menu...");
					MenuSelectionManager.defaultManager().clearSelectedPath();
				}});
			}
			catch(InvocationTargetException | InterruptedException e) {
				throw new RuntimeException("Guitar Replayer was interrupted", e);
			}
		}
		replayerController.afterStep();
	}

	public boolean compareWindow(PropertyType searchProp, GWindow gWindow)
	{
		if(gWindow == null)
			return false;
		if(!(gWindow instanceof JFCXWindow))
			return false;
		JFCXWindow jWindow = (JFCXWindow)gWindow;
		String jWinId = jWindow.getEventID();
		if(jWinId == null)
			return false;
		for(String s : searchProp.getValue())
			if(s.equals(jWinId))
				return true;

		return false;

	}
	public List<PropertyTypeWrapper> cthEventIDAdapter(String sWidgetID, String sAction, JavaTestInteractions myJTI)
	{
		PropertyType cthEventIDProp = (new ObjectFactory()).createPropertyType();
		cthEventIDProp.setName(GUITARConstants.CTH_EVENT_ID_NAME);


//		String componentType = sWidgetID.split(GUITARConstants.NAME_SEPARATOR)[0];
		ComponentTypeWrapper compTypeWrapper = guiStructureAdapter.getComponentFromID(sWidgetID);
		String componentType = compTypeWrapper.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);
		componentType = componentType.replace('_', ' ').toLowerCase();
		if(componentType.equals(AccessibleRole.PANEL.toDisplayString())) {
			// get the two Strings that might correspond to this widget
			String actionID = myJTI.matchActionWithID(sWidgetID, ActionClass.ACTION.actionName);
			String textID = myJTI.matchActionWithID(sWidgetID, ActionClass.TEXT.actionName);
			if(actionID.equals(JavaTestInteractions.hasNoID)
			&& textID.equals(JavaTestInteractions.hasNoID)) {
				throw new ComponentNotFound();
			}
			if(actionID.equals(JavaTestInteractions.hasNoID))
				actionID = GUIEventModel.noAction;
			else if(textID.equals(JavaTestInteractions.hasNoID))
				textID = GUIEventModel.noText;
			cthEventIDProp.setValue(Arrays.asList(new String[]{actionID, textID, GUIEventModel.noParSelect}));
		}
		else if(componentType.equals(AccessibleRole.COMBO_BOX.toDisplayString())) {
			// combo boxes always have action and select id's, but no text id.
			String actionID = myJTI.matchActionWithID(sWidgetID, ActionClass.ACTION.actionName);
			String selectID= myJTI.matchActionWithID(sWidgetID, ActionClass.PARSELECT.actionName);
			if(actionID.equals(JavaTestInteractions.hasNoID))
				actionID = GUIEventModel.noAction;
			if(selectID.equals(JavaTestInteractions.hasNoID))
				selectID = GUIEventModel.noParSelect;

			if(actionID.equals(JavaTestInteractions.hasNoID)
			&& selectID.equals(JavaTestInteractions.hasNoID)) {
				throw new ComponentNotFound();
			}
			cthEventIDProp.setValue(Arrays.asList(new String[]{actionID, GUIEventModel.noText, selectID}));
		}
		else {
			String theID = myJTI.matchActionWithID(sWidgetID, sAction);
			if(theID.equals(JavaTestInteractions.hasNoID))
				throw new ComponentNotFound();
			cthEventIDProp.setValue(Arrays.asList(new String[]{theID}));
		}
		List<PropertyTypeWrapper> IDAdapter = new ArrayList<PropertyTypeWrapper>();
		IDAdapter.add(new PropertyTypeWrapper(cthEventIDProp));//add property to IDAdapter
		return IDAdapter;
	}
	public void executeStep(Step taskStep)
	{
		// if we're replaying a java application, we need to scan for valid java interactions.
		// so that we can assign proper eventID's to window components.
		if(replaysJavaApp) {
			System.out.println("Replayer: Gathering valid interactions for java app...\n");
			javaInteractions = ((JavaReplayerController)replayerController).getValidInteractions();
			System.out.println("Replayer: Valid java app interactions saved. " +
					javaInteractions.size() + " window(s) were scanned.\n");

			HeadTable.allInteractions = javaInteractions;
		}

		String sWindowID = "";
		String sWidgetID = "";
		String sAction = "";

		// Get widget ID
		sWidgetID = taskStep.getEventId();

		if(sWidgetID.contains(GUITARConstants.NAME_VERSION_SEPARATOR)) {
			int indexOfNVS = sWidgetID.indexOf(GUITARConstants.NAME_VERSION_SEPARATOR);
			sWidgetID = sWidgetID.substring(indexOfNVS+1);
		}
//		int lastSep = sWidgetID.lastIndexOf(GUITARConstants.NAME_SEPARATOR);
//		if(lastSep != -1)
//			sWidgetID = sWidgetID.substring(0, lastSep);
		// find the second underscore if one exists, and remove all content after it.
		int[] seps = StringTools.findNCharactersIn(sWidgetID, GUITARConstants.NAME_SEPARATOR.charAt(0), 2);
		if(seps[1] != -1)
			sWidgetID = sWidgetID.substring(0, seps[1]); // remove the action string from this event ID.


		//action
		sAction = taskStep.getAction();

		//window ID
		sWindowID = taskStep.getWindowId();

		if (sWindowID == null) {
			System.out.println("Replayer: Window Title not found");
			throw new ComponentNotFound();
		}

		System.out.println("Replayer: Window Title: *" + sWindowID + "*");
		System.out.println("Replayer: Widget ID: *" + sWidgetID + "*\n");

		System.out.println("Replayer: Finding window *");

		GWindow gWindow = monitor.getWindow(sWindowID);
		System.out.println("Replayer: FOUND\n");

		GComponent container = gWindow.getContainer();
		if(container==null)
			throw new WindowNotFound();

		System.out.println("Replayer: Finding widget *" + sWidgetID + "*....");

		ComponentTypeWrapper comp = null;
		GComponent gComponent = null;
		List<PropertyTypeWrapper> IDAdapter = new ArrayList<PropertyTypeWrapper>();
		Hashtable<String, List<String>> optionalValues = null;

		// retrieve the component by parameters given

		// JTI
		PropertyType cthEventIDProp = new PropertyType();
		cthEventIDProp.setName(GUITARConstants.CTH_EVENT_ID_NAME);
		JavaTestInteractions myJTI = HeadTable.getInteractionsForWindowName(sWindowID);

		if(myJTI.isEmpty())
			throw new ComponentNotFound();
		// check eventID
		String componentType = sWidgetID.split(GUITARConstants.NAME_SEPARATOR)[0];

		// properly set the adapter to load the component.
		ArrayList<String> orderedIds = new ArrayList<String>();
		for(ActionClass ac : GUIEventModel.getSupportedActionsFor(componentType))
			orderedIds.add(myJTI.matchActionWithID(sWidgetID, ac.actionName));
		cthEventIDProp.setValue(orderedIds);

		// search for EventID.
		IDAdapter.add(new PropertyTypeWrapper(cthEventIDProp));//add property to IDAdapter
		// THE FOLLOWING IS A METHOD WITH A LONG RUNNING TIME
		gComponent = container.getFirstChild(IDAdapter);

		if(gComponent!=null)
			comp = new ComponentTypeWrapper(gComponent.extractProperties());

		if(comp==null)
			throw new ComponentNotFound();
		else {
			// Optional data
			AttributesType optional = comp.getDComponentType().getOptional();

			if (optional != null) {
				optionalValues = new Hashtable<String, List<String>>();
				for (PropertyType property : optional.getProperty())
					optionalValues.put(property.getName(), property.getValue());
			}
		}


		// Actions
		GEvent gEvent = monitor.getAction(sAction);
		List<String> parameters = null;
		String param = taskStep.getParameter();
		parameters = new ArrayList<String>();
		parameters.add(param);

		//CogTool-Helper - Amanda 11/6/2011
		boolean skip = replayerController.beforeStep(gComponent, comp, parameters, gEvent.getClass().getSimpleName());
		if(skip)
			return;

		System.out.println("Replayer: FOUND");
		System.out.println("Replayer: Widget Title: *" + gComponent.getTitle() + "*\n");

		if (!gComponent.isEnable())
			throw new ComponentDisabled();

		System.out.println("Replayer: Action: *" + sAction);

		if (parameters.size() == 0)
			gEvent.perform(gComponent, optionalValues);
		else
			gEvent.perform(gComponent, parameters, optionalValues);

		if(replaysJavaApp) {
			JFCXComponent javaComp = (JFCXComponent)gComponent;
			AccessibleRole jRole = javaComp.getAccessibleContext().getAccessibleRole();
			boolean mustCloseMenus = javaComp.getComponent() instanceof MenuElement && !jRole.equals(AccessibleRole.MENU);
			boolean mustCloseCombos = jRole.equals(AccessibleRole.COMBO_BOX) && !sAction.equals(ActionClass.ACTION.actionName);

			if(mustCloseMenus || mustCloseCombos) try{
				SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					System.out.println("Replayer: Closing Menu...");
					MenuSelectionManager.defaultManager().clearSelectedPath();
				}});
			}
			catch(InvocationTargetException | InterruptedException e) {
				throw new RuntimeException("Guitar Replayer was interrupted", e);
			}
		}
//		if(replaysJavaApp)
//			selectionDetectorJava((JFCXComponent)gComponent);
		replayerController.afterStep();
	}
	public ReplayerController getReplayerController() {
		return replayerController;
	}

	public void setReplayerController(ReplayerController replayerControl) {
		this.replayerController = replayerControl;
	}
}
