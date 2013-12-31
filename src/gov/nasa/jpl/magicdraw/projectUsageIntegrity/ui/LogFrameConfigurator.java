package gov.nasa.jpl.magicdraw.projectUsageIntegrity.ui;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.swing.SwingUtilities;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.actions.ActionsGroups;
import com.nomagic.magicdraw.actions.ActionsID;
import com.nomagic.magicdraw.actions.MDActionsCategory;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.task.BackgroundTaskManager;
import com.nomagic.magicdraw.ui.ProjectWindow;
import com.nomagic.magicdraw.ui.ProjectWindowListener;
import com.nomagic.magicdraw.ui.ProjectWindowsManager;
import com.nomagic.magicdraw.ui.WindowComponentInfo;
import com.nomagic.magicdraw.ui.WindowsManager;
import com.nomagic.magicdraw.utils.MDLog;
import com.nomagic.task.Task;
import com.nomagic.task.TaskConfig;

public class LogFrameConfigurator implements AMConfigurator {

	WindowComponentInfo info = new WindowComponentInfo("SECAE Log Window", "SECAE log window", null, WindowsManager.SIDE_SOUTH, WindowsManager.STATE_DOCKED, true);
	ProjectWindow window = null;

	protected LogFrame logFrame;

	public LogFrameConfigurator(LogFrame logFrame) {
		this.logFrame = logFrame;

	}
	public LogFrame getLogFrame() {
		return logFrame;
	}

	public void showLogFrame() {
		ProjectWindowsManager projectWindowsManager = Application.getInstance().getMainFrame().getProjectWindowsManager();
		if (window == null)
		{
			// register new log window
			window = new ProjectWindow(info, logFrame);
			window.addProjectWindowListener(new ProjectWindowListener()
			{
				@Override
				public void closed(ProjectWindow window)
				{
					// clear log info from text area
					((TextArea)window.getContent().getWindowComponent()).setText("");
				}
			});
		}
		projectWindowsManager.addWindow(window);
	}
	
	@Override
	public int getPriority() {
		return AMConfigurator.MEDIUM_PRIORITY;
	}

	public static String CATEGORY = "SECAE LOG FRAME";
	public static String NAME = "SECAE Log Frame";

	@Override
	public void configure(ActionsManager mngr) {

		ActionsCategory category = mngr.getCategory(ActionsID.WINDOW); 
		if (category == null) {
			category = mngr.getCategory(CATEGORY);
			if (category == null) {
				MDLog.getPluginsLog().info(String.format("LogFrameConfig: Creating category '%s'", CATEGORY));
				category = new MDActionsCategory(CATEGORY, NAME);
				mngr.addCategory(category);
			} else {
				MDLog.getPluginsLog().info(String.format("LogFrameConfig: Found category '%s'", CATEGORY));
			}
		} else {			
			MDLog.getPluginsLog().info(String.format("LogFrameConfig: Found category '%s'", ActionsID.WINDOW));
		}

		category.addAction(new NMAction("CREATE_SECAE_LOG_WINDOW", "Create SECAE log window", null, ActionsGroups.PROJECT_OPENED_RELATED)
		{
			@Override
			public void actionPerformed(@CheckForNull ActionEvent e)
			{
				showLogFrame();
			}
		});


		category.addAction(new NMAction("CLEAR_SECAE_LOG", "Clear SECAE Log window", null, ActionsGroups.PROJECT_OPENED_RELATED)
		{
			@Override
			public void updateState() {
				setEnabled(window != null);
			}

			@Override
			public void actionPerformed(@CheckForNull ActionEvent e)
			{
				if (window != null)
				{
					ProjectWindowsManager projectWindowsManager = Application.getInstance().getMainFrame().getProjectWindowsManager();
					projectWindowsManager.activateWindow(info.getId());

					Task task = new Task("Log info")
					{
						@Override
						public void execute() throws Exception
						{
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									logFrame.clear();
								};
							});

						}
					};

					BackgroundTaskManager.getInstance().showProgress(task, TaskConfig.CANCEL_NEW_THREAD);
				}
			}
		});

	}

}
