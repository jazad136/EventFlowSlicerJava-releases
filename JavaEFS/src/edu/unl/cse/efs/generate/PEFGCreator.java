package edu.unl.cse.efs.generate;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.data.*;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.GUIStructureWrapper;
import edu.unl.cse.jontools.widget.TaskListConformance;

/**
 * This class is responsible for creating EFG graphs with events that
 * are parameterized with specific parameters.
 * We pull these parameters from the TaskList
 * We separate out the parameters list that is included in EFG, so that
 * each event is assigned one parameter.
 *
 */
public class PEFGCreator
{
	private EFG baseGraph;
	private EFG workingEFG;
	private SearchPack sp;
	private GUIStructureWrapper gsw;
	private final TaskList workingConstraints;
	private List<RowType> graphRows;
	private List<Widget> allWidgets;
	private List<EventType> allEvents;
	public int[][] similar;
	private static ObjectFactory fact = new ObjectFactory();
	private boolean getTooltips;

	/**
	 * Constructor for the PEFGCreator.
	 * @param efg
	 * @param constraints
	 */
	public PEFGCreator(EFG efg, TaskList constraints, SearchPack sp, GUIStructureWrapper gsw)
	{
		this.sp = sp;
		this.gsw = gsw;
		this.gsw.parseData();
		getTooltips = true;
		baseGraph = efg;
		workingEFG = EFG.copyOf(efg);

		allEvents = workingEFG.getEvents().getEvent();
		graphRows = workingEFG.getEventGraph().getRow();

		workingConstraints = constraints;
		if(constraints.getWidget() != null)
			allWidgets = constraints.getWidget();
		else
			allWidgets = new ArrayList<Widget>();
	}

	public PEFGCreator(EFG efg, TaskList constraints)
	{
		getTooltips = false;
		baseGraph = efg;
		workingEFG = EFG.copyOf(efg);

		allEvents = workingEFG.getEvents().getEvent();
		graphRows = workingEFG.getEventGraph().getRow();

		workingConstraints = constraints;
		if(constraints.getWidget() != null)
			allWidgets = constraints.getWidget();
		else
			allWidgets = new ArrayList<Widget>();
	}



	/**
	 * Preconditions: all events in allEvents have different eventID's!
	 * Postconditions: events with eventIDs are parameterized with their
	 * respective parameters from the tasklist, if there exists two
	 * or more widgets with similar event ID's. If there is only
	 * one, the event is not parameterized.
	 */
	public EFG augmentEvents()
	{
		ArrayList<int[]> newMut = new ArrayList<int[]>();
		// holds the number of variables we added to the allEvents list
		int lastOriginal = allEvents.size()-1;
		for(int i = 0; i <= lastOriginal; i++) {
			int simE = 1;
			if(hasParameter(i)) {
				// find the corresponding widget
				int idx = findWidgetAfter(i, -1);
				EventType primary = paramEventTypeFromWidget(allEvents.get(i), idx);
				allEvents.set(i, primary);

				// find the second widget if it exists.
				int idx2 = findWidgetAfter(i, idx);
				if(idx2 != -1) {
					ArrayList<Integer> newI = new ArrayList<Integer>();
					newI.add(i);
					newI.add(allEvents.size());
					EventType secondary = paramEventTypeFromWidget(allEvents.get(i), idx2);
					allEvents.add(secondary);
					extendRows(i);
					simE++;
					idx2 = findWidgetAfter(i, idx2);
					while(idx2 != -1) {
						newI.add(allEvents.size());
						EventType tertiary = paramEventTypeFromWidget(allEvents.get(i), idx2);
						allEvents.add(tertiary);
						extendRows(i);
						simE++;
						idx2 = findWidgetAfter(i, idx2);
					}
					int[] found = new int[simE];
					for(int m = 0; m < simE; m++)
						found[m] = newI.get(m);

					newMut.add(found);
				}
			}
		}
		int add = 0;
		similar = new int[newMut.size()][];
		for(int[] mut : newMut) {
			similar[add] = mut;
			formAddlEdges(add);
			add++;
		}
		return workingEFG;
	}
	public void extendRows(int eIdx)
	{
		//
		RowType rt = fact.createRowType();
		RowType rte = graphRows.get(eIdx);

		// new row
		rt.getE().addAll(rte.getE());
		// new column
		for(int i = 0; i < graphRows.size(); i++) {
			List<Integer> newE = graphRows.get(i).getE();
			newE.add(graphRows.get(i).getE().get(eIdx));
		}

		// add self edge
		rt.getE().add(rte.getE().get(eIdx));
		// add self row
		graphRows.add(rt);
	}

	public static EFG copyOf(EFG theGraph)
	{
		ObjectFactory fact = new ObjectFactory();
		EFG toReturn = fact.createEFG();
		EventGraphType egt = fact.createEventGraphType();
		EventsType ev = fact.createEventsType();
		toReturn.setEvents(ev);
		toReturn.setEventGraph(egt);
		for(int i = 0; i < theGraph.getEventGraph().getRow().size(); i++) {
			RowType rt = fact.createRowType();
			rt.getE().addAll(theGraph.getEventGraph().getRow().get(i).getE());
			toReturn.getEventGraph().getRow().add(rt);
		}
		toReturn.getEvents().getEvent().addAll(theGraph.getEvents().getEvent());
		return toReturn;
	}

	public String toString()
	{
		return workingEFG == null ? "" : workingEFG.toString();
	}

	/**
	 * Given the event index specified, find a widget after the widget index specified that
	 * matches that event's id.
	 * @param ei
	 * @param after
	 * @return
	 */
	private int findWidgetAfter(int ei, int afterWI)
	{
		String eId = allEvents.get(ei).getEventId();
		for(int j = afterWI+1; j < allWidgets.size(); j++) {
			String wId = allWidgets.get(j).getEventID();
			if(eId.equals(wId))
				return j;
		}
		return -1;
	}
//	private int findEventByID(String id)
//	{
//		for(int i = 0; i < allEvents.size(); i++) {
//
//		}
//	}

	/**
	 * Create a parameterized event type from the widget in allWidgets
	 * specified by i.
	 */
	public EventType paramEventTypeFromWidget(EventType event, int wi)
	{
		Widget w = allWidgets.get(wi);
		EventType et = fact.createEventType();
		et.setEventId(event.getEventId());
		et.setWidgetId(event.getWidgetId());
		et.setType(event.getType());
		et.setInitial(event.isInitial());
		et.setAction(event.getAction());
		et.setOptional(event.getOptional());
		ParameterListType plt1 = fact.createParameterListType();

//		if(getTooltips) {
//			ComponentTypeWrapper ctw = TaskListConformance.getComponentByEvent(event, gsw);
//			String ttText = ctw.getFirstValueByName(GUITARConstants.TOOLTIPTEXT_TAG_NAME);
//			sp.indexOfPerformer(w);
//			sp.searchTerms.add(ttText);
//			plt1.getParameter().add(ttText);
//		}
//		else {
		plt1.getParameter().add(w.getParameter());
//		}
		et.getParameterList().add(plt1);
		return et;
	}

	public static String filterTooltipText(String input)
	{
		String toReturn = "";
		StringTokenizer st = new StringTokenizer(input);
		if(st.hasMoreTokens())
			toReturn += st.nextToken();
		if(st.hasMoreTokens())
			toReturn += st.nextToken();

		return toReturn;
	}

	/**
	 * Return if this event has a widget in the tasklist
	 * with a parameter that is non-null and nonempty.
	 * @param ei
	 * @return
	 */
	private boolean hasParameter(int ei)
	{
		int idx = findWidgetAfter(ei, -1);
		if(idx != -1) {
			String param = allWidgets.get(idx).getParameter();
			return param != null && !param.isEmpty();
		}
		return false;
	}

	/**
	 * Form additional edges between the elements of
	 * the set of similar elements specified by simSet.
	 */
	public void formAddlEdges(int simSet)
	{
		int[] group = similar[simSet];
		EventType mainEvent = allEvents.get(group[0]);
		boolean mutual = mainEvent.getAction().equalsIgnoreCase(ActionClass.TEXT.actionName);
		if(mutual) {
			for(int i = 0; i < group.length; i++) {
				for(int j = 0; j < group.length; j++) {
					if(i != j) {
						RowType rti = graphRows.get(group[i]);
						rti.getE().set(group[j], GUITARConstants.FOLLOW_EDGE);
					}
				}
			}
		}
	}
}
