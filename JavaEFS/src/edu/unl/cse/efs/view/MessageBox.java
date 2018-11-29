package edu.unl.cse.efs.view;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
public class MessageBox
{

	public static String loadPrefs1 = "epreferences.xml was not found in the working directory.";
	public static String loadPrefs2 = "Choose whether to load preferences from a different location or cancel.";
	public static void main(String[] args)
	{
		MessageBox mb = new MessageBox();
		mb.showAndWait(loadPrefs1, loadPrefs2, "Load Prefernces File");
	}
	public boolean clickedOK;
	public MessageBox()
	{
		clickedOK = false;
	}


	public void showAndWait(String message1, String message2, String title)
	{
		final JDialog stage = new JDialog();
		stage.setTitle(title);
		// stage.setminimumsize
		stage.setPreferredSize(Sizes.getMinimumSize());
		stage.setMinimumSize(Sizes.getMinimumSize());
		JLabel lbl1 = new JLabel(message1);
		JLabel lbl2 = new JLabel(message2);
		JButton btnOK = new JButton();
		JButton btnCancel = new JButton();
		btnOK.setText("OK");
		btnOK.addActionListener(new ActionListener() {public void actionPerformed(ActionEvent ae){close(true);}});
		JPanel pane = new JPanel();
		BoxLayout pLayout = new BoxLayout(pane, BoxLayout.PAGE_AXIS);
		pane.setLayout(pLayout);
		pane.add(Box.createRigidArea(new Dimension(0, 20)));
		pane.setBorder(new EmptyBorder(25, 20, 20, 20));
		lbl1.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		lbl2.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		BoxLayout bLayout = new BoxLayout(pane, BoxLayout.LINE_AXIS);
		JPanel bPane = new JPanel();

		btnOK.setAlignmentX(JComponent.CENTER_ALIGNMENT);

		pane.add(lbl1);
		pane.add(Box.createRigidArea(new Dimension(0, 20)));
		pane.add(lbl2);
		pane.add(Box.createRigidArea(new Dimension(0, 20)));
		pane.add(btnOK);

		stage.getContentPane().add(pane);
		stage.setVisible(true);
	}

	public void close(boolean ok)
	{
		clickedOK = ok;
	}
	/**
	 * Method to show a message box with one message.
	 */
	public static void show(String message, String title)
	{
		JDialog stage = new JDialog();

		stage.setTitle(title);
		stage.setPreferredSize(Sizes.getMinimumSize());

//		Label lbl = new Label();
//		lbl.setText(message);
//
//		Button btnOK = new Button();
//		btnOK.setText("OK");
//		btnOK.setOnAction(e -> stage.close());
//
//		VBox pane = new VBox(20);
//		pane.getChildren().addAll(lbl, btnOK);
//		pane.setAlignment(Pos.CENTER);
//
//		Scene scene = new Scene(pane);
//		stage.setScene(scene);
//		stage.showAndWait();
	}
	public static class Sizes
	{
		static int minSizeWidth = 550;
		static int minSizeHeight = 275;
		public static Dimension getMinimumSize(){ return new Dimension(minSizeWidth, minSizeHeight);}
	}
}
