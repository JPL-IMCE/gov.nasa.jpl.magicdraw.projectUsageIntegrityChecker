/*
 *
 * License Terms
 *
 * Copyright (c) 2013-2016, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * *   Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *   Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 *
 * *   Neither the name of Caltech nor its operating division, the Jet
 *    Propulsion Laboratory, nor the names of its contributors may be
 *    used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

import java.lang.String;

import com.nomagic.ci.persistence.local.ProjectState;
import org.eclipse.emf.common.util.URI;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

public class DigestRepresenter extends Representer {

	public DigestRepresenter() {
		this.representers.put(URI.class, new RepresentURI());
		this.representers.put(ProjectClassification.class, new RepresentProjectClassification());
		this.representers.put(ProjectState.class, new RepresentProjectState());
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
	
	private class RepresentProjectState implements Represent {

		@Override
		public Node representData(Object data) {
			if (!(data instanceof ProjectState))
				throw new IllegalArgumentException("ProjectStateRepresenter -- data should be a ProjectState");
			ProjectState ps = (ProjectState) data;
			String value = ps.name();
			return representScalar(new Tag("!SSCAE.ProjectState"), value);
		}
		
	}
}