package gov.nasa.jpl.magicdraw.projectUsageIntegrity.options;

import gov.nasa.jpl.magicdraw.projectUsageIntegrity.options.resources.SSCAEProjectUsageIntegrityOptionsResources;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.nomagic.magicdraw.core.ApplicationEnvironment;
import com.nomagic.magicdraw.core.options.AbstractPropertyOptionsGroup;
import com.nomagic.magicdraw.pathvariables.RecursivePathVariableException;
import com.nomagic.magicdraw.properties.BooleanProperty;
import com.nomagic.magicdraw.properties.ChoiceProperty;
import com.nomagic.magicdraw.properties.FileProperty;
import com.nomagic.magicdraw.properties.NumberProperty;
import com.nomagic.magicdraw.properties.Property;
import com.nomagic.magicdraw.properties.PropertyResourceProvider;
import com.nomagic.magicdraw.utils.MDLog;

public class SSCAEProjectUsageIntegrityOptions extends AbstractPropertyOptionsGroup {

	public static final String ID = "SSCAE.ProjectUsageIntegrity.Options";
	public static final String NAME = "SSCAE_PROJECT_USAGE_INTEGRITY_OPTIONS_NAME";

	public static final String DEFAULT_GROUP = "DEFAULT_GROUP";
	public static final String DEFAULT_COMMAND_DESC = "DEFAULT_COMMAND_ID_DESCRIPTION";
	public static final String DEFAULT_DOT = "DEFAULT_COMMAND_ID_DOT_OPTION";
	public static final String DEFAULT_GVIZ = "DEFAULT_COMMAND_ID_GRAPHVIZ_OPTION";

	public static final String DOT_GROUP				= "DOT_GROUP";
	public static final String DOT_COMMAND_DESC	 	= "DOT_COMMAND_ID_DESCRIPTION";
	public static final String DOT_COMMAND_LINUX 	= "DOT_COMMAND_LINUX";
	public static final String DOT_COMMAND_MACOSX 	= "DOT_COMMAND_MACOSX";
	public static final String DOT_COMMAND_WINDOWS 	= "DOT_COMMAND_WINDOWS";

	public static final String GRAPHVIZ_GROUP		= "GRAPHVIZ_GROUP";
	public static final String GRAPHVIZ_APP_DESC	= "GRAPHVIZ_APP_ID_DESCRIPTION";
	public static final String GRAPHVIZ_APP_LINUX 	= "GRAPHVIZ_APP_LINUX";
	public static final String GRAPHVIZ_APP_MACOSX 	= "GRAPHVIZ_APP_MACOSX";
	public static final String GRAPHVIZ_APP_WINDOWS = "GRAPHVIZ_APP_WINDOWS";

	public static final String STATUS_GROUP				= "STATUS_REFRESH_RATE_GROUP";
	public static final String STATUS_DESC	 			= "STATUS_REFRESH_RATE_ID_DESCRIPTION";
	public static final String STATUS_ID	 			= "STATUS_REFRESH_RATE_ID";
	public static final String STATUS_DEFAULT	 	= "STATUS_REFRESH_RATE";

	public static final String PERFORMANCE_INFORMATION_GROUP		= "PERFORMANCE_INFORMATION_GROUP";
	public static final String PERFORMANCE_INFORMATION_DESC	 		= "PERFORMANCE_INFORMATION_ID_DESCRIPTION";
	public static final String PERFORMANCE_INFORMATION_ID 			= "PERFORMANCE_INFORMATION_ID";
	public static final String PERFORMANCE_INFORMATION_DEFAULT		= "PERFORMANCE_INFORMATION";

	public static final String ADVANCED_INFORMATION_GROUP		= "ADVANCED_INFORMATION_GROUP";
	public static final String ADVANCED_INFORMATION_DESC	 		= "ADVANCED_INFORMATION_ID_DESCRIPTION";
	public static final String ADVANCED_INFORMATION_ID 			= "ADVANCED_INFORMATION_ID";
	public static final String ADVANCED_INFORMATION_DEFAULT		= "ADVANCED_INFORMATION";
	
	public static final String LOAD_DIAGRAMS_GROUP		= "LOAD_DIAGRAMS_GROUP";
	public static final String LOAD_DIAGRAMS_DESC	 		= "LOAD_DIAGRAMS_ID_DESCRIPTION";
	public static final String LOAD_DIAGRAMS_ID 			= "LOAD_DIAGRAMS_ID";
	public static final String LOAD_DIAGRAMS_DEFAULT		= "LOAD_DIAGRAMS";



	public static enum Platform { LINUX, MACOSX, WINDOWS, UNKNOWN };

	private static Platform currentPlatform;

	public static Platform getCurrentPlatform() {

		if (null == currentPlatform) {
			if (ApplicationEnvironment.isLinux()) {
				currentPlatform = Platform.LINUX;
			} else if (ApplicationEnvironment.isMacOS()) {
				currentPlatform = Platform.MACOSX;
			} else if (ApplicationEnvironment.isWindows()) {
				currentPlatform = Platform.WINDOWS;
			} else {
				currentPlatform = Platform.UNKNOWN;
			}

			if (currentPlatform == Platform.UNKNOWN)
				throw new IllegalArgumentException(UNSUPPORTED_PLATFORM);
		}

		return currentPlatform;
	}

	public static final String UNSUPPORTED_PLATFORM = "Unsupported/unknown SSCAE platform for running MagicDraw (SSCAE-supported platforms are: linux, macosx, windows)";

	public static final Map<Platform, String> DOT_COMMAND_PATH_PLATFORM = new HashMap<Platform, String>();
	public static final Map<Platform, String> DOT_COMMAND_ID_PLATFORM = new HashMap<Platform, String>();
	public static final Map<Platform, String> GRAPHVIZ_APP_PATH_PLATFORM = new HashMap<Platform, String>();
	public static final Map<Platform, String> GRAPHVIZ_APP_ID_PLATFORM = new HashMap<Platform, String>();

	static {
		DOT_COMMAND_PATH_PLATFORM.put(Platform.LINUX, 	DOT_COMMAND_LINUX);
		DOT_COMMAND_PATH_PLATFORM.put(Platform.MACOSX,	DOT_COMMAND_MACOSX);
		DOT_COMMAND_PATH_PLATFORM.put(Platform.WINDOWS,	DOT_COMMAND_WINDOWS);

		DOT_COMMAND_ID_PLATFORM.put(Platform.LINUX, 		DOT_COMMAND_LINUX + "_ID");
		DOT_COMMAND_ID_PLATFORM.put(Platform.MACOSX,		DOT_COMMAND_MACOSX + "_ID");
		DOT_COMMAND_ID_PLATFORM.put(Platform.WINDOWS,	DOT_COMMAND_WINDOWS + "_ID");

		GRAPHVIZ_APP_PATH_PLATFORM.put(Platform.LINUX, 	GRAPHVIZ_APP_LINUX);
		GRAPHVIZ_APP_PATH_PLATFORM.put(Platform.MACOSX,	GRAPHVIZ_APP_MACOSX);
		GRAPHVIZ_APP_PATH_PLATFORM.put(Platform.WINDOWS,	GRAPHVIZ_APP_WINDOWS);

		GRAPHVIZ_APP_ID_PLATFORM.put(Platform.LINUX, 	GRAPHVIZ_APP_LINUX + "_ID");
		GRAPHVIZ_APP_ID_PLATFORM.put(Platform.MACOSX,	GRAPHVIZ_APP_MACOSX + "_ID");
		GRAPHVIZ_APP_ID_PLATFORM.put(Platform.WINDOWS,	GRAPHVIZ_APP_WINDOWS + "_ID");
	}

	public SSCAEProjectUsageIntegrityOptions() {
		super(ID);
	}

	public static final PropertyResourceProvider PROPERTY_RESOURCE_PROVIDER = new PropertyResourceProvider() {
		@Override
		public String getString(String key, Property property)
		{
			return SSCAEProjectUsageIntegrityOptionsResources.getString(key);
		}
	};

	@Override
	public void setDefaultValues() {
		Platform platform = getCurrentPlatform();
		String dotProperty = DOT_COMMAND_PATH_PLATFORM.get(platform);
		String dotCommand = SSCAEProjectUsageIntegrityOptionsResources.getString(dotProperty);
		setDotCommandProperty(DOT_COMMAND_ID_PLATFORM.get(platform), dotCommand);

		String gvProperty = GRAPHVIZ_APP_PATH_PLATFORM.get(platform);
		String gvApplication = SSCAEProjectUsageIntegrityOptionsResources.getString(gvProperty);
		setGraphvizAppProperty(GRAPHVIZ_APP_ID_PLATFORM.get(platform), gvApplication);

		List <Object> defaultValues = new LinkedList<Object>();
		defaultValues.add(SSCAEProjectUsageIntegrityOptionsResources.getString(DEFAULT_DOT)); defaultValues.add(SSCAEProjectUsageIntegrityOptionsResources.getString(DEFAULT_GVIZ));
		setDefaultCommandProperty(DEFAULT_COMMAND_DESC, defaultValues, 1);
		setStatusRefreshProperty();
		setShowPerformanceStatsProperty(Boolean.getBoolean(SSCAEProjectUsageIntegrityOptionsResources.getString(PERFORMANCE_INFORMATION_DEFAULT)));
		setShowAdvancedInformationProperty(Boolean.getBoolean(SSCAEProjectUsageIntegrityOptionsResources.getString(ADVANCED_INFORMATION_DEFAULT)));
		setLoadDiagramsProperty(Boolean.getBoolean(SSCAEProjectUsageIntegrityOptionsResources.getString(LOAD_DIAGRAMS_DEFAULT)));

	}

	public void setDefaultCommandProperty(String id, List <Object> values, int defaultValue){
		ChoiceProperty p = new ChoiceProperty(id, defaultValue, values);
		p.setResourceProvider(PROPERTY_RESOURCE_PROVIDER);
		p.setGroup(DEFAULT_GROUP);
		addProperty(p, DEFAULT_COMMAND_DESC);
	}

	public Object getDefaultCommandProperty(){
		String pName = DEFAULT_COMMAND_DESC;
		Property p = getProperty(pName);

		if (!(p instanceof ChoiceProperty))
			return null;
		return p.getValue();

	}

	public void setDotCommandProperty(String id, String value) {
		FileProperty p = new FileProperty(id, value, FileProperty.FILES_ONLY);
		p.setResourceProvider(PROPERTY_RESOURCE_PROVIDER);
		p.setGroup(DOT_GROUP);
		addProperty(p, DOT_COMMAND_DESC);
	}

	public String getDotCommandProperty() {
		String pName = DOT_COMMAND_ID_PLATFORM.get(getCurrentPlatform());
		Property p = getProperty(pName);
		if (!(p instanceof FileProperty))
			return null;
		try {
			return ((FileProperty) p).getFile(null).getAbsolutePath();
		} catch (RecursivePathVariableException e) {
			MDLog.getPluginsLog().error(String.format("%s -- ignoring exception", this.getClass().getName()), e);
			return null;
		}
	}

	public void setGraphvizAppProperty(String id, String value) {
		int fileSelectionMode = (getCurrentPlatform() == Platform.MACOSX) ? FileProperty.FILES_AND_DIRECTORIES: FileProperty.FILES_ONLY;
		FileProperty p = new FileProperty(id, value, fileSelectionMode);
		p.setResourceProvider(PROPERTY_RESOURCE_PROVIDER);
		p.setGroup(GRAPHVIZ_GROUP);
		addProperty(p, GRAPHVIZ_APP_DESC);
	}

	public String getGraphvizAppProperty() {
		String pName = GRAPHVIZ_APP_ID_PLATFORM.get(getCurrentPlatform());
		Property p = getProperty(pName);
		if (!(p instanceof FileProperty))
			return null;
		try {
			return ((FileProperty) p).getFile(null).getAbsolutePath();
		} catch (RecursivePathVariableException e) {
			MDLog.getPluginsLog().error(String.format("%s -- ignoring exception", this.getClass().getName()), e);
			return null;
		}
	}

	public void setStatusRefreshProperty(){
		NumberProperty p = new NumberProperty(STATUS_ID, 0.2, 1, 0.2, 1.0D/0.0D);
		p.setResourceProvider(PROPERTY_RESOURCE_PROVIDER);
		p.setGroup(STATUS_GROUP);
		addProperty(p, STATUS_DESC);

	}

	public Double getStatusRefreshProperty(){
		Property p = getProperty(STATUS_ID);
		if (!(p instanceof NumberProperty))
			return null;
		double value = ((NumberProperty) p).getDouble();
		return value;
	}

	public void setShowPerformanceStatsProperty(boolean value){
		BooleanProperty p = new BooleanProperty(PERFORMANCE_INFORMATION_ID, value);
		p.setResourceProvider(PROPERTY_RESOURCE_PROVIDER);
		p.setGroup(PERFORMANCE_INFORMATION_GROUP);
		addProperty(p, PERFORMANCE_INFORMATION_DESC);
	}

	public boolean getShowPerformanceStatusProperty(){
		Property p = getProperty(PERFORMANCE_INFORMATION_ID);
		if (!(p instanceof BooleanProperty))
			return false;

		return ((BooleanProperty) p).getBoolean();
	}
	

	public void setShowAdvancedInformationProperty(boolean value){
		BooleanProperty p = new BooleanProperty(ADVANCED_INFORMATION_ID, value);
		p.setResourceProvider(PROPERTY_RESOURCE_PROVIDER);
		p.setGroup(ADVANCED_INFORMATION_GROUP);
		addProperty(p, ADVANCED_INFORMATION_DESC);
	}

	public boolean getShowAdvancedInformationProperty(){
		Property p = getProperty(ADVANCED_INFORMATION_ID);
		if (!(p instanceof BooleanProperty))
			return false;

		return ((BooleanProperty) p).getBoolean();
	}
	
	public void setLoadDiagramsProperty(boolean value){
		BooleanProperty p = new BooleanProperty(LOAD_DIAGRAMS_ID, value);
		p.setResourceProvider(PROPERTY_RESOURCE_PROVIDER);
		p.setGroup(LOAD_DIAGRAMS_GROUP);
		addProperty(p, LOAD_DIAGRAMS_DESC);
	}

	public boolean getLoadDiagramsProperty(){
		Property p = getProperty(LOAD_DIAGRAMS_ID);
		if (!(p instanceof BooleanProperty))
			return false;

		return ((BooleanProperty) p).getBoolean();
	}


	@Override
	public String getName() {
		return SSCAEProjectUsageIntegrityOptionsResources.getString(NAME);
	}

}
