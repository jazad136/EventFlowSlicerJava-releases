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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.kohsuke.args4j.CmdLineException;
import org.xml.sax.SAXException;

import edu.umd.cs.guitar.exception.GException;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.Step;
import edu.umd.cs.guitar.model.data.StepType;
import edu.umd.cs.guitar.model.data.Task;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.umd.cs.guitar.replayer.GReplayerConfiguration;
import edu.umd.cs.guitar.replayer.JFCReplayerConfiguration;
import edu.umd.cs.guitar.replayer.JFCReplayerMonitor;
import edu.umd.cs.guitar.util.GUITARLog;

public class JFCReplayerEFS {
	ReplayerController cgHelper;
	GUIStructure replayerGUI;
	EFG replayerEFG;
	private ReplayerEFS replayer;

	public JFCReplayerEFS()
	{
		
	}
    /**
     * @param configuration
     */
    public JFCReplayerEFS(GUIStructure replayerGUI, EFG replayerEFG) 
    {
    	this.replayerGUI = replayerGUI;
    	this.replayerEFG = replayerEFG;
    }
	public void setCgHelper(ReplayerController cgRep){
		this.cgHelper = cgRep;
	}

	
	/**
    * Map the fields from a test case object to a task object, 
    * 
    */
	public static Task mapTestCaseToTask(TestCase inputTestCase)
	{
	// instantiate the steps
		ObjectFactory fact = new ObjectFactory();
		Task toReturn = fact.createTask();
		List<Step> taskSteps = new LinkedList<Step>();
		for(StepType s : inputTestCase.getStep()) {
			Step oneStep = fact.createStep();
			oneStep.setEventId(s.getEventId()); // eid
			oneStep.setReachingStep(s.isReachingStep()); // rs
			oneStep.setWindowId(s.getWindowID());
//			oneStep.setAction(s.getAction());
			Iterator<String> pIt = s.getParameter().iterator();
			String theParameter = "";
			if(pIt.hasNext()) 
				theParameter = pIt.next();
			while(pIt.hasNext())
					theParameter += GUITARConstants.NAME_SEPARATOR + pIt.next();
			oneStep.setParameter(theParameter);
			taskSteps.add(oneStep);
		}
		toReturn.setStep(taskSteps);

		return toReturn;	
	}
   public void setupEnv() throws IOException, SAXException, ParserConfigurationException
   {
	   Task task = null;
	      TestCase tc = null;
	      XMLHandler handler= new XMLHandler();
	      try{		
	    	  Object taskObj = handler.readObjFromFile(JFCReplayerConfiguration.TESTCASE, Task.class);
	    	  if(taskObj instanceof TestCase)
	    		  tc = (TestCase)taskObj;
	    	  else if(taskObj != null)
	    		  task = (Task)taskObj;
	      }
	      catch(Exception e){
	    	  System.err.println("\nTask file ould not be loaded to task object. \n" + 
	    			  e.getClass() + ":" + e.getMessage());
	      }
	      
	      
	      
	      try {
	        if (task == null && tc == null) {
	        	System.err.println("JFCReplayer: Task file could not be read.");
	        	throw new FileNotFoundException("Task (.tst) file could not be read from the file system.");
	        }
	        
	        if(task != null)  
	        	replayer = new ReplayerEFS(task, true);
	        
	        else 
	        	replayer = new ReplayerEFS(tc, JFCReplayerConfiguration.GUI_FILE, JFCReplayerConfiguration.EFG_FILE, true);
//	        	replayer = new ReplayerEFS(tc, )
	        
	        // instantiate the replayer monitor. 
	        JFCReplayerMonitor jMonitor =
	           new JFCReplayerMonitor(JFCReplayerConfiguration.MAIN_CLASS);

	        // Add a pause monitor and ignore time out monitor if needed
	        
//	        if (JFCReplayerConfiguration.PAUSE) {
//	           GTestMonitor pauseMonitor = new PauseMonitor();
//	           replayer.addTestMonitor(pauseMonitor);
//	        } 


	        // Add Terminal monitor
//	        if (JFCReplayerConfiguration.TERMINAL_SEARCH) {
//	           GTestMonitor terminalMonitor = new JFCTerminationMonitor();
//	           Map<Integer, String> mTerminalLabels =
//	              getMTerminalLabels(JFCReplayerConfiguration.CONFIG_FILE);
//	            	((JFCTerminationMonitor) terminalMonitor).setmTerminalLabels(mTerminalLabels);
//	            	((JFCTerminationMonitor) terminalMonitor).setDelay(JFCReplayerConfiguration.DELAY);
//	           replayer.addTestMonitor(terminalMonitor);
//	         }

	         // Set up string comparator
	         jMonitor.setUseReg(JFCReplayerConfiguration.USE_REG);
	         // Set replayer monitors
	         replayer.setReplayerController(cgHelper);
	         replayer.setMonitor(jMonitor);
	      } 
	      catch (GException e) 
	      {
	    	  throw e;
	      } 
	      catch (IOException e) 
	      {
	    	  GUITARLog.log.error("Unable to find TST file.");
  	      	  throw e;
	      } 
	      catch (Exception e) 
	      {
	    	  throw e;
	      }
   }
   public void execute() throws Exception, GException, FileNotFoundException
   {	   
	   if (JFCReplayerConfiguration.HELP) 
		   throw new CmdLineException("");
	   
	   if(!JFCReplayerConfiguration.LOG_FILE.isEmpty()) 
		   GUITARLog.addFileAppender(JFCReplayerConfiguration.LOG_FILE);
	   
	   try {
		   checkArgs();
		   setupEnv();
		   replayer.execute();
	   } 
	   catch (Exception e) {
		   System.err.println("Error while replaying");
  			if(!JFCReplayerConfiguration.LOG_FILE.isEmpty()) 
  				GUITARLog.log.error("Error while replaying");
  			throw e;
  		}
      
   }

   /**
    * Check for command-line arguments
    * 
    * @throws CmdLineException
    * 
    */
   private void checkArgs() throws CmdLineException
	{
      // Check argument
      if (GReplayerConfiguration.HELP) {
         throw new CmdLineException("");
      }

      boolean isPrintUsage = false;

      if (JFCReplayerConfiguration.MAIN_CLASS == null) {
         System.err.println("missing '-c' argument");
         isPrintUsage = true;
      }
      
      if (JFCReplayerConfiguration.GUI_FILE == null) {
         System.err.println("missing '-g' argument");
         isPrintUsage = true;
      }

      if (JFCReplayerConfiguration.EFG_FILE == null) {
         System.err.println("missing '-e' argument");
         isPrintUsage = true;
      }

      if (JFCReplayerConfiguration.TESTCASE == null) {
         System.err.println("missing '-t' argument");
         isPrintUsage = true;
      }

      if (isPrintUsage) 
         throw new CmdLineException("");
   }

} // End of class
