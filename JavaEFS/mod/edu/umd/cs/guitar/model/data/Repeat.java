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


package edu.umd.cs.guitar.model.data;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for Repeat complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Repeat"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Widget" type="{}Widget" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complex&gt;ontent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Repeat", propOrder = {
    "widget"
})
public class Repeat {

	
    @XmlElement(name = "Widget", required = true)
    protected List<Widget> widget;
    
    @XmlAttribute(name="minBound")
    protected String minBound;
    
    @XmlAttribute(name="maxBound")
    protected String maxBound;
    
    public static final String UNBOUNDED_SETTING = "unbounded";
    public static final String INVALID_SETTING = "invalid";
    /**
     * Gets the value of the widget property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the widget property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWidget().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Widget }
     * 
     * 
     */
    public List<Widget> getWidget() {
        if (widget == null) {
            widget = new ArrayList<Widget>();
        }
        return this.widget;
    }
    
    /**
     * Return a string representation of this Repeat.
     */
    public String toString()
    {
    	if(widget == null)
    		return "";
    	
    	String toReturn = "";
    	for(Widget w : widget) 
    		toReturn += w + "\n";
    	
    	return toReturn;
    }
    
    /**
     * Sets the minBound to newBound.
     * This method will only change the minBound to the desired value, 
     * if it is in the required range (>= 0)  
     * or equivalent to the string "unbounded" ignoring case (changed to "").
     * If outside the range, the bound is set to INVALID_SETTING. 
     */
    public void setMinBound(String newBound)
    {
    	minBound = newBound;
    	checkMinBound();
    }
    
    public String checkMinBound()
    {
    	if(minBound == null || minBound.isEmpty())
    		minBound = "";
    	else if(minBound.equalsIgnoreCase("unbounded")) 
			minBound = "";
		else 
			try {
				int bNum = Integer.parseInt(minBound);
				if(bNum < 0) 
					throw new NumberFormatException();
			}
			catch (NumberFormatException e) {
				minBound = INVALID_SETTING;
			}
    	return minBound;
    }
    
    /**
     * Sets the maxBound to newBound.
     * This method will only change the maxBound to the desired value, 
     * if it is in the required range (> 0)  
     * or equivalent to the string "unbounded" ignoring case (changed to "").
     * If outside the range, the bound is set to INVALID_SETTING. 
     */
    public void setMaxBound(String newBound)
    {
    	maxBound = newBound;
    	checkMaxBound();
    }
    public String checkMaxBound()
    {
    	if(maxBound == null || maxBound.isEmpty())
    		maxBound = "";
    	else if(maxBound.equalsIgnoreCase("unbounded")) 
			maxBound = "";
		else 
			try {
				int bNum = Integer.parseInt(maxBound);
				if(bNum < 0) 
					throw new NumberFormatException();
			}
			catch (NumberFormatException e) {
				maxBound = INVALID_SETTING;
			}
    	return maxBound;
    }
    
    public String getMinBound()
    {
    	if(minBound.isEmpty())
    		return UNBOUNDED_SETTING;
    	return minBound;
    }
    public String getMaxBound()
    {
    	if(maxBound.isEmpty())
    		return UNBOUNDED_SETTING;
    	return maxBound;
    }
    
    /**
     * Acts specifically on whether the minBound is unbounded or not.
     * If minBound is unbounded, return the integer passed in, if it is set to an integer,
     * return the integer value. 
     */
    public int testAndReturnMinBound(int altParam)
    {
    	minBound = checkMinBound();
    	if(minBound.isEmpty())
    		return 0; // default for min is 0 if only max is specified. 
    	else if(minBound.equals(INVALID_SETTING))
    		return altParam;
    	else
    		return Integer.parseInt(minBound);
    }
    /**
     * Acts specifically on whether the minBound is unbounded or not.
     * If minBound is unbounded, return the integer passed in, if it is set to an integer,
     * return the integer value. 
     */
    public int testAndReturnMaxBound(int altParam)
    {
    	maxBound = checkMaxBound();
    	if(maxBound.isEmpty()) 
    		return Integer.MAX_VALUE; // default for max is unbounded if only max is specified 
    	else if(maxBound.equals(INVALID_SETTING))
    		return altParam;
    	else
    		return Integer.parseInt(maxBound);
    }
}
