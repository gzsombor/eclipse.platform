/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class ChangeSetResourceMapping extends ResourceMapping {

	private final CVSActiveChangeSet changeSet;

	public ChangeSetResourceMapping(CVSActiveChangeSet changeSet) {
		this.changeSet = changeSet;
	}

	public Object getModelObject() {
		return changeSet;
	}

	public String getModelProviderId() {
		return ChangeSetModelProvider.ID;
	}

	public IProject[] getProjects() {
		Set result = new HashSet();
		IResource[] resources = changeSet.getResources();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			result.add(resource.getProject());
		}
		return (IProject[]) result.toArray(new IProject[result.size()]);
	}

	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		IResource[] resources = changeSet.getResources();
		if (resources.length == 0) {
			return new ResourceTraversal[0];
		}
		return new ResourceTraversal[] {
				new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE)
		};
	}

}
