package edu.unl.cse.efs.tools;

import java.util.LinkedList;

import edu.umd.cs.guitar.model.data.Repeat;
import edu.umd.cs.guitar.model.data.Widget;

public class RepeatList extends HyperList<Widget>{
	LinkedList<String> minSettings;
	LinkedList<String> maxSettings;
	public RepeatList()
	{
		minSettings = new LinkedList<>();
		maxSettings = new LinkedList<>();
	}
	
	public boolean add(Widget element)
	{
		minSettings.add(Repeat.UNBOUNDED_SETTING);
		maxSettings.add(Repeat.UNBOUNDED_SETTING);
		return super.add(element);
	}
}
