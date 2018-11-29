package edu.unl.cse.efs.view.ft;

public class InvalidWidgetException extends Exception 
{
	public enum Attribute{
		EVENT_ID("event ID"), NAME("name"), 
		TYPE("type"), WINDOW("window"), ACTION("action");
		
		public final String nameString;
		Attribute(String nameString)
		{
			this.nameString = nameString;
		}
	}
	public final Attribute cause;
	public InvalidWidgetException(Attribute att)
	{
		this.cause = att;
	}
}