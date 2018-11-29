package edu.unl.cse.efs.replay;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

/**
 * This class provides methods for capturing an image of the current screen
 * jsaddle - removed references to com.sun.star packages. 
 * @author Amanda Swearngin
 * @author Jonathan Saddler
 * @version 1.0
 *
 */

public class CaptureScreenImage {
    /**
     * Capture a screenshot the size of the current screen (full-screen) and write it to a given file
     * 
     * @param String fileName
     * 		The name of the file for the image
     * @throws IOException
     * @throws FileNotFoundException
     */
	public void captureScreen(String fileName)  {
		//capture the image
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize(); 
		Rectangle screenRectangle = new Rectangle(dim);
		Robot robot;
		try {
			robot = new Robot();
			BufferedImage image = robot.createScreenCapture(screenRectangle);
			ImageIO.write(image, "jpg", new File(fileName));	
		} catch (AWTException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
  	   
		}
	
    /**
     * Capture a screenshot of a given origin and size and write it to a given file
     * 
     * @param String fileName
     * 		The name of the file for the image
     * @param Rectangle myRectangle
     * 		The Rectangle on the screen to be captured
     * @throws IOException
     * @throws FileNotFoundException
     */
	public void captureScreen(String fileName, Rectangle rect)  {
		//capture the image
		Robot robot;
		try {
			robot = new Robot();
			BufferedImage image = robot.createScreenCapture(rect);
			ImageIO.write(image, "jpg", new File(fileName));	
		} catch (AWTException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
  	   
		}
	
    /**
     * Find the smallest rectangle enclosing all windows
     * @return The smallest rectangle that will contain all open windows 
     */
    public Rectangle getRectangleSize(){
    	//obtain coordinates/widths/heights
    	Window[] windows = Window.getWindows();
    	ArrayList<Integer> xCoordinates = new ArrayList<Integer>();
    	ArrayList<Integer> yCoordinates = new ArrayList<Integer>();
    	ArrayList<Integer> widths = new ArrayList<Integer>();
    	ArrayList<Integer> heights = new ArrayList<Integer>();
    	for (int i=0; i<windows.length; i++) {   
    		if((!windows[i].getName().equals("backgroundFrame"))&&windows[i].isVisible()){
    			xCoordinates.add(windows[i].getX());
    			yCoordinates.add(windows[i].getY());
    			widths.add(windows[i].getWidth());
    			heights.add(windows[i].getHeight());
    		}
        }
    	
    	//compute the rectangle
    	int x = -1; 
    	int y = -1; 
    	int right = -1; 
    	int bottom = -1; 
    	
    	//find lowest x coordinate 
    	for(int j=0; j<xCoordinates.size(); j++){
    		//furthest left
    		if((x<0)||(xCoordinates.get(j)<=x)){
    				x = xCoordinates.get(j);
    		}
    		//furthest right
    		int curRight = xCoordinates.get(j) + widths.get(j);
    		if((right<0)||curRight>=right){
    			right = curRight;
    		}
    		
    	}
    	
    	//find lowest y coordinate
    	for(int k=0; k<yCoordinates.size(); k++){
    		
    		//closest to top
    		if((y<0)||(yCoordinates.get(k)<=y)){
    			y = yCoordinates.get(k);
    		}
    		
    		//closest to bottom
    		int curBottom = yCoordinates.get(k) + heights.get(k);
    		if((bottom<0)||curBottom>=bottom){
    			bottom = curBottom;
    		}
    	}

    	//add extra for spacing
    	Dimension dim = Toolkit.getDefaultToolkit().getScreenSize(); 
    	if(x<15){
    		x = 0;
    	}
    	else{
    		x = x - 15;
    	}
    	
    	if(y<15){
    		y = 0; 
    	}
    	else{
    		y = y - 15;
    	}
 
    	if(bottom<(dim.getHeight()-15)){
    		bottom = bottom + 15;
    	}
    	else{
    		bottom = (int)dim.getHeight();
    	}
    	if(right<(dim.getWidth()-15)){
    		right = right + 15;
    	}
    	else{
    		right = (int)dim.getWidth();
    	}
    	
    	
    	int width  = right - x; 
    	int height = bottom - y;
		return new Rectangle(x,y,width,height);
    }
	
	
}


