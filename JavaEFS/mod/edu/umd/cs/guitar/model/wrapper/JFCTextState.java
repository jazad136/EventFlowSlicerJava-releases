package edu.umd.cs.guitar.model.wrapper;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;

import edu.umd.cs.guitar.model.data.PropertyType;

public class JFCTextState {
	
	public String 
		caret_position,
		character_bounds,
		character,
		character_state,
		character_handle,
		selection_text,
		selection_end;
	
	public JFCTextState()
	{
		caret_position =
		character_bounds =
		character = 
		character_state =
		character_handle = 
		selection_text = 
		selection_end = "";
	}
	
	public static List<PropertyType> handleAndStateProperties(String handle, String value)
	{
		LinkedList<PropertyType> toReturn = new LinkedList<>();
		PropertyType handleProperty = new PropertyType();
		handleProperty.setName("Character Handle");
		handleProperty.getValue().add(handle);
		PropertyType stateProperty = new PropertyType();
		stateProperty.setName("Character State");
		stateProperty.getValue().add(value);
		toReturn.add(handleProperty);
		toReturn.add(stateProperty);
		return toReturn;
	}
	
	public static List<PropertyType> fontFamilyState(AttributeSet attributes)
	{
		String handle = StyleConstants.FontFamily.toString();
		String fontFamily = StyleConstants.getFontFamily(attributes);
		return handleAndStateProperties(handle, fontFamily);
	}
	
	public static List<PropertyType> fontSizeState(AttributeSet attributes)
	{
		String handle = StyleConstants.Size.toString();
		String size = String.valueOf(StyleConstants.getFontSize(attributes));
		return handleAndStateProperties(handle, size);
	}
	
	public static List<PropertyType> italicUnderlineBoldState(AttributeSet attributes)
	{
		LinkedList<PropertyType> iubStates = new LinkedList<>();
		iubStates.addAll(italicState(attributes));
		iubStates.addAll(underlineState(attributes));
		iubStates.addAll(boldState(attributes));
		return iubStates;
	}
	
	public static List<PropertyType> italicState(AttributeSet attributes)
	{
		String handle = StyleConstants.Italic.toString();
		String italicOn = String.valueOf(StyleConstants.isItalic(attributes));
		return handleAndStateProperties(handle, italicOn);
	}
	
	public static List<PropertyType> underlineState(AttributeSet attributes)
	{
		String handle = StyleConstants.Underline.toString();
		String underlineOn = String.valueOf(StyleConstants.isUnderline(attributes));
		return handleAndStateProperties(handle, underlineOn);
	}
	
	public static List<PropertyType> boldState(AttributeSet attributes)
	{
		String handle = StyleConstants.Underline.toString();
		String boldOn = String.valueOf(StyleConstants.isBold(attributes));
		return handleAndStateProperties(handle, boldOn);
	}
	
	public static List<PropertyType> alignmentState(AttributeSet attributes)
	{
		String handle = StyleConstants.Alignment.toString();
		String alignment;
		switch(StyleConstants.getAlignment(attributes)) {
			case StyleConstants.ALIGN_RIGHT		: alignment = "align right"; break;
			case StyleConstants.ALIGN_CENTER	: alignment = "align center"; break;
			case StyleConstants.ALIGN_JUSTIFIED : alignment = "align justified"; break;
			case StyleConstants.ALIGN_LEFT		:
			default								: alignment = "align left"; break; 
		}
		return handleAndStateProperties(handle, alignment);
	}
	
	public static List<PropertyType> foregroundColorState(AttributeSet attributes)
	{
		String handle = StyleConstants.Foreground.toString();
		Color foreColor = StyleConstants.getForeground(attributes);
		String colorString = 
				Integer.toHexString(foreColor.getRed()) + "-" +
				Integer.toHexString(foreColor.getGreen()) + "-" +
				Integer.toHexString(foreColor.getBlue());
		return handleAndStateProperties(handle, colorString);
	}
	
	public static List<PropertyType> backgroundColorState(AttributeSet attributes)
	{
		String handle = StyleConstants.Background.toString();
		Color backColor = StyleConstants.getBackground(attributes);
		String colorString = 
				Integer.toHexString(backColor.getRed()) + "-" +
				Integer.toHexString(backColor.getGreen()) + "-" +
				Integer.toHexString(backColor.getBlue());
		return handleAndStateProperties(handle, colorString);
	}
}
