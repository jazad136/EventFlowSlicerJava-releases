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
package edu.umd.cs.guitar.event;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleEditableText;
import javax.accessibility.AccessibleRole;

import edu.umd.cs.guitar.exception.EventPerformException;
import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCXComponent;

/**
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 *
 * @author Jonathan Saddler
 */
public class JFCEditableTextHandler extends JFCEventHandler {

	public static long giveAnAdditionalTime = 500;
	public static long keyPressTime = 60;
	public static String textInsertionCommand = "TextInsert";
	public static String textReplacementCommand = "TextInsert";
	/**
     *
     */
	public JFCEditableTextHandler() {
	}

	/**
     *
     */
	private static String GUITAR_DEFAULT_TEXT = "GUITAR DEFAULT TEXT: "
			+ "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
	// ";

	@Override
	public void performImpl(GObject gComponent,Hashtable<String, List<String>> optionalData) {

		List<String> args = new ArrayList<String>();
		args.add(GUITAR_DEFAULT_TEXT);
		performImpl(gComponent, args,optionalData);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void performImpl(GObject gComponent, Object parameters, Hashtable<String, List<String>> optionalData) {

		if (gComponent == null) {
			System.err.println("JFCEditableTextHandler: PerformImpl was passed a null component.");
			return;
		}

		if (parameters instanceof List) {
			 // set the text to be edited.
			List<String> lParameter = (List<String>) parameters;
			String sInputText;
			final String[][] nParam = new String[lParameter.size()][];
			if(lParameter.isEmpty())
				sInputText = GUITAR_DEFAULT_TEXT; // retain original default behavior.
			else {
				for(int i = 0; i < lParameter.size(); i++) {
					String param = lParameter.get(i);
					String[] pComponents = param.split(GUITARConstants.NAME_SEPARATOR);
					if(pComponents.length < 2) {
						System.err.println("Text Step not supported: Invalid Parameter Set. Continuing.");
						return;
					}
					nParam[i] = pComponents;
				}
			}
			final Component component = getComponent(gComponent);
			long sleepTime = eventPerformWaitTime;
			final AccessibleContext aContext = component.getAccessibleContext();

			for(String[] set : nParam) {
				final String command = set[0];
				final String finalSInput = set[1];

				if(command.equals("Cursor"))
					continue;
				 // check for compatibility
				try {// perform the task or bail
					//Step 1: grab focus.
					javax.swing.SwingUtilities.invokeAndWait(new Runnable() {public void run() {
						component.requestFocus();
					}});

					// Step 2: wait some time.
					setWaitTime(eventPerformWaitTime + 500);
					headThread.interrupt();
					Thread.interrupted();
					Thread.sleep(sleepTime); // wait some time.

					// Step 3: then continue to insert the text
					if(command.equals("Command")) {
						String delimString = GUITARConstants.CMD_ARGUMENT_SEPARATOR;
						delimString += "[]";
						StringTokenizer st = new StringTokenizer(finalSInput, delimString);

						Component source = component;
						int idReleased = KeyEvent.KEY_RELEASED;
						int idPressed = KeyEvent.KEY_PRESSED;
						int idTyped = KeyEvent.KEY_TYPED;

						int modifiers = 0;
						while(st.hasMoreTokens()) {
							String nextI = st.nextToken();
							long when = System.currentTimeMillis(); // get the now time;

							switch(nextI.toUpperCase()) {
								case "ENTER": case "SPACE": {
									int llKeyCode = KeyEvent.VK_UNDEFINED; // default value.
									char tyKeyChar = KeyEvent.CHAR_UNDEFINED; // default value.
									switch(nextI.toUpperCase()) {
									case "ENTER" : llKeyCode = KeyEvent.VK_ENTER; tyKeyChar = '\n'; break;
									case "SPACE" : llKeyCode = KeyEvent.VK_SPACE; tyKeyChar = ' '; break;
									}
									int tyKeyCode = KeyEvent.VK_UNDEFINED;
									char llKeyChar = KeyEvent.CHAR_UNDEFINED;

									final KeyEvent myKeyEventP = new KeyEvent(source, idPressed, when, modifiers, llKeyCode, llKeyChar);
									final KeyEvent myKeyEventT = new KeyEvent(source, idTyped, when, modifiers, tyKeyCode, tyKeyChar);
									final KeyEvent myKeyEventR = new KeyEvent(source, idReleased, when+60, modifiers, llKeyCode, llKeyChar);

									javax.swing.SwingUtilities.invokeLater(new Runnable() {public void run() {
										Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(myKeyEventP);
										Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(myKeyEventT);
										Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(myKeyEventR);
									}});
								}
								case "RIGHT": case "LEFT": case "UP": case "DOWN":
									int llKeyCode = KeyEvent.VK_UNDEFINED;
									switch(nextI.toUpperCase()) {
									case "RIGHT": llKeyCode = KeyEvent.VK_RIGHT; break;
									case "LEFT":  llKeyCode = KeyEvent.VK_LEFT; break;
									case "UP":    llKeyCode = KeyEvent.VK_UP; break;
									case "DOWN":  llKeyCode = KeyEvent.VK_DOWN; break;
									}
									char llKeyChar = KeyEvent.CHAR_UNDEFINED;
									int location = KeyEvent.KEY_LOCATION_STANDARD;

									final KeyEvent myKeyEventP = new KeyEvent(source, idPressed, when, modifiers, llKeyCode, llKeyChar, location);
									final KeyEvent myKeyEventR = new KeyEvent(source, idReleased, when+keyPressTime, modifiers, llKeyCode, llKeyChar, location);
									javax.swing.SwingUtilities.invokeLater(new Runnable() {public void run() {
										Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(myKeyEventP);
										Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(myKeyEventR);
									}}); break;
								default: // unsupported keystroke.
							}
							headThread.interrupt();
							Thread.interrupted();
							Thread.sleep(sleepTime); // wait some time.
						}
					}
					else if(command.equals(textInsertionCommand) || command.equals(textReplacementCommand)) {
						final AccessibleEditableText aTextEvent = aContext.getAccessibleEditableText();
						if(aTextEvent == null) { // type the keys "manually" without Accessibility API.
							Component source = component;
							int idTyped = KeyEvent.KEY_TYPED;
							int modifiers = 0;
							int keyCode = KeyEvent.VK_UNDEFINED;
							for(int i = 0; i < finalSInput.length(); i++) {
								long when = System.currentTimeMillis();
								final KeyEvent myKeyEvent = new KeyEvent(source, idTyped, when, modifiers, keyCode, finalSInput.charAt(i));
								javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() {
									Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(myKeyEvent);
								}});
								Thread.sleep(keyPressTime);
							}
						}
						else { // use accessibility api.
							javax.swing.SwingUtilities.invokeLater(new Runnable() {public void run() {
//								String currentText = aTextEvent.getTextRange(0, aTextEvent.getCharCount());
//								if(currentText == null || command.equals("TextReplace"))
									aTextEvent.setTextContents(finalSInput);
//								else
//									aTextEvent.setTextContents(currentText + finalSInput);
								aTextEvent.selectText(aTextEvent.getCharCount(), aTextEvent.getCharCount()); // replace the cursor
							}});
						}
					} // end text insert clause
				}// end try.
				catch (	SecurityException | IllegalArgumentException | InvocationTargetException | InterruptedException e) {
					// this thread should never be interrupted.
					System.err.println("Error caused by " + e.getClass().getName() + ": " + e.getLocalizedMessage());
					throw new EventPerformException();
				}
				finally {
					resetWaitTime();
				}
			} // end for loop.
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.guitar.event.GEvent#isSupportedBy(edu.umd.cs.guitar.model.GComponent)
	 */
	@Override
	public boolean isSupportedBy(GObject gComponent) {
		if (!(gComponent instanceof JFCXComponent))
			return false;
		JFCXComponent jComponent = (JFCXComponent) gComponent;
		Component component = jComponent.getComponent();
		AccessibleContext aContext = component.getAccessibleContext();

		if (aContext == null)
			return false;

		Object event;

		// Text
		event = aContext.getAccessibleEditableText();
		if (event != null) {
			return true;
		}
		else if(aContext.getAccessibleRole().equals(AccessibleRole.PANEL)
		&& JFCXComponent.hasListeners(component, "textbox")
		&& JFCXComponent.hasListeners(component, "button"))
		{
			return true; // if we're a panel with both listeners, we're a typing panel.
		}
		return false;
	}
}
