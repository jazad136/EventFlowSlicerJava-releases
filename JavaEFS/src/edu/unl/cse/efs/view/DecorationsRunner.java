package edu.unl.cse.efs.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * Source for the DecorationsRunner class. Written by Jonathan Saddler for the support
 * of the new EventFlowSlicer GUI Tool.<br>
 * Policies:<br>
 * Placing an element in a certain row can fail if the choice to edit a certain row, by calling {@link DecorationsRunner#editingRow(int, int)}, results in
 * choosing an invalid row (the same is true for columns and calling @link {@link DecorationsRunner#editingColumn(int, int)})
 * <br>
 *
 * @author Jonathan Saddler
 *
 */
public class DecorationsRunner
{
	public static JPanel[][] smallPanels;
	private static Container targetContainer;
	private static boolean[][] erase;
	private static int cols, rows = -1;
	private static GridBagConstraints addDefaults;
	private static double rowWeight, colWeight;

	/** Row **/
	private static int currentR = -1;
	/** Column **/
	private static int currentC = -1;
	/** Spot (cell) **/
	private static int currentS = -1;

	public static void resetCurrent()
	{
		currentR = currentS = currentC = -1;
	}

	public static class Test{
		public static void main(String[] args)
		{
			mainTest(args);
		}
	}

	public static <T extends Container> void pushOld(T newTarget, int numCols, int numRows)
	{
		pushed.push(new RunnerSet());
		initialize(newTarget, numCols, numRows);
	}
	public static void pop()
	{
		RunnerSet oldV = pushed.pop();
		rows = oldV.nrows;
		cols = oldV.ncols;
		smallPanels = oldV.pan;
		erase = oldV.er;
		currentR = oldV.cR;
		currentC = oldV.cC;
		currentS = oldV.cS;
	}
//
	public static class RunnerSet
	{
		int ncols, nrows;
		boolean rowMode, colMode;
		boolean[][] er;
		JPanel[][] pan;
		int cR, cC, cS;
		public RunnerSet()
		{
			ncols = cols;
			nrows = rows;
			cS = currentS; cR = currentR; cC = currentC;
			er = new boolean[cols][rows];
			pan = new JPanel[cols][rows];
			for(int i = 0; i < cols; i++)
				for(int j = 0; j < rows; j++) {
					er[i][j] = erase[i][j];
					pan[i][j] = smallPanels[i][j];
				}
		}
	}
	private static LinkedList<RunnerSet> pushed = new LinkedList<RunnerSet>();
	public static void mainTest(String[] args)
	{
		JFrame myFrame = new JFrame("Window Test");
		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myFrame.setPreferredSize(new Dimension(450, 450));
		JPanel content = new JPanel();
		myFrame.add(content);
		initialize(content, 5, 5, true);
		hardenEdits();
		editingStartOfRow(0);
		button("btn_added", "Added!");
		button("btn_second", "Added!");

		myFrame.pack();
		myFrame.setVisible(true);
	}



	/**
	 * Initialize this (laid-out) panel with a grid pattern corresponding to numCols, numRows.
	 * @param target
	 * @param numCols
	 * @param numRows
	 * @return
	 */
	public static <T extends Container> T initialize(T target, int numCols, int numRows)
	{
		return initialize(target, numCols, numRows, false);
	}

	public static JPanel initializeNew(int numCols, int numRows)
	{
		return initialize(new JPanel(), numCols, numRows);
	}

	public static JPanel initializeNewWithGrid(int numCols, int numRows)
	{
		return initialize(new JPanel(), numCols, numRows, true);
	}

	/**
	 * This method does the necessary setup to allow for the layout of
	 * the target container to be changed using the methods provided by decorations.
	 * Consider this as a constructor to initialize the instance variables used by Decorations
	 * to do its necessary job with the gridbaglayout it stores.
	 */
	public static <T extends Container> T initialize(T target, int numCols, int numRows, boolean bordersOn)
	{
		targetContainer = target;
		target.setLayout(new GridBagLayout());
		cols = numCols;
		rows = numRows;

		smallPanels = new JPanel[cols][rows];
		erase = new boolean[cols][rows];

		resetAddDefaults();
		GridBagConstraints gridSpots = cloneAddDefaults();
		gridSpots.fill = GridBagConstraints.BOTH;
		rowWeight = 1.0/(cols-Math.ulp(1.0/rows));
		colWeight = 1.0/(rows-Math.ulp(1.0/cols));
		gridSpots.weightx = colWeight;
		gridSpots.weighty = rowWeight;

		for(int i = 0; i < cols; i++)
			for(int j = 0; j < rows; j++) {
				gridSpots.gridx = i;
				gridSpots.gridy = j;
				smallPanels[i][j] = new JPanel();
				smallPanels[i][j].setLayout(new GridBagLayout());
				if(bordersOn)
					smallPanels[i][j].setBorder(new LineBorder(Color.LIGHT_GRAY));

				target.add(smallPanels[i][j], gridSpots);
			}
		resetCurrent();
		return target;
	}

	private <T extends Container> void assignGridSpots(T target, GridBagConstraints defaultConstraints, JPanel[][] panels)
	{
		for(int i = 0; i < cols; i++)
			for(int j = 0; j < rows; j++) {
				defaultConstraints.gridx = i;
				defaultConstraints.gridy = j;
				panels[i][j] = new JPanel();
				panels[i][j].setLayout(new GridBagLayout());
				target.add(panels[i][j], defaultConstraints);
			}
	}
	private JPanel[][] resetSmallPanels(Container target, JPanel[][] pans, boolean bordersOn)
	{
		for(int i = 0; i < cols; i++)
			for(int j = 0; j < rows; j++) {
				smallPanels[i][j] = new JPanel();
				smallPanels[i][j].setLayout(new GridBagLayout());
				if(bordersOn)
					smallPanels[i][j].setBorder(new LineBorder(Color.LIGHT_GRAY));
			}
		return pans;
	}
	/**
	 * Implements cell positioning policy, using a (-1 BASED) system of controlling where
	 * the next cell to be edited will be.
	 * A cell is editable if and only if the position is less than maxCell, and position is >= -1.
	 * Returns true if the next cell is a valid cell that can handle placement.
	 *
	 * Passing a value < -1, moves cell position to -1.
	 * Returns false otherwise.
	 *
	 * Preconditions: 	maxCell points to the last cell within the structure
	 * 					where placement of objects on the GUI is being observed
	 * Postconditions: CurrentS is the index of nextCell.
	 *
	 * @return
	 */
	private static boolean cellPosition(int nextCell, int maxCell)
	{
		if(nextCell < -1) {
			currentS = -1;
			return true;
		}
		else if(nextCell >= maxCell) {
			currentS = maxCell; // ensure currentS is within range, but also that we can't edit the next cell.
			return false;
		}
		currentS = nextCell; // currentS points to the spot before the next cell to be edited.
		return true;
	}

	/**
	 * Begin the next edit as a row edit at row specified by the zero-indexed parameter provided.
	 * Let the next cell to an editing method place the object at nextCell.
	 * Return true if this runner is allowed to edit the next cell specified.
	 *
	 * FROM HERE ON DOWN THE CALL CHAIN TO SET CELL POSITION RELATED VARIABLES,
	 * CELL POSITION IS THE POSITION BEHIND THE NEXT CELL TO BE EDITED.
	 *
	 * This method has dominant knowledge over cellPosition, a simpler method.
	 */
	public static boolean editingRow(int rowNumber, int nextCell)
	{
		currentC = -1;
		if(rowNumber >= 0 && rowNumber < rows && nextCell >= 0) {
			currentR = rowNumber;
			return cellPosition(nextCell-1, cols); // return true if the cell edit is valid.
		}
		else {
			currentR = -1;
			cellPosition(nextCell-1, cols);
			return false; // because the row is invalid, the cell edit must also be invalid.
		}
	}

	/**
	 * Begin the next edit as a column edit at the column specified by the zero-indexed parameter provided.
	 * Let the next cell to an editing method place the object at nextCell.
	 * Return true if this runner is allowed to edit the next cell specified.
	 *
	 * FROM HERE ON DOWN THE CALL CHAIN TO SET CELL POSITION RELATED VARIABLES,
	 * CELL POSITION IS THE POSITION BEHIND THE NEXT CELL TO BE EDITED.
	 *
	 *  This method has dominant knowledge over cellPosition, a simpler method.
	 */
	public static boolean editingColumn(int columnNumber, int nextCell)
	{
		currentR = -1;
		if(columnNumber >= 0 && columnNumber < cols && nextCell >= 0) {
			currentC = columnNumber;
			return cellPosition(nextCell-1,rows); // return true if the cell edit is valid
		}
		else {
			currentC = -1;
			cellPosition(nextCell-1, rows);
			return false; // because the row is invalid, the cell edit must also be invalid.
		}
	}


	/**
	 * Begin the next edit as a row edit at the row specified by the zero-indexed parameter provided.
	 * Let the next cell to an editing method place the object at spot 0 in this row.
	 */
	public static boolean editingStartOfRow(int rowNumber)
	{
		return editingRow(rowNumber, 0);
	}

	/**
	 * Begin the next edit as a column edit at the column specified by the zero-indexed parameter provided.
	 * Let the next cell to an editing method place the object at spot 0 in this row.
	 */
	public static boolean editingStartOfColumn(int columnNumber)
	{
		return editingColumn(columnNumber, 0);
	}


	/**
	 * Begin the next edit at the next cell after the current cell.
	 * @param columnNumber
	 * @return
	 */
	public static boolean jumpRowOrColumn()
	{
		if(currentC == -1 && currentR == -1)
			return false;

		else if(currentC == -1) { // placing in a row
			if(currentS >= cols-1) // nowhere to jump
				return false;
		}
		else { // placing in a row
			if(currentS >= rows-1) // nowhere to jump.
				return false;
		}
		currentS++;
		return true;
	}


	/**
	 * This method does the necessary teardown for the layout of the target container
	 * so that all changes made by this object are checked for their constraints, and
	 * then run to their completion. Column and row width changes are an example of such a constraint
	 * that is only partially configured during the editing process, and which must be finalized
	 * using this method, so that columns and rows beneath or beside rows and columns that have wide
	 * spans get erased that should be covered up by another spanning row or column.
	 */
	public static Container hardenEdits()
	{
		for(int i = 0; i < smallPanels.length; i++)
			for(int j = 0; j < smallPanels[i].length; j++)
				if(erase[i][j]) {
					targetContainer.remove(smallPanels[i][j]);
					erase[i][j] = false;
				}
		return targetContainer;
	}

	public static void resetAddDefaults()
	{
		addDefaults = new GridBagConstraints();
		addDefaults.weightx = 0;
		addDefaults.weighty = 0;
		addDefaults.fill = GridBagConstraints.BOTH;
		addDefaults.anchor = GridBagConstraints.CENTER;
	}
	private static GridBagConstraints cloneAddDefaults()
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = addDefaults.gridx;
		gbc.gridy = addDefaults.gridy;
		gbc.weightx = addDefaults.weightx;
		gbc.weighty = addDefaults.weighty;
		gbc.fill = addDefaults.fill;
		gbc.anchor = addDefaults.anchor;
		return gbc;
	}

	/**
	 * Change to use modes rather than int assignments.
	 * Return 0 if we're using an invalid row
	 * Return 1 if we're using
	 * Return 2 if we're using an invalid col.
	 *
	 * @return
	 */
	public static int rowColStatus(int nextCellWidth)
	{
		if(!(currentC == -1 && currentR == -1)) {
			if(currentC == -1) {
				if(currentS + nextCellWidth >= cols)
					return 0; // means using invalid row
				else
					return 1; // means using valid row
			}
			else {
				if(currentS + nextCellWidth >= rows)
					return 2; // means using invalid col
				else
					return 3; // means valid col
			}
		}
		return -1;
	}
	public static <T extends JComponent> T place(T component, int cellWidth)
	{
		if(cellWidth <= 0)
			return component;
		int row, col;
		boolean adjustingC = false;
		if(currentC == -1 && currentR == -1)
			return component;

		// determine status of next placement.
		if(currentC == -1) { // placing in a row
			if(currentS >= cols-cellWidth)
				return component; // spot is invalid.
			row = currentR;
			col = currentS+1;
		}
		else { // placing in a column
			adjustingC = true;
			if(currentS >= rows-cellWidth)
				return component; // spot is invalid.
			row = currentS+1;
			col = currentC;
		}
		GridBagConstraints gridCon = ((GridBagLayout)targetContainer.getLayout()).getConstraints(smallPanels[col][row]);
		GridBagConstraints littleCon = cloneAddDefaults();

		if(adjustingC) {
			gridCon.gridheight = cellWidth;
			for(int i = currentS+2; i <= currentS+cellWidth; i++)
				erase[currentC][i] = true;
		}
		else {
			gridCon.gridwidth = cellWidth;
			for(int i = currentS+2; i <= currentS+cellWidth; i++)
				erase[i][currentR] = true;
		}

		smallPanels[col][row].add(component, littleCon);
 		targetContainer.add(smallPanels[col][row], gridCon);
 		currentS += cellWidth;
		return component;
	}


	/**
	 * Place a component in the next cell specified.
	 */
	public static <T extends JComponent> T place(T component)
	{

		int row, col;
		if(currentC == -1 && currentR == -1)
			return component; // do nothing.
		if(currentC == -1) { // placing in a row
			if(currentS == cols-1)
				return component; // spot is invalid.
			row = currentR;
			col = currentS+1;
		}
		else { // placing in a row
			if(currentS == rows-1)
				return component; // spot is invalid.
			row = currentS+1;
			col = currentC;
		}
		// create some defaults used to add elements to a cell within the JPanel we're using.
		GridBagConstraints littleCon = cloneAddDefaults();

		smallPanels[col][row].add(component, littleCon);
 		currentS++;
		return component;
	}
	public static void increaseVerticalMargin(int onTop, int onBottom)
	{
		int row, col;
		if(currentC == -1 && currentR == -1)
			return;
		if(currentC == -1) { // placing in a row
			if(currentS == cols-1)
				return;
			row = currentR;
			col = currentS+1;
		}
		else { // placing in a row
			if(currentS == rows-1)
				return;
			row = currentS+1;
			col = currentC;
		}
		GridBagConstraints littleCon = ((GridBagLayout)targetContainer.getLayout()).getConstraints(smallPanels[col][row]);
		littleCon.insets.top+= onTop;
		littleCon.insets.bottom += onBottom;
		targetContainer.add(smallPanels[col][row], littleCon);

	}

	public static void horWeight(double newWeight)
	{
		int row, col;
		if(currentC == -1 && currentR == -1)
			return;
		if(currentC == -1) { // placing in a row
			if(currentS == cols-1)
				return;
			row = currentR;
			col = currentS+1;
		}
		else { // placing in a row
			if(currentS == rows-1)
				return;
			row = currentS+1;
			col = currentC;
		}

		GridBagConstraints littleCon = ((GridBagLayout)targetContainer.getLayout()).getConstraints(smallPanels[col][row]);
		littleCon.weightx = newWeight;
		targetContainer.add(smallPanels[col][row], littleCon);
	}

	public static JButton button(String buttonName, String buttonText)
	{
		JButton button = new JButton(buttonText);
		button.getAccessibleContext().setAccessibleName(buttonName);
		return place(button);
	}


	public static JTextField field(String fieldName, int widthInCharacters)
	{
		JTextField field = new JTextField(widthInCharacters);
		field.setMinimumSize(field.getPreferredSize());
		field.getAccessibleContext().setAccessibleName(fieldName);
		return place(field);
	}

	public static JTextField field(String fieldName)
	{
		JTextField field = new JTextField();
		field.getAccessibleContext().setAccessibleName(fieldName);

		return place(field);
	}
	public static JButton button(String buttonName, float fontSize, String labelText)
	{
		JButton button = new JButton(labelText);
		button.getAccessibleContext().setAccessibleName(buttonName);
		button.setFont(button.getFont().deriveFont(fontSize));
		return place(button);
	}


	public static JLabel label(String labelName, float fontSize, String labelText)
	{
		JLabel label = new JLabel(labelText);
		label.setFont(label.getFont().deriveFont(fontSize));
		label.getAccessibleContext().setAccessibleName(labelName);
		return place(label);
	}

	public static JLabel spanningLabel(String labelName, float fontSize, String labelText, int cellSpan)
	{
		JLabel label = new JLabel(labelText);
		label.setFont(label.getFont().deriveFont(fontSize));
		label.getAccessibleContext().setAccessibleName(labelName);
		return place(label, cellSpan);
	}

	public static <E> JList<E> listInViewport(String listName, float fontSize, ListModel<E> listModel)
	{
		JList<E> list = new JList<E>(listModel);
		list.setFont(list.getFont().deriveFont(fontSize));
		list.getAccessibleContext().setAccessibleName(listName);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setMinimumSize(scrollPane.getPreferredSize());
		place(scrollPane);
		return list;
	}

	public static <E> JList<E> listInViewport(String listName, JList<E> list)
	{
		list.getAccessibleContext().setAccessibleName(listName);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setMinimumSize(scrollPane.getPreferredSize());
		place(scrollPane);
		return list;
	}

	public static <E> JList<E> spanningListInViewport(String listName, float fontSize, ListModel<E> listModel, int numCells)
	{
		JList<E> list = new JList<E>(listModel);
		list.setFont(list.getFont().deriveFont(fontSize));
		list.getAccessibleContext().setAccessibleName(listName);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setMinimumSize(scrollPane.getPreferredSize());
		place(scrollPane, numCells);
		return list;
	}

	public static <E> JList<E> list(String listName, float fontSize, ListModel<E> listModel)
	{
		JList<E> list = new JList<E>(listModel);
		list.setFont(list.getFont().deriveFont(fontSize));
		list.getAccessibleContext().setAccessibleName(listName);
		return place(list);
	}

	public static <E> Box spacer(String boxName)
	{
		Box box = new Box(BoxLayout.PAGE_AXIS);
		box.getAccessibleContext().setAccessibleName(boxName);
		return place(box);
	}

	public static <E> JPanel innerGroupBox(String boxName, String boxText)
	{
		JPanel groupbox = new JPanel();
		groupbox.getAccessibleContext().setAccessibleName(boxName);
		groupbox.setBorder(BorderFactory.createTitledBorder(boxText));
		return place(groupbox);
	}

	public static <E> Component filler(int preferredMinimumX, int preferredMinimumY)
	{
		Dimension fillerDim = new Dimension(preferredMinimumX, preferredMinimumY);
		Box.Filler bf = new Box.Filler(fillerDim, fillerDim, fillerDim);
		return place(bf);
	}


	public static void topAnchor(){addDefaults.anchor = GridBagConstraints.NORTH;}
	public static void bottomAnchor(){addDefaults.anchor = GridBagConstraints.SOUTH;}
	public static void leftAnchor(){addDefaults.anchor = GridBagConstraints.WEST;}
	public static void rightAnchor(){addDefaults.anchor = GridBagConstraints.EAST;}
	public static void centerAnchor(){addDefaults.anchor = GridBagConstraints.CENTER;}
	public static void bothFill() {addDefaults.fill = GridBagConstraints.BOTH; }
	public static void horzFill() {addDefaults.fill = GridBagConstraints.HORIZONTAL;}
	public static void vertFill() {addDefaults.fill = GridBagConstraints.VERTICAL;}
	public static void noFill() {addDefaults.fill = GridBagConstraints.NONE;}
}
