package gov.nasa.jpl.magicdraw.projectUsageIntegrity.ui;

import java.awt.Component;
import java.awt.TextArea;

import com.nomagic.magicdraw.ui.browser.WindowComponentContent;

/**
 * @author Nicolas F Rouquette
 * @author Paulius Grigaliunas, 11/22/13
 * @see https://support.nomagic.com/browse/MDUMLCS-11835
 */
public class LogFrame implements WindowComponentContent {

	private TextArea mTextArea = new TextArea();

	@Override
	public Component getWindowComponent()
	{
		return mTextArea;
	}

	@Override
	public Component getDefaultFocusComponent()
	{
		return mTextArea;
	}
	
	public void clear() {
		mTextArea.setText("");
	}
	
	public void append(String text) {
		mTextArea.append(text);
	}
}
