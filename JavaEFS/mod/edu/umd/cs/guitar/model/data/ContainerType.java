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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;


/**
 * <p>Java class for ContainerType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ContainerType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{}ComponentType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Contents" type="{}ContentsType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ContainerType", propOrder = {
    "contents"
})
public class ContainerType extends ComponentType
{

    @XmlElement(name = "Contents", required = true)
    protected ContentsType contents;

    /**
     * Gets the value of the contents property.
     * 
     * @return
     *     possible object is
     *     {@link ContentsType }
     *     
     */
    public ContentsType getContents() {
        return contents;
    }

    /**
     * Sets the value of the contents property.
     * 
     * @param value
     *     allowed object is
     *     {@link ContentsType }
     *     
     */
    public void setContents(ContentsType value) {
        this.contents = value;
    }
    
    public String toString()
    {
    	String toReturn = super.toString();
    	if(contents != null) {
    		toReturn += "\nCONTENTS:" + (!contents.getWidgetOrContainer().isEmpty() ? " (non-empty)" : "");
    		
    		for(ComponentType child : contents.getWidgetOrContainer()) {
    			if(child != null) {
	    			ComponentTypeWrapper ctw = new ComponentTypeWrapper(this);
	        		String title = ctw.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);
	        		if(title != null && !title.isEmpty()) 
	        			toReturn += "\n" + title;
    			}
    		}
    	}
    	return toReturn;
    }

}
