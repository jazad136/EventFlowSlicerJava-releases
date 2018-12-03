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

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleExtendedTable;
import javax.accessibility.AccessibleTable;

import edu.umd.cs.guitar.event.JFCCellClickHandler;
import edu.umd.cs.guitar.model.data.TaskList;

public abstract class CaptureTestCase extends Thread{
	
	protected String captureBackPort;
	protected boolean captureBack, eventPrintOn;
	public enum CommandKey {
		ENTER(KeyEvent.VK_ENTER, "[Space]"), 
		UP(KeyEvent.VK_UP, "[Up]"), 
		DOWN(KeyEvent.VK_DOWN, "[Down]"), 
		LEFT(KeyEvent.VK_LEFT, "[Left]"), 
		RIGHT(KeyEvent.VK_RIGHT, "[Right]"),
		SPACE(KeyEvent.VK_SPACE, "[Space]");
		
		public final String keyText;
		public final int replicationVK;
		CommandKey(int vk, String keyText)
		{
			replicationVK = vk;
			this.keyText = keyText;
		}
	}
	
	public CaptureTestCase()
	{
		eventPrintOn = false;
		captureBack = false;
		captureBackPort = "";
	}
	
	/**
	 * Set a variable that determines whether this test case will send its results over the network, using
	 * the number represented in portArgument as the port to use. 
	 * @param on
	 */
	public void setCaptureBackOnPort(String portArgument)
	{
		if(portArgument.isEmpty()) 
			captureBack = false;
		
		else {
			captureBack = true;
			captureBackPort = portArgument;
			setEventPrint(false);
		}
	}
	
	/**
	 * If capturing has been started, events are printed to console if
	 * printOn is true, otherwise, events are never printed to console. 
	 * 
	 * Preconditions: 	none
	 * Postconditions: 	Events are printed to console if captureOn is true
	 * 				 	and printOn is true. Otherwise, events are 
	 * 					never printed to console. 
	 */
	public void setEventPrint(boolean printOn)
	{
		eventPrintOn = printOn;
	}
	
	protected class TableModel 
	{
		public final int[][] indexPositions;
		public final int[][] columnHeaderPositions;
		public final int[][] rowHeaderPositions;
		
		/**
		 * Construct a table model. A table model is an important piece of defining how events enacted on tables
		 * are treated. When constructed the model will contain three different datastructures that identify 
		 * cells in the table, and how much space in the table each consumes. 
		 * 
		 * Preconditions: 	tContext is not null
		 * Postconditions: 	The three fields defining the table model: indexPositions, columnHeaderPositions and rowHeaderPositions are filled.
		 */
		public TableModel(AccessibleContext tContext)
		{
			AccessibleTable myTable = tContext.getAccessibleTable();
			if(myTable == null) {
				indexPositions = new int[0][0];
				columnHeaderPositions = new int[0][0];
				rowHeaderPositions = new int[0][0];
				return;
			}
			
			AccessibleTable columnHeaders, rowHeaders;
			// set up models for column headers, row headers, and table itself.
			
			
			// table
			indexPositions = mapTableToModel(myTable, tContext.getAccessibleChildrenCount());
			
			// columns
			columnHeaders = tContext.getAccessibleTable().getAccessibleColumnHeader();
			if(columnHeaders != null)
				columnHeaderPositions = mapTableToModel(columnHeaders, columnHeaders.getAccessibleColumnCount());
			else 
				columnHeaderPositions = new int[0][0];
			
			//rows
			rowHeaders = tContext.getAccessibleTable().getAccessibleRowHeader();
			if(rowHeaders != null) 
				rowHeaderPositions = mapTableToModel(rowHeaders, rowHeaders.getAccessibleRowCount());
			else 
				rowHeaderPositions = new int[0][0];
		}
		
		public String sp(int rowCol, boolean before)
		{
			if(rowCol < 10) 
				return "  ";
			else if(rowCol < 100) {
				return before ? " " : "  ";
			}
			else
				return "";
		}
		public String toString()
		{
			String strModel = "";
			int lastCell = Integer.MIN_VALUE;
			for(int i = 0; i < indexPositions.length; i++) {
				for(int j = 0; j < indexPositions[i].length; j++) {
					strModel += "|";
					if(indexPositions[i][j] != lastCell) {
						lastCell = indexPositions[i][j];
						strModel += sp(lastCell, true) + lastCell + sp(lastCell, false) + "|";
					}
					else 
						strModel += "  <  |";
				}
				strModel += "\n";
			}
			return strModel;
		}
		
		/**
		 *  If a table model exists within the context passed to tContext, map the model of the table 
		 */
		private int[][] mapTableToModel(AccessibleTable myTable, int childrenCount) 
		{
			int maxRows = myTable.getAccessibleRowCount();
			int maxCols = myTable.getAccessibleColumnCount();
			int[][] model = new int[maxRows][maxCols];
			
			if(model.length == 0)
				return new int[0][0];
			
			for(int i = 0; i < model.length; i++) 
				for(int j = 0; j < model[i].length; j++) 
					model[i][j] = -1;
			
			int r, c, cellRs, cellCs;
			
			// find the row and column this accessible goes into using special methods
			if(myTable instanceof AccessibleExtendedTable) {
				AccessibleExtendedTable aet = (AccessibleExtendedTable)myTable;
				for(int child = 0; child < childrenCount; child++) {
					r = aet.getAccessibleRow(child);
					c = aet.getAccessibleColumn(child);
					cellRs = aet.getAccessibleRowExtentAt(r, c);
					cellCs = aet.getAccessibleColumnExtentAt(r,c);
					for(int i = r; i < r + cellRs; i++) 
						for(int j = c; j < c + cellCs; j++) 
							model[i][j] = child;
				}
			}
			else {			
				// find the row and column this accessible goes into using counting
				r = c = 0;
				for(int child = 0; child < childrenCount; child++) {
					if(maxRows == 0 && maxCols == 0) break; // something is very wrong with this table!
					if(r > maxRows) break;
					if(model[r][c] != -1) continue;
					// instead of using a counter that looks like this, we need a boolean matrix to "map out"
					// how these tables are really going to look.
					cellRs = myTable.getAccessibleRowExtentAt(r, c);
					cellCs = myTable.getAccessibleColumnExtentAt(r, c);
					for(int i = r; i < r + cellRs; i++) 
						for(int j = c; j < c + cellCs; j++) 
							model[i][j] = child;
					c += cellCs;
					if(c == maxCols) {
						r++;
						c = 0;
					}
				}
			}
			return model;
		}
		
		
		public String[] cellsForIndices(Collection<Integer> targetIndices) 
		{
			ArrayList<String> toReturn = new ArrayList<String>();
			for(int i = 0; i < indexPositions.length; i++)
				for(int j = 0; j < indexPositions[i].length; j++)
					for(int idx : targetIndices) 
						if(idx == indexPositions[i][j]) 
							toReturn.add("("+ i + ", " + j+")");
			return toReturn.toArray(new String[0]);
		}
	}
	public abstract TaskList endAndGetTaskList();
}
