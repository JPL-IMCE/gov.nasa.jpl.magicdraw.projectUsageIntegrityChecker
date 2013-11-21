package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

import javax.annotation.Nonnull;

import com.nomagic.ci.persistence.spi.local.ILocalProjectInternal;
import com.nomagic.magicdraw.core.Project;

public class MDLocalAttachedProject extends MDLocalProject implements MDAttachedProject {

	private SSCAEProjectDigest digest;
	
	public MDLocalAttachedProject() {
		super();
	}
	
	public static void configure(MDLocalProject that, @Nonnull Project rootProject, ILocalProjectInternal p, String index) {
		MDLocalProject.configure(that, rootProject, p, index);
	}

	public SSCAEProjectDigest getDigest() {
		return digest;
	}

	public void setDigest(SSCAEProjectDigest digest) {
		this.digest = digest;
	}
	
	@Override
	public void updateIndex(String index) {		
		setLabel("[L" + index + "]" + getLabel().substring(2 + getIndex().length()));
		setIndex("L" + index);
	}
}
