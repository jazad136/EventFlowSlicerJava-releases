package edu.unl.cse.efs.generate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.guitar.model.data.Widget;

/**
 * DirectionalPacks are selector packs that specify the direction that EFS
 * should use to create constraints that deal with the stored list of widgets.
 *
 */
public class DirectionalPack extends SelectorPack
{
	public List<Widget> hovers;
	public List<Widget> other;
	boolean fromRight;
	boolean bidirectional;
	boolean allHoversFirst;

	public DirectionalPack(boolean allHoversFirst, Collection<Widget> selected)
	{
		this(false, allHoversFirst, selected);
		this.bidirectional = true;
	}
	public DirectionalPack(boolean fromRight, boolean allHoversFirst, Collection<Widget> selected)
	{
		this.fromRight = fromRight;
		this.allHoversFirst = allHoversFirst;
		this.list = new LinkedList<>(selected);
		hovers = filterHovers(selected);
		other = new LinkedList<>(selected);
		other.removeAll(hovers);
	}

	public DirectionalPack(boolean fromRight, boolean allHoversFirst, Widget... selected)
	{
		this(fromRight, allHoversFirst, Arrays.asList(selected));
	}

	public boolean isBidirectional() 	{ return bidirectional; }
	public boolean isLeftDirectional() 	{ return !fromRight; }
	public boolean isRightDirectional()	{ return fromRight; }

	public static List<DirectionalPack> makeLeftAndRightHovers(List<Widget> widgets, boolean indirect)
	{
		ArrayList<DirectionalPack> twoDirPacks = new ArrayList<DirectionalPack>();
		twoDirPacks.add(new DirectionalPack(true, indirect, widgets));
		twoDirPacks.add(new DirectionalPack(false, indirect, widgets));
		return twoDirPacks;
	}

	public static List<DirectionalPack> makeDirectAndIndirectHovers(List<Widget> widgets, boolean fromRight)
	{
		ArrayList<DirectionalPack> twoDirPacks = new ArrayList<DirectionalPack>();
		twoDirPacks.add(new DirectionalPack(fromRight, true, widgets));
		twoDirPacks.add(new DirectionalPack(fromRight, false, widgets));
		return twoDirPacks;
	}

	/**
	 * Split the list provided into two separate lists, one for
	 * the widgets that hover and the other for the widgets that don't hover.
	 */
	public void resetList(List<Widget> newList)
	{
		this.list = new LinkedList<>(newList);
		hovers = filterHovers(newList);
		other = new LinkedList<>(newList);
		other.removeAll(hovers);
	}

	public String toString()
	{
		String toReturn = "DIR PK\n" + "in scope: ";
		for(Widget w : list)
			toReturn += "\n" + w.getEventID();
		return toReturn;
	}
}