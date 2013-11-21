package gov.nasa.jpl.magicdraw.projectUsageIntegrity.options.resources;

import com.nomagic.magicdraw.resources.ResourceManager;

public final class SSCAEProjectUsageIntegrityOptionsResources {

	public static final String BUNDLE_NAME = "gov.nasa.jpl.magicdraw.projectUsageIntegrity.options.resources.SSCAEProjectUsageIntegrityOptionsResources";
	
	private SSCAEProjectUsageIntegrityOptionsResources() {}
	
	public static String getString(String key) {
		return ResourceManager.getStringFor(key, BUNDLE_NAME, SSCAEProjectUsageIntegrityOptionsResources.class.getClassLoader());
	}
}
