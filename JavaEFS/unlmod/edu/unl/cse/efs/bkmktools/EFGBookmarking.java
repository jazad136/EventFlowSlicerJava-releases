/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package edu.unl.cse.efs.bkmktools;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.accessibility.AccessibleRole;

import static javax.accessibility.AccessibleRole.*;
import edu.umd.cs.guitar.awb.ActionTypeProvider;
import edu.umd.cs.guitar.awb.JavaActionTypeProvider;
import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.EventsType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.GUIStructureWrapper;
import edu.unl.cse.guitarext.GUIEventModel;
import edu.unl.cse.efs.tools.PathConformance;



/**
 * Source for the EFGBookmarking class. The EFGBookmarking class is
 * useful for adding to EFG files useful information from GUI files, a process called bookmarking.
 * @author Jonathan Saddler
 */
public class EFGBookmarking {

	private static final List<AccessibleRole> recognizedRoles = new ArrayList<AccessibleRole>(Arrays.asList(new AccessibleRole[]{
			LIST, PUSH_BUTTON,
			TOGGLE_BUTTON, RADIO_BUTTON,
			CHECK_BOX, COMBO_BOX,
			MENU, MENU_ITEM,
			PAGE_TAB_LIST, PANEL, TABLE, TEXT, WINDOW, FRAME, DIALOG
	}));
	private static final List<String> recognizedRoleStrings = new ArrayList<String>();
	{
		for(AccessibleRole role : recognizedRoles)
			recognizedRoleStrings.add(role.toDisplayString());
	}
	private ObjectFactory fact;
	private XMLHandler handler;
	private EFG backingEFG;
	private List<EventType> allEvents;
	private static String NAME_PART_SEPARATOR = "_";
	private static String HIERARCHY_SEPARATOR = "|";
	private static String NAME_VERSION_SEPARATOR = ":";
	private GUIStructureWrapper guiData;

	public static void main(String[] args)
	{
		try{
			doBookmarkAndWriteTo(args[2], args[1], args[0]);
		}
		catch(RuntimeException | FileNotFoundException e) {
			System.err.println("EFG Bookmarking aborted by " + e.getClass().getSimpleName() + ". Now Exiting...");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done.");
	}

	/**
	 * Bookmarks an EFG and writes the file, named with a modification of the efgInputFilename name, to the output directory specified.
	 * @param outputDirectory
	 * @param efgInputFilename
	 * @param guiInputFilename
	 * @throws FileNotFoundException
	 */
	public static void doBookmarkAndWriteTo(String outputDirectory, String efgInputFilename, String guiInputFilename) throws FileNotFoundException
	{
		EFGBookmarking marker = new EFGBookmarking(efgInputFilename, guiInputFilename);
		EFG newEFG = marker.getBookmarked();
		// write the file
		XMLHandler handler = new XMLHandler();
		String newFileName = outputDirectory + File.separator;
		newFileName += PathConformance.parseApplicationName(efgInputFilename) + "_BKMK.EFG";
		System.out.println("Writing file to \n\"" + newFileName + "\"");
		handler.writeObjToFile(newEFG, newFileName);
	}

	public EFGBookmarking(EFG EFGObject, GUIStructure GUIObject)
	{
		fact = new ObjectFactory();
		handler = new XMLHandler();
		backingEFG = EFGObject;
		allEvents = backingEFG.getEvents().getEvent();
		guiData = new GUIStructureWrapper(GUIObject);
		guiData.parseData();
	}

	public EFGBookmarking(String EFGFilename, String GUIFilename) throws FileNotFoundException
	{
		fact = new ObjectFactory();
		handler = new XMLHandler();
		File EFGFile = new File(EFGFilename);
		// test to see if file exists.
		if(!EFGFile.exists())
			throw new FileNotFoundException(EFGFile.getAbsolutePath());
		File GUIFile = new File(GUIFilename);
		if(!GUIFile.exists())
			throw new FileNotFoundException(EFGFile.getAbsolutePath());


		backingEFG = (EFG)handler.readObjFromFile(EFGFile, EFG.class);
		allEvents = backingEFG.getEvents().getEvent();
		GUIStructure jaxbGUI = (GUIStructure)handler.readObjFromFile(GUIFile, GUIStructure.class);
		guiData = new GUIStructureWrapper(jaxbGUI);
		guiData.parseData();
	}


	/**
	 * Solve the problem of detecting the hierarchy of a the item specified from the GUI model (a ComponentTypeWrapper object).
	 * This method provides the hierarchy name if the argument given belongs to a menu hierarchy,
	 * and provides the simple name of the item if the argument is at the top of a menu hierarchy,
	 * or is not a menu element.
	 */
	private static String climbMenuTreeForName(ComponentTypeWrapper item)
	{
		String toReturn = constructRealBase(item);
		ComponentTypeWrapper itemParent = item.getParent();
		if(itemParent != null) {
			String itemParentClass = itemParent.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);
			boolean parentIsMenuElement = itemParentClass != null && itemParentClass.equalsIgnoreCase(AccessibleRole.MENU.toDisplayString());
//			boolean parentIsMenuBarElement = itemParentClass != null && itemParentClass.equals(AccessibleRole.MENU_BAR.toDisplayString());
			if(itemParentClass != null && parentIsMenuElement)
				toReturn = climbMenuTreeForName(itemParent) + HIERARCHY_SEPARATOR + toReturn;
		}
		return toReturn;
	}

	/**
	 * Constructs an event flow graph using the bookmarking algorithm.
	 * @return
	 */
	public static EFG getUnBookmarked(EFG inputEFG)
	{
		ArrayList<EventType> newEvents = new ArrayList<EventType>();
		ObjectFactory fact = new ObjectFactory();
		List<EventType> allEvents = inputEFG.getEvents().getEvent();
		for(int i = 0; i < allEvents.size(); i++) {
			EventType oldEvent = allEvents.get(i);
			String oldEventId = allEvents.get(i).getEventId();
			// assuming the event Id colon is already in place
			// if it's not, do nothing
			int colonPos;
			colonPos = oldEventId.indexOf(':');
			String newEventId;
			if(colonPos != -1)
				newEventId = oldEventId.substring(0, colonPos);
			else
				newEventId = oldEventId;
			EventType newEvent = fact.createEventType();
			newEvent.setType(oldEvent.getType());
			newEvent.setInitial(oldEvent.isInitial());
			newEvent.setAction(oldEvent.getAction());
			newEvent.setWidgetId(oldEvent.getWidgetId());
			newEvent.setEventId(newEventId);
			newEvents.add(newEvent);
		}

		EventsType allNew = fact.createEventsType();
		allNew.setEvent(newEvents);

		EFG toReturn = fact.createEFG();
		toReturn.setEventGraph(inputEFG.getEventGraph());
		toReturn.setEvents(allNew);
		return toReturn;
	}


	/**
	 * Constructs an event flow graph using the bookmarking algorithm.
	 * @return
	 */
	public EFG getBookmarked(boolean resolveMenuHierarchies)
	{
		GUIParser myParser;
		myParser = new GUIParser(guiData);
		ArrayList<EventType> newEvents = new ArrayList<EventType>();
		GUIStrings retStrings;
		String menuString = AccessibleRole.MENU.toDisplayString();
		String menuItemString = AccessibleRole.MENU_ITEM.toDisplayString();
		String menuBarString = AccessibleRole.MENU_BAR.toDisplayString();
		boolean[] menuElement = new boolean[allEvents.size()];
		for(int i = 0; i < allEvents.size(); i++) {
			EventType oldEvent = allEvents.get(i);
			retStrings = myParser.lookupViaJAXB(oldEvent.getWidgetId());
			EventType newEvent = parseIntoEvent(oldEvent, retStrings);
			String guiClass = retStrings.classString().replace('_', ' ');

			menuElement[i] = guiClass.equalsIgnoreCase(menuString) ||
					guiClass.equalsIgnoreCase(menuItemString) ||
					guiClass.equalsIgnoreCase(menuBarString);
			newEvents.add(newEvent);
		}

		if(resolveMenuHierarchies) {
			for(int i = 0; i < newEvents.size(); i++)
				if(menuElement[i]) {
					// get the new widget and its role
					EventType refinedEvent = newEvents.get(i);
					// ensure that the signature is of a valid search parameter type.
					AttributesType signature = trimToSignatureQuality(refinedEvent.getOptional());
					ComponentTypeWrapper ewr = guiData.getComponentBySignaturePreserveTree(signature);
					String role = ewr.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);
					// fix the role string.
					role = role.toLowerCase().replace("_"," ");
					// create the new widget name.
					String newWName = role + NAME_PART_SEPARATOR + climbMenuTreeForName(ewr);
//					refinedEvent.setWidgetId(newWName);
					// create the new event name.
					String newEName = allEvents.get(i).getEventId() + NAME_VERSION_SEPARATOR + newWName + NAME_PART_SEPARATOR + handlerActionTag(refinedEvent.getAction());

					refinedEvent.setEventId(newEName);
					// replace the event in the newEvents list.
					newEvents.set(i, refinedEvent);
				}
		}
		EventsType allNew = fact.createEventsType();
		allNew.setEvent(newEvents);

		EFG toReturn = fact.createEFG();
		toReturn.setEventGraph(backingEFG.getEventGraph());
		toReturn.setEvents(allNew);
		return toReturn;
	}

	public static String handlerActionTag(String actionString)
	{
		String simpleHandlerName = actionString;
		if(simpleHandlerName.contains("."))
			simpleHandlerName = simpleHandlerName.substring(simpleHandlerName.lastIndexOf('.')+1);

		String type;
		if(simpleHandlerName.substring(0, 2).equals("OO"))  // open office actions always begin with "Oh-Oh"
			type = ActionTypeProvider.getTypeFromActionHandler(simpleHandlerName);
		else if(simpleHandlerName.substring(0,3).equals("JFC"))  // open office actions always begin with "JFC"
			type = JavaActionTypeProvider.getTypeFromActionHandler(simpleHandlerName);
		else
			type = "";

		if(type == null)
			return "CLICK";
		else if(type.equals("Click"))
			return "CLICK";
		else if(type.equals("Select"))
			return "SELECT";
		else if(type.equals("Select From Parent"))
			return "SELECT";
		else if(type.equals("Type"))
			return "SELECT"; // selecting within the typed field is the main action
		else if(type.equals("Set Value"))
			return "ASSIGN";
		else if(type.equals("Keyboard Shortcut"))
			return "KEYSTROKE";
		else if(type.equals("Keyboard Access"))
			return "KEYSTROKE"; // the main action is pressing a key
		else if(type.equals("Hover"))
			return "HOVER";
		else if(type.equals("Selective Hover"))
			return "HOVER";
		else if(type.equals("Window Action"))
			return "WINDOWCLOSE";
		else
			return "CLICK";
	}
	private EFG getBookmarked() throws FileNotFoundException
	{
		return getBookmarked(true);
	}

	/**
	 * Trim the attributesType specified down to include only property type elements
	 * that have A) a non-empty value list, and B) a value list containing at least one non
	 * empty, non-null component.
	 * @param origOptional
	 * @return
	 */
	private AttributesType trimToSignatureQuality(AttributesType origOptional)
	{
		ArrayList<PropertyType> newProps = new ArrayList<PropertyType>();
		for(PropertyType oldProp : origOptional.getProperty()) {
			boolean containsSomething = true;
			if(!oldProp.getValue().isEmpty()) {
				for(String value : oldProp.getValue())
					if(value != null && !value.isEmpty()) {
						containsSomething = false;
						break;
					}
			}
			if(!containsSomething)
				newProps.add(oldProp);
		}
		AttributesType attType = fact.createAttributesType();
		attType.setProperty(newProps);
		return attType;
	}
	private EventType parseIntoEvent(EventType oldEventType, GUIStrings guiChunk)
	{
		PropertyType[] attProps = new PropertyType[7];
		attProps[0] = new PropertyType();
		attProps[0].setName(GUITARConstants.ID_TAG_NAME);
		attProps[0].getValue().add(oldEventType.getWidgetId());

		attProps[1] = new PropertyType();
		attProps[1].setName(GUITARConstants.TITLE_TAG_NAME);
		attProps[1].getValue().add(guiChunk.titleString());

		attProps[2] = new PropertyType();
		String theClass = guiChunk.classString();
		attProps[2].setName(GUITARConstants.CLASS_TAG_NAME);
		attProps[2].getValue().add(theClass);

		attProps[3] = new PropertyType();
		attProps[3].setName(GUITARConstants.TYPE_TAG_NAME);
		attProps[3].getValue().add(guiChunk.typeString());

		attProps[4] = new PropertyType();
		attProps[4].setName(GUITARConstants.EVENT_TAG_NAME);
		attProps[4].getValue().addAll(Arrays.asList(guiChunk.ractStrings()));

		attProps[5] = new PropertyType();
		attProps[5].setName(GUITARConstants.DESCRIPTION_TAG_NAME);
		attProps[5].getValue().add(guiChunk.descString());

		attProps[6] = new PropertyType();
		attProps[6].setName(GUITARConstants.TOOLTIPTEXT_TAG_NAME);
		attProps[6].getValue().add(guiChunk.tooltipString());


		EventType toReturn = fact.createEventType();

		// add attributes from parameters.
		AttributesType newOpt = fact.createAttributesType();
		newOpt.setProperty(Arrays.asList(attProps));
		toReturn.setOptional(newOpt);

		toReturn.setType(oldEventType.getType());
		toReturn.setInitial(oldEventType.isInitial());
		toReturn.setAction(oldEventType.getAction());

		// construct widget id and event id.
		String real;
		if(!recognizedRoleStrings.contains(theClass))
			real = constructRealName(guiChunk);
		else {
			real = constructCTHName(guiChunk, oldEventType);
			if(real.equals(GUIEventModel.noAction)
			|| real.equals(GUIEventModel.noText)
			|| real.equals(GUIEventModel.noHover)
			|| real.equals(GUIEventModel.noParSelect)
			|| real.equals(GUIEventModel.noSelect)
			|| real.equals(GUIEventModel.noSelHover)
			|| real.equals(GUIEventModel.hasNoID)
			)
				real = constructRealName(guiChunk);
		}
		toReturn.setWidgetId(oldEventType.getWidgetId());
        toReturn.setEventId(oldEventType.getEventId() + NAME_VERSION_SEPARATOR + real + NAME_PART_SEPARATOR + handlerActionTag(oldEventType.getAction()));
		return toReturn;
	}

	/**
	 * Construct the realName of the event specified by oldEventType.
	 *
	 * If there is one cthEventID return it as the real name.
	 * If there are multiple, return the cthevent id that matches the action specified.
	 * @param guiChunk
	 * @param oldEventType
	 * @return
	 */
	public static String constructCTHName(GUIStrings guiChunk, EventType oldEventType)
	{
		String[] cthStrings = guiChunk.cthEventIDStrings();

//		String old = oldEventType.getEventId();
		String toReturn = cthStrings[0];
		if(cthStrings.length > 1) { // what is the action of this event?
			String oldAction = oldEventType.getAction();
			String eventRole = guiChunk.classString();
			// select the ID that has the proper role attached to it.
			if(eventRole.equals(PANEL.toDisplayString())) {
				if(oldAction.equals(ActionClass.TEXT.actionName))
					toReturn = cthStrings[1]; // if action is text, select the second option
				// else leave the selection as the first option
			}
			else if(eventRole.equals(COMBO_BOX.toDisplayString())) {
				if(oldAction.equals(ActionClass.PARSELECT.actionName))
					toReturn = cthStrings[2]; // if action is parselect, select the third option.
				// else leave the selection as the first option
			}
		}

		if(toReturn.equals(GUIEventModel.noAction)
			|| toReturn.equals(GUIEventModel.noText)
			|| toReturn.equals(GUIEventModel.noHover)
			|| toReturn.equals(GUIEventModel.noParSelect)
			|| toReturn.equals(GUIEventModel.noSelect)
			|| toReturn.equals(GUIEventModel.noSelHover)
			|| toReturn.equals(GUIEventModel.hasNoID)
			|| toReturn.equals(GUIEventModel.noWindow))
			toReturn = cthStrings[0];
		return toReturn;
	}
	public static String constructRealName(GUIStrings guiChunk)
	{
		String role = guiChunk.classString();
		// fix the role string.
		role = role.toLowerCase().replace("_"," ");
		String baseName = "";
		String titleString = guiChunk.titleString();
		if(!titleString.isEmpty())
			baseName = titleString;
		else {
			String desc = guiChunk.descString();
			if(!desc.isEmpty())
				baseName = desc;
			else {
				String tooltip = guiChunk.tooltipString();
				if(!tooltip.isEmpty())
					baseName = tooltip;
				else
					baseName = "Unnamed " + role;
			}
		}
		return role + NAME_PART_SEPARATOR + baseName;
	}


	/**
	 * Get the real name from an XML element within a GUI file.
	 * Choose the title of this CTW, (and if no title choose description, and if no choose the description, and if no description choose
	 * the tooltip, and if no tooltip choose the string "unnamed 'classtype'") to be returned from this method as the
	 * designated real name of the ComponentTypeWrapper specified.
	 */
	public static String constructRealBase(ComponentTypeWrapper ctw)
	{
		String role = ctw.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);
		// fix the role string.
		role = role.toLowerCase().replace("_"," ");
		String baseName = "";
		String titleString = ctw.getFirstValueByName(GUITARConstants.TITLE_TAG_NAME);
		if(!(titleString == null) && !titleString.isEmpty())
			baseName = titleString;
		else {
			String desc = ctw.getFirstValueByName(GUITARConstants.DESCRIPTION_TAG_NAME);
			if(!(desc == null) && !desc.isEmpty())
				baseName = desc;
			else {
				String tooltip = ctw.getFirstValueByName(GUITARConstants.TOOLTIPTEXT_TAG_NAME);
				if(!(tooltip == null) && !tooltip.isEmpty())
					baseName = tooltip;
				else
					baseName = "Unnamed " + role;
			}
		}
		return baseName;
	}

	public static class EFGUnBookmarking
	{
		private File EFGFile;
		private EFG backingEFG;
		private List<EventType> allEvents;
		private XMLHandler handler;
		private ObjectFactory fact;

		public EFGUnBookmarking(String EFGFilename) throws FileNotFoundException
		{
			EFGFile = new File(EFGFilename);
			// test to see if file exists.
			if(!EFGFile.exists())
				throw new FileNotFoundException(EFGFile.getAbsolutePath());
			handler = new XMLHandler();
			backingEFG = (EFG)handler.readObjFromFile(EFGFile, EFG.class);
			allEvents = backingEFG.getEvents().getEvent();
			fact = new ObjectFactory();
		}

		public EFG getUnBookmarked()
		{
			ArrayList<EventType> newEvents = new ArrayList<EventType>();
			for(int i = 0; i < allEvents.size(); i++) {
				EventType oldEvent = allEvents.get(i);
				String oldEventId = allEvents.get(i).getEventId();
				// assuming the event Id colon is already in place
				// if it's not, do nothing
				int colonPos;
				colonPos = oldEventId.indexOf(':');
				String newEventId;
				if(colonPos != -1)
					newEventId = oldEventId.substring(0, colonPos);
				else
					newEventId = oldEventId;
				EventType newEvent = fact.createEventType();
				newEvent.setType(oldEvent.getType());
				newEvent.setInitial(oldEvent.isInitial());
				newEvent.setAction(oldEvent.getAction());
				newEvent.setWidgetId(oldEvent.getWidgetId());
				newEvent.setEventId(newEventId);
				// don't set optional.
				newEvents.add(newEvent);
			}

			EventsType allNew = fact.createEventsType();
			allNew.setEvent(newEvents);
			EFG toReturn = fact.createEFG();
			toReturn.setEventGraph(backingEFG.getEventGraph());
			toReturn.setEvents(allNew);
			return toReturn;
		}
	}

}
