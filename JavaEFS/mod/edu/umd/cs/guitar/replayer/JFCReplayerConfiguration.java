/*	
 *  Copyright (c) 2009-@year@. The GUITAR group at the University of Maryland. Names of owners of this group may
 *  be obtained by sending an e-mail to atif@cs.umd.edu
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *  documentation files (the "Software"), to deal in the Software without restriction, including without 
 *  limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *	the Software, and to permit persons to whom the Software is furnished to do so, subject to the following 
 *	conditions:
 * 
 *	The above copyright notice and this permission notice shall be included in all copies or substantial 
 *	portions of the Software.
 *
 *	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 *	LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO 
 *	EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *	IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *	THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 */
package edu.umd.cs.guitar.replayer;

import org.kohsuke.args4j.Option;

import edu.umd.cs.guitar.model.GUITARConstants;

/**
 * Class contains the runtime configurations of JFC GUITAR Replayer
 * 
 * <p>
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 */
public class JFCReplayerConfiguration extends GReplayerConfiguration {

	public static int STEP_DELAY = 3000;
	
	@Option(name = "-cf", usage = "configure file for the gui recorder to recognize the terminal widgets", aliases = "--configure-file")
	public static String CONFIG_FILE = "";

	@Option(name = "-g", usage = "<REQUIRED> GUI file path", aliases = "--gui-file")
	public static String GUI_FILE = null;

	@Option(name = "-e", usage = "<REQUIRED> EFG file path", aliases = "--efg-file")
	public static String EFG_FILE = null;

	public static String TESTCASE = null;

	// jsaddler: GUI Test control parameters. 
	@Option(name = "-gs", usage = "gui state file path", aliases = "--gui-state")
	public static String GUI_STATE_FILE = "GUITAR-Default.STA";

	@Option(name = "-l", usage = "log file name ", aliases = "--log-file")
	public static String LOG_FILE = "";

	@Option(name = "-i", usage = "initial waiting time for the application to get stablized before being ripped", aliases = "--wait-time")
	public static int INITIAL_WAITING_TIME = 500;

	@Option(name = "-to", usage = "test case timeout", aliases = "--testcase-timeout")
	public static int TESTCASE_TIMEOUT = 30000;

	@Option(name = "-so", usage = "test step timeout", aliases = "--teststep-timeout")
	public static int TESTSTEP_TIMEOUT = 4000;

	// jsaddler: class specific parameters: MUST BE EXPLICITLY SET
	// jsaddler: java class used to access Application Under Test 
	@Option(name = "-c", usage = "<REQUIRED> main class name for the Application Under Test ", aliases = "--main-class")
	public static String MAIN_CLASS = "";
	
	static public String LONG_PATH_TO_APP = "";
	
	@Option(name = "-a", usage = "arguments for the Application Under Test, separated by ':' ", aliases = "--arguments")
	public static String ARGUMENT_LIST;

	@Option(name = "-u", usage = "URLs for the Application Under Test, separated by ':' ", aliases = "--urls")
	public static String URL_LIST = "";

	// jsaddler: monitor options: may be explicitly set. 
	@Option(name = "-p", usage = "pause after each step", aliases = "--pause")
	public static boolean PAUSE = false;

	@Option(name = "-r", usage = "compare string using regular expression", aliases = "--regular-expression")
	public static boolean USE_REG = false;

	@Option(name = "-m", usage = "use image based identification for GUI components", aliases = "--image")
	public static boolean USE_IMAGE = false;

	// Cobertura Coverage collection
	@Option(name = "-cd", usage = "cobertura coverage output dir", aliases = "--coverage-dir")
	public static String COVERAGE_DIR = null;

	@Option(name = "-cc", usage = "cobertura coverage clean file ", aliases = "--coverage-clean")
	public static String COVERAGE_CLEAN_FILE = null;

	@Option(name = "-jar", usage = "automatically looking for the main class name in jar file specified by -c")
	public static boolean USE_JAR = false;

	@Option(name = "-ts", usage = "automatically searching and perform terminal button to fully terminate the test case", aliases = "--terminal-search")
	public static boolean TERMINAL_SEARCH = false;
	
	@Option(name = "-wg", usage = "Run replayer without relying on GUI/EFG", aliases = "--run-without-gui") // jsaddler: flag to run the replayer without relying on GUI file. 
	public static boolean RUN_WITHOUT_GUI = false;
	
	
	
	public static void setBasicAttributes(String mainClass, String guiFile, String efgFile, String testCase)
	{
		MAIN_CLASS = mainClass;
		GUI_FILE = guiFile;
		EFG_FILE = efgFile;
		TESTCASE = testCase;
	}
	
	public static void setAppDefaults(String[] urls, boolean useJar, boolean pauseMonitorOn, boolean runWithoutGUI)
	{
		PAUSE = pauseMonitorOn;
		USE_JAR = useJar;
		String toSet = urls[0];
		for(int i = 1; i<urls.length; i++) 
			toSet += GUITARConstants.NEW_CMD_ARGUMENT_SEPARATOR + urls[i];
		URL_LIST = toSet;
		RUN_WITHOUT_GUI = runWithoutGUI;
		
	}
	
	public static void setWaitTimes(int initialWaitingTime, int stepDelay)
	{
		INITIAL_WAITING_TIME = initialWaitingTime;
		STEP_DELAY = stepDelay;
	}

}
