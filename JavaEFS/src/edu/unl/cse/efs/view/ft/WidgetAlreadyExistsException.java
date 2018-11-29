package edu.unl.cse.efs.view.ft;

public class WidgetAlreadyExistsException extends Exception
{
	public final String widgetName;
	public WidgetAlreadyExistsException(String widgetName)
	{
		this.widgetName = widgetName;
	}
}