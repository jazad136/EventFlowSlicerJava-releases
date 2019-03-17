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
package edu.unl.cse.efs;

import java.io.File;

import edu.unl.cse.efs.tools.TimeFormats;

/**
 * Source for the ApplicationData class.
 * ApplicationData holds and answers questions regarding information on
 * - files used by EventFlowSlicer.<br>
 * - and arguments to applications EventFlowSlicer is testing.<br>
 *
 * @author jsaddle
 *
 */
public class ApplicationData {
	private File applicationFilePath;
	private File workingGUIFile;
	private File workingEFGFile;
	private File workingTaskListFile;
	private String subdirectoryFiller;
	private File outputDirectory;
	private File workingTestCaseDirectory;
	private String outputDirectoryProvided;
	private String testCaseDirectoryProvided;
	private String extrasAppend;
	private String outputAppend;
	private String timeLogAppend;
	private String constraintsAppend;
	private String customMainClass;
	private File argumentsAppFile;
	private File argumentsVMFile;
	private File preferencesFile;
	private File ripConfigFile;
	private File replayConfigFile;

	/**
	 * java application time to delay before maximizing window
	 */
	public static int maximizeDelay = 6000;
	/**
	 * java application open wait time
	 */
	public static int openWaitTime = 8000;

	/**
	 * Default filename to assign to logs
	 * This class does not typically manage guitar logs, but it is provided here in
	 * case the situation arises where a default is needed.
	 */
	public static final String DEFAULT_GUITARLOG_FILENAME = TimeFormats.ymdhsTimeStamp() + ".log";

	public ApplicationData()
	{
		applicationFilePath = new File("");
		workingGUIFile = new File("");
		workingEFGFile = new File("");
		workingTaskListFile = new File("");
		workingTestCaseDirectory = new File("");
		argumentsAppFile = new File("");
		argumentsVMFile = new File("");
		preferencesFile = new File("");
		outputDirectory = new File("");
		ripConfigFile = new File("");
		replayConfigFile = new File("");
		extrasAppend = "Extra_Output";
		outputAppend = "gen_out";
		timeLogAppend = "times_log";
		constraintsAppend = "tasklist";
		outputDirectoryProvided = null;
		testCaseDirectoryProvided = null;
		customMainClass = "";
		resetSubdirectoryFiller();
	}

	public void setAppFilePath(String filepath)
	{
		if(filepath == null)
			filepath = "";
		else
			filepath = filepath.trim();
		applicationFilePath = new File(filepath);
	}
	public File getAppFile()
	{
		return applicationFilePath;
	}

	public boolean hasAppFile()
	{
		return !applicationFilePath.getName().isEmpty();
	}

	public boolean applicationFileExists()
	{
		return applicationFilePath.exists() && !applicationFilePath.isDirectory();
	}


	// Rip Configuration File
	public void setRipConfigurationFile(String filepath)
	{
		if(filepath == null)
			filepath = "";
		else
			filepath = filepath.trim();
		ripConfigFile = new File(filepath);
	}
	public File getRipConfigurationFile()
	{
		return ripConfigFile;
	}
	public boolean hasRipConfigurationFile()
	{
		return !ripConfigFile.getName().isEmpty();
	}
	public boolean ripConfigurationFileExists()
	{
		return ripConfigFile.exists() && !ripConfigFile.isDirectory();
	}

	// Replay configuration File
	public void setReplayConfigurationFile(String filepath)
	{
		if(filepath == null)
			filepath = "";
		else
			filepath = filepath.trim();
		replayConfigFile = new File(filepath);
	}
	public File getReplayConfigurationFile()
	{
		return replayConfigFile;
	}
	public boolean hasReplayConfigurationFile()
	{
		return !replayConfigFile.getName().isEmpty();
	}
	public boolean replayConfigurationFileExists()
	{
		return replayConfigFile.exists() && !replayConfigFile.isDirectory();
	}
	//Custom Main Class
	public void setCustomMainClass(String classPath)
	{
		if(classPath == null)
			classPath = "";
		else
			classPath = classPath.trim();
		customMainClass = classPath;
	}
	public String getCustomMainClass()
	{
		return customMainClass;
	}
	public boolean hasCustomMainClass()
	{
		return !customMainClass.isEmpty();
	}
	public void setDefaultWorkingTestCaseDirectory()
	{
		if(!hasOutputDirectory())
			throw new RuntimeException("Cannot set default working test case directory without output directory.");
		workingTestCaseDirectory = new File(getOutputDirectory().getPath());
	}
	public void setDefaultWorkingTaskListFile()
	{
		if(!hasOutputDirectory())
			throw new RuntimeException("Cannot set default working task list file without output directory.");
		workingTaskListFile = new File(getOutputDirectory().getPath(), getConstraintsFileAppend() + ".xml");
	}

	public void setDefaultWorkingGUIFile()
	{
		if(!hasAppFile())
			throw new RuntimeException("Cannot set default working GUI file without App file.");
		if(!hasOutputDirectory())
			throw new RuntimeException("Cannot set default working GUI file without output directory.");

		String fileStem = getAppFile().getName();
		int classPt = fileStem.lastIndexOf(".class");
		if(classPt != -1)
			fileStem = fileStem.substring(0, classPt);
		else {
			int jarPt = fileStem.lastIndexOf(".jar");
			if(jarPt != -1)
				fileStem = fileStem.substring(0, jarPt);
		}
		// what if we have a long class name? Take out the content before the actual class name.
		int lastDotPt = fileStem.lastIndexOf(".");
		if(lastDotPt != -1) {
			fileStem = fileStem.substring(lastDotPt+1);
		}
		// overwrite the GUI file
		workingGUIFile = new File(getOutputDirectory().getAbsolutePath() + File.separator + fileStem + ".GUI");
	}

	//GUI File
	public void setWorkingGUIFile(String filepath)
	{
		if(filepath == null)
			filepath = "";
		else
			filepath = filepath.trim();
		workingGUIFile = new File(filepath);
	}
	public File getWorkingGUIFile()
	{
		return workingGUIFile;
	}
	public boolean workingGUIFileExists()
	{
		return workingGUIFile.exists() && !workingGUIFile.isDirectory();
	}
	public boolean hasWorkingGUIFile()
	{
		return !workingGUIFile.getName().isEmpty();
	}
	// EFG File
	public void setWorkingEFGFile(String filepath)
	{
		if(filepath == null)
			filepath = "";
		else
			filepath = filepath.trim();
		workingEFGFile = new File(filepath);
	}
	public void setDefaultWorkingEFGFile()
	{
		if(!hasAppFile())
			throw new RuntimeException("Cannot set default working EFG file without App file.");
		if(!hasOutputDirectory())
			throw new RuntimeException("Cannot set default working EFG file without output directory.");

		String fileStem = getAppFile().getName();
		int classPt = fileStem.lastIndexOf(".class");
		if(classPt != -1)
			fileStem = fileStem.substring(0, classPt);
		else {
			int jarPt = fileStem.lastIndexOf(".jar");
			if(jarPt != -1)
				fileStem = fileStem.substring(0, jarPt);
		}
		// what if we have a long class name? Take out the content before the actual class name.
		int lastDotPt = fileStem.lastIndexOf(".");
		if(lastDotPt != -1) {
			fileStem = fileStem.substring(lastDotPt+1);
		}

		// overwrite the name of the EFG file
		workingEFGFile = new File(getOutputDirectory().getAbsolutePath() + File.separator + fileStem + ".EFG");
	}
	public File getWorkingEFGFile()
	{
		return workingEFGFile;
	}

	public boolean workingEFGFileExists()
	{
		return workingEFGFile.exists() && !workingEFGFile.isDirectory();
	}
	public boolean hasWorkingEFGFile()
	{
		return !workingEFGFile.getName().isEmpty();
	}
	// TaskList File
	public void setWorkingTaskListFile(String filepath)
	{
		if(filepath == null)
			filepath = "";
		else
			filepath = filepath.trim();
		workingTaskListFile = new File(filepath);
	}
	public File getWorkingTaskListFile()
	{
		return workingTaskListFile;
	}
	public boolean workingTaskListFileExists()
	{
		return workingTaskListFile.exists() && !workingTaskListFile.isDirectory();
	}
	public boolean hasWorkingTaskListFile()
	{
		return !workingTaskListFile.getName().isEmpty();
	}
	public void setArgumentsAppFile(String filepath)
	{
		if(filepath == null)
			filepath = "";
		else
			filepath = filepath.trim();
		argumentsAppFile = new File(filepath);
	}
	public File getArgumentsAppFile()
	{
		return argumentsAppFile;
	}
	public boolean argumentsAppFileExists()
	{
		return argumentsAppFile.exists() && !argumentsAppFile.isDirectory();
	}
	public boolean hasArgumentsAppFile()
	{
		return !argumentsAppFile.getName().isEmpty();
	}
	// Arguments
	public void setArgumentsVMFile(String filepath)
	{
		if(filepath == null)
			filepath = "";
		else
			filepath = filepath.trim();
		argumentsVMFile = new File(filepath);
	}
	public File getArgumentsVMFile()
	{
		return argumentsVMFile;
	}
	public boolean argumentsVMFileExists()
	{
		return argumentsVMFile.exists() && !argumentsVMFile.isDirectory();
	}

	public boolean hasArgumentsVMFile()
	{
		return !argumentsVMFile.getName().isEmpty();
	}

	// Preferences File
	public void setPreferencesFile(String filepath)
	{
		filepath = filepath.trim();
		if(filepath == null)
			filepath = "";
		preferencesFile = new File(filepath);
	}
	public File getPreferencesFile()
	{
		return preferencesFile;
	}
	public String getTestCaseDirectoryProvided()
	{
		return testCaseDirectoryProvided;
	}
	// Test Case Directory
	public void setWorkingTestCaseDirectory(String filepath)
	{
		if(filepath != null)
			filepath = filepath.trim();
		testCaseDirectoryProvided = filepath;
		if(filepath == null || filepath.isEmpty())
			filepath = "";
		else {
			filepath = trimDirPath(filepath);
			filepath = fillDirPath(filepath);
		}
		workingTestCaseDirectory = new File(filepath);
	}
	public File getWorkingTestCaseDirectory()
	{
		return workingTestCaseDirectory;
	}
	public boolean workingTestCaseDirectoryExists()
	{
		return workingTestCaseDirectory.exists() && workingTestCaseDirectory.isDirectory();
	}
	public boolean hasWorkingTestCaseDirectory()
	{
		return !workingTestCaseDirectory.getName().isEmpty();
	}

	/**
	 * Return the relevant character data from the filepath specified if filepath is ".",
	 * the characters from the absolute path represented by new File(".") (which is
	 * normally System.getProperty("user.dir"))
	 */
	public String fillDirPath(String filepath)
	{
		if(filepath.equals(".")) {
			File dotFile = new File(".");
			filepath = dotFile.getAbsolutePath();
			// the behavior of this function is to typically place /. after the name of the path.
			// remove these extraneous characters
			filepath = filepath.substring(0, filepath.length()-2);
		}
		return filepath;
	}

	// Output Capture Directory
	public void setOutputDirectory(String filepath)
	{
		if(filepath != null)
			filepath = filepath.trim();
		outputDirectoryProvided = filepath;
		if(filepath == null || filepath.isEmpty())
			filepath = "";
		else {
			filepath = trimDirPath(filepath);
			filepath = fillDirPath(filepath);
			filepath += File.separator + subdirectoryFiller;
		}
		outputDirectory = new File(filepath);
	}
	public File getOutputDirectory()
	{
		return outputDirectory;
	}
	public boolean outputDirectoryExists()
	{
		return outputDirectory.exists() && outputDirectory.isDirectory();
	}

	public boolean hasOutputDirectory()
	{
		return !outputDirectory.getName().isEmpty();
	}

	/**
	 * Returns a trimmed version of the String provided to the setOutputDirectory method.
	 * @return
	 */
	public String getOutputDirectoryProvided()
	{
		return outputDirectoryProvided;
	}
	public File getOutputGenBaseFile()
	{
		if(!outputDirectoryExists())
			throw new RuntimeException("Cannot return generator base file: Output directory does not exist or is invalid.");

		return new File(getOutputGenExtrasDirectory(), outputAppend);
	}
	public File getOutputGenExtrasDirectory()
	{
		if(!outputDirectoryExists())
			throw new RuntimeException("Cannot return extras directory: Output directory does not exist or is invalid.");
		return new File(outputDirectory, extrasAppend);
	}
	public boolean outputGenExtrasDirectoryExists()
	{
		return getOutputGenExtrasDirectory().exists() && getOutputGenExtrasDirectory().isDirectory();
	}
	public File getOutputExtrasTimeLogFile()
	{
		if(!outputDirectoryExists())
			throw new RuntimeException("Cannot return time log file: Gen directory does not exist or is invalid.");
		return new File(getOutputGenExtrasDirectory(), timeLogAppend);
	}


	private String trimDirPath(String toTrim)
	{
		if(toTrim.length() > 1 && toTrim.charAt(toTrim.length()-1) == File.separatorChar)
			toTrim = toTrim.substring(0, toTrim.length()-1);
		toTrim = toTrim.replace("." + File.separator, "");
		return toTrim;
	}

	public void unsetSubdirectoryFiller()
	{
		subdirectoryFiller = "";
	}
	public void resetSubdirectoryFiller()
	{
		subdirectoryFiller = "EventFlowSlicerResults_" + TimeFormats.nowMilitary();
	}
	public String getSubdirectoryFiller()
	{
		return subdirectoryFiller;
	}
	public String getConstraintsFileAppend()
	{
		return constraintsAppend;
	}
}
