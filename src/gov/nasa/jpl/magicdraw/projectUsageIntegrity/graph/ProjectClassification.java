package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

public enum ProjectClassification {
	INVALID,
	IS_PROJECT, 
	IS_PROJECT_WITH_PROXIES_FOR_RECOVERED_ELEMENTS, 
	IS_PROJECT_WITH_PROXIES_FOR_MISSING_ELEMENTS, 
	IS_PROJECT_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS, 
	IS_MODULE, 
	IS_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS, 
	IS_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS, 
	IS_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS, 
	IS_HYBRID_PROJECT_MODULE,
	IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_RECOVERED_ELEMENTS,
	IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_ELEMENTS, 
	IS_HYBRID_PROJECT_MODULE_WITH_PROXIES_FOR_MISSING_AND_RECOVERED_ELEMENTS
}