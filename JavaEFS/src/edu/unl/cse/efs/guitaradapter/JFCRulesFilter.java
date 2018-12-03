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
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.*;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.ripper.JFCRipperMointor;
import edu.umd.cs.guitar.ripper.adapter.GRipperAdapter;
import edu.umd.cs.guitar.ripper.adapter.IgnoreSignExpandFilter; 
import edu.umd.cs.guitar.ripper.adapter.JFCTabFilter;
import edu.umd.cs.guitar.util.GUITARLog;
import edu.unl.cse.efs.ripper.JFCRipperConfigurationEFS;
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
	
	private TaskList taskList;
	private List<Widget> parameterizedWidgets;
	public List<FullComponentType> myComponentTypes;
	public static boolean FULL_RIP = true;
	private boolean useTaskList = false;
	String[] targetClasses;
	String[] targetNames;
	JFCRipperMointor monitor;
	private int stepCount;
	private ObjectFactory factory;
	private ComponentListType lOpenWindowComps;
	private ComponentListType lCloseWindowComp;
	private GUIStructure guiStructure;
	private List<String> widgetIDs;
	/**
	 * This variable stores XML being <strong>returned</strong> from component and container extractions,
	 * so that caller methods can use it.
	 */
	private ComponentType storedComponent;
	private boolean keepFound;
	private boolean componentIgnoreOn, tabbingOn;
	
	private IgnoreSignExpandFilter signatureSubFilter;
	private JFCTabFilter tabFilter;
	
	public JFCRulesFilter()
	{
		guiStructure = (new ObjectFactory()).createGUIStructure();
		XMLHandler handler = new XMLHandler(); 
		taskList = (TaskList) handler.readObjFromFile(JFCRipperConfigurationEFS.RULES_FILE, TaskList.class);
		if(taskList == null) {
			throw new RuntimeException("Could not read flowbehind rules file: " + JFCRipperConfigurationEFS.RULES_FILE);
		}
		myComponentTypes = new LinkedList<FullComponentType>();
		factory = new ObjectFactory();
		lOpenWindowComps = factory.createComponentListType();
		lCloseWindowComp = factory.createComponentListType();
		widgetIDs = widgetIDsFromTaskList();
		useTaskList = true;
		componentIgnoreOn = false;
		parameterizedWidgets = new ArrayList<Widget>();
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
	 * ensure that their parameters are saved to a matching widget file, 
	 * so that after this step is complete the parameters can be retrieved.
	 */
	private void inferParametersIfAvailable(JFCXComponent component, GWindow window)
	{
		String theClass = component.getClassVal();
		
//		if(theClass.equals(AccessibleRole.PAGE_TAB_LIST.toDisplayString())) {
		// if we have a page tab or mouse panel parent.
		if( theClass.equals(AccessibleRole.PAGE_TAB_LIST.toDisplayString())) {
//			AccessibleContext aContext = component.getComponent().getAccessibleContext();
			ContainerType currentType = (ContainerType)storedComponent; // a page tab list is a container.
			List<ComponentType> possibles = currentType.getContents().getWidgetOrContainer();
			
			LinkedList<String> indices = new LinkedList<String>();
			for(int i = 0; i < possibles.size(); i++) {
				ComponentTypeWrapper ctw = new ComponentTypeWrapper(possibles.get(i)); 
				String index = ctw.getFirstValueByName(JFCConstants.INDEX_TAG);
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
			}			
		}
		// what if we have a mouse panel?
		// no point in handling this case
//		else if(theClass.equals(AccessibleRole.PANEL.toDisplayString())) {
//			
//		}
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
		
		ArrayList<String> allKeptC = new ArrayList<String>();
		for (int i = 0; i < nChildren; i++) {
			Component child = jTab.getComponent(i);
			GComponent gChild = new JFCXComponent(child,window);
			GEvent eTabSelect = new JFCSelectFromParent();
			eTabSelect.perform(gChild, null);
			storedComponent = ripComponent(gChild, window);
			
			if(keepFound) {
				System.out.println("** RETAINING widget \"" + gChild.getTitle() + "\" ** ");
				inferParametersIfAvailable((JFCXComponent)gChild, window);
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
	
		JFCXComponent javaComponent = (JFCXComponent)component;
		AccessibleContext rootContext = javaComponent.getAccessibleContext();
		if (rootContext == null) 
			return null;
		
		try {
			if(tabbingOn && tabFilter.isProcess(component, window)) 
				return ripTabComponent(component, window);
			System.out.println("\n------------ BEGIN COMPONENT ----------");
			System.out.println("Ripping component: *" + component.getClassVal() + " " + component.getTitle() + "*");
			
			keepFound = ripCheck(component, window);
			retComp = storedComponent;
			
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
				stepCount++;
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
				for (GWindow newWins : lNewWindows) 
					System.out.println("*Title:*" + newWins.getTitle() + "*");
				

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
//						System.out.println("Window " + sWindowTitle + " found.");
						retComp = retCompWrapper.getDComponentType();

						// Go through a gauntlet to check to see if 
						boolean ignoreThisWindow = monitor.isIgnoredWindow(gNewWin);
						
						if(!ignoreThisWindow && componentIgnoreOn) 
							ignoreThisWindow = signatureSubFilter.isProcess((GComponent)gWinComp, gNewWin);
						if (!ignoreThisWindow) {
							if (!monitor.isRippedWindow(gNewWin)) {
								gNewWin.setRoot(false);
								GUIType dWindow = ripWindow(gNewWin);
								if (dWindow != null && keepFound)
									guiStructure.getGUI().add(dWindow);
							} 
							else {
								System.out.println("Window rip complete.");
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
			
			// dfs walk.
			if (nChildren > 0) {
				ArrayList<String> allKeptC = new ArrayList<String>();
				while (i < nChildren) {
					GComponent gChild = gChildrenList.get(i);
					storedComponent = ripComponent(gChild, window);
					if(keepFound) {
						System.out.println("** RETAINING widget \"" + gChild.getTitle() + "\" ** ");
						inferParametersIfAvailable((JFCXComponent)gChild, window);
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
				}
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
				// TODO: Must throw exception
				if(e != null)
					GUITARLog.log.error("ripComponent exception: " + e);
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
	
	
	/**
	 * Check to see if the ripped component passes our special test.
	 * 
	 * Preconditions: none
	 * Postconditions: if component is one we should keep in the GUIStructure file, true is returned from this
	 * function, else, false is returned from this function.
	 */
	public boolean ripCheck(GComponent component, GWindow window)
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
			String eventIDInComp = wrapper.getFirstValueByName(GUITARConstants.CTH_EVENT_ID_NAME);
			String widgetEID 
			, widgetType 
			, widgetWindow
			, widgetAction
			;
			for(Widget w : taskList.getWidget()) {
				widgetEID = w.getEventID();
				widgetType = w.getType();
				widgetWindow = w.getWindow();
				widgetAction = w.getAction();
				if(widgetEID == null && widgetType == null && widgetWindow == null && actionOfComp == null)
					continue;
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
						if(!subComp.equals(subWidg)) {
							continue;
						}
					}
					else if(!eventIDInComp.equals(widgetEID))
						continue;
				}
				
				if(classInComp != null) 
					if(!classInComp.equals(widgetType))
						continue;
				
				if(windowOfComp != null) {
					if(!JavaTestInteractions.windowTitlesAreSame(windowOfComp, widgetWindow))
//					if(!windowOfComp.equals(widgetWindow))
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
