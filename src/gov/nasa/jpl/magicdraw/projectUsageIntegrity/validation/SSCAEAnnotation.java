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
package gov.nasa.jpl.magicdraw.projectUsageIntegrity.validation;

import java.lang.String;
import java.util.List;

import javax.annotation.Nonnull;

import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;

/**
 * @author Nicolas F. Rouquette (JPL)
 */
public class SSCAEAnnotation extends Annotation {
	
	public final String graphSignature;
	
	public SSCAEAnnotation(
			@Nonnull String graphSignature,
			@Nonnull EnumerationLiteral level, 
			@Nonnull String abbrev, 
			@Nonnull String message, 
			@Nonnull BaseElement element) {
		super(level, abbrev, message, element);
		this.graphSignature = graphSignature;
	}
	
	public SSCAEAnnotation(
			@Nonnull String graphSignature,
			@Nonnull EnumerationLiteral level, 
			@Nonnull String abbrev, 
			@Nonnull String message, 
			@Nonnull BaseElement element,
			@Nonnull List<? extends NMAction> actions) {
		super(level, abbrev, message, element, actions);
		this.graphSignature = graphSignature;
	}
	
	public SSCAEAnnotation(
			@Nonnull String graphSignature,
			@Nonnull BaseElement element, 
			@Nonnull Constraint constraint, 
			@Nonnull String message, 
			@Nonnull List<? extends NMAction> actions) {
		super(element, constraint, message, actions);
		this.graphSignature = graphSignature;
	}
}