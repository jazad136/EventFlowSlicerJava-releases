package edu.unl.cse.efs.tools;

import java.util.List;

import edu.umd.cs.guitar.model.data.*;

public class ReportTranslation {


	/**
	 * Prints out all the elements of the Repeat rules in reportList
	 * with the right connector and the right number of the rule. The default connector between widgets is comma (,)
	 * between rules is newline.
	 *
	 * Preconditions: none.
	 * Postconditions: A String containing all the Repeat rules in the TaskList reportList
	 * is returned from the function.
	 */
	public static String repeatReport(TaskList reportList)
	{
		String toReturn = "";
		if(reportList == null || reportList.getRepeat().isEmpty())
			return toReturn;
		List<Repeat> ruleList = reportList.getRepeat();
		for(int i = 0; i < ruleList.size(); i++)
			toReturn += combine(toReturn, "Rule " + widgetReport(ruleList.get(i).getWidget(), i+1, ","));
		return toReturn;
	}

	/**
	 * Prints out all the elements of the Required rules in reportList
	 * with the right connector and the right number of the rule. The default connector between widgets is comma (,)
	 * between rules is newline.
	 *
	 * Preconditions: none.
	 * Postconditions: A String containing all the Required rules in the TaskList reportList
	 * is returned from the function.
	 */
	public static String requiresReport(TaskList reportList)
	{
		String toReturn = "";
		if(reportList == null || reportList.getRequired().isEmpty())
			return toReturn;
		List<Required> ruleList = reportList.getRequired();
		for(int i = 0; i < ruleList.size(); i++)
			toReturn += combine(toReturn, "Rule " + widgetReport(ruleList.get(i).getWidget(), i+1, ","));
		return toReturn;
	}

	/**
	 * Prints out all the elements of the Exclusion rules in reportList
	 * with the right connector and the right number of the rule. The default connector between widgets is comma (,)
	 * between rules is newline.
	 *
	 * Preconditions: none.
	 * Postconditions: A String containing all the Exclusion rules in the TaskList reportList
	 * is returned from the function.
	 */
	public static String exclusionReport(TaskList reportList)
	{
		String toReturn = "";
		if(reportList == null || reportList.getExclusion().isEmpty())
			return toReturn;
		List<Exclusion> ruleList = reportList.getExclusion();
		for(int i = 0; i < ruleList.size(); i++)
			toReturn += combine(toReturn, "Rule " + widgetReport(ruleList.get(i).getWidget(), i+1, ","));
		return toReturn;
	}

	/**
	 * Prints out all the elements of the Atomic rules in reportList
	 * with the right connector and the right number of the rule.
	 * The default connector between widgets is comma (,), between atomic groups is newline,
	 * between rules is newline//newline.
	 *
	 * Preconditions: none.
	 * Postconditions: A String containing all the atomic rules in the TaskList reportList
	 * is returned from the function.
	 */
	public static String atomicReport(TaskList reportList)
	{
		String toReturn = "";
		if(reportList == null || reportList.getAtomic().isEmpty())
			return toReturn;
		List<Order> ruleList = reportList.getOrder();
		OrderGroup next;
		for(int i = 0; i < ruleList.size(); i++) {
			toReturn += "Rule " + (i+1);
			for(int j = 0; j < ruleList.get(i).getOrderGroup().size(); j++) {
				next = ruleList.get(i).getOrderGroup().get(j);
				toReturn = combine(toReturn, "Group " + widgetReport(next.getWidget(), i+1, ","));
			}
			toReturn = addBigBreak(toReturn);
		}
		return toReturn;
	}
	/**
	 * Prints out all the elements of the Order rules in reportList
	 * with the right connector and the right number of the rule.
	 * The default connector between widgets is comma (,), between order groups is newline,
	 * between rules is newline//newline.
	 *
	 * Preconditions: none.
	 * Postconditions: A String containing all the order rules in the TaskList reportList
	 * is returned from the function.
	 */
	public static String orderReport(TaskList reportList)
	{
		String toReturn = "";
		if(reportList == null || reportList.getOrder().isEmpty())
			return toReturn;
		List<Atomic> ruleList = reportList.getAtomic();
		AtomicGroup next;
		for(int i = 0; i < ruleList.size(); i++) {
			toReturn += "Rule " + (i+1);

			for(int j = 0; j < ruleList.get(i).getAtomicGroup().size(); j++) {
				next = ruleList.get(i).getAtomicGroup().get(j);
				toReturn = combine(toReturn, "Group " + widgetReport(next.getWidget(), i+1, ","));
			}
			toReturn = addBigBreak(toReturn);
		}
		return toReturn;
	}

	/**
	 * Combine two parts of a report with a special delimiter
	 * @param prefix
	 * @param newFix
	 * @return
	 */
	public static String combine(String prefix, String newFix)
	{
		return prefix + "\n" + newFix;
	}

	/**
	 * Append a special large delimiter to the end of a smaller report.
	 * @param infix
	 * @return
	 */
	public static String addBigBreak(String infix)
	{
		return infix + "\n//\n";
	}

	public static String widgetReport(List<Widget> widgets, int number, String widgetDelimiter)
	{
		String toReturn = "" + number + ": ";
		for(int i = 0; i < widgets.size(); i++)
			toReturn += widgets.get(i) + widgetDelimiter;

		return toReturn;
	}
}
