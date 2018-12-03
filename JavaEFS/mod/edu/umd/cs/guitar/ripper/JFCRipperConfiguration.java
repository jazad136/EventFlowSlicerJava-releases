/*
 *  Copyright (c) 2009-@year@. The  GUITAR group  at the University of
 *  Maryland. Names of owners of this group may be obtained by sending
 *  an e-mail to atif@cs.umd.edu
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files
 *  (the "Software"), to deal in the Software without restriction,
 *  including without limitation  the rights to use, copy, modify, merge,
 *  publish,  distribute, sublicense, and/or sell copies of the Software,
 *  and to  permit persons  to whom  the Software  is furnished to do so,
 *  subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO  EVENT SHALL THE  AUTHORS OR COPYRIGHT  HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR  OTHER LIABILITY,  WHETHER IN AN  ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.umd.cs.guitar.ripper;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;

import edu.umd.cs.guitar.util.Util;

/**
 * Class contains the runtime configurations of GUITAR's JFC Ripper
 * implementation.
 * 
 * <p>
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 */
public class JFCRipperConfiguration extends GRipperConfiguration {
	// GUITAR runtime parameters
//	@Option(name = "-g", usage = "destination GUI file path", aliases = "--gui-file")
	static public String GUI_FILE = "GUITAR-Default.GUI";

//	@Option(name = "-l", usage = "log file name ", aliases = "--log-file")
	static public String LOG_FILE = Util.getTimeStamp() + ".log";

	static public Integer INITIAL_WAITING_TIME = 500;

	// Application Under Test
	//@Option(name = "-c", usage = "main class name for the Application Under Test ", aliases = "--main-class", required = false)
	static public String MAIN_CLASS = "";

	static public String LONG_PATH_TO_APP = "";
	
	@Option(name = "-a", usage = "arguments for the Application Under Test, separated by a colon (:) ", aliases = "--arguments")
	static public String ARGUMENT_LIST;

	@Option(name = "-r", usage = "use regex for matching GUI windows ", aliases = "--regular-expression")
	static public boolean USE_REG = false;

//	@Option(name = "-rw", usage = "use random-walk strategy to explore the GUI", aliases = "--random-walk")
//	static public boolean RANDOM_WALK = false;
	
	@Option(name = "-rw", usage = "use random-walk strategy to explore the GUI, requires random-walk steps", aliases = "--random-walk-steps")
	static public Integer RANDOM_WALK_STEPS = null;
	
	@Option(name = "-m", usage = "save image for GUI components ", aliases = "--image")
	static public boolean USE_IMAGE = false;

	@Option(name = "-u", usage = "URLs for the Application Under Test, separated by a colon (:) ", aliases = "--urls")
	static public String URL_LIST = "";

	@Option(name = "-j", usage = "Java Virtual Machine options for the Application Under Test", aliases = "--jvm-options")
	static public String JVM_OPTIONS;

	@Option(name = "-ripcon", usage = "configure file for the ripper defining terminal, ignored components and ignored windows", aliases = "-ripcon, --configure-file")
	public static String CONFIG_FILE = "";
	
	@Option(name = "-ce", usage = "customized event list (usually aut-specific events)", aliases = "--event-list")
	public static String CUSTOMIZED_EVENT_LIST = null;

	@Option(name = "-jar", usage = "Automatically looking for the main class name in jar file specified b-c")
	public static boolean USE_JAR = false;

	@Option(name = "-p", usage = "ripper plugin", aliases = "--plugin", required = false)
	
	//public static List<String> PLUGIN_LIST = new ArrayList<String>();
	public static List<String> PLUGIN_LIST = new ArrayList<String>();
}
