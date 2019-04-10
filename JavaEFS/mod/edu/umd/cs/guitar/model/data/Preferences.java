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
 *     Amanda Swearngin - initial API and implementation
 *     Jonathan A. Saddler - modifications
 *******************************************************************************/


package edu.umd.cs.guitar.model.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Preferences complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Preferences"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="Application" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="Results" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="GUI" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="ImageFormat" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="UNO" type="{}UNO"/&gt;
 *         &lt;element name="CogTool" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="DoPredictions" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="Arguments" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Preferences", propOrder = {
    "application", 
    "arguments",
    "argumentsVM",
    "runtimeClass",
    "results", 
    "gui", 
    "imageformat", 
    "uno",
    "cogtoolLocation",
    "doPredictions"
})

@XmlRootElement(name = "Preferences")
public class Preferences {

    @XmlElement(name = "Application", required = true)
    protected String application;
    
    @XmlElement(name = "Arguments", required = false)
    protected String arguments;
    
    @XmlElement(name = "ArgumentsVM", required = false)
    protected String argumentsVM;
    
    @XmlElement(name = "RuntimeClass", required = false)
    protected String runtimeClass;
    
    @XmlElement(name = "Results", required = true)
    protected String results;
    
    @XmlElement(name = "GUI", required = true)
    protected String gui;
    
    @XmlElement(name = "ImageFormat", required = false)
    protected String imageformat;
    
    @XmlElement(name = "UNO", required = false)
    protected UNO uno;
    
    @XmlElement(name = "CogTool", required = false)
    protected String cogtoolLocation;
    
    @XmlElement(name = "DoPredictions", required = false)
    protected String doPredictions;
    
    
    /**
     * Gets the value of the doPredictions boolean.
     * @return
     */
    public String getDoPredictionsValue()
    {
    	return this.doPredictions;
    }
    
    /**
     * Sets the value of the doPredictions property
     * @param doPredictionsOn
     */
    public void setDoPredictions(String newDoPredictions)
    {
    	this.doPredictions = newDoPredictions;
    }
    /**
     * Gets the value of the cogToolLocation property
     * @return
     */
    public String getCogTool()
    {
    	return this.cogtoolLocation;
    }
    
    /**
     * Sets the value of the cogtoolLocation property
     */
    public void setCogTool(String ctl)
    {
    	this.cogtoolLocation = ctl;
    }
    
    /**
     * Gets the value of the arguments property
     * @return
     */
    public String getAppArguments()
    {
    	return this.arguments;
    }
    
    /**
     * Sets the value of the arguments property
     */
    public void setAppArguments(String args)
    {
    	this.arguments = args;
    }
    
    /**
     * Gets the value of the argumentsVM property
     * @return
     */
    public String getVMArguments()
    {
    	return this.argumentsVM;
    }
    
    /**
     * Sets the value of the argumentsVM property
     */
    public void setVMArguments(String args)
    {
    	this.argumentsVM = args;
    }
    
    /**
     * Gets the value of the runtime class property
     * @return
     */
    public String getRuntimeClass()
    {
    	return this.runtimeClass;
    }
    
    /**
     * Sets the value of the runtime class property
     * @param className
     */
    public void setRuntimeClass(String className)
    {
    	this.runtimeClass = className;
    }
    /**
     * Gets the value of the application property.
     *  
     * Method is used to
     * run a check against the AUT's system path for validity by the ViewController
     *  
     */
    public String getApplication() {
        return this.application;
    }
    
    /**
     * Sets the value of the application property.
     * @param app
     */
	public void setApplication(String app) {
		this.application = app; 
	}
    
    /**
     * Gets the value of the application property.
     * 
     */
    public String getResults() {
        return this.results;
    }
    
    /**
     * Sets the value of the results property.
     * @param resultsFolder
     */
	public void setResults(String resultsFolder) {
		this.results = resultsFolder;
	}
    
    /**
     * Gets the value of the GUI property.
     * 
     */
    public String getGUI() {
    	return this.gui;
    }
    
    /**
     * Sets the value of the GUI property.
     * @param guiFile
     */
	public void setGuiFile(String guiFile) {
		this.gui = guiFile; 
	}
    
    /**
     * Gets the value of the ImageFormat property.
     * 
     */
    public String getImageFormat() {
        return this.imageformat;
    }
    
    /**
     * Sets the value of the ImageFormat property.
     * @param imageData
     */
	public void setImageFormat(String imageData) {
		this.imageformat = imageData; 
	}
    
    /**
     * Gets the value of the application property.
     * 
     */
    public UNO getUNO() {
        return this.uno;
    }

    /**
     * Sets the value of the UNO property
     * @param unoPrefs
     */
	public void setUNO(UNO unoProp) {
		this.uno = unoProp; 
	}

}
