

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;


import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.Required;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.model.data.Widget;
import edu.unl.cse.efs.generate.FocusOnPack;
import edu.unl.cse.efs.tools.TaskListConformance;
import edu.unl.cse.efs.generate.DirectionalPack;
import edu.unl.cse.efs.generate.EFGPacifier;
import edu.unl.cse.efs.generate.TestCaseGenerate;

public class TestCaseGenerate_Test {


	private static ObjectFactory fact = new ObjectFactory();
//	private static DirectionalPack testP;
	static ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	static ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	static PrintStream oldStd, oldErr;

	public static void setUpBeforeClass() throws Exception
	{
//		testP = testDirPackJEditLS(true);

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


	public GUIStructure test1_setupGUIFile()
	{
		Object myFileObject = null;
		File f;
		Unmarshaller um;
		JAXBContext context;

		try {
			context = JAXBContext.newInstance(GUIStructure.class);
			um = context.createUnmarshaller();
			f = new File("/Users/jsaddle/Desktop/ResearchResults/EventFlowSlicer/Testing/GUIs/JEditLS/JEditLS.GUI");
			myFileObject = JAXBIntrospector.getValue(um.unmarshal(f));
			if(!(myFileObject instanceof GUIStructure))
				throw new RuntimeException("GUI File could not be read.");
		} catch(JAXBException e) {
			throw new RuntimeException("GUI File Not Found.");
		}
		return (GUIStructure)myFileObject;
	}
	public EFG test1_setupEFGFile()
	{
		Object myFileObject = null;
		File f;
		Unmarshaller um;
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(EFG.class);
			um = context.createUnmarshaller();
			f = new File("/Users/jsaddle/Desktop/ResearchResults/EventFlowSlicer/Testing/EFGs/JEditLS/JEditLS_BKMK.EFG");
			myFileObject = JAXBIntrospector.getValue(um.unmarshal(f));
			if(!(myFileObject instanceof EFG))
				throw new RuntimeException("EFG File could not be read.");
			} catch(JAXBException e) {
				throw new RuntimeException("EFG File Not Found.");
		}
		return (EFG)myFileObject;
	}

	public EFG pacify(GUIStructure ripperArtifact, EFG workingEventFlow, TaskList tl, File genBase)
	{
		TaskList workingTaskList = testTasklistJEditLS();
		workingTaskList = TaskListConformance.checkAndSetConstraintsWidgets(workingTaskList, ripperArtifact, workingEventFlow);

		EFGPacifier pacifier = new EFGPacifier(workingEventFlow, workingTaskList, genBase.getAbsolutePath());
		return pacifier.pacifyInputGraphAndWriteResultsRev3();
	}

	public void test6_rightstopsindirect()
	{
		EFG workingEventFlow = test1_setupEFGFile();
		GUIStructure ripperArtifact = test1_setupGUIFile();

		// setup directory structure.
		File outputDirectory = new File("/Users/jsaddle/Desktop/ResearchResults/EventFlowSlicer/Testing/GenOutput/try6_rli");
		File extrasDirectory = new File(outputDirectory, "Extra_Output_Test");
		File genBase = new File(extrasDirectory, "gen_out");
		genBase.mkdirs();

		// pacify
		TaskList workingTaskList = testTasklistJEditLS();
		workingTaskList = TaskListConformance.checkAndSetConstraintsWidgets(workingTaskList, ripperArtifact, workingEventFlow);

		EFGPacifier pacifier = new EFGPacifier(workingEventFlow, workingTaskList, genBase.getAbsolutePath());
		workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev3();

		// setup selector packs.
		// dPack
		DirectionalPack testP = testDirPackJEditLS(true, true);
		List<Widget> checkedWidgets = TaskListConformance.checkAndSetWidgets(testP.list, ripperArtifact, workingEventFlow);
		testP.resetList(checkedWidgets);
		// fPack
		FocusOnPack testP2 = testStopPackJEditLS();
		testP2.resetList(TaskListConformance.checkAndSetWidgets(testP2.list, ripperArtifact, workingEventFlow));
		// generate tests.
		TestCaseGenerate tcg = new TestCaseGenerate(
				workingEventFlow, ripperArtifact, outputDirectory.getAbsolutePath());

		tcg.setupIndexSets(workingTaskList, testP, testP2);
		tcg.resetTimes();
		int testCases = 0;
		try {
			testCases = tcg.runAlgorithmAndWriteResultsToOutputDirectory(false);
		} catch(IOException e) {
			throw new RuntimeException("Generator could not write files.");
		}
		System.out.println(testCases);
	}
	public void test5_leftstopsindirect()
	{
		EFG workingEventFlow = test1_setupEFGFile();
		GUIStructure ripperArtifact = test1_setupGUIFile();

		// setup directory structure.
		File outputDirectory = new File("/Users/jsaddle/Desktop/ResearchResults/EventFlowSlicer/Testing/GenOutput/try5_lri");
		File extrasDirectory = new File(outputDirectory, "Extra_Output_Test");
		File genBase = new File(extrasDirectory, "gen_out");
		genBase.mkdirs();

		// pacify
		TaskList workingTaskList = testTasklistJEditLS();
		workingTaskList = TaskListConformance.checkAndSetConstraintsWidgets(workingTaskList, ripperArtifact, workingEventFlow);

		EFGPacifier pacifier = new EFGPacifier(workingEventFlow, workingTaskList, genBase.getAbsolutePath());
		workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev3();

		// setup selector packs.
		// dPack
		DirectionalPack testP = testDirPackJEditLS(false, true);
		List<Widget> checkedWidgets = TaskListConformance.checkAndSetWidgets(testP.list, ripperArtifact, workingEventFlow);
		testP.resetList(checkedWidgets);
		// fPack
		FocusOnPack testP2 = testStopPackJEditLS();
		testP2.resetList(TaskListConformance.checkAndSetWidgets(testP2.list, ripperArtifact, workingEventFlow));
		// generate tests.
		TestCaseGenerate tcg = new TestCaseGenerate(
				workingEventFlow, ripperArtifact, outputDirectory.getAbsolutePath());

		tcg.setupIndexSets(workingTaskList, testP, testP2);
		tcg.resetTimes();
		int testCases = 0;
		try {
			testCases = tcg.runAlgorithmAndWriteResultsToOutputDirectory(false);
		} catch(IOException e) {
			throw new RuntimeException("Generator could not write files.");
		}
		System.out.println(testCases);
	}

	public void test4_rightstops()
	{
		EFG workingEventFlow = test1_setupEFGFile();
		GUIStructure ripperArtifact = test1_setupGUIFile();

		// setup directory structure.
		File outputDirectory = new File("/Users/jsaddle/Desktop/ResearchResults/EventFlowSlicer/Testing/GenOutput/try4_rl");
		File extrasDirectory = new File(outputDirectory, "Extra_Output_Test");
		File genBase = new File(extrasDirectory, "gen_out");
		genBase.mkdirs();

		// pacify
		TaskList workingTaskList = testTasklistJEditLS();
		workingTaskList = TaskListConformance.checkAndSetConstraintsWidgets(workingTaskList, ripperArtifact, workingEventFlow);

		EFGPacifier pacifier = new EFGPacifier(workingEventFlow, workingTaskList, genBase.getAbsolutePath());
		workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev3();

		// setup selector packs.
		// dPack
		DirectionalPack testP = testDirPackJEditLS(true, false);
		List<Widget> checkedWidgets = TaskListConformance.checkAndSetWidgets(testP.list, ripperArtifact, workingEventFlow);
		testP.resetList(checkedWidgets);
		// fPack
		FocusOnPack testP2 = testStopPackJEditLS();
		testP2.resetList(TaskListConformance.checkAndSetWidgets(testP2.list, ripperArtifact, workingEventFlow));
		// generate tests.
		TestCaseGenerate tcg = new TestCaseGenerate(
				workingEventFlow, ripperArtifact, outputDirectory.getAbsolutePath());

		tcg.setupIndexSets(workingTaskList, testP, testP2);
		tcg.resetTimes();
		int testCases = 0;
		try {
			testCases = tcg.runAlgorithmAndWriteResultsToOutputDirectory(false);
		} catch(IOException e) {
			throw new RuntimeException("Generator could not write files.");
		}
		System.out.println(testCases);
	}
	public void test3_leftstops()
	{
		EFG workingEventFlow = test1_setupEFGFile();
		GUIStructure ripperArtifact = test1_setupGUIFile();

		// setup directory structure.
		File outputDirectory = new File("/Users/jsaddle/Desktop/ResearchResults/EventFlowSlicer/Testing/GenOutput/try3_lr");
		File extrasDirectory = new File(outputDirectory, "Extra_Output_Test");
		File genBase = new File(extrasDirectory, "gen_out");
		genBase.mkdirs();

		// pacify
		TaskList workingTaskList = testTasklistJEditLS();
		workingTaskList = TaskListConformance.checkAndSetConstraintsWidgets(workingTaskList, ripperArtifact, workingEventFlow);

		EFGPacifier pacifier = new EFGPacifier(workingEventFlow, workingTaskList, genBase.getAbsolutePath());
		workingEventFlow = pacifier.pacifyInputGraphAndWriteResultsRev3();

		// setup selector packs.
		// dPack
		DirectionalPack testP = testDirPackJEditLS(false, false);
		List<Widget> checkedWidgets = TaskListConformance.checkAndSetWidgets(testP.list, ripperArtifact, workingEventFlow);
		testP.resetList(checkedWidgets);
		// fPack
		FocusOnPack testP2 = testStopPackJEditLS();
		testP2.resetList(TaskListConformance.checkAndSetWidgets(testP2.list, ripperArtifact, workingEventFlow));
		// generate tests.
		TestCaseGenerate tcg = new TestCaseGenerate(
				workingEventFlow, ripperArtifact, outputDirectory.getAbsolutePath());

		tcg.setupIndexSets(workingTaskList, testP, testP2);
		tcg.resetTimes();
		int testCases = 0;
		try {
			testCases = tcg.runAlgorithmAndWriteResultsToOutputDirectory(false);
		} catch(IOException e) {
			throw new RuntimeException("Generator could not write files.");
		}
		System.out.println(testCases);
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
//		Required r3 = fact.createRequired();
//		r3.getWidget().add(testWidgetCommentHover());
//		Required r4 = fact.createRequired();
//		r4.getWidget().add(testWidgetShiftHover());
		tl.getRequired().add(r1);
		tl.getRequired().add(r2);
//		tl.getRequired().add(r3);
//		tl.getRequired().add(r4);
		return tl;
	}
	public static DirectionalPack testDirPackJEditLS(boolean fromRight, boolean allHoversFirst)
	{
		DirectionalPack dp = new DirectionalPack(fromRight, allHoversFirst,
				testWidgetComment(),
				testWidgetCommentHover(),
				testWidgetShift(),
				testWidgetShiftHover(),
				testWidgetSelectLine(),
				testWidgetSelectLineHover());
		return dp;
	}
	public static FocusOnPack testStopPackJEditLS()
	{
		FocusOnPack fp = new FocusOnPack(Arrays.asList(
			testWidgetComment(),
			testWidgetShift())
		);
		
		return fp;
	}
}
