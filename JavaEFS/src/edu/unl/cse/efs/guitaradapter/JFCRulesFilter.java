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
package edu.unl.cse.efs.guitaradapter;


import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.MenuElement;

import edu.umd.cs.guitar.awb.JavaActionTypeProvider;
import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.event.GEvent;
import edu.umd.cs.guitar.event.JFCMenuReopenHandler;
import edu.umd.cs.guitar.event.JFCSelectFromParent;
import edu.umd.cs.guitar.model.GComponent;
import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.JFCConstants;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.JFCXWindow;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.*;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.ripper.JFCRipperMointor;
import edu.umd.cs.guitar.ripper.adapter.GRipperAdapter;
import edu.umd.cs.guitar.ripper.adapter.IgnoreSignExpandFilter;
import edu.umd.cs.guitar.ripper.adapter.JFCTabFilter;
import edu.umd.cs.guitar.util.GUITARLog;
import edu.unl.cse.efs.ripper.JFCRipperConfigurationEFS;
import edu.unl.cse.efs.tools.TaskListConformance;
import edu.unl.cse.guitarext.JavaTestInteractions;


/**
 * Source for the IgnoreComponentByType filter for the JFC Swing libraries.
 * The IgnoreComponentByType filter is a ripper adapter designed to
 * help in the process of ripping components. If this filter is detected
 * during the ripping process, only certain widgets will actually be ripped from
 * the GUI that match types specified in the Widgets list in some file
 * specified to the constructor. This filter can also be configured to
 * match only the types specified in a list of ignored components.
 *
 * @author Jonathan Saddler
 */
public class JFCRulesFilter extends GRipperAdapter {

	public static boolean FULL_RIP = true;
	private TaskList taskList;
	private List<Widget> parameterizedWidgets;
	public List<FullComponentType> myComponentTypes;
	static ObjectFactory fact = new ObjectFactory();
	private boolean useTaskList = false;
	String[] targetClasses;
	String[] targetNames;
	JFCRipperMointor monitor;
	private ObjectFactory factory;
	private ComponentListType lOpenWindowComps;
	private GUIStructure guiStructure;
	/**
	 * This variable stores XML being <strong>returned</strong> from component and container extractions,
	 * so that caller methods can use it.
	 */
	private ComponentType storedComponent;
	private boolean keepFound;
	private boolean componentIgnoreOn, tabbingOn;

	private IgnoreSignExpandFilter signatureSubFilter;
	private JFCTabFilter tabFilter;
	/** This Variable stores a list of the strings of the windows we've ripped, and updates
	 * the monitor's list of windows too */
	public static LinkedHashSet<String> rippedWindows;
	public ArrayList<Widget> pHolderWidgets;
	public ArrayList<Integer> pHolderI;
	public ArrayList<String> pHolderStrings;
	public boolean[] pHolderCovered;
	public JFCRulesFilter(JFCRipperMointor monitor)
	{
		rippedWindows = monitor.lRippedWindow;
		guiStructure = (new ObjectFactory()).createGUIStructure();
		XMLHandler handler = new XMLHandler();
		taskList = (TaskList) handler.readObjFromFile(JFCRipperConfigurationEFS.RULES_FILE, TaskList.class);
		if(taskList == null) {
			throw new RuntimeException("Could not read flowbehind rules file: " + JFCRipperConfigurationEFS.RULES_FILE);
		}
		pHolderWidgets = new ArrayList<Widget>();
		pHolderStrings = new ArrayList<String>();
		pHolderI = new ArrayList<Integer>();
		for(int i = 0; i < taskList.getWidget().size(); i++) {
			Widget w = taskList.getWidget().get(i);
			if(!placeholder(w).isEmpty()) {
				pHolderWidgets.add(w);
				pHolderStrings.add(placeholder(w));
				pHolderI.add(i);
			}
		}
		pHolderCovered = new boolean[pHolderWidgets.size()];

		myComponentTypes = new LinkedList<FullComponentType>();
		factory = new ObjectFactory();
		lOpenWindowComps = factory.createComponentListType();
//		widgetIDs = widgetIDsFromTaskList();
		useTaskList = true;
		componentIgnoreOn = false;
		parameterizedWidgets = new ArrayList<Widget>();
	}


	private static void addRippedList(GWindow window)
	{
		String windowTitle = window.getTitle();
		rippedWindows.add(windowTitle);
	}
	public static LinkedList<String> getRippedWindowListCopy()
	{
		return new LinkedList<>(rippedWindows);
	}

	public TaskList getTasklist()
	{
		return taskList;
	}

	/**
	 * Return the list of widgets that were ripped that contain special parameter values.
	 * @return
	 */
	public List<Widget> getParameterizedWidgets()
	{
		return parameterizedWidgets;
	}


	/**
	 * If parameters of a specific widget are available at the time of the rip,
	 * ensure that their parameters are saved to a matching tasklist file,
	 * so that after this step is complete the parameters can be retrieved.
	 *
	 * If the GUI artifact has too many or too few actions attached to it to be
	 * useful in an EFG, according to the captured widgets in the tasklist,
	 * restore the output content's state to match the tasklist specification
	 * for what replayable actions are supported.
	 */
	private void modifyArtifacts(JFCXComponent component, GWindow window)
	{
		String theClass = component.getClassVal();
		// modify the GUI file.
		ComponentTypeWrapper ctw = new ComponentTypeWrapper(storedComponent);

		boolean inferPTAction = false;
		// if we have a page tab or mouse panel parent.
		if(theClass.equals(AccessibleRole.PAGE_TAB_LIST.toDisplayString())) {
			ContainerType currentType = (ContainerType)storedComponent; // a page tab list is a container.
			List<ComponentType> possibles = currentType.getContents().getWidgetOrContainer();

			LinkedList<String> indices = new LinkedList<String>();
			for(int i = 0; i < possibles.size(); i++) {
				ComponentTypeWrapper innerW = new ComponentTypeWrapper(possibles.get(i));
				String index = innerW.getFirstValueByName(JFCConstants.INDEX_TAG);
				if(index != null)
					indices.add(index);
			}
			if(!indices.isEmpty()) // Make "widgets" for the new indices, and add these to the parameterized widgets list.
				for(String ind : indices) {
					Widget nw = factory.createWidget();
					JFCXComponent.nowRipping = false;
					nw.setEventID(component.getCTHEventID());
					JFCXComponent.nowRipping = true;
					nw.setName(component.getTitle());
					nw.setAction(ActionClass.PARSELECT.actionName);
					nw.setType(theClass);
					nw.setWindow(window.getTitle());
					nw.setParameter(ind);
					parameterizedWidgets.add(nw);
					System.out.println("\n\n----- INFERRED WIDGET ----- ");
					System.out.println("----- " + nw.getEventID() + " -----\n\n");
				}
			else { // let the index be 0.
				Widget nw = factory.createWidget();
				JFCXComponent.nowRipping = false;
				nw.setEventID(component.getCTHEventID());
				JFCXComponent.nowRipping = true;
				nw.setName(component.getTitle());
				nw.setAction(ActionClass.PARSELECT.actionName);
				nw.setType(theClass);
				nw.setWindow(window.getTitle());
				nw.setParameter("0");
				parameterizedWidgets.add(nw);
				System.out.println("\n\n----- INFERRED WIDGET ----- ");
				System.out.println("----- " + nw.getEventID() + " -----\n\n");
				inferPTAction = true;
			}
		}
//		String win = window.getTitle();
		// remove unnecessary actions.
		PropertyType act = ctw.getFirstPropertyByName(GUITARConstants.EVENT_TAG_NAME);
		PropertyType eid = ctw.getFirstPropertyByName(GUITARConstants.CTH_EVENT_ID_NAME);
		if(act != null) {
			for(int i = 0; i < act.getValue().size(); i++) {
				String nextEId = eid.getValue().get(i);
				String nextAct = act.getValue().get(i);
				// iterate through the values and
				// if the widget is not found by our methods.
				if(inferPTAction)
					if(ActionClass.PARSELECT.actionName.equals(nextAct))
						// we just added this above. Needs to be kept
						continue;

				int idx = findWidgetByEventId(nextEId, taskList.getWidget());
				if(idx == -1) {
					// remove this action from the stored component
					act.getValue().remove(i);
					eid.getValue().remove(i);
					if(act.getValue().isEmpty()) {
						ctw.removeProperty(GUITARConstants.CTH_EVENT_ID_NAME);
						ctw.removeProperty(GUITARConstants.EVENT_TAG_NAME);
						break;
					}
				}
				else {
					// handle placeholders.
					Widget matched = taskList.getWidget().get(idx);
					int phIdx = pHolderStrings.indexOf(matched.getEventID());
					if(phIdx != -1 && !pHolderCovered[phIdx]) {
						// get the tooltip and replace the widget.
						Widget copy = pHolderWidgets.get(phIdx).copyOf(fact);

						String ttText = ctw.getFirstValueByName(GUITARConstants.TOOLTIPTEXT_TAG_NAME);
						pHolderWidgets.get(phIdx).setParameter("TextInsert_" + ttText);

						propagateParameters(pHolderWidgets.get(phIdx), copy);
						pHolderCovered[phIdx] = true;
					}
				}
			}
		}


	}
	public List<Widget> getPlaceHolderWidgets()
	{
		return pHolderWidgets;
	}

	/**
	 * Throughout the tasklist, make sure the parameters for this widget are matched
	 */
	private void propagateParameters(Widget pw, Widget cw)
	{
		for(Required r : taskList.getRequired()) {
			for(Widget w : r.getWidget()) {
				if(w.equals(cw)) {
					w.setParameter(pw.getParameter());
				}
			}
		}
		for(Exclusion e : taskList.getExclusion()) {
			for(Widget w : e.getWidget()) {
				if(w.equals(cw)) {
					w.setParameter(pw.getParameter());
				}
			}
		}
		for(Order o : taskList.getOrder()) {
			for(OrderGroup og : o.getOrderGroup()) {
				for(Widget w : og.getWidget()) {
					if(w.equals(cw))
						w.setParameter(pw.getParameter());
				}
			}
		}
		for(Atomic o : taskList.getAtomic()) {
			for(AtomicGroup og : o.getAtomicGroup()) {
				for(Widget w : og.getWidget()) {
					if(w.equals(cw))
						w.setParameter(pw.getParameter());
				}
			}
		}
		for(Repeat r : taskList.getRepeat()) {
			for(Widget w : r.getWidget()) {
				if(w.equals(cw))
					w.setParameter(pw.getParameter());
			}
		}
		for(Stop s : taskList.getStop()) {
			for(Widget w : s.getWidget()) {
				if(w.equals(cw))
					w.setParameter(pw.getParameter());
			}
		}
	}
	private String placeholder(Widget w)
	{
		if(w.getParameter() != null && !w.getParameter().isEmpty()) {
			String wParam = w.getParameter();
			String target = TaskListConformance.TARGET_PHOLDER_KEYWORD;
			if(wParam.length() >= target.length() && wParam.substring(0, target.length()).equals(target)) {
				int sep = wParam.indexOf(GUITARConstants.NAME_SEPARATOR);
				return wParam.substring(sep+1);
			}
		}
		return "";
	}
	protected int findWidgetByEventId(String eventId, List<Widget> allWidgets)
	{
		for(int i = 0; i < allWidgets.size(); i++) {
			if(eventId.equals(allWidgets.get(i).getEventID()))
				return i;
		}
		return -1;
	}
	/**
	 * action, eventID, and window.
	 * @param eventId
	 * @param action
	 * @param window
	 * @return
	 */
	protected int findWidgetByAttributes(String eventId, String action, String window, List<Widget> allWidgets)
	{
		String actionType = JavaActionTypeProvider.getTypeFromActionHandler(action);
		for(int i = 0; i < allWidgets.size(); i++) {
			if(TaskListConformance.matchingNonNullCoreNames(allWidgets.get(i).getEventID(), eventId)
			&& actionType.equalsIgnoreCase(allWidgets.get(i).getAction())
			&& JavaTestInteractions.windowTitlesAreSame(allWidgets.get(i).getWindow(), window))
				return i;
		}
		return -1;
	}
	public void setMonitor(JFCRipperMointor monitor)
	{
		this.monitor = monitor;
	}


	public List<String> widgetIDsFromTaskList()
	{
		ArrayList<String> toReturn = new ArrayList<String>();
		if(taskList!=null){
			List<Widget> widgets = taskList.getWidget();
			for(int i=0; i<widgets.size(); i++){
				Widget w = widgets.get(i);
				toReturn.add(w.getName());
			}
		}
		return toReturn;
	}


	public void ignoreComponents(IgnoreSignExpandFilter ignorer)
	{
		if(ignorer == null)
			throw new NullPointerException("Null Ignore filter was passed to JFCFlowBehindRulesFilter");
		signatureSubFilter = ignorer;
		componentIgnoreOn = true;
	}

	public void expandTabs(JFCTabFilter filter)
	{
		if(filter == null)
			throw new NullPointerException("Null Tab Filter was passed to JFCFlowBehindRulesFilter");
		tabFilter = filter;
		tabbingOn = true;
	}

	public void dontExpandTabs()
	{
		tabbingOn = false;
	}

	public void useComponentList(Collection<FullComponentType> ignoredComponents)
	{
		myComponentTypes = new LinkedList<FullComponentType>(ignoredComponents);
		HashSet<String> classes = new HashSet<String>();
		HashSet<String> names = new HashSet<String>();
		for(FullComponentType fct : myComponentTypes) {
			String widgetType = "";
			String widgetName = "";
			List<PropertyType> properties = fct.getComponent().getAttributes().getProperty();
			for(PropertyType p : properties) {
				if(p.getName().equals(GUITARConstants.CLASS_TAG_NAME))
					widgetType = p.getValue().get(0);
				else if(p.getName().equals(GUITARConstants.NAME_TAG_NAME))
					widgetName = p.getValue().get(0);
			}
			if(widgetType.isEmpty())
				classes.add(widgetType);
			if(widgetName.isEmpty())
				names.add(widgetName);
		}
		targetNames = names.toArray(new String[0]);
		targetClasses = classes.toArray(new String[0]);
		useTaskList = false;
	}

	/**
	 * Preconditions: component must be a JFCXComponent
	 */
	@Override
	public boolean isProcess(GComponent component, GWindow window)
	{
		if(component == null)
			return false;
		JFCXComponent javaComponent = (JFCXComponent) component;
		AccessibleContext aContext = javaComponent.getAccessibleContext();

		// if no accessible context, forget it.
		if (aContext == null)
			return false;
		return true;
	}



	public GUIStructure getResult()
	{
		return guiStructure;
	}

	/**
	 * Process a tabbed pane component.
	 */
	public ComponentType ripTabComponent(GComponent component, GWindow window)
	{
		System.out.println("\n------------ BEGIN TABBED COMPONENT ----------");
		System.out.println("Ripping tab component: *" + component.getClassVal() + " " + component.getTitle() + "*");

		keepFound = ripCheck(component, window);
		ComponentType retComp = storedComponent;
		JFCXComponent jComponent = (JFCXComponent) component;
		JTabbedPane jTab = (JTabbedPane) jComponent.getComponent();

		int nChildren = jTab.getComponentCount();
		// if the number of children we've collected is not empty,
		// we should cast the return value as a container upon returning from the function
		boolean has1Child = false;
		if (nChildren > 0 && !(retComp instanceof ContainerType)) {
			ContainerType container = factory.createContainerType();
			container.setContents(factory.createContentsType());
			container.setAttributes(retComp.getAttributes());
			retComp = container;
		}

		boolean keepMe = keepFound;
		ArrayList<String> allKeptC = new ArrayList<String>();
		for (int i = 0; i < nChildren; i++) {
			Component child = jTab.getComponent(i);
			GComponent gChild = new JFCXComponent(child,window);
			GEvent eTabSelect = new JFCSelectFromParent();
			eTabSelect.perform(gChild, null);
			storedComponent = ripComponent(gChild, window);

			if(keepFound) {
				System.out.println("** RETAINING widget \"" + gChild.getTitle() + "\" ** ");
				modifyArtifacts((JFCXComponent)gChild, window);
				ComponentType guiChild = storedComponent;
				allKeptC.add(gChild.getTitle());
				try {
					((ContainerType) retComp).getContents().getWidgetOrContainer().add(guiChild);
					has1Child = true;
				}
				catch (java.lang.ClassCastException e) {
					System.out.println("ClassCastException");
					e.printStackTrace();
					throw e;
				}
			}
		}
		if(has1Child) {
			System.out.println("\n\n** RETAINING parents of " + allKeptC.toString() + " **\n\n");
			keepFound = true;
		}
		else
			keepFound = keepMe;

		return retComp;
	}

	/**
	 * Thanks to JMenu.buildElementArray(JMenu leaf)
	 *
	 * @see JMenu#buildMenuElementArray(JMenu)
	 */
	private MenuElement[] currentMenuPath(JFCXComponent currentComponent)
	{
		Component current = currentComponent.getComponent();
		Vector<MenuElement> path = new Vector<MenuElement>();
		JPopupMenu pop;
		JMenuItem menu; // changed this to menuItem
		JMenuBar bar;

		Component parent = current.getParent();
		AccessibleRole parentRole;
		while(current != null) {
			 parentRole = parent.getAccessibleContext().getAccessibleRole();
			 if(parentRole.equals(AccessibleRole.POPUP_MENU)) {
				 pop = (JPopupMenu)parent;
				 path.insertElementAt(pop, 0);
				 parent = pop.getInvoker();
			 }
			 else if(parentRole.equals(AccessibleRole.MENU)
					 || parentRole.equals(AccessibleRole.MENU_ITEM)) {
				 menu = (JMenuItem)parent;
				 path.insertElementAt(menu, 0);
				 parent = menu.getParent();
			 }
			 else if(parentRole.equals(AccessibleRole.MENU_BAR)) {
				 bar = (JMenuBar)parent;
				 path.insertElementAt(bar, 0);
				 break;
			 }
			 else
				 break;
		}
		MenuElement me[] = new MenuElement[path.size()];
        path.copyInto(me);
        return me;
//
//		return MenuSelectionManager.defaultManager().getSelectedPath();
	}

	/**
	 * Returns whether component is a child of a java menu.
	 * @param component
	 * @return
	 */
	private boolean inAMenu(JFCXComponent component)
	{
		final JFCXComponent parent = (JFCXComponent)component.getParent();
		if(parent != null && parent.isMenu())
			return true;
		return false;
	}
	/**
	 * Recursively processes the component hierarchy under component.
	 */
	@Override
	public ComponentType ripComponent(GComponent component, GWindow window)
	{
		keepFound = false;
		ComponentType retComp = null;
		ComponentType firstComp = null;

		JFCXComponent javaComponent = (JFCXComponent)component;
		JFCXWindow javaWindow = (JFCXWindow)window;
		if(javaComponent.getComponent() == (Component)javaWindow.getWindow()) {
			addRippedList(window);
		}
		AccessibleContext rootContext = javaComponent.getAccessibleContext();
		if (rootContext == null)
			return null;


		try {
			if(tabbingOn && tabFilter.isProcess(component, window))
				return ripTabComponent(component, window);
			System.out.println("\n------------ BEGIN COMPONENT ----------");
			System.out.println("Ripping component: *" + component.getClassVal() + " " + component.getTitle() + "*");
			if(component.getClassVal().equals(AccessibleRole.LIST.toDisplayString())) {
				int stop = 1;
			}

			keepFound = ripCheck(component, window);
			retComp = storedComponent;
			firstComp = storedComponent;
			if(!FULL_RIP)
				return retComp;

			retComp = (new ComponentTypeWrapper(retComp)).getDComponentType();

			boolean expandMe = true;
			if(componentIgnoreOn)
				if(signatureSubFilter.isProcess(component, window))
					expandMe = false;

			if(!expandMe) {
				System.out.println("Component's expandable subhierarchy was ignored.");
				return retComp;
			}

			// 2.1 Try to perform action on the component
			// to reveal more windows/components

			// clear window opened cache before performing actions
			monitor.resetWindowCache();
			MenuElement[] savedPath = new MenuElement[0];
			boolean inAMenu = false;
			if (monitor.isExpandable(component, window)) {
				// Actions before expanding the component
				monitor.expandGUI(component);
				// get the current menu path if it exists.
				inAMenu = inAMenu(javaComponent);
				if(inAMenu)
					savedPath = currentMenuPath(javaComponent);
//				stepCount++;
				// Actions after expanding the component
			}
			else
				System.out.println("Component is Unexpandable");


			if (monitor.isNewWindowOpened()) {
				List<FullComponentType> lOpenComp = lOpenWindowComps
						.getFullComponent();
				FullComponentType cOpenComp = factory.createFullComponentType();
				cOpenComp.setWindow(window.extractWindow().getWindow());
				cOpenComp.setComponent(retComp);
				lOpenComp.add(cOpenComp);
				lOpenWindowComps.getFullComponent().clear();
				lOpenWindowComps.getFullComponent().addAll(lOpenComp);

				LinkedList<GWindow> lNewWindows = monitor
						.getOpenedWindowCache();
				monitor.resetWindowCache();
				System.out.println("*_*" + lNewWindows.size() + " new window(s) opened.*_*");
				for (GWindow newWins : lNewWindows) {
					System.out.println("*Title:*" + newWins.getTitle() + "*");
				}


				// Process the opened windows in a FIFO order
				while (!lNewWindows.isEmpty()) {
					GWindow gNewWin = lNewWindows.getLast();
					lNewWindows.removeLast();

					GObject gWinComp = gNewWin.getContainer();

					if (gWinComp != null) {

						// Add invokelist property for the component
						String sWindowTitle = gNewWin.getTitle();
						ComponentTypeWrapper retCompWrapper = new ComponentTypeWrapper(retComp);
						retCompWrapper.addValueByName(GUITARConstants.INVOKELIST_TAG_NAME, sWindowTitle);
						retComp = retCompWrapper.getDComponentType();

						// Go through a gauntlet to check to see if
						boolean ignoreThisWindow = monitor.isIgnoredWindow(gNewWin);

						if(!ignoreThisWindow && componentIgnoreOn)
							ignoreThisWindow = signatureSubFilter.isProcess((GComponent)gWinComp, gNewWin);
						if (!ignoreThisWindow) {
							if (!monitor.isRippedWindow(gNewWin)) {
								gNewWin.setRoot(false);
								monitor.addRippedList(gNewWin);
								GUIType dWindow = ripWindow(gNewWin);
								if (dWindow != null && keepFound)
									guiStructure.getGUI().add(dWindow);
							}
						}
						else {
							System.out.println("Window was ignored.");
						}
					}
					monitor.closeWindow(gNewWin);
					if(inAMenu) {
						System.out.println("Ripper: Re-opening Menu...");
						JFCMenuReopenHandler action = new JFCMenuReopenHandler();
						action.perform(null, savedPath, null);
					}

				}
			}
			List<GComponent> gChildrenList = component.getChildren();
			int nChildren = gChildrenList.size();

			int i = 0;
			// Change the type of return component to container if there are new children added
			if (nChildren > 0 && !(retComp instanceof ContainerType)) {
				ContainerType container = factory.createContainerType();
				container.setContents(factory.createContentsType());
				container.setAttributes(retComp.getAttributes());
				retComp = container;
			}
			boolean keepMe = keepFound;
			// dfs walk.
			if (nChildren > 0) {
				ArrayList<String> allKeptC = new ArrayList<String>();
				while (i < nChildren) {
					GComponent gChild = gChildrenList.get(i);
					storedComponent = ripComponent(gChild, window);
					if(keepFound) {
						System.out.println("** RETAINING widget \"" + gChild.getTitle() + "\" ** ");
						modifyArtifacts((JFCXComponent)gChild, window);
						ComponentType guiChild = storedComponent;
						allKeptC.add(gChild.getTitle());
						try {((ContainerType) retComp).getContents().getWidgetOrContainer().add(guiChild);}
						catch (java.lang.ClassCastException e) {
							System.out.println("ClassCastException");
							e.printStackTrace();
							throw e;
						}
					}
					i++;
				}
				// if the number of children we've collected is not empty,
				// we should keep the parent of this container upon returning from the function
				if(!((ContainerType)retComp).getContents().getWidgetOrContainer().isEmpty()) {
					System.out.println("\n\n** RETAINING parents of " + allKeptC.toString() + " **\n\n");
					keepFound = true;
					// if we're dealing with a window, make sure to modify artifacts.
					if(isWindow(javaComponent)) {
						storedComponent = firstComp;
						modifyArtifacts(javaComponent, javaWindow);
					}
				}
				else
					keepFound = keepMe;
			}

		}
		catch (Exception e) {

			if (e.getClass().getName().contains("StaleElementReferenceException")) {
				/**
				 * This can happen when performing an action causes a page
				 * navigation in the current window, for example, when
				 * submitting a form.
				 */
				System.err.println("Element went away: " + e.getMessage());
			} else {
				if(e != null)
					System.out.println("ripComponent exception: " + e.getClass().getSimpleName());
				System.out.println("Crash");
			}

			/**
			 * We'll return the component we calculated anyway so it gets added
			 * to the GUI map. I'm not entirely sure this is the right thing to
			 * do, but it gets us further anyway.
			 */
			return retComp;
		}
		return retComp;
	}

	public static boolean isWindow(JFCXComponent component)
	{
		if(component == null)
			return false;
		String classV = component.getClassVal();
		return classV != null &&
				(  classV.equals(AccessibleRole.FRAME.toDisplayString())
				|| classV.equals(AccessibleRole.DIALOG.toDisplayString())
				|| classV.equals(AccessibleRole.WINDOW.toDisplayString()));
	}


	/**
	 * Check to see if the ripped component passes our special test.<br>
	 * <br>
	 * This standard test checks whether the component is in the tasklist solely
	 * using the widget's eventID.
	 *
	 * Preconditions: none
	 * Postconditions: if component is one we should keep in the GUIStructure file,
	 * true is returned, false otherwise.
	 */
	public boolean ripCheck(GComponent component, GWindow window)
	{
		// rip the component and all its children.
		storedComponent = component.extractProperties();
		if(!useTaskList)
			return false;
		if(storedComponent == null)
			return false; // this component contains no valuable information.

		// search for widget by type and name (class and title) and window
		// if any or all are available
		ComponentTypeWrapper wrapper = new ComponentTypeWrapper(storedComponent);

		List<String> eidsInComp = wrapper.getValueListByName(GUITARConstants.CTH_EVENT_ID_NAME);
		if(eidsInComp == null)
			return false; // no event ID to match.
		String widgetEID;
		for(Widget w : taskList.getWidget()) {
			widgetEID = w.getEventID();
			if(widgetEID == null)
				continue;
			boolean foundEID = false;
			for(String eventIDInComp : eidsInComp) {
				if(eventIDInComp != null) {
					if(eventIDInComp.contains("panel")) {
						int end = eventIDInComp.indexOf(JavaTestInteractions.eventId_separator);
						if(end == -1) end = eventIDInComp.length();
						int begin = eventIDInComp.indexOf(GUITARConstants.NAME_SEPARATOR) + 1;
						// get the substring pertaining to the name.
						String subComp = eventIDInComp.substring(begin, end).trim();

						end = widgetEID.length();
						begin = widgetEID.indexOf(GUITARConstants.NAME_SEPARATOR) + 1;
						String subWidg = widgetEID.substring(begin, end).trim();
						if(subComp.equals(subWidg))
							foundEID = true;
					}
					else if(eventIDInComp.equals(widgetEID))
						foundEID = true;

					if(foundEID)
						return true;
				}
			}// end event IDs search
		}// end widgets search
		return false;// we don't need to keep this component, or any of its children.
	}

	/**
	 * Check to see if the ripped component passes our special test.<br>
	 * <br>
	 * This special test checks whether the component is in the tasklist using the
	 * elements of a widget's signature: its class, type, and component name.
	 * Preconditions: none
	 * Postconditions: if component is one we should keep in the GUIStructure file, true is returned from this
	 * function, else, false is returned from this function.
	 */
	public boolean ripCheckBySignature(GComponent component, GWindow window)
	{
		// rip the component and all its children.
		storedComponent = component.extractProperties();
		if(storedComponent == null)
			return false; // this component contains no valuable information.

		// search for widget by type and name (class and title) and window
		// if any or all are available
		ComponentTypeWrapper wrapper = new ComponentTypeWrapper(storedComponent);
		if(useTaskList) {
			String classInComp = wrapper.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);
			String wrapperAction = wrapper.getFirstValueByName(GUITARConstants.EVENT_TAG_NAME);
			String actionOfComp = JavaActionTypeProvider.getTypeFromActionHandler(wrapperAction);
			String windowOfComp = window.getTitle();

//			String eventIDInComp = wrapper.getFirstValueByName(GUITARConstants.CTH_EVENT_ID_NAME);
			String widgetClass
			, widgetWindow
			, widgetAction
			;
			for(Widget w : taskList.getWidget()) {
				widgetClass = w.getType();
				widgetWindow = w.getWindow();
				widgetAction = w.getAction();
				if(widgetClass == null && widgetWindow == null && actionOfComp == null)
					continue;
				if(classInComp != null)
					if(!classInComp.equals(widgetClass))
						continue;

				if(windowOfComp != null) {
					if(!JavaTestInteractions.windowTitlesAreSame(windowOfComp, widgetWindow))
						continue;
				}
				if(actionOfComp != null)
					if(!actionOfComp.equals(widgetAction))
						continue;
				return true; // we need to keep this component, because it matches
							 // a widget in the rules list.
			}
		}
		return false; // we don't need to keep this component, or any of its children.
	}

	public GUIType ripWindow(GWindow gWindow) throws Exception, IOException {
		// Actions before ripping the window

		GUITARLog.log.info("------- BEGIN WINDOW -------");
		GUITARLog.log.info("Ripping window: *" + gWindow.getTitle() + "*");

		// 3. Rip all components of this window
		try {

			GUIType retGUI = gWindow.extractWindow();
			GComponent gWinContainer = gWindow.getContainer();

			ComponentType container = null;

			// Replace window title with pattern if requested (useReg)
			if (gWinContainer != null)
				container = ripComponent(gWinContainer, gWindow);

			if (container != null) {
				retGUI.getContainer().getContents().getWidgetOrContainer()
						.add(container);
			}

			GUITARLog.log.info("-------- END WINDOW --------");


			return retGUI;
		} catch (Exception e) {
			throw e;
		}
	}

}
