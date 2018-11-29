package edu.unl.cse.efs.generate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.guitar.awb.JavaActionTypeProvider;
import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.Widget;
import edu.unl.cse.efs.bkmktools.EFGBookmarking;
import edu.unl.cse.efs.tools.TaskListConformance;
import edu.unl.cse.guitarext.JavaTestInteractions;

/**
 * A Selector Pack is a set of related widgets that a test case selector has specified, that
 * the generator can make special arrangements to handle.
 * @author jsaddle
 *
 */
public class SelectorPack
{
	public List<Widget> list; // public for a reason.
	static ObjectFactory fact = new ObjectFactory();
	public void resetList(List<Widget> newList)
	{
		this.list = new LinkedList<>(newList);
	}

	public static List<Widget> filterHovers(Collection<Widget> input)
	{
		LinkedList<Widget> toReturn = new LinkedList<>();

		for(Widget w : input) {
			boolean isHover = w.getAction().equals(
					JavaActionTypeProvider.getTypeFromActionHandler(ActionClass.HOVER.actionName));
			boolean isSelHover = w.getAction().equals(
					JavaActionTypeProvider.getTypeFromActionHandler(ActionClass.SELECTIVE_HOVER.actionName));
			if(isHover || isSelHover)
				toReturn.add(w);
		}
		return toReturn;
	}
	public static List<Widget> filterClicksFromHovers(Collection<Widget> nonHovers, Collection<Widget> hovers)
	{
		LinkedList<Widget> targetWidgets = new LinkedList<Widget>();
		String hoverSuffix = EFGBookmarking.handlerActionTag(ActionClass.HOVER.actionName);
		String clickSuffix = EFGBookmarking.handlerActionTag(ActionClass.ACTION.actionName);
		for(Widget w : hovers) {
			for(Widget cw : nonHovers) {
				if(TaskListConformance.matchingNonNullCoreNames(w.getEventID(), cw.getEventID())) {
					if(cw.getEventID().contains(clickSuffix)) {
						targetWidgets.add(cw);
						break;
					}
				}
			}
		}
		return targetWidgets;
	}
}

