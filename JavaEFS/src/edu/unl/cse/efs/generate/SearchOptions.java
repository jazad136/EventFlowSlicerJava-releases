package edu.unl.cse.efs.generate;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.guitar.model.data.Widget;

public class SearchOptions
{
	final Widget helpOpen, helpClose, helpRun, helpList;
	final ArrayList<Widget> searchExamples;
	final ArrayList<Widget> searchSubjects;

	public SearchOptions(Widget helpOpen, Widget helpClose, Widget helpRun, Widget helpList, List<Widget> searchExamples, List<Widget> searchSubjects)
	{
		this.helpOpen = helpOpen;
		this.helpClose = helpClose;
		this.helpRun = helpRun;
		this.helpList = helpList;
		this.searchExamples = new ArrayList<>(searchExamples);
		this.searchSubjects = new ArrayList<>(searchSubjects);
	}
	public SearchOptions(SearchOptions other, List<Widget> newSearchExamples)
	{
		this.helpOpen = other.helpOpen;
		this.helpClose = other.helpClose;
		this.helpRun = other.helpRun;
		this.helpList = other.helpList;
		this.searchSubjects = new ArrayList<>(other.searchSubjects);

		this.searchExamples = new ArrayList<>(newSearchExamples);
	}

}