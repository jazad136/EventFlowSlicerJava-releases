/*******************************************************************************
 *    Copyright (c) 2018 Jonathan A. Saddler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    Contributors:
 *     Jonathan A. Saddler - initial API and implementation
 *******************************************************************************/
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
	
	@Option(name = "-ripcon", usage = "configure file for the ripper defining terminal, ignored components and ignored windows", aliases = "-ripcon, --configure-file")
	public static String RIP_CONFIG_FILE = "";
}
