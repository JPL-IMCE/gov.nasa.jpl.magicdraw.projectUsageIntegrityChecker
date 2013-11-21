package gov.nasa.jpl.magicdraw.projectUsageIntegrity.graph;


import org.eclipse.emf.common.util.URI;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

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

public class DigestConstructor extends Constructor {
	
	public DigestConstructor() {
		super(SSCAEProjectDigest.class);
		this.yamlConstructors.put(new Tag("!emf.URI"), new ConstructURI());
		YamlDigestHelper.configureConstructors(this);
	}

	private class ConstructURI extends AbstractConstruct {
		public Object construct(Node node) {
			if (!(node instanceof ScalarNode))
				throw new IllegalArgumentException("ConstructURI -- node should be a scalar");
			String val = (String) constructScalar((ScalarNode) node);
			return URI.createURI(val);
		}
	}
}
