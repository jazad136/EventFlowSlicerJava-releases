package edu.unl.cse.efs.java;

import java.util.ArrayList;
import java.util.List;

public class StepTypeHandler {
    protected String window;
    protected String componentName; 
    protected String action; 
    protected int componentRole; 
    protected String parentName; 
    protected List<String> parameters;
    protected int parentRole; 
    protected int x; 
    protected int y; 
    protected int width; 
    protected int height; 

    public StepTypeHandler(){
    	window = ""; 
    	componentName = ""; 
    	componentRole = 0; 
    	parentName = ""; 
    	parentRole = 0; 
    	x = 0; 
    	y = 0; 
    	width = 0; 
    	height = 0; 
    	parameters = new ArrayList<String>(); 
    	
    }
    
    public void setX(int xC){
    	x = xC;
    }
    
    public int getX(){
    	return x;
    }
    
    public void setY(int yC){
    	y = yC; 
    }
    
    public int getY(){
    	return y; 
    }
    
    public void setWidth(int w){
    	width = w;
    }
    
    public int getWidth(){
    	return width;
    }
    
    public void setHeight(int h){
    	height = h; 
    }
    
    public int getHeight(){
    	return height;
    }
    
    
    public int getRole(){
    	return componentRole;
    }
    
    public void setRole(int r){
    	componentRole = r;
    }
    
    public String getParentName(){
    	return parentName;
    }
    
    public void setParentName(String nm){
    	parentName = nm; 
    }
    
    public int getParentRole(){
    	return parentRole;
    }
    
    public void setParentRole(int pRole){
    	parentRole = pRole;
    }
    
    /**
     * Gets the value of the window property.    
     */
    public String getWindow() {
        return window;
    }

    /**
     * Sets the value of the window property.  
     */
    public void setWindow(String value) {
        this.window = value;
    }

    /**
     * Gets the value of the component property.   
     */
    public String getComponent() {
        return componentName;
    }

    /**
     * Sets the value of the component property.   
     */
    public void setComponent(String value) {
        this.componentName = value;
    }

    /**
     * Sets the value of the parameters property
     */
    public void setParameters(List<String> params){
    	this.parameters = params; 
    }
    
    /**
     * Gets the value of the parameters property
     */
    public List<String> getParameters(){
    	if(this.parameters == null)
    		this.parameters = new ArrayList<String>();
    	return this.parameters; 
    }

    /**
     * Sets the value of the action property
     * @param handler
     */
	public void setAction(String handler) {
		this.action = handler;
	}
	
	/**
	 * Gets the value of the action property
	 */
	public String getAction(){
		return this.action; 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((window == null) ? 0 : window.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StepTypeHandler other = (StepTypeHandler) obj;
		if (action == null) {
			if (other.action != null)
				return false;
		} else if (!action.equals(other.action))
			return false;
		if (window == null) {
			if (other.window != null)
				return false;
		} else if (!window.equals(other.window))
			return false;
		return true;
	}

    
}

