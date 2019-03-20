package edu.unl.cse.efs.generate.test;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.Required;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.model.data.Widget;
import edu.unl.cse.efs.tools.TaskListConformance;
import edu.unl.cse.efs.generate.EFGPacifier;

public class TestCaseGenerate_Test {
	
	private static ObjectFactory fact = new ObjectFactory();
	static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	static ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	static PrintStream oldStd, oldErr;

	public static void setUpBeforeClass() throws Exception
	{
	}


	public static void setUpStreams()
	{
		oldStd = System.out;
		oldErr = System.err;
	    System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}


	public static void cleanUpStreams() {
	    System.setOut(oldStd);
	    System.setErr(oldErr);
	}

	public EFG pacify(GUIStructure ripperArtifact, EFG workingEventFlow, TaskList tl, File genBase)
	{
		TaskList workingTaskList = testTasklistJEditLS();
		workingTaskList = TaskListConformance.checkAndSetConstraintsWidgets(workingTaskList, ripperArtifact, workingEventFlow);

		EFGPacifier pacifier = new EFGPacifier(workingEventFlow, workingTaskList, genBase.getAbsolutePath());
		return pacifier.pacifyInputGraphAndWriteResultsRev3();
	}

	public static Widget testWidgetComment()
	{
		Widget widget = fact.createWidget();
		widget.setEventID("push button_img comment edit");
		widget.setName("comment_edit");
		widget.setType("push button");
		widget.setWindow("CommentedText.java");
		widget.setAction("Click");
		return widget;
	}
	public static Widget testWidgetCommentHover()
	{
		Widget widget = fact.createWidget();
		widget.setEventID("push button hover_img comment edit");
		widget.setName("comment_edit");
		widget.setType("push button");
		widget.setWindow("CommentedText.java");
		widget.setAction("Hover");
		return widget;
	}

	public static Widget testWidgetShift()
	{
		Widget widget = fact.createWidget();
		widget.setEventID("push button_img shift r edit");
		widget.setName("shift_r_edit");
		widget.setType("push button");
		widget.setWindow("CommentedText.java");
		widget.setAction("Click");
		return widget;
	}

	public static Widget testWidgetShiftHover()
	{
		Widget widget = fact.createWidget();
		widget.setEventID("push button hover_img shift r edit");
		widget.setName("shift_r_edit");
		widget.setType("push button");
		widget.setWindow("CommentedText.java");
		widget.setAction("Hover");
		return widget;
	}

	public static Widget testWidgetSelectLine()
	{
		Widget widget = fact.createWidget();
		widget.setEventID("push button_img go-last");
		widget.setName("go-last");
		widget.setType("push button");
		widget.setWindow("CommentedText.java");
		widget.setAction("Click");
		return widget;
	}
	public static Widget testWidgetSelectLineHover()
	{
		Widget widget = fact.createWidget();
		widget.setEventID("push button hover_img go-last");
		widget.setName("go-last");
		widget.setType("push button");
		widget.setWindow("CommentedText.java");
		widget.setAction("Hover");
		return widget;
	}

	public static TaskList testTasklistJEditLS()
	{
		TaskList tl = fact.createTaskList();
		tl.getWidget().addAll(Arrays.asList(new Widget[]{
				testWidgetComment(),
				testWidgetCommentHover(),
				testWidgetShift(),
				testWidgetShiftHover(),
				testWidgetSelectLine(),
				testWidgetSelectLineHover()
		}));
		Required r1 = fact.createRequired();
		r1.getWidget().add(testWidgetComment());
		Required r2 = fact.createRequired();
		r2.getWidget().add(testWidgetShift());
		tl.getRequired().add(r1);
		tl.getRequired().add(r2);
		return tl;
	}
}
