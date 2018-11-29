package edu.unl.cse.efs.java;

import static java.awt.event.KeyEvent.VK_BACK_SPACE;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Semaphore;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JPopupMenu;

import edu.unl.cse.efs.commun.JavaRMICaptureMonitor;
import edu.unl.cse.efs.java.JavaCaptureMonitor;
import edu.unl.cse.efs.view.EventFlowSlicerErrors;
import edu.unl.cse.guitarext.JavaTestInteractions;
import edu.unl.cse.jontools.string.StringTools;
import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.event.JFCBasicHoverHandler;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.unl.cse.efs.CaptureTestCase;
import edu.unl.cse.efs.app.EventFlowSlicer;
import edu.unl.cse.efs.capture.java.JavaStepType;
import edu.unl.cse.efs.commun.giveevents.NetCommunication;


import static edu.unl.cse.efs.java.JavaCaptureUtils.*;

public class JavaCaptureTaskList extends CaptureTestCase implements NetCommunication{
	private JavaLaunchApplication guiLauncher;
	private LinkedHashSet<JavaStepType> savedSteps;
	private JavaCaptureMonitor captureMonitor;
	private Timer printTimer;
	private JavaStepType workingTextStep, workingListSelectionStep, workingComboStep;
	private Component workingList, workingCombo;
	private String[] captureOffCommands;
	private final int appOpenDelay;
	private int savingText, NO_SAVE = 0, TEXT_STRING_SAVE = 1, COMMAND_SAVE = 2;
	private boolean savingASelection;
	private String savedText;
	private boolean runAsServer;
	private NetCommunication networkStub;
	private int numSteps;
	public Thread rmiCaptureThread;
	Semaphore activated;
	public static final int TYPICAL_OPEN_DELAY = 9;
	private int hoverWaitSeconds = 3;

	/**
	 * Constructor for the JavaCaptureTaskList instance.<br>
	 * This constructor implements a parameter to control the delay to wait for the sub-application to open.
	 */
	public JavaCaptureTaskList(JavaLaunchApplication appLauncher, int appOpenDelayInSeconds, String... rmiArguments)
	{
		appOpenDelay = appOpenDelayInSeconds;
		this.guiLauncher = appLauncher;
		if(rmiArguments.length > 0) {
			runAsServer = true;
			guiLauncher.useRMI(rmiArguments, this);
		}
		else {
			runAsServer = false;
			guiLauncher.dontUseRMI();
		}

		savedSteps = new LinkedHashSet<JavaStepType>();
		savingText = NO_SAVE;
		savedText = "";
		savingASelection = false;
		captureOffCommands = new String[0];

		activated = new Semaphore(1);
		try {activated.acquire();}
		catch(InterruptedException e) {
			throw new RuntimeException("ReplayTestCase: Capture Sequence was interrupted during initialization.");
		}
	}


	/**
	 * Constructor for the JavaCaptureTaskList instance.<br>
	 * As long as no server arguments are given, this code will run a version of the capture in house
	 * Else, the capture will run in a new application instance and connect to this instance in via an RMI connection using the
	 * arguments specified in serverArgs.
	 *
	 * @param guiLauncher
	 * @param serverArgs
	 */
	public JavaCaptureTaskList(JavaLaunchApplication guiLauncher, String... rmiArguments)
	{
		this(guiLauncher, TYPICAL_OPEN_DELAY, rmiArguments);
	}
	/**
	 * If capturing has been started, events are printed to console if
	 * printOn is true, otherwise, events are never printed to console.
	 *
	 * Preconditions: 	none
	 * Postconditions: 	Events are printed to console if captureOn is true
	 * 				 	and printOn is true. Otherwise, events are
	 * 					never printed to console.
	 */
	public void setEventPrint(boolean printOn)
	{
		eventPrintOn = printOn;
	}
	/**
	 * Start printing the steps taken down by this capture to the terminal.
	 */
	public void startPrintSteps()
	{
		printTimer = new java.util.Timer();
		java.util.TimerTask myTask = new java.util.TimerTask() { public void run() {
			System.out.println(stepsString(false));
		}};
		printTimer.schedule(myTask,new java.util.Date(), 2000);
	}
	/**
	 * Stops the timer from printing more steps to console.
	 */
	public void stopPrintSteps()
	{
		if(printTimer != null)
			printTimer.cancel();
	}
	/**
	 * Return a string containing all the steps from this test case,
	 * a tabulated string if tabbed is true, an un-tabulated string otherwise.
	 */
	private String stepsString(boolean tabbed)
	{
		String toReturn = "";
		if(tabbed) toReturn += ">\t";
		toReturn += savedSteps.size() + " Steps: [\n";
		Iterator<JavaStepType> steps = savedSteps.iterator();
		while(steps.hasNext()) {
			if(tabbed) toReturn += ">\t";
			toReturn += steps.next() + ", ";
			toReturn += "\n";
		}
		if(tabbed) toReturn += ">\t";
		toReturn += "]";
		return toReturn;
	}
	/**
	 * Starts the capture sequence. If we're running as client, send any steps that we receive from
	 * capture over the network to the stub. Else process steps directly using this JavaCaptureTaskList.
	 *
	 * Preconditions:	none
	 * Postconditions: 	The capture sequence is started
	 */
	public void run()
	{
	System.out.println("JavaCaptureTaskList: Setting up Capture.");

		if(runAsServer) {
			final Thread appThread = new RMICapture();
			appThread.start();
			System.out.println("\n>\t Now starting the application, please wait up to 10 seconds for application to settle...");
			numSteps = 0;
			try {
				appThread.join(); // joining the app thread causes us to
								  // wait for the other application to finish its business
					// before returning from run();

			} catch(InterruptedException e) {

			}
		}

		else {
			Thread appThread = new Thread(guiLauncher);
			appThread.start();
			 // wait a few seconds for the application to settle just in case
			try{
				System.out.println("JavaCaptureTaskList: Now waiting " + appOpenDelay/1000 + " seconds for application to settle...");
				System.out.println("JavaCaptureTaskList: Please resize all windows now,\n  before this delay ends.");
				Thread.sleep(appOpenDelay);
			}
			catch(InterruptedException e) {
				// if this stage was interrupted, just return.
				return;
			}

			if(captureBack) {
				networkStub = EventFlowSlicer.beginRMISession(true, captureBackPort);
				captureMonitor = new JavaRMICaptureMonitor(guiLauncher.getAppRelatedGWindows(), networkStub);
				captureMonitor.setWindowEventPrint(eventPrintOn);
				captureMonitor.turnOffCaptureOf(captureOffCommands);
				boolean capturing = captureMonitor.captureWindowsProvided();
				if(!capturing)
					endAndGetTaskList();
			}
			else {
				captureMonitor = new JavaCaptureMonitor(guiLauncher.getAppRelatedGWindows(), this);
				captureMonitor.setWindowEventPrint(eventPrintOn);
				captureMonitor.turnOffCaptureOf(captureOffCommands);
				boolean capturing = captureMonitor.captureWindowsProvided();
				if(capturing)
					startPrintSteps();
				else
					endAndGetTaskList();
			}
		}
	}
	public Collection<JavaStepType> getSavedSteps()
	{
		return savedSteps;
	}
	/**
	 * Complete the capture process and retrieve a task list containing
	 * steps that were saved while capture was running.
	 * @return
	 */
	public TaskList endAndGetTaskList()
	{

		TaskList tl;
		if(runAsServer && guiLauncher.started) {
			// finish final touchup maneuvers
			endTrackingProcess(false, true);
			tl = getTranslatedTaskList(savedSteps);
		}
		else if(!runAsServer && captureMonitor != null) {
			stopPrintSteps();
			endTrackingProcess(true, true);
			tl = getTranslatedTaskList(savedSteps);
		}
		else
			tl = (new ObjectFactory()).createTaskList();

		// we're done capturing. Signal this to all listeners.
		try{
			activated.release();
			Thread.sleep(1000);
			activated.acquire();
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			System.out.println("JavaCaptureTaskList: RMI Capture thread shutdown was interrupted.");
		}


		return tl;
	}

	/**
	 * End the tracking process
	 */
	private void endTrackingProcess(boolean stopCaptureMonitor, boolean printFinalSteps)
	{
		flushTextEntryToSavedSteps();
		flushListItemSelectionToSavedSteps();

		if(printFinalSteps)
			System.out.println(stepsString(true));

		if(stopCaptureMonitor)
			captureMonitor.stopCapture();
		guiLauncher.closeApplicationInstances();
	}

	public void saveBackgroundClickEvent(MouseEvent mouseEvent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(mouseEvent);
		JavaStepType newStep = new JavaStepType();

		Component buttonComponent = mouseEvent.getComponent();
		// for check box, check menu_item, menu, radio button, radio menu_item, menu item, and push button,
		// x, y, height, and width
		newStep.setX(JFCXComponent.getGUITAROffsetX(buttonComponent));
		newStep.setY(JFCXComponent.getGUITAROffsetY(buttonComponent));
		newStep.setHeight(buttonComponent.getHeight());
		newStep.setWidth(buttonComponent.getWidth());

		String componentName = JavaCaptureUtils.getCaptureComponentName(buttonComponent);
		newStep.setComponent(componentName);
		AccessibleContext buttonContext = buttonComponent.getAccessibleContext();
		if(buttonContext != null && buttonContext.getAccessibleRole() != null)
			newStep.setRoleName(buttonContext.getAccessibleRole().toDisplayString());
		else
			newStep.setRoleName(AccessibleRole.PANEL.toDisplayString());

		newStep.setWindow(windowName);
		Component buttonParent = buttonComponent.getParent();
		newStep.setParentName(buttonParent.getAccessibleContext().getAccessibleName());
		newStep.setParentRoleName(buttonParent.getAccessibleContext()
				.getAccessibleRole()
				.toDisplayString());


		// set ComponentID
		newStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
				buttonComponent,
				newStep.getWindow(),
				newStep.getAction()));

		newStep.getParameters().add("Click" +
				GUITARConstants.NAME_SEPARATOR + mouseEvent.getX() +
				GUITARConstants.NAME_SEPARATOR + mouseEvent.getY()
		);

		savedSteps.add(newStep);
	}

	public void saveWindowClose(WindowEvent we)
	{
		Window w = we.getWindow();
		String windowName = w.getAccessibleContext().getAccessibleName();
		JavaStepType newStep = new JavaStepType();

		newStep.setX(0);
		newStep.setY(0);
		newStep.setHeight(w.getSize().height);
		newStep.setWidth(w.getSize().width);
		newStep.setAction(ActionClass.WINDOW.actionName);
		newStep.setComponent(windowName);
		newStep.setRoleName(AccessibleRole.WINDOW.toDisplayString());
		newStep.setComponentID("window_" + windowName);
		captureMonitor.interactionsForWindow(windowName).lookupID(
				w, windowName, AccessibleRole.WINDOW.toDisplayString());
		newStep.setWindow(windowName);
		savedSteps.add(newStep);
	}


	/**
	 * Save an action that led to a push button (button, men item, checkbox) being clicked.
	 * @param mouseEvent
	 * @param windowName
	 */
	public void saveButtonClick(MouseEvent mouseEvent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(mouseEvent);

		JavaStepType newStep = new JavaStepType();

		Component buttonComponent = mouseEvent.getComponent();
		// for check box, check menu_item, menu, radio button, radio menu_item, menu item, and push button,
		// x, y, height, and width
		newStep.setX(JFCXComponent.getGUITAROffsetX(buttonComponent));
		newStep.setY(JFCXComponent.getGUITAROffsetY(buttonComponent));
		newStep.setHeight(buttonComponent.getHeight());
		newStep.setWidth(buttonComponent.getWidth());


		AccessibleContext buttonContext = buttonComponent.getAccessibleContext();
		newStep.setAction(ActionClass.ACTION.actionName);

		String componentName = JavaCaptureUtils.getCaptureComponentName(buttonComponent);
		newStep.setComponent(componentName);

		// get the role from the accessibleContext too.
		if(buttonContext.getAccessibleRole() != null)
			newStep.setRoleName(buttonContext.getAccessibleRole().toDisplayString());
		else
			newStep.setRoleName(AccessibleRole.PANEL.toDisplayString());

		newStep.setWindow(windowName);
		Component buttonParent = buttonComponent.getParent();
		newStep.setParentName(buttonParent.getAccessibleContext().getAccessibleName());
		newStep.setParentRoleName(buttonParent.getAccessibleContext()
				.getAccessibleRole()
				.toDisplayString());


		// set ComponentID
		newStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
				buttonComponent,
				newStep.getWindow(),
				newStep.getAction()));

		if(newStep.getRoleName().equals(AccessibleRole.PANEL.toDisplayString())) {
			newStep.getParameters().add("Click" +
					GUITARConstants.NAME_SEPARATOR + mouseEvent.getX() +
					GUITARConstants.NAME_SEPARATOR + mouseEvent.getY()
			);
		}
		savedSteps.add(newStep);
	}

	/**
	 * Save an action that led to a button-like widget (button, menu item, checkbox) being hovered over.
	 */
	public void saveHoverEvent(String windowName, Component hoverComponent, int mouseXPosition, int mouseYPosition)
	{
		JavaStepType newStep = new JavaStepType();
		newStep.setX(JFCXComponent.getGUITAROffsetX(hoverComponent));
		newStep.setY(JFCXComponent.getGUITAROffsetY(hoverComponent));
		newStep.setHeight(hoverComponent.getWidth());
		newStep.setWidth(hoverComponent.getHeight());
		newStep.setWindow(windowName);

		newStep.setAction(ActionClass.HOVER.actionName);
		String componentName = getCaptureComponentName(hoverComponent);
		newStep.setComponent(componentName);

		AccessibleContext hoverContext = hoverComponent.getAccessibleContext();
		if(hoverContext.getAccessibleRole() != null)
			newStep.setRoleName(hoverContext.getAccessibleRole().toDisplayString());
		else
			newStep.setRoleName(AccessibleRole.PANEL.toDisplayString());

		Component hoverParent = hoverComponent.getParent();
		newStep.setParentName(getCaptureComponentName(hoverParent));
		newStep.setParentRoleName(hoverParent.getAccessibleContext()
				.getAccessibleRole().toDisplayString());
		newStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
				hoverComponent, newStep.getWindow(),
				newStep.getAction()));
		newStep.getParameters().add(JFCBasicHoverHandler.Hover_Command
				+ GUITARConstants.NAME_SEPARATOR + mouseXPosition
				+ GUITARConstants.NAME_SEPARATOR + mouseYPosition);
		newStep.getParameters().add(JFCBasicHoverHandler.Wait_Command
				+ GUITARConstants.NAME_SEPARATOR + hoverWaitSeconds);
		savedSteps.add(newStep);
	}

	/**
	 * Save an action that led to a container-like widget (text label, flat list, combo box) being hovered over.
	 */
	public void saveSelectionOrientedHover(String windowName, Component hoverComponent, int mouseXPosition, int mouseYPosition)
	{
		// not implemented yet.
//		JavaStepType newStep = new JavaStepType();
//		newStep.setX(JFCXComponent.getGUITAROffsetX(hoverComponent));
//		newStep.setY(JFCXComponent.getGUITAROffsetY(hoverComponent));
//		newStep.setHeight(hoverComponent.getWidth());
//		newStep.setWidth(hoverComponent.getHeight());
//		newStep.setWindow(windowName);
//
//		newStep.setAction(ActionClass.SELECTION_HOVER.actionName);
//		String componentName = getCaptureComponentName(hoverComponent);
//		newStep.setComponent(componentName);
//
//		AccessibleContext hoverContext = hoverComponent.getAccessibleContext();
//		if(hoverContext.getAccessibleRole() != null)
//			newStep.setRoleName(hoverContext.getAccessibleRole().toDisplayString());
//		else
//			newStep.setRoleName(AccessibleRole.PANEL.toDisplayString());
//
//		Component hoverParent = hoverComponent.getParent();
//		newStep.setParentName(getCaptureComponentName(hoverParent));
//		newStep.setParentRoleName(hoverParent.getAccessibleContext()
//				.getAccessibleRole().toDisplayString());
//		newStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
//				hoverComponent, newStep.getWindow(),
//				newStep.getAction()));
//		newStep.getParameters().add(JFCBasicHoverHandler.Hover_Command
//				+ GUITARConstants.NAME_SEPARATOR + mouseXPosition
//				+ GUITARConstants.NAME_SEPARATOR + mouseYPosition);
//		newStep.getParameters().add(JFCBasicHoverHandler.Wait_Command
//				+ GUITARConstants.NAME_SEPARATOR + hoverWaitSeconds);
//		savedSteps.add(newStep);
	}
	/**
	 *
	 * Save a button click sent over RMI.
	 * @param mouseEvent
	 * @param componentID
	 * @param componentRoleName
	 * @param windowName
	 */
	public void saveMinimalButtonClick(AWTEvent event, String componentID, String windowName, String componentRoleName)
	{
		if(eventPrintOn)
			System.out.println(event);

		JavaStepType newStep = new JavaStepType();

		newStep.setX(0);
		newStep.setY(0);
		newStep.setHeight(0);
		newStep.setWidth(0);
		String[] idParts = componentID.split(JavaTestInteractions.name_version_separator, -1);
		newStep.setComponentID(idParts[0]);
		newStep.setComponent(idParts[1]);

		newStep.setParentName("");
		newStep.setParentRoleName("");
		newStep.setAction(ActionClass.ACTION.actionName);
		if(StringTools.charactersIn(componentRoleName, GUITARConstants.NAME_SEPARATOR.charAt(0)) == 2) {
			String[] parts = componentRoleName.split(GUITARConstants.NAME_SEPARATOR);
			String roleString, xClick, yClick;
			roleString = parts[0]; xClick = parts[1]; yClick = parts[2];

			newStep.setRoleName(roleString);
			newStep.getParameters().add("Click"
			+ GUITARConstants.NAME_SEPARATOR + xClick
			+ GUITARConstants.NAME_SEPARATOR + yClick);
		}
		else
			newStep.setRoleName(componentRoleName);

		newStep.setWindow(windowName);
		savedSteps.add(newStep);
	}
	/**
	 * Save an action that led to a toggle button being clicked.
	 */
	public void saveToggleButtonClick(MouseEvent mouseEvent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(mouseEvent);

		JavaStepType newStep = new JavaStepType();
		Component buttonComponent = mouseEvent.getComponent();

		// x, y, height, and width
		newStep.setX(JFCXComponent.getGUITAROffsetX(buttonComponent));
		newStep.setY(JFCXComponent.getGUITAROffsetY(buttonComponent));
		newStep.setHeight(buttonComponent.getHeight());
		newStep.setWidth(buttonComponent.getWidth());

		AccessibleContext buttonContext = buttonComponent.getAccessibleContext();
		newStep.setAction(ActionClass.ACTION.actionName);
		// set name
		newStep.setComponent(JavaCaptureUtils.getCaptureComponentName(buttonComponent));

		newStep.setRoleName(buttonContext.getAccessibleRole().toDisplayString());

		newStep.setWindow(windowName);
		Component buttonParent = buttonComponent.getParent();
		newStep.setParentName(buttonParent.getAccessibleContext().getAccessibleName());
		newStep.setParentRoleName(buttonParent.getAccessibleContext()
				.getAccessibleRole()
				.toDisplayString());

		newStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
				buttonComponent,
				newStep.getWindow(),
				newStep.getAction()));

		savedSteps.add(newStep);
	}
	/**
	 * An enabled JMenuItem from CogToolHelper AUT has been clicked. Save the menu item selection
	 * itself AND its parent menu item buttons, leading up to its root: a JMenuBar, or JPopupMenu's hook.
	 *
	 * Preconditions: 	mouseEvent was generated by a menuItem component.
	 * Postconditions:	The menu button that was clicked is saved.
	 */
	public void saveMenuItemSelection(AWTEvent mouseEvent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(mouseEvent);

		JavaStepType newStep = new JavaStepType();
		Component itemComponent = (Component)mouseEvent.getSource();;
		newStep.setAction(ActionClass.ACTION.actionName); // for menu or menu item
		newStep.setWindow(windowName);

		// x and y coordinates are irrelevant in menu items and menus.
		newStep.setX(0);
		newStep.setY(0);

		newStep.setHeight(itemComponent.getHeight());
		newStep.setWidth(itemComponent.getWidth());
		newStep.setComponent(JavaCaptureUtils.getCaptureComponentName(itemComponent));
//		newStep.setComponent(itemComponent.getAccessibleContext().getAccessibleName());
		// get the role from the context too.
		newStep.setRoleName(AccessibleRole.MENU_ITEM.toDisplayString());

		// in order to add the menu steps in order, use a linked list
		// and then reverse the order of all steps captured.
		LinkedList<JavaStepType> menuSteps = new LinkedList<JavaStepType>();
		menuSteps.push(newStep);

		 // save a reference to the last known element in this menu tree.
		Component menuStepRoot = itemComponent.getParent();
		newStep.setParentName(menuStepRoot.getAccessibleContext().getAccessibleName());

		AccessibleRole parentRole;

		// set component ID
		newStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
				itemComponent,
				newStep.getWindow(),
				newStep.getAction()));

		// search for parent JMENUS in the menu tree that lead to this one.
		// make sure that we never attempt to search a null parent via this search.
		// parent check
		while(menuStepRoot!=null) {
			parentRole = menuStepRoot.getAccessibleContext().getAccessibleRole();

			if(menuStepRoot instanceof JPopupMenu)
				menuStepRoot = ((JPopupMenu)menuStepRoot).getInvoker(); // skip JPopupMenu container elements.
			else if(parentRole.equals(AccessibleRole.MENU)
					|| parentRole.equals(AccessibleRole.MENU_ITEM)) {
				// create a new step before the last saved menu item.
				JavaStepType parentStep = new JavaStepType();
				parentStep.setX(0);
				parentStep.setY(0);
				parentStep.setHeight(menuStepRoot.getHeight());
				parentStep.setWidth(menuStepRoot.getWidth());
				parentStep.setComponent(menuStepRoot.getAccessibleContext().getAccessibleName());
				parentStep.setRoleName(parentRole.toDisplayString());
				parentStep.setWindow(windowName);
				parentStep.setAction(ActionClass.ACTION.actionName);

				// set component ID
				parentStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
						menuStepRoot,
						parentStep.getWindow(),
						parentStep.getAction()));

				menuStepRoot = menuStepRoot.getParent();
				if(menuStepRoot != null)
					parentStep.setParentName(menuStepRoot.getAccessibleContext().getAccessibleName());
				else
					parentStep.setParentName("");

				// save this new step root.
				menuSteps.push(parentStep);
			}
			else
				break; // if we find any menuElement that isn't a menu item or menu.
		}
		savedSteps.addAll(menuSteps);
	}
	public void saveMinimalMenuItemSelection(String[][] components, String windowName)
	{
		JavaStepType newStep = new JavaStepType();

		for(int i = 0; i < components.length; i++) {
			String componentID = components[i][0];
			newStep.setX(0);
			newStep.setY(0);
			newStep.setHeight(0);
			newStep.setWidth(0);

			String[] idParts = componentID.split(JavaTestInteractions.name_version_separator, -1);
			newStep.setComponentID(idParts[0]);
			newStep.setComponent(idParts[1]);

			newStep.setParentName("");
			newStep.setParentRoleName("");
			newStep.setAction(ActionClass.ACTION.actionName);
			newStep.setRoleName(components[i][1]);
			newStep.setWindow(windowName);

			savedSteps.add(newStep);
			newStep = new JavaStepType();
		}
	}

	public void saveKeyEntry(KeyEvent keyEvent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(keyEvent);
		String eventCommand = getCommandKey(keyEvent, windowName);

		if((keyEvent.isActionKey() || isModifierOnly(keyEvent)) && eventCommand.isEmpty())
			return; // unrecognized command key.

		// CONTINUATION STEP
		// if this is a command
		if(eventCommand.equals(CommandKey.SPACE.keyText) && isAccessibleTextEditor(keyEvent.getComponent()))
			eventCommand = ""; // clear out the command status of this space bar.
		if(!eventCommand.isEmpty()) {
			// if we were saving text, start over
			if(savingText == TEXT_STRING_SAVE)
				flushTextEntryToSavedSteps();
			if(savingText == NO_SAVE)
				newWorkingTextStep(keyEvent.getComponent(), windowName);
			savingText = COMMAND_SAVE;
		}
		else {
			// if we were entering commands, start over
			if(savingText == COMMAND_SAVE)
				flushCommandKeysToSavedSteps();
			if(savingText == NO_SAVE)
				newWorkingTextStep(keyEvent.getComponent(), windowName);
			savingText = TEXT_STRING_SAVE;
		}

		// SAVE WORKING ELEMENT step
		if(!eventCommand.isEmpty()) {
			if(savedText.isEmpty())
				savedText += eventCommand;
			else
				savedText += GUITARConstants.CMD_ARGUMENT_SEPARATOR + eventCommand;
		}
		else {
			if(isModifierOnly(keyEvent))
				return;
			else if(keyEvent.getKeyCode() == VK_BACK_SPACE) {
				handleDeletion(1);
				return;
			}
			else {
				String keyMeaning;
				if(keyEvent.getKeyChar() == KeyEvent.CHAR_UNDEFINED)
					keyMeaning = KeyEvent.getKeyText(keyEvent.getKeyCode());
				else
					keyMeaning = "" + keyEvent.getKeyChar();
				// check if the key meaning could not be found.
				if(keyMeaning == null || keyMeaning.equals(String.valueOf("" + null)))
					return;
				savedText += keyMeaning;
			}
		}
	}
	/**
	 * Modify the current text step by augmenting the text currently being saved.
	 * @param numChars
	 */
	private void handleDeletion(int numChars)
	{
		if(savedText.length() >= numChars)
			savedText = savedText.substring(0, savedText.length() - numChars);
	}
	/**
	 * Instantiate a new working text step for use with the saveKey methods
	 */
	private void newWorkingTextStep(Component textComponent, String windowName)
	{
		workingTextStep = new JavaStepType();

		 // save component related information
		 // location information
		workingTextStep.setAction(ActionClass.TEXT.actionName); // for paragraph, text, or document
		workingTextStep.setX(JFCXComponent.getGUITAROffsetXInWindow(textComponent));
		workingTextStep.setY(JFCXComponent.getGUITAROffsetYInWindow(textComponent));
		workingTextStep.setHeight(textComponent.getHeight());
		workingTextStep.setWidth(textComponent.getWidth());

		 // use the accessible name to set the step name.
		AccessibleContext textContext = textComponent.getAccessibleContext();
		workingTextStep.setComponent(JavaCaptureUtils.getCaptureComponentName(textComponent));

		workingTextStep.setRoleName(textContext.getAccessibleRole().toDisplayString());

		 // set the window, parent, and parent role information.
		workingTextStep.setWindow(windowName);
		Component textParent = textComponent.getParent();
		workingTextStep.setParentName(textParent.getAccessibleContext().getAccessibleName());
		workingTextStep.setParentRoleName(textParent
				.getAccessibleContext()
				.getAccessibleRole()
				.toDisplayString());
		 // set componentID
		workingTextStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
				textComponent,
				workingTextStep.getWindow(),
				workingTextStep.getAction()));
	}
	public void saveMinimalKeyEntry(String[] keyData, char keyChar, String componentID, String windowName, String componentRoleName)
	{
		int keyCode = Integer.parseInt(keyData[0]);
		boolean consumed = Boolean.parseBoolean(keyData[1]);
		String keyName = keyData[2];
		if(eventPrintOn)
			System.out.println("KeyEvent: " + keyName + " KeyChar: " + keyChar + " consumed: " + consumed);
		String[] splitCID = componentID.split(JavaTestInteractions.name_version_separator, -1);
		String eventCommand = getCommandKey(keyCode, consumed, windowName);

		if(isUnrecognizedActionKey(keyCode) && eventCommand.isEmpty())
			return; // unrecognized command key.
		// CONTINUATION STEP
		if(eventCommand.equals(CommandKey.SPACE.keyText) && isAccessibleTextEditorRole(componentRoleName))
			eventCommand = "";
		// clear out the command status of this space bar because we're using it in a text editor.

		if(!eventCommand.isEmpty()) {
			// if we were saving text, or are editing a new component than before, start over
			if(workingTextStep != null && !workingTextStep.getComponentID().equals(splitCID[0]))
				flushTextEntryToSavedSteps();
			else if(savingText == TEXT_STRING_SAVE)
				flushTextEntryToSavedSteps();

			if(savingText != NO_SAVE)
				newMinimalWorkingTextStep(componentID, windowName, componentRoleName);
			savingText = COMMAND_SAVE;
		}
		else {
			// if we were entering commands, or are editing a new component than before, start over
			if(workingTextStep != null && !workingTextStep.getComponentID().equals(splitCID[0]))
				flushTextEntryToSavedSteps();
			else if(savingText == COMMAND_SAVE)
				flushCommandKeysToSavedSteps();

			if(savingText == NO_SAVE)
				newMinimalWorkingTextStep(componentID, windowName, componentRoleName);
			savingText = TEXT_STRING_SAVE;
		}

		// SAVED TEXT STEP
		if(!eventCommand.isEmpty()) {
			if(savedText.isEmpty())
				savedText += eventCommand;
			else
				savedText += GUITARConstants.CMD_ARGUMENT_SEPARATOR + eventCommand;
		}
		else {
			if(isModifierOnly(keyCode))
				return;
			else if(keyCode == VK_BACK_SPACE) {
				handleDeletion(1);
				return;
			}
			else {
				String keyMeaning;
				if(keyChar == KeyEvent.CHAR_UNDEFINED)
					keyMeaning = KeyEvent.getKeyText(keyCode);
				else
					keyMeaning = "" + keyChar;
				// check if the key meaning could not be found.
				if(keyMeaning == null || keyMeaning.equals(String.valueOf("" + null)))
					return;
				savedText += keyMeaning;
			}
		}
	}

	/**
	 * Respond to a text event sent over RMI.
	 * @param keyEvent
	 * @param componentID
	 * @param windowName
	 * @param componentRoleName
	 */
	public void saveMinimalKeyEntry(KeyEvent keyEvent, String componentID,  String windowName, String componentRoleName)
	{
		if(eventPrintOn)
			System.out.println(keyEvent);

		String[] splitCID = componentID.split(JavaTestInteractions.name_version_separator, -1);
		String eventCommand = getCommandKey(keyEvent, windowName);

		if(keyEvent.isActionKey() && eventCommand.isEmpty())
			return; // unrecognized command key.
		// CONTINUATION STEP
		if(eventCommand.equals(CommandKey.SPACE.keyText) && isAccessibleTextEditorRole(componentRoleName))
			eventCommand = "";
		// clear out the command status of this space bar because we're using it in a text editor.

		if(!eventCommand.isEmpty()) {
			// if we were saving text, or are editing a new component than before, start over
			if(workingTextStep != null && !workingTextStep.getComponentID().equals(splitCID[0]))
				flushTextEntryToSavedSteps();
			else if(savingText == TEXT_STRING_SAVE)
				flushTextEntryToSavedSteps();

			if(savingText != NO_SAVE)
				newMinimalWorkingTextStep(componentID, windowName, componentRoleName);
			savingText = COMMAND_SAVE;
		}
		else {
			// if we were entering commands, or are editing a new component than before, start over
			if(workingTextStep != null && !workingTextStep.getComponentID().equals(splitCID[0]))
				flushTextEntryToSavedSteps();
			else if(savingText == COMMAND_SAVE)
				flushCommandKeysToSavedSteps();

			if(savingText == NO_SAVE)
				newMinimalWorkingTextStep(componentID, windowName, componentRoleName);
			savingText = TEXT_STRING_SAVE;
		}

		// SAVED TEXT STEP
		if(!eventCommand.isEmpty()) {
			if(savedText.isEmpty())
				savedText += eventCommand;
			else
				savedText += GUITARConstants.CMD_ARGUMENT_SEPARATOR + eventCommand;
		}
		else {
			if(isModifierOnly(keyEvent))
				return;
			else if(keyEvent.getKeyCode() == VK_BACK_SPACE) {
				handleDeletion(1);
				return;
			}
			else {
				String keyMeaning;
				if(keyEvent.getKeyChar() == KeyEvent.CHAR_UNDEFINED)
					keyMeaning = KeyEvent.getKeyText(keyEvent.getKeyCode());
				else
					keyMeaning = "" + keyEvent.getKeyChar();
				// check if the key meaning could not be found.
				if(keyMeaning == null || keyMeaning.equals(String.valueOf("" + null)))
					return;
				savedText += keyMeaning;
			}
		}
	}
	/**
	 * Instantiate a new working text step for use with the saveMinimalKey methods.
	 * @param componentID
	 * @param windowName
	 * @param roleName
	 */
	private void newMinimalWorkingTextStep(String componentID, String windowName, String roleName)
	{

		workingTextStep = new JavaStepType();
		workingTextStep.setX(0);
		workingTextStep.setY(0);
		workingTextStep.setHeight(0);
		workingTextStep.setWidth(0);
		String[] idParts = componentID.split(JavaTestInteractions.name_version_separator);
		if(idParts.length > 0) {
			workingTextStep.setComponentID(idParts[0]);
			workingTextStep.setComponent(idParts[1]);
		}
		else
			workingTextStep.setComponentID(idParts[0]);

		workingTextStep.setParentName("");
		workingTextStep.setParentRoleName("");

		workingTextStep.setAction(ActionClass.TEXT.actionName);
		workingTextStep.setRoleName(roleName);
		workingTextStep.setWindow(windowName);
	}
	/**
	 * Save the action of the user clicking a combo box component, and detect the sequence
	 * of actions leading up to the user completing a selection.
	 * @param actionEvent
	 * @param windowName
	 */
	public void saveComboClick(MouseEvent mouseEvent, Component comboComponent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(mouseEvent);

		AccessibleContext comboContext = comboComponent.getAccessibleContext();

		JavaStepType comboClickStep;
		comboClickStep = new JavaStepType();
		//x, y, width, height
		comboClickStep.setX(JFCXComponent.getGUITAROffsetX(comboComponent));
		comboClickStep.setY(JFCXComponent.getGUITAROffsetY(comboComponent));
		comboClickStep.setWidth(comboComponent.getWidth());
		comboClickStep.setHeight(comboComponent.getHeight());

		// name and role from context
		comboClickStep.setComponent(JavaCaptureUtils.getCaptureComponentName(comboComponent));
		comboClickStep.setRoleName(comboContext.getAccessibleRole().toDisplayString());

		Component parent = comboComponent.getParent();

		// parent name and role
		if(parent.getAccessibleContext() != null) {
			comboClickStep.setParentName(parent.getAccessibleContext().getAccessibleName());
			comboClickStep.setParentRoleName(parent.getAccessibleContext().getAccessibleRole().toDisplayString());
		}
		else {
			comboClickStep.setParentName("");
			comboClickStep.setParentRoleName("");
		}

		comboClickStep.setWindow(windowName);
		//action: since this >Clicks< the combo box, this is a JFCAction
		comboClickStep.setAction(ActionClass.ACTION.actionName);

		comboClickStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
				comboComponent,
				comboClickStep.getWindow(),
				ActionClass.ACTION.actionName)); // combo boxes normally using the action SelectFromParent.
		workingComboStep = comboClickStep;
		workingCombo = comboComponent;
	}
	/**
	 * Save a combo box selection event.
	 */
	public void saveComboSelect(ActionEvent actionEvent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(actionEvent);

		Component comboComponent = workingCombo;
		AccessibleContext comboContext = comboComponent.getAccessibleContext();

		JavaStepType comboSelectStep =  new JavaStepType();
		//x, y, width, height
		comboSelectStep.setX(JFCXComponent.getGUITAROffsetX(comboComponent));
		comboSelectStep.setY(JFCXComponent.getGUITAROffsetY(comboComponent));
		comboSelectStep.setWidth(comboComponent.getWidth());
		comboSelectStep.setHeight(comboComponent.getHeight());

		// name and role from context
		comboSelectStep.setComponent(JavaCaptureUtils.getCaptureComponentName(comboComponent));
		comboSelectStep.setRoleName(comboContext.getAccessibleRole().toDisplayString());

		Component parent = comboComponent.getParent();

		// parent name and role
		if(parent.getAccessibleContext() != null) {
			comboSelectStep.setParentName(parent.getAccessibleContext().getAccessibleName());
			comboSelectStep.setParentRoleName(parent.getAccessibleContext().getAccessibleRole().toDisplayString());
		}
		else {
			comboSelectStep.setParentName("");
			comboSelectStep.setParentRoleName("");
		}

		comboSelectStep.setWindow(windowName);
		//action: this action "selects" an item from the combo box.
		comboSelectStep.setAction(ActionClass.PARSELECT.actionName);
		List<String> parameters = new LinkedList<String>();
		List<Integer> cSelection = getAccessibleSelectionFrom(comboComponent);
		for(int i : cSelection)
			parameters.add(Integer.toString(i));
		comboSelectStep.setParameters(parameters);

		comboSelectStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
				comboComponent,
				comboSelectStep.getWindow(),
				ActionClass.PARSELECT.actionName));

		savedSteps.add(workingComboStep);
		savedSteps.add(comboSelectStep);
	}
	/**
	 * Save a combo box selection sent over RMI
	 */
	public void saveMinimalComboSelect(String componentID, String windowName, List<Integer> selection)
	{
		JavaStepType newStep = new JavaStepType();

		newStep.setX(0);
		newStep.setY(0);
		newStep.setHeight(0);
		newStep.setWidth(0);
		String[] idParts = componentID.split(JavaTestInteractions.name_version_separator);
		if(idParts.length > 0) {
			newStep.setComponentID(idParts[0]);
			newStep.setComponent(idParts[1]);
		}
		else
			newStep.setComponentID(idParts[0]);

//		newStep.setComponent("");
//		newStep.setComponentID(componentID);

		newStep.setParentName("");
		newStep.setParentRoleName("");

		newStep.setAction(ActionClass.PARSELECT.actionName);
		newStep.setRoleName(AccessibleRole.COMBO_BOX.toDisplayString());
		newStep.setWindow(windowName);

		LinkedList<String> parameters = new LinkedList<String>();
		for(int i : selection)
			parameters.add(Integer.toString(i));
		newStep.setParameters(parameters);

		savedSteps.add(newStep);
	}
	/**
	 * Collect actions that result in list items being selected.
	 */
	public void saveListItemSelection(MouseEvent focusEvent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(focusEvent);

		Component listComponent = focusEvent.getComponent();
		AccessibleContext listContext = listComponent.getAccessibleContext();

		// pointer comparison. Is the list we're looking at the same as the one we had before?
		if(listComponent != workingList) {
			flushListItemSelectionToSavedSteps();
			savingASelection = false;
		}

		if(!savingASelection) {
			workingListSelectionStep = new JavaStepType();
			// x, y, width, and height
			Component parentPane = listComponent.getParent();
			AccessibleContext parentContext = parentPane.getAccessibleContext();
			while(parentContext != null && !parentContext.getAccessibleRole().equals(AccessibleRole.SCROLL_PANE)) {
				parentPane = parentPane.getParent();
				parentContext = parentPane.getAccessibleContext();
			}
			workingListSelectionStep.setX(JFCXComponent.getGUITAROffsetX(parentPane));
			workingListSelectionStep.setY(JFCXComponent.getGUITAROffsetY(parentPane));
			workingListSelectionStep.setHeight(listComponent.getHeight());
			workingListSelectionStep.setWidth(listComponent.getWidth());

			// name and role from context
//			workingListSelectionStep.setComponent(listContext.getAccessibleName());
			workingListSelectionStep.setComponent(JavaCaptureUtils.getCaptureComponentName(listComponent));
			workingListSelectionStep.setRoleName(listContext.getAccessibleRole().toDisplayString());

			 // parent name and role
			AccessibleContext listParentContext = null;
			if(listComponent.getParent() != null)
				listParentContext = listComponent.getParent().getAccessibleContext();

			if(listParentContext == null) {
				workingListSelectionStep.setParentName("");
				workingListSelectionStep.setParentRoleName("");
			}

			else {
				workingListSelectionStep.setParentName(listParentContext.getAccessibleName());
				workingListSelectionStep.setParentRoleName(listParentContext.getAccessibleRole().toDisplayString());
			}

			workingListSelectionStep.setAction(ActionClass.SELECTION.actionName); // for list and list selections.
			workingListSelectionStep.setWindow(windowName);

			// set component ID
			workingListSelectionStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupLargeObjectID(
					listComponent,
					workingListSelectionStep.getWindow(),
					workingListSelectionStep.getAction()));

			workingList = listComponent;
			savingASelection = true;
		}
	}
	/**
	 * Save a list item selection sent from RMI.
	 */
	public void saveMinimalListItemSelection(String componentID, String windowName)
	{
		workingListSelectionStep = new JavaStepType();
		workingListSelectionStep.setX(0);
		workingListSelectionStep.setY(0);
		workingListSelectionStep.setHeight(0);
		workingListSelectionStep.setWidth(0);
		String[] idParts = componentID.split(JavaTestInteractions.name_version_separator, -1);
		if(idParts.length > 0) {
			workingListSelectionStep.setComponentID(idParts[0]);
			workingListSelectionStep.setComponent(idParts[1]);
		}
		else
			workingListSelectionStep.setComponentID(idParts[0]);
		workingListSelectionStep.setParentName("");
		workingListSelectionStep.setParentRoleName("");

		workingListSelectionStep.setAction(ActionClass.SELECTION.actionName);
		workingListSelectionStep.setRoleName(AccessibleRole.LIST.toDisplayString());
		workingListSelectionStep.setWindow(windowName);
		savingASelection = true;
	}
	public void saveTabSelection(MouseEvent mouseEvent, PropertyChangeEvent changeEvent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(changeEvent);

		JavaStepType newStep = new JavaStepType();
		Component tabsComponent = mouseEvent.getComponent();
		Accessible pageComponent = (Accessible)changeEvent.getSource();
		newStep.setX(JFCXComponent.getGUITAROffsetX(tabsComponent));
		newStep.setY(JFCXComponent.getGUITAROffsetY(tabsComponent));
		newStep.setHeight(tabsComponent.getHeight());
		newStep.setWidth(tabsComponent.getWidth());

		newStep.setAction(ActionClass.PARSELECT.actionName);

		AccessibleContext tabsContext = tabsComponent.getAccessibleContext();
		newStep.setComponent(JavaCaptureUtils.getCaptureComponentName(tabsComponent));

//		String tabsName = tabsContext.getAccessibleName();
//		newStep.setComponent(tabsName);
		// set component name and window name
//		if(tabsName == null || tabsName.isEmpty())
//			newStep.setComponent("");
		newStep.setRoleName(tabsContext.getAccessibleRole().toDisplayString());
		newStep.setWindow(windowName);
		// set parent
		Component tabsParent = tabsComponent.getParent();
		newStep.setParentName(tabsParent.getAccessibleContext().getAccessibleName());
		newStep.setParentRoleName(tabsParent.getAccessibleContext()
				.getAccessibleRole()
				.toDisplayString());


		// set component ID.
		newStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupID(
				tabsComponent,
				newStep.getWindow(),
				newStep.getAction()));
		LinkedList<String> myParams = new LinkedList<String>();
		myParams.add(""+pageComponent.getAccessibleContext().getAccessibleIndexInParent());
		newStep.setParameters(myParams);
		savedSteps.add(newStep);
	}
	/**
	 * Save the click of a table cell in a table interface.
	 */
	public void saveTableCellClick(MouseEvent mouseEvent, String windowName)
	{
		if(eventPrintOn)
			System.out.println(mouseEvent);

		Component tableComponent = mouseEvent.getComponent();
		AccessibleContext tableContext = tableComponent.getAccessibleContext();
		JavaStepType newStep = new JavaStepType();
		newStep.setX(JFCXComponent.getGUITAROffsetX(tableComponent));
		newStep.setY(JFCXComponent.getGUITAROffsetY(tableComponent));
		newStep.setWidth(tableComponent.getWidth());
		newStep.setHeight(tableComponent.getHeight());
//		String tableName = tableContext.getAccessibleName();
//		newStep.setComponent(tableName);
//		if(tableName == null)
//			newStep.setComponent("");
		newStep.setComponent(JavaCaptureUtils.getCaptureComponentName(tableComponent));
		newStep.setRoleName(tableContext.getAccessibleRole().toDisplayString());

		// parent, parent name, and role
		Component parent = tableComponent.getParent();
		if(parent.getAccessibleContext() != null) {
			newStep.setParentName(parent.getAccessibleContext().getAccessibleName());
			newStep.setParentRoleName(parent.getAccessibleContext().getAccessibleRole().toDisplayString());
		}
		else {
			newStep.setParentName("");
			newStep.setParentRoleName("");
		}

		newStep.setWindow(windowName);
		// action: since this step "selects" a specific cell within the table, this is a JFCSelectFromParent interaction
		newStep.setAction(ActionClass.PARSELECT.actionName);
		TableModel model = new TableModel(tableContext);
		List<String> parameters = new LinkedList<String>();
		List<Integer> cSelection = getAccessibleSelectionFrom(tableComponent);
		for(String s : model.cellsForIndices(cSelection))
			parameters.add(s);

		newStep.setParameters(parameters);
		newStep.setComponentID(captureMonitor.interactionsForWindow(windowName).lookupLargeObjectID(
				tableComponent,
				newStep.getWindow(),
				newStep.getAction()));
		savedSteps.add(newStep);
	}
	// END OF SAVE METHODS
	/**
	 * Save all previously saved text entered into a text object, and save the text insertion step.
	 *
	 * Preconditions:	A capture has started and is currently running
	 * Postconditions: 	Text that was entered previously is saved to a "typed text entry" step.
	 * 					The test case recorded that it is currently not saving text.
	 * 					True is returned if a new step was added to savedSteps. False otherwise.
	 */
	public boolean flushTextEntryToSavedSteps()
	{
		if(!savedText.isEmpty()) {
			// try to combine this new action with the last one.
			JavaStepType[] all = savedSteps.toArray(new JavaStepType[0]);
			int recent = savedSteps.size()-1;
			if(!savedSteps.isEmpty() && all[recent].equals(workingTextStep)) {
				all[recent].addParameter("TextInsert" + GUITARConstants.NAME_SEPARATOR + savedText);
				savedSteps = new LinkedHashSet<JavaStepType>(Arrays.asList(all));
			}
			// otherwise use the newly created working text step.
			else {
				workingTextStep.addParameter("TextInsert" + GUITARConstants.NAME_SEPARATOR + savedText);
				savedSteps.add(workingTextStep);
			}
			savedText = "";
			savingText = NO_SAVE;
			return true;
		}
		return false;
	}

	public boolean flushTextEntryToSavedStepsWithCommand()
	{
		if(!savedText.isEmpty()) {
			// try to combine this new action with the last one.
			JavaStepType[] all = savedSteps.toArray(new JavaStepType[0]);
			int recent = savedSteps.size()-1;
			if(!savedSteps.isEmpty() && all[recent].equals(workingTextStep)) {
				all[recent].addParameter("TextInsert" + GUITARConstants.NAME_SEPARATOR + savedText);
				savedSteps = new LinkedHashSet<JavaStepType>(Arrays.asList(all));
			}
			// otherwise use the newly created working text step.
			else {
				workingTextStep.addParameter("TextInsert" + GUITARConstants.NAME_SEPARATOR + savedText);
				savedSteps.add(workingTextStep);
			}
			savedText = "";
			savingText = NO_SAVE;
			return true;
		}
		return false;
	}

	/**
	 * Save a key command to a text object, and save the step to the currently running savedSteps.
	 *
	 * Preconditions:	A capture has started and is currently running
	 * Postconditions: 	the Command denoted by commandName is saved to a "typed command" step.
	 *
	 */
	public boolean flushCommandKeysToSavedSteps()
	{
		if(!savedText.isEmpty()) {
			JavaStepType[] all = savedSteps.toArray(new JavaStepType[0]);
			int recent = savedSteps.size()-1;
			if(!savedSteps.isEmpty() && all[recent].equals(workingTextStep)) {
				//workingTextStep.setParameters(Arrays.asList());
				//savedSteps.peekLast().addParameter("Command" + GUITARConstants.NAME_SEPARATOR + savedText);
				all[recent].addParameter("Command" + GUITARConstants.NAME_SEPARATOR + savedText);
				savedSteps = new LinkedHashSet<JavaStepType>(Arrays.asList(all));
			}
			// otherwise use the newly created working text step.
			else {
				workingTextStep.addParameter("Command" + GUITARConstants.NAME_SEPARATOR + savedText);
				savedSteps.add(workingTextStep);
			}
			savedText = "";
			savingText = NO_SAVE;
			return true;
		}
		return false;
	}
	public boolean flushStoredKeystrokes()
	{
		if(savingText == TEXT_STRING_SAVE)
			return flushTextEntryToSavedSteps();
		else if(savingText == COMMAND_SAVE)
			return flushCommandKeysToSavedSteps();
		return false;
	}

	/**
	 * Network call to perform the operation that flushes text items depending on the current state of things.
	 */
	@Override
	public void flushTextItems() throws RemoteException
	{
		boolean printSteps = flushStoredKeystrokes();
		if(printSteps) {
			numSteps = savedSteps.size();
			if(runAsServer) {
				System.out.println("\n>\t" + numSteps + " steps.");
				System.out.println(stepsString(true));
			}
		}
	}

	@Override
	public void flushListItems(List<Integer> selection) throws RemoteException
	{
		flushListItemSelectionToSavedSteps(selection);
		if(savedSteps.size() != numSteps) {
			numSteps = savedSteps.size();
			System.out.println("\n>\t" + numSteps + " steps.");
			System.out.println(stepsString(true));
		}
	}
	/**
	 * Flush the current selection performed on lists
	 * to the working list selection step, and
	 * save that step in the savedSteps list.
	 *
	 * Preconditions:	none
	 * Postconditions:	if a list item is being saved, save the list selection to the list of saved steps, and clear
	 * 					the list selection.
	 */
	public void flushListItemSelectionToSavedSteps()
	{
		List<Integer> workingListSelection = getAccessibleSelectionFrom(workingList);

		if(!workingListSelection.isEmpty()) {
			LinkedList<String> parameters = new LinkedList<String>();
			for(int i : workingListSelection)
				parameters.add(Integer.toString(i));

			workingListSelectionStep.setParameters(parameters);
			savedSteps.add(workingListSelectionStep);
			savingASelection = false;
			workingList = null;
		}
	}
	/**
	 * Another method to save the list items collected.
	 * @param workingListSelection
	 */
	public void flushListItemSelectionToSavedSteps(List<Integer> workingListSelection)
	{
		if(!workingListSelection.isEmpty() && savingASelection) {
			LinkedList<String> parameters = new LinkedList<String>();
			for(int i : workingListSelection)
				parameters.add(Integer.toString(i));

			workingListSelectionStep.setParameters(parameters);
			savedSteps.add(workingListSelectionStep);
			savingASelection = false;
		}
	}

	public void gotWindowCloseEvent(String componentID, String windowName)
	{
		JavaStepType newStep = new JavaStepType();
		newStep.setX(0);
		newStep.setY(0);
		newStep.setHeight(0);
		newStep.setWidth(0);
		newStep.setAction(ActionClass.WINDOW.actionName);
		newStep.setComponent(windowName);
		newStep.setRoleName(AccessibleRole.WINDOW.toDisplayString());
		newStep.setComponentID(componentID);
		newStep.setWindow(windowName);
		savedSteps.add(newStep);

		if(savedSteps.size() != numSteps) {
			numSteps = savedSteps.size();
			System.out.println("\n>\t" + numSteps + " steps.");
			System.out.println(stepsString(true));
		}
	}

	//RMI RETRIEVALMETHODS
	@Override
	public void shutMeDown() throws RemoteException
	{
		System.out.println("Shutting down other application...");
		guiLauncher.closeApplicationInstances();
		boolean ended = EventFlowSlicer.endRMISession(true, guiLauncher.getRMIRegistryPort());
		if(!ended)
			System.err.println("JavaCaptureTaskList: App Capture is still in session.");
		else
			System.out.println("Other App was shut down.");
		endAndGetTaskList();
	}


	@Override
	public void gotKeyEvent(String[] keyData, char keyChar, String componentID, String windowName, String componentRoleName)
	{
		saveMinimalKeyEntry(keyData, keyChar, componentID, windowName, componentRoleName);
		if(savedSteps.size() != numSteps) {
			numSteps = savedSteps.size();
			System.out.println("\n>\t" + numSteps + " steps.");
			System.out.println(stepsString(true));
		}
	}


	public void gotHoverEvent(String componentID, String componentRoleName, String windowName)
	{
		JavaStepType newStep = new JavaStepType();

		newStep.setX(0);
		newStep.setY(0);
		newStep.setHeight(0);
		newStep.setWidth(0);
		String[] idParts = componentID.split(JavaTestInteractions.name_version_separator, -1);
		newStep.setComponentID(idParts[0]);
		newStep.setComponent(idParts[1]);

		newStep.setParentName("");
		newStep.setParentRoleName("");
		newStep.setAction(ActionClass.HOVER.actionName);
		if(StringTools.charactersIn(componentRoleName, GUITARConstants.NAME_SEPARATOR.charAt(0)) == 2) {
			String[] parts = componentRoleName.split(GUITARConstants.NAME_SEPARATOR);
			String roleString, xClick, yClick;
			roleString = parts[0]; xClick = parts[1]; yClick = parts[2];

			newStep.setRoleName(roleString);
			newStep.getParameters().add("Hover"
			+ GUITARConstants.NAME_SEPARATOR + xClick
			+ GUITARConstants.NAME_SEPARATOR + yClick
			+ GUITARConstants.NAME_SEPARATOR + "Wait"
			+ GUITARConstants.NAME_SEPARATOR + "3");
		}
		else
			newStep.setRoleName(componentRoleName);

		newStep.setWindow(windowName);
		savedSteps.add(newStep);
		if(savedSteps.size() != numSteps) {
			numSteps = savedSteps.size();
			System.out.println("\n>\t" + numSteps + " steps.");
			System.out.println(stepsString(true));
		}
	}
	@Override
	public void gotEvent(AWTEvent nextEvent, String eventID, String windowName, String componentRoleData)
		throws RemoteException
	{
		switch(nextEvent.getID())
		{
		case ActionEvent.ACTION_PERFORMED	:
		case MouseEvent.MOUSE_CLICKED	:
		case MouseEvent.MOUSE_PRESSED	:
		case MouseEvent.MOUSE_RELEASED 	:
			saveMinimalButtonClick(nextEvent, eventID, windowName, componentRoleData);
			break;
		case KeyEvent.KEY_PRESSED		:
		case KeyEvent.KEY_RELEASED		:
		case KeyEvent.KEY_TYPED			:
			saveMinimalKeyEntry((KeyEvent)nextEvent, eventID, windowName, componentRoleData);
			break;
		}

		if(savedSteps.size() != numSteps) {
			numSteps = savedSteps.size();
			System.out.println("\n>\t" + numSteps + " steps.");
			System.out.println(stepsString(true));
		}
	}

	public void gotBackgroundClickEvent(String componentID, String windowName, String clickType, int xClick, int yClick)
	{
		JavaStepType newStep = new JavaStepType();

		newStep.setX(0);
		newStep.setY(0);
		newStep.setHeight(0);
		newStep.setWidth(0);
		String[] idParts = componentID.split(JavaTestInteractions.name_version_separator, -1);
		newStep.setComponentID(idParts[0]);
		newStep.setComponent(idParts[1]);

		newStep.setParentName("");
		newStep.setParentRoleName("");
		newStep.setAction(ActionClass.ACTION.actionName);
		newStep.setRoleName(AccessibleRole.PANEL.toDisplayString());
		newStep.setWindow(windowName);
		newStep.getParameters().add("Click"
			+ GUITARConstants.NAME_SEPARATOR + xClick
			+ GUITARConstants.NAME_SEPARATOR + yClick
		);
		savedSteps.add(newStep);
		if(savedSteps.size() != numSteps) {
			numSteps = savedSteps.size();
			System.out.println("\n>\t" + numSteps + " steps.");
			System.out.println(stepsString(true));
		}
	}
	public void gotPageTabEvent(String eventId, String windowName, String tabData)
	{
		saveMinimalPageTabSelection(eventId, windowName, tabData);
	}

	public void saveMinimalPageTabSelection(String componentID, String windowName, String tabData)
	{
		JavaStepType newStep = new JavaStepType();

		newStep.setX(0);
		newStep.setY(0);
		newStep.setHeight(0);
		newStep.setWidth(0);
		String[] idParts = componentID.split(JavaTestInteractions.name_version_separator, -1);
		newStep.setComponentID(idParts[0]);
		newStep.setComponent(idParts[1]);

		newStep.setParentName("");
		newStep.setParentRoleName("");
		newStep.setAction(ActionClass.ACTION.actionName);
		newStep.setRoleName(AccessibleRole.PAGE_TAB_LIST.toDisplayString());
		newStep.setWindow(windowName);

		ArrayList<String> params = new ArrayList<String>();
		params.add(tabData);
		newStep.setParameters(params);

		savedSteps.add(newStep);
		if(savedSteps.size() != numSteps) {
			numSteps = savedSteps.size();
			System.out.println("\n>\t" + numSteps + " steps.");
			System.out.println(stepsString(true));
		}
	}

	@Override
	public void gotListEvent(String eventID, String windowName) throws RemoteException
	{
		saveMinimalListItemSelection(eventID, windowName);
	}



	public void gotMenuItemEvent(String[][] components, String windowName) throws RemoteException
	{
		saveMinimalMenuItemSelection(components, windowName);
		if(savedSteps.size() != numSteps) {
			numSteps = savedSteps.size();
			System.out.println("\n>\t" + numSteps + " steps.");
			System.out.println(stepsString(true));
		}
	}
	@Override
	public void gotComboSelectEvent(String eventID, String windowName, List<Integer> selection) throws RemoteException
	{
		saveMinimalComboSelect(eventID, windowName, selection);
		if(savedSteps.size() != numSteps) {
			numSteps = savedSteps.size();
			System.out.println("\n>\t" + numSteps + " steps.");
			System.out.println(stepsString(true));
		}
	}
	/**
	 * A very complicated yet carefully selected sequence of steps that spawns an RMI thread
	 * that allows us to begin capturing user events.
	 * @author jsaddle
	 *
	 */
	private class RMICapture extends Thread implements Thread.UncaughtExceptionHandler
	{
		private final Thread guiLauncherThread;

		public RMICapture()
		{
			this.guiLauncherThread = new Thread(guiLauncher);
		}
		public void run()
		{
			try {
				Thread.interrupted();
				guiLauncherThread.start();
				guiLauncherThread.join();
				activated.release();
				Thread.sleep(1000);
				activated.acquire();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				System.out.println("JavaCaptureTaskList: RMI Capture thread shutdown was interrupted.");
			}
		}
		@Override
		public void uncaughtException(Thread t, Throwable e)
		{
			if(guiLauncher.started)
				guiLauncher.closeApplicationInstances();
			EventFlowSlicerErrors.errorOut(e);
		}
	}
}
