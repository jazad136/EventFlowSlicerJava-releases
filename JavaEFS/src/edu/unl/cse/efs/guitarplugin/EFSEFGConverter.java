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

package edu.unl.cse.efs.guitarplugin;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.guitar.awb.ActionTypeProvider;
import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.EventGraphType;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.EventsType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.RowType;
import edu.umd.cs.guitar.model.wrapper.GUIStructureWrapper;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.EventWrapper;
import edu.umd.cs.guitar.model.wrapper.GUITypeWrapper;
import edu.umd.cs.guitar.ripper.plugin.GraphConverter;

/**
 * Gui Structure to Event Flow Graph converter for purposes of EventFlowSlicer replay.
 * The following changes have been made to this class over the GUITAR version.
 * A removal of auxilary methods that were not necessary in this EFGGenerator and were related to graphviz,
 * The addition of a shouldProcess method that helps filter events we don't want to appear in the EFG.
 * this is the only real change.
 *
 * @author Jonathan Saddler
 *
 * @version 1.1
 */
public class EFSEFGConverter implements GraphConverter {

	public String graphicsLibrary;
	public boolean handleHoverEvents;

	/**
	 * By default, set the graphics library to be JFC.
	 */
	public EFSEFGConverter()
	{
		this.graphicsLibrary = "JFC";
		handleHoverEvents = true;
	}

	/**
	 * Construct a new CTHEFGConverter that utilizes event wrappers as perscribed by the graphics
	 * library parameter. (For now, only JFC CTH event wrappers are supported.)
	 * @param graphicsLibrary
	 */
	public EFSEFGConverter(String graphicsLibrary)
	{
		if(graphicsLibrary.toUpperCase().equals("JFC"))
			this.graphicsLibrary = "JFC";
		else
			this.graphicsLibrary = "JFC";
		handleHoverEvents = true;
	}
	/**
	 *
	 */
	private static final String EVENT_ID_SPLITTER = "_";
	/**
     *
     */
	private static final String EVENT_ID_PREFIX = "e";
	ObjectFactory factory = new ObjectFactory();

	EFG efg;
	EventsType dEventList;
	List<List<String>> eventGraph;

	/**
	 * Event list wrapper
	 */
	List<EventWrapper> wEventList = new ArrayList<EventWrapper>();

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.umd.cs.guitar.graph.plugin.GraphConverter#getInputType()
	 */
	public Class<?> getInputType() {
		return GUIStructure.class;
	}

	public void setHandleHoverEvents(boolean on)
	{
		handleHoverEvents = on;
	}

	/**
	 * Convert the GUI Structure to an Event Flow Graph and Return the Event
	 * Flow Graph Object
	 *
	 * @param obj GUI Structure to convert
	 * @return the Event Flow Graph corresponding to the i,put GUI Structure
	 * @see GraphConverter
	 */
	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.umd.cs.guitar.graph.plugin.GraphConverter#generate(java.lang.Object)
	 */
	public EFG generate(Object obj) throws InstantiationException {
		if (!(obj instanceof GUIStructure)) {
			throw new InstantiationException("The input class should be "
					+ GUIStructure.class.getName());
		}

		GUIStructure dGUIStructure = (GUIStructure) obj;

		dGUIStructure.getGUI().get(0).getContainer().getContents()
				.getWidgetOrContainer();
		GUIStructureWrapper wGUIStructure = new GUIStructureWrapper(
				dGUIStructure);

		wGUIStructure.parseData();

		List<GUITypeWrapper> wWindowList = wGUIStructure.getGUIs();

		for(int i=0; i<wWindowList.size(); i++)
			System.out.println(wWindowList.get(i).getTitle());

		System.out.println("size: " + wWindowList.size());

		/* Read event list */
		for (GUITypeWrapper window : wWindowList)
			readEventList(window.getContainer());


		efg = factory.createEFG();

		// -------------------------------------
		// Reading event name
		// -------------------------------------
		dEventList = factory.createEventsType();
		for (EventWrapper wEvent : wEventList) {
			EventType dEvent = factory.createEventType();

			String index = getIndexFromWidget(wEvent);

			// dEvent.setEventId(EVENT_ID_PREFIX + index);
			dEvent.setEventId(wEvent.getID());

			dEvent.setWidgetId(wEvent.getComponent().getFirstValueByName(
					GUITARConstants.ID_TAG_NAME));

			dEvent.setType(wEvent.getType());
			dEvent.setName(wEvent.getName());
			dEvent.setAction(wEvent.getAction());
			dEvent.setListeners(wEvent.getListeners());

			if (wEvent.getComponent().getWindow().isRoot()
					&& !wEvent.isHidden())
				dEvent.setInitial(true);
			else
				dEvent.setInitial(false);

			dEventList.getEvent().add(dEvent);
		}

		efg.setEvents(dEventList);

		// -----------------------------
		// Building graph
		// -----------------------------
		eventGraph = new ArrayList<List<String>>();
		EventGraphType dEventGraph = factory.createEventGraphType();
		List<RowType> lRowList = new ArrayList<RowType>();

		for (EventWrapper firstEvent : wEventList) {
			int indexFirst = wEventList.indexOf(firstEvent);
			RowType row = factory.createRowType();

			/* Check follows edge*/
			for (EventWrapper secondEvent : wEventList) {
				int indexSecond = wEventList.indexOf(secondEvent);

				int cellValue = firstEvent.isFollowedBy(secondEvent);

				row.getE().add(indexSecond, cellValue);
			}

			lRowList.add(indexFirst, row);
		}
		dEventGraph.setRow(lRowList);
		efg.setEventGraph(dEventGraph);
		return efg;
	}

	public StringBuffer printGraphviz() {

		StringBuffer result = new StringBuffer();
		result.append("{\n");
		// result.append("/* NODES */");
		// result.append("\n");
		// for (EventWrapper event : wEventList) {
		//
		// result.append("\t\""
		// + event.getComponent().getFirstValueByName(
		// GUITARConstants.TITLE_TAG_NAME) + "\"");
		// result.append("\n");
		// }

		List<RowType> lRow = efg.getEventGraph().getRow();
		result.append("\n");
		result.append("/* EDGES */");
		result.append("\n");
		for (int row = 0; row < lRow.size(); row++) {
			List<Integer> rowVals = lRow.get(row).getE();

			for (int col = 0; col < rowVals.size(); col++) {

				int cell = rowVals.get(col);
				if (cell > 0) {
					EventWrapper firstEvent = wEventList.get((row));
					EventWrapper secondEvent = wEventList.get(col);
					String firstAction = firstEvent.getAction();
					int ind = firstAction.lastIndexOf(".");
					String firstActionName = firstAction.substring(ind+1, firstAction.length());

					String secondAction = secondEvent.getAction();
					int ind2 = secondAction.lastIndexOf(".");
					String secondActionName = secondAction.substring(ind2+1, secondAction.length());



					String sFirstTitle// = firstEvent.getID();



					 =firstEvent.getComponent().getFirstValueByName(GUITARConstants.TITLE_TAG_NAME) + " " +
					 firstEvent.getComponent().getFirstValueByName(GUITARConstants.CLASS_TAG_NAME) + " "
					 + "(" + firstEvent.getComponent().getParent().getFirstValueByName(GUITARConstants.TITLE_TAG_NAME) + ") "
					 + ActionTypeProvider.getTypeFromActionHandler(firstActionName);
					String sSecondTitle //= secondEvent.getID();
					 =secondEvent.getComponent().getFirstValueByName(GUITARConstants.TITLE_TAG_NAME) + " " +
					secondEvent.getComponent().getFirstValueByName(GUITARConstants.CLASS_TAG_NAME) + " " +
					"(" + secondEvent.getComponent().getParent().getFirstValueByName(GUITARConstants.TITLE_TAG_NAME) + ") " +
					ActionTypeProvider.getTypeFromActionHandler(secondActionName);


					result.append("\t" + "\"" + sFirstTitle + "\"" + "->"
							+ "\"" + sSecondTitle + "\"");
					result.append("\t/*" + cell + "*/");

					result.append("\n");

				}

			}

		}
		result.append("}\n");
		return (result);
	}

	/**
	 * @param wEvent
	 * @return
	 */
	private String getIndexFromWidget(EventWrapper wEvent) {
		// TODO Auto-generated method stub

		String index = wEvent.getComponent().getFirstValueByName(
				GUITARConstants.ID_TAG_NAME);
		index = index.substring(1);
		return index;
	}

	/**
	 * Get the event list contained in a component
	 *
	 * For each ReplayableAction stored in component,
	 * save the widgetId, eventID (derived from widgetId),
	 * listeners, the componentTypeWrapper itself,
	 * and the name of the action into an EventWrapper object,
	 * and store this list in the global wEventList of this EFGConverter instance.
	 *
	 * Do this recursively for all children of component.
	 * @param component
	 * @return
	 */
	private void readEventList(ComponentTypeWrapper component) {

		List<String> sActionList = component.getValueListByName(GUITARConstants.EVENT_TAG_NAME);
		System.out.println(component.getValueListByName(GUITARConstants.TITLE_TAG_NAME));

		if (sActionList != null)
			for (String action : sActionList) {
				// jsaddle: this is the only real change to this class, other than a removal of a few other methods

				EventWrapper wEvent;
				if(this.graphicsLibrary.equals("JFC"))
					wEvent = new EFSJFCEventWrapper();
				else
					wEvent = new EventWrapper();

				// Calculate event ID
				String sWidgetID = component.getFirstValueByName(GUITARConstants.ID_TAG_NAME);

				sWidgetID = sWidgetID.substring(1);

				String sEventID = EVENT_ID_PREFIX + sWidgetID;

				String posFix = (sActionList.size() <= 1) ? ""
						: EVENT_ID_SPLITTER
								+ Integer.toString(sActionList.indexOf(action));
				sEventID = sEventID + posFix;

				wEvent.setID(sEventID);
				wEvent.setAction(action);
				wEvent.setComponent(component);
				wEvent.setListeners(component
						.getValueListByName("ActionListeners"));

				if(shouldProcess(wEvent))
					wEventList.add(wEvent);
			}

		List<ComponentTypeWrapper> wChildren = component.getChildren();
		if (wChildren != null)
			for (ComponentTypeWrapper wChild : wChildren) {
				readEventList(wChild);
			}
	}

	/**
	 * Returns true if this EventWrapper wrapper should be processed and inserted into the EFG
	 * and edges should be calculated leading to and from it, and returns false if it should not
	 * be inserted into the EFG, and edges should not be calculated for this wrapper.
	 */
	public boolean shouldProcess(EventWrapper wrapper)
	{
		if(!handleHoverEvents)
			return wrapper.getAction().equals(ActionClass.HOVER.actionName) == false;
		return true;

	}
}
