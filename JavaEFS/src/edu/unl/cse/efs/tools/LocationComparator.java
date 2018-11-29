package edu.unl.cse.efs.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.Slice;
import edu.umd.cs.guitar.model.data.Widget;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.GUIStructureWrapper;
import edu.unl.cse.efs.generate.TestCaseGenerate;

public class LocationComparator implements Comparator<Widget>
{
	protected GUIStructureWrapper xml;
	protected Widget[] x;
	protected ArrayList<EventType> e;
	protected TreeMap<Integer, LinkedList<Widget>> map;
	public HashMap<Widget, Integer> backMap;
	public LocationComparator(GUIStructureWrapper gsw, Collection<Widget> toPosition, Collection<EventType> events)
	{
		xml = gsw;
		this.e = new ArrayList<EventType>(events);
		LinkedList<Widget> w = new LinkedList<Widget>(toPosition);
		x = new Widget[w.size()];
		Collections.sort(w, this);
		for(int i = 0; i < w.size(); i++)
			x[i] = w.get(i);
	}
	/**
	 * create an integer-based ordered mapping of the classes of order which widgets provided in interest
	 * belong to, according to the comparisons previously
	 * done on these widget's positions by this object's constructor. If a widget in interest
	 * was not pass to the call to this object's constructor, it is left out of the mapping.
	 * @return
	 */
	public TreeMap<Integer, LinkedList<Widget>> getMappedWidgets(List<Widget> interest, boolean fromRight)
	{
		map = new TreeMap<Integer, LinkedList<Widget>>();
		backMap = new HashMap<>();
		int lastI = -1;
		if(!fromRight) {
			// put widgets furthest to the left in first.
			for(int i = 0; i < x.length; i++)
				if(interest.contains(x[i])) {
					lastI = addToMap(i, x[i], lastI);
					backMap.put(x[i], i);
				}
		}
		else {
			// counter puts widgets farthest to the right in first.
			for(int i = x.length-1, counter = 0; i > -1; i--, counter++)
				if(interest.contains(x[i])) {
					lastI = addToMap(counter, x[i], lastI);
					backMap.put(x[i], counter);
				}
		}

		return map;
	}

	/**
	 * Add a single widget to the map to be returned by getMappedWidgets.
	 */
	private int addToMap(int index, Widget toAdd, int lastI)
	{
		boolean found = false;
		if(lastI != -1) {
			// if toAdd is the same as the last widget added, then add both to the same list.
			found = compare(toAdd, map.get(lastI).getLast()) == 0;
			if(found) {
				LinkedList<Widget> current = map.get(lastI);
				current.add(toAdd);
				map.put(lastI, current);
				return lastI;
			}
		}
		// if it wasn't the same as the last one, then it's a new one.
		LinkedList<Widget> newList = new LinkedList<>();
		newList.add(toAdd);
		map.put(index, newList);
		return index;
	}

	public int compare(Widget one, Widget two)
	{
		if(one == null)
			return 1;
		if(two == null)
			return -1;
		int eidI1 = TaskListConformance.findEvent(one, e);
		int eidI2 = TaskListConformance.findEvent(two, e);
		if(eidI1 == -1)
			return 1;
		if(eidI2 == -1)
			return -1;
		String id1 = e.get(eidI1).getWidgetId();
		String id2 = e.get(eidI2).getWidgetId();
		ComponentTypeWrapper wxml1 = xml.getComponentFromID(id1);
		ComponentTypeWrapper wxml2 = xml.getComponentFromID(id2);

		String xStr1 = wxml1.getFirstValueByName(GUITARConstants.X_TAG_NAME);
		String xStr2 = wxml2.getFirstValueByName(GUITARConstants.X_TAG_NAME);
		if(xStr1 == null || xStr1.isEmpty())
			return -1;
		else if(xStr1 == null || xStr2.isEmpty())
			return 1;
		int xVal1 = Integer.parseInt(xStr1);
		int xVal2 = Integer.parseInt(xStr2);
		return Integer.compare(xVal1, xVal2);
	}
}