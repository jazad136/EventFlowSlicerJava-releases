package edu.unl.cse.efs.generate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.guitar.model.data.Widget;

public class SearchPack extends SelectorPack
{
	SearchOptions forSearch;
	/** Corresponds to the widget in the selected set that points to the open help menu widget */
	int helpOpen;
	/** Corresponds to the widget in the selected set that points to the open help menu widget */
	int helpClose;
	/** Corresponds to the widget in the selected set that points to the button that runs the search */
	int helpRun;
	/** Corresponds to the widget in the selected set that points to the results list in search window*/
	int helpList;
	/** Corresponds to the terms used to search in the search window. */
	ArrayList<String> searchTerms;
	ArrayList<Integer> searchSubjects;
	ArrayList<Integer> searchPerformers;

	public SearchPack(List<Widget> fullScope, SearchOptions forSearch)
	{
		this.list = new LinkedList<Widget>(fullScope);
		this.forSearch = forSearch;
		searchTerms = new ArrayList<String>();
		searchSubjects = new ArrayList<Integer>();
		searchPerformers = new ArrayList<Integer>();
		resetList(fullScope);
	}
	public Widget getSearchPerformer(int idx)
	{
		return list.get(searchPerformers.get(idx));
	}
	public void setSearchPerformer(int listIdx, Widget newWidget)
	{
		int count = 0;
		for(int i : searchPerformers) {
			if(listIdx == i) {
				ArrayList<Widget> modify = new ArrayList<Widget>(forSearch.searchExamples);
				modify.set(count, newWidget);
				forSearch = new SearchOptions(forSearch, modify);
				break;
			}
			count++;
		}
	}
	public List<Widget> getSearchPerformers()
	{
		List<Widget> toReturn = new ArrayList<Widget>();
		for(int p : searchPerformers)
			toReturn.add(list.get(p));
		return toReturn;
	}

	public int indexOfPerformer(Widget w)
	{
		int index = list.indexOf(w);
		if(index != -1) {
			int perfIndex = searchPerformers.indexOf(index);
			if(perfIndex != -1)
				return perfIndex;
		}
		return -1;
	}
	public void resetList(List<Widget> newFullScope, List<Widget> newSearchPerformers)
	{
		forSearch = new SearchOptions(forSearch, newSearchPerformers);
		resetList(newFullScope);
	}
	public void resetList(List<Widget> newList)
	{
		this.list = new LinkedList<>(newList);
		helpOpen = helpClose = helpList = helpRun = -1;
		searchSubjects.clear();
		searchPerformers.clear();
		for(int i = 0; i < list.size(); i++) {
			if(list.get(i).equals(forSearch.helpOpen) && helpOpen == -1)
				helpOpen = i;
			else if(list.get(i).equals(forSearch.helpClose) && helpClose == -1)
				helpClose = i;
			else if(list.get(i).equals(forSearch.helpList) && helpList == -1)
				helpList = i;
			else if(list.get(i).equals(forSearch.helpRun) && helpRun == -1) {
				helpRun = i;
			}
			else if(forSearch.searchSubjects.contains(list.get(i)))
				searchSubjects.add(i);
			else if(forSearch.searchExamples.contains(list.get(i)))
				searchPerformers.add(i);
		}
		searchTerms = new ArrayList<String>();
	}
	public String toString()
	{
		String toReturn = "SRCH PK\n"
				+ "open " + helpOpen + " close " + helpClose + " list " + helpList + "\n"
				+ "in scope: ";
		for(Widget w : list)
			toReturn += "\n" + w.getEventID();
		return toReturn;
	}

}