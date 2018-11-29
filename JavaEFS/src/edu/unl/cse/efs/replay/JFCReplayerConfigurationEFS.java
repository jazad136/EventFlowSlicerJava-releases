package edu.unl.cse.efs.replay;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import edu.umd.cs.guitar.replayer.JFCReplayerConfiguration;

public class JFCReplayerConfigurationEFS extends JFCReplayerConfiguration
{
	@Option(name = "-delay", usage = "EFS initial delay before opening application.")
	public static int APPLICATION_OPEN_DELAY = -1;

	@Option(name = "-constfile", usage = "file containing ripping rules", aliases = "--rules-list")
	public static String RULES_FILE = "";

	@Option(name = "-tcdir", usage = "<REQUIRED> test case directory", aliases = "--test-case-dir", required = true)
	public static String TESTCASE_DIRECTORY = "";

	@Option(name = "-resdir", usage = "<REQUIRED> Output Directory", required = true)
	public static String RESULTS_DIRECTORY = "";

	@Option(name = "-args", usage = "application arguments file", required = false)
	public static String APP_ARGS_FILE = "";

	@Option(name = "-vm", usage = "VM arguments file", required = false)
	public static String VM_ARGS_FILE = "";

	@Option(name = "-noressubdir", usage = "turn off auto-create subdirectory", required = false)
	public static boolean NO_RES_SUBDIR = false;

	@Argument
	public static List<String> CMD_LINE_ARGS = new ArrayList<String>();

	@Option(name = "-cmc", usage = "Custom Main Class name", required = false)
	public static String CUSTOM_MAIN_CLASS = "";

	@Option(name = "-repcon", usage = "configure file for the replay defining terminal, ignored components and ignored windows", aliases = "-repcon, --configure-file")
	public static String RIP_CONFIG_FILE = "";
}
