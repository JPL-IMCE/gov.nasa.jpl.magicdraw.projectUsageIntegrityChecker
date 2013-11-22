package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.local.spi.localproject.ILocalProjectInternal;
import com.nomagic.magicdraw.core.Project;

public class MDLocalPrimaryProject extends MDLocalProject {

	public MDLocalPrimaryProject() {
		super();
	}
	
	public static void configure(MDLocalProject that, @Nonnull Project rootProject, ILocalProjectInternal p, String index) {
		MDLocalProject.configure(that, rootProject, p, index);
	}
}
