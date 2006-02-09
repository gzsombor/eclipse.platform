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
package org.eclipse.team.ui.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.*;
import org.eclipse.team.internal.ui.*;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A model operation that executes a merge according to the merge lifecycle
 * associated with an {@link IMergeContext} and {@link IResourceMappingMerger}
 * instances obtained from the model providers involved.
 * 
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * 
 * @since 3.2
 */
public abstract class ModelMergeOperation extends ModelOperation {

	/**
	 * Validate the merge context with the model providers that have mappings in
	 * the scope of the context. The {@link IResourceMappingMerger} for each
	 * model provider will be consulted and any non-OK status will be
	 * accumulated and returned,
	 * 
	 * @param context
	 *            the merge context being validated
	 * @param monitor
	 *            a progress monitor
	 * @return a status or multi-status that identify any conditions that should
	 *         force a preview of the merge
	 */
	public static IStatus validateMerge(IMergeContext context, IProgressMonitor monitor) {
		ModelProvider[] providers = context.getScope().getModelProviders();
		monitor.beginTask(null, 100 * providers.length);
		List notOK = new ArrayList();
		for (int i = 0; i < providers.length; i++) {
			ModelProvider provider = providers[i];
			IStatus status = validateMerge(provider, context, Policy.subMonitorFor(monitor, 100));
			if (!status.isOK())
				notOK.add(status);
		}
		if (notOK.isEmpty())
			return Status.OK_STATUS;
		if (notOK.size() == 1)
			return (IStatus)notOK.get(0);
		return new MultiStatus(TeamUIPlugin.ID, 0, (IStatus[]) notOK.toArray(new IStatus[notOK.size()]), TeamUIMessages.ResourceMappingMergeOperation_3, null);
	}
	
	/*
	 * Validate the merge by obtaining the {@link IResourceMappingMerger} for the
	 * given provider.
	 * @param provider the model provider
	 * @param context the merge context
	 * @param monitor a progress monitor
	 * @return the status obtained from the merger for the provider
	 */
	private static IStatus validateMerge(ModelProvider provider, IMergeContext context, IProgressMonitor monitor) {
		IResourceMappingMerger merger = getMerger(provider);
		if (merger == null)
			return Status.OK_STATUS;
		return merger.validateMerge(context, monitor);
	}
	
	/*
	 * Return the auto-merger associated with the given model provider using the
	 * adaptable mechanism. If the model provider does not have a merger
	 * associated with it, a default merger that performs the merge at the file
	 * level is returned.
	 * 
	 * @param provider
	 *            the model provider of the elements to be merged (must not be
	 *            <code>null</code>)
	 * @return a merger
	 */
	private static IResourceMappingMerger getMerger(ModelProvider provider) {
		Assert.isNotNull(provider);
		return (IResourceMappingMerger)Utils.getAdapter(provider, IResourceMappingMerger.class);
	}
	
	/**
	 * Create a model merge operation.
	 * @param part the workbench part from which the operation was requested or <code>null</code>
	 * @param manager the scope manager
	 */
	protected ModelMergeOperation(IWorkbenchPart part, ISynchronizationScopeManager manager) {
		super(part, manager);
	}
	
	/**
	 * Perform a merge. First {@link #initializeContext(IProgressMonitor)} is
	 * called to determine the set of resource changes. Then the
	 * {@link #executeMerge(IProgressMonitor)} method is invoked.
	 * 
	 * @param monitor a progress monitor
	 */
	protected void execute(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		try {
			monitor.beginTask(null, 100);
			initializeContext(Policy.subMonitorFor(monitor, 75));
			executeMerge(Policy.subMonitorFor(monitor, 25));
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Perform a merge. This method is invoked frome
	 * {@link #execute(IProgressMonitor)} after the context has been
	 * initialized. If there are changes in the context, they will be validating
	 * by calling {@link #validateMerge(IMergeContext, IProgressMonitor)}. If
	 * there are no validation problems, {@link #performMerge(IProgressMonitor)}
	 * will then be called to perform the merge. If there are problems encounted
	 * or if a preview was requested, {@link #handlePreviewRequest()} is called.
	 * 
	 * @param monitor a progress monitor
	 */
	protected void executeMerge(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(null, 100);
		if (!hasChangesOfInterest()) {
			handleNoChanges();
		} else if (isPreviewRequested()) {
			handlePreviewRequest();
		} else {
			IStatus status = ModelMergeOperation.validateMerge(getMergeContext(), Policy.subMonitorFor(monitor, 10));
			if (!status.isOK()) {
				handleValidationFailure(status);
			}
			status = performMerge(Policy.subMonitorFor(monitor, 90));
			if (!status.isOK()) {
				handleMergeFailure(status);
			}
		}
		monitor.done();
	}
	
	/**
	 * A preview of the merge has been requested. By default, this method does
	 * nothing. Subclasses that wish to support previewing must override this
	 * method to preview the merge and the {@link #getPreviewRequestMessage()}
	 * to have the option presented to the user if the scope changes.
	 */
	protected void handlePreviewRequest() {
		// Do nothing
	}

	/**
	 * Initialize the merge context for this merge operation.
	 * After this method is invoked, the {@link #getContext()}
	 * method must return an instance of {@link IMergeContext}
	 * that is fully initialized.
	 * @param monitor a progress monitor
	 * @throws CoreException
	 */
	protected abstract void initializeContext(IProgressMonitor monitor) throws CoreException;

	/**
	 * Method invoked when the context contains changes that failed validation
	 * by at least one {@link IResourceMappingMerger}.
	 * By default, the user is prompted to inform them that unmergeable changes were found
	 * and the {@link #handlePreviewRequest()} method is invoked.
	 * Subclasses may override.
	 * @param status the status returned from the mergers that reported the validation failures
	 */
	protected void handleValidationFailure(IStatus status) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(getShell(), "Validation Failure", "Better message to come.");
			};
		});
		handlePreviewRequest();
	}

	/**
	 * Method invoked when the context contains unmergable changes.
	 * By default, the user is promted to inform them that unmergeable changes were found.
	 * Subclasses may override.
	 * @param status the status returned from the merger that reported the conflict
	 */
	protected void handleMergeFailure(IStatus status) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(getShell(), TeamUIMessages.MergeIncomingChangesAction_0, TeamUIMessages.MergeIncomingChangesAction_1);
			};
		});
		handlePreviewRequest();
	}

	/**
	 * Method invoked when the context contains no changes.
	 * By default, the user is promted to inform them that no changes were found.
	 * Subclasses may override.
	 */
	protected void handleNoChanges() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(getShell(), TeamUIMessages.ResourceMappingMergeOperation_0, TeamUIMessages.ResourceMappingMergeOperation_1);
			};
		});
	}
	
	/**
	 * Attempt a headless merge of the elements in the context of this
	 * operation. The merge is performed by obtaining the
	 * {@link IResourceMappingMerger} for the model providers in the context's
	 * scope. The merger of the model providers are invoked in the order
	 * determined by the {@link ModelOperation#sortByExtension(ModelProvider[])}
	 * method. The method will stop on the first conflict encountered.
	 * This method will gthrow a runtime exception
	 * if the operation does not have a merge context.
	 * 
	 * @param monitor
	 *            a progress monitor
	 * @return a status that indicates whether the merge succeeded.
	 * @throws CoreException
	 *             if an error occurred
	 */
	protected IStatus performMerge(IProgressMonitor monitor) throws CoreException {
		ISynchronizationContext sc = getContext();
		if (sc instanceof IMergeContext) {
			IMergeContext context = (IMergeContext) sc;		
			final ModelProvider[] providers = sortByExtension(context.getScope().getModelProviders());
			final IStatus[] result = new IStatus[] { Status.OK_STATUS };
			context.run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					try {
						monitor.beginTask(null, IProgressMonitor.UNKNOWN);
						for (int i = 0; i < providers.length; i++) {
							ModelProvider provider = providers[i];
							IStatus status = performMerge(provider, Policy.subMonitorFor(monitor, IProgressMonitor.UNKNOWN));
							if (!status.isOK()) {
								// Stop at the first failure
								result[0] = status;
								return;
							}
							try {
								Platform.getJobManager().join(getContext(), monitor);
							} catch (InterruptedException e) {
								// Ignore
							}
						}
					} finally {
						monitor.done();
					}
				}
			}, null /* scheduling rule */, IResource.NONE, monitor);
			return result[0];
		}
		return noMergeContextAvailable();
	}
	
	/**
	 * Attempt to merge all the mappings that come from the given provider.
	 * Return a status which indicates whether the merge succeeded or if
	 * unmergeable conflicts were found. By default, this method invokes
	 * the {@link IResourceMappingMerger#merge(IMergeContext, IProgressMonitor)}
	 * method but does not wait for the context to update (see {@link ISynchronizationContext}.
	 * Callers that are invoking the merge on multiple models should wait until the
	 * context has updated before invoking merge on another merger. The following
	 * line of code will wait for the context to update:
	 * <pre>
	 * Platform.getJobManager().join(getContext(), monitor);
	 * </pre>
	 * <p>
	 * This method will throw a runtime exception
	 * if the operation does not have a merge context.
	 * @param provider the model provider whose mappings are to be merged
	 * @param monitor a progress monitor
	 * @return a non-OK status if there were unmergable conflicts
	 * @throws CoreException if an error occurred
	 */
	protected IStatus performMerge(ModelProvider provider, IProgressMonitor monitor) throws CoreException {
		ISynchronizationContext sc = getContext();
		if (sc instanceof IMergeContext) {
			IMergeContext context = (IMergeContext) sc;
			IResourceMappingMerger merger = getMerger(provider);
			if (merger != null) {
				IStatus status = merger.merge(context, monitor);
				if (status.isOK() || status.getCode() == IMergeStatus.CONFLICTS) {
					return status;
				}
				throw new TeamException(status);
			}
			return Status.OK_STATUS;
		}
		return noMergeContextAvailable();
	}
	
	private IStatus noMergeContextAvailable() {
		throw new IllegalStateException("Merges should only be attemped for operations that have a merge context");
	}
	
	/**
	 * Return whether the context of this operation has changes that are
	 * of interest to the operation. Sublcasses may override.
	 * @return whether the context of this operation has changes that are
	 * of interest to the operation
	 */
	protected boolean hasChangesOfInterest() {
		return !getContext().getDiffTree().isEmpty() && hasIncomingChanges(getContext().getDiffTree());
	}
	
	private boolean hasIncomingChanges(IDiffTree tree) {
		return tree.hasMatchingDiffs(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), new FastDiffFilter() {
			public boolean select(IDiff node) {
				if (node instanceof IThreeWayDiff) {
					IThreeWayDiff twd = (IThreeWayDiff) node;
					int direction = twd.getDirection();
					if (direction == IThreeWayDiff.INCOMING || direction == IThreeWayDiff.CONFLICTING) {
						return true;
					}
				} else {
					// Return true for any two-way change
					return true;
				}
				return false;
			}
		});
	}
	
	private IMergeContext getMergeContext() {
		return (IMergeContext)getContext();
	}
}
