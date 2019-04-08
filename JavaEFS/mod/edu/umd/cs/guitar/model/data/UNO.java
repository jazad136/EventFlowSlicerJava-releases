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


/**
 * <p>Java class for Preferences complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Preferences">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Port" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="Family" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="Linux" type="{}Linux"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UNO", propOrder = {
    "port",
    "family", 
    "linux"
})

public class UNO {

    @XmlElement(name = "Port", required = true)
    protected int port;
    
    @XmlElement(name = "Family", required = true)
    protected String family;    
    
    @XmlElement(name = "Linux")
    protected Linux linux;  

    /**
     * Gets the value of the port property.
     *  
     */
    public int getPort() {
        return this.port; 
    }
    
    /**
     * Sets the value of the property.
     *  
     */
    public void setPort(int portNum) {
        this.port = portNum;  
    }
    
    /**
     * Gets the value of the family property.
     *  
     */
    public String getFamily() {
        return this.family; 
    }
    
    /**
     * Sets the value of the family property.
     *  
     */
    public void setFamily(String fam){
    	this.family = fam;
    }
    
    /**
     * Gets the value of the linux property.
     *  
     */
    public Linux getLinux() {
        return this.linux; 
    }
    
    /**
     * Sets the value of the linux property.
     *  
     */
    public void setLinux(Linux lin) {
       this.linux = lin;  
    }


}
