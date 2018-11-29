package edu.unl.cse.efs.view;

import java.awt.Dimension;

import javax.swing.JFrame;

import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.LauncherData;
import edu.unl.cse.efs.app.EventFlowSlicer;

public class TestCaseSelector {
	public static Dimension currentFrameDimension;
	public static JFrame dialogParentFrame;
	static ApplicationData ad;
	static LauncherData ld;
	
	public TestCaseSelector(ApplicationData someAD, LauncherData someLD)
	{
		ad = someAD;
		ld = someLD;
	}
	
	public TestCaseSelector(ApplicationData someAD)
	{
		this(someAD, new LauncherData(EventFlowSlicer.DEFAULT_JAVA_RMI_PORT));
	}
	public class TopContent
	{
		public TopContent()
		{
			
		}
	}
	public class MidContent
	{
		public MidContent()
		{
			
		}
	}
	
}
