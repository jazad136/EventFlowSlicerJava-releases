package edu.unl.cse.efs.generate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.guitar.model.data.Widget;

public class FocusOnPack extends SelectorPack
{
	public FocusOnPack(Collection<Widget> selected)
	{
		this.list = new LinkedList<Widget>(selected);
	}

	public void resetList(List<Widget> newList)
	{
		this.list = new LinkedList<>(newList);
	}

	public String toString()
	{
		String toReturn = "FOCUS PK"
				+ "\nin scope: ";
		for(Widget w : list)
			toReturn += "\n" + w.getEventID();
		return toReturn;
	}
}