package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;

/**
 * Copyright 2013, by the California Institute of Technology.
 * ALL RIGHTS RESERVED. United States Government Sponsorship acknowledged. 
 * Any commercial use must be negotiated with the Office of 
 * Technology Transfer at the California Institute of Technology.
 *
 * This software may be subject to U.S. export control laws. 
 * By acepting this software, the user agrees to comply with all applicable U.S. export laws 
 * and regulations. User has the responsibility to obtain export licenses,
 * or other export authority as may be required before exprting such information 
 * to foreign countries or providing access to foreign persons.
 *
 * Inquiries about this notice should be addressed to:
 *
 * JPL Software Release Authority
 * Phone: +1-818-393-3421
 * mailto:SoftwareRelease@jpl.nasa.gov
 */

import org.eclipse.emf.common.util.URI;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import  org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

public class DigestRepresenter extends Representer {

	public DigestRepresenter() {
		this.representers.put(URI.class, new RepresentURI());
		this.representers.put(ProjectClassification.class, new RepresentProjectClassification());
		YamlDigestHelper.configureRepresenter(this);
	}

	private class RepresentURI implements Represent {
		@Override
		public Node representData(Object data) {
			if (!(data instanceof URI))
				throw new IllegalArgumentException("URIRepresenter -- data should be a URI");
			URI uri = (URI) data;
			String value = uri.toString();
			return representScalar(new Tag("!emf.URI"), value);
		}

	}
	
	private class RepresentProjectClassification implements Represent {

		@Override
		public Node representData(Object data) {
			if (!(data instanceof ProjectClassification))
				throw new IllegalArgumentException("ProjectClassificationRepresenter -- data should be a ProjectClassification");
			ProjectClassification pc = (ProjectClassification) data;
			String value = pc.name();
			return representScalar(new Tag("!SSCAE.Classification"), value);
		}
		
	}
}
