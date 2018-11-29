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
package edu.unl.cse.efs.ripper;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import edu.umd.cs.guitar.ripper.JFCRipperConfiguration;

/**
 * Class contains the runtime configurations of EventFlowSlicer's JFC Ripper
 * implementation.
 *
 * <p>
 *
 * @author Jonathan Saddler
 */
public class JFCRipperConfigurationEFS extends JFCRipperConfiguration
{

	@Option(name = "-delay", usage = "EFS initial delay before opening application.")
	public static int APPLICATION_OPEN_DELAY = -1;

	@Option(name = "-constfile", usage = "file containing ripping rules", aliases = "--rules-list", required = true)
	public static String RULES_FILE = "";


	@Option(name = "-resdir", usage = "Output Directory", required = true)
	public static String RESULTS_DIRECTORY = "";

	@Option(name = "-nf", usage = "output destination for names discovered during rip", aliases = "--names-list", required = false)
	public static String NAMES_FILE = "";

	@Option(name = "-inf", usage = "infer actionable widgets discovered during rip and modify rules file following rip.", aliases = "--infer-widgets", required = false)
	public static boolean INFER_WIDGETS = false;

	@Option(name = "-e", usage = "destination EFG file path", aliases = "--efg-file", required = false)
	public static String EFG_FILE = "";

	@Option(name = "-args", usage = "application arguments file", required = false)
	public static String APP_ARGS_FILE = "";

	@Option(name = "-vm", usage = "VM arguments file", required = false)
	public static String VM_ARGS_FILE = "";

	@Option(name = "-cmc", usage = "Custom Main Class name", required = false)
	public static String CUSTOM_MAIN_CLASS = "";

	@Option(name = "-noressubdir", usage = "turn off auto-create subdirectory", required = false)
	public static boolean NO_RES_SUBDIR = false;

	@Argument
	public static List<String> CMD_LINE_ARGS = new ArrayList<String>();

}
