/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.resources;
import org.eclipse.core.runtime.*;
import org.eclipse.core.internal.events.InternalBuilder;
import java.util.Map;
/**
 * The abstract base class for all incremental project builders. This class
 * provides the infrastructure for defining a builder and fulfills the contract
 * specified by the <code>org.eclipse.core.resources.builders</code> standard
 * extension point.
 * <p>
 * All builders must subclass this class according to the following guidelines:
 * <ul>
 * <li>must re-implement at least <code>build</code></li>
 * <li>may implement other methods</li>
 * <li>must supply a public, no-argument constructor</li>
 * </ul>
 * On creation, the <code>setInitializationData</code> method is called with
 * any parameter data specified in the declaring plug-in's manifest.
 */
public abstract class IncrementalProjectBuilder extends InternalBuilder
		implements
			IExecutableExtension {
	/*
	 * ====================================================================
	 * Constants related to build requests
	 * ====================================================================
	 */
	/**
	 * Build kind constant (value 10) indicating an incremental build request.
	 * 
	 * @see IProject#build
	 * @see IWorkspace#build
	 */
	public static final int INCREMENTAL_BUILD = 10;
	/**
	 * Build kind constant (value 6) indicating a full build request.
	 * 
	 * @see IProject#build
	 * @see IWorkspace#build
	 */
	public static final int FULL_BUILD = 6;
	/**
	 * Build kind constant (value 9) indicating an automatic build request.
	 *  
	 */
	public static final int AUTO_BUILD = 9;
	/**
	 * Build kind constant (value 15) indicating a build clean request
	 * 
	 * @see IProject#build
	 * @see IWorkspace#build
	 * @since 3.0
	 */
	public static final int CLEAN_BUILD = 15;
	/**
	 * Runs this builder in the specified manner. Subclasses should implement
	 * this method to do the processing they require.
	 * <p>
	 * If the build kind is <code>INCREMENTAL_BUILD</code> or
	 * <code>AUTO_BUILD</code>, the <code>getDelta</code> method can be
	 * used during the invocation of this method to obtain information about
	 * what changes have occurred since the last invocation of this method. Any
	 * resource delta acquired is valid only for the duration of the invocation
	 * of this method.
	 * </p>
	 * <p>
	 * After completing a build, this builder may return a list of projects for
	 * which it requires a resource delta the next time it is run. This
	 * builder's project is implicitly included and need not be specified. The
	 * build mechanism will attempt to maintain and compute deltas relative to
	 * the identified projects when asked the next time this builder is run.
	 * Builders must re-specify the list of interesting projects every time they
	 * are run as this is not carried forward beyond the next build. Projects
	 * mentioned in return value but which do not exist will be ignored and no
	 * delta will be made available for them.
	 * </p>
	 * <p>
	 * This method is long-running; progress and cancellation are provided by
	 * the given progress monitor. All builders should report their progress and
	 * honor cancel requests in a timely manner. Cancelation requests should be
	 * propagated to the caller by throwing
	 * <code>OperationCanceledException</code>.
	 * </p>
	 * <p>
	 * All builders should try to be robust in the face of trouble. In
	 * situations where failing the build by throwing <code>CoreException</code>
	 * is the only option, a builder has a choice of how best to communicate the
	 * problem back to the caller. One option is to use the
	 * <code>BUILD_FAILED</code> status code along with a suitable message;
	 * another is to use a multi-status containing finer-grained problem
	 * diagnoses.
	 * </p>
	 * 
	 * @param kind the kind of build being requested. Valid values are
	 * <ul>
	 * <li><code>FULL_BUILD</code>- indicates a full build.</li>
	 * <li><code>INCREMENTAL_BUILD</code>- indicates an incremental build.
	 * </li>
	 * <li><code>AUTO_BUILD</code>- indicates an automatically triggered
	 * incremental build (auto-building on).</li>
	 * </ul>
	 * @param args a table of builder-specific arguments keyed by argument name
	 * (key type: <code>String</code>, value type: <code>String</code>);
	 * <code>null</code> is equivalent to an empty map
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 * reporting and cancellation are not desired
	 * @return the list of projects for which this builder would like deltas the
	 * next time it is run or <code>null</code> if none
	 * @exception CoreException if this build fails.
	 * @see IProject#build
	 */
	protected abstract IProject[] build(int kind, Map args,
			IProgressMonitor monitor) throws CoreException;
	/**
	 * Clean is an opportunity for a builder to discard any additional state
	 * that has been computed as a result of previous builds. The platform will
	 * take care of discarding the builder's last built state (there is no need
	 * to call <code>forgetLastBuiltState</code>), and will remove all
	 * markers of type <code>IMarker.PROBLEM</code> that are associated with
	 * the project.
	 * </p>
	 * <p>
	 * This method is called as a result of invocations of
	 * <code>IWorkspace.build</code> or <code>IProject.build</code> where
	 * the build kind is <code>CLEAN_BUILD</code>.
	 * <p>
	 * This default implementation does nothing. Subclasses may override.
	 * <p>
	 * This method is long-running; progress and cancellation are provided by
	 * the given progress monitor. All builders should report their progress and
	 * honor cancel requests in a timely manner. Cancelation requests should be
	 * propagated to the caller by throwing
	 * <code>OperationCanceledException</code>.
	 * </p>
	 * 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 * reporting and cancellation are not desired
	 * @exception CoreException if this build fails.
	 * @see IWorkbspace#build
	 * @see #CLEAN_BUILD
	 * @since 3.0
	 */
	protected void clean(IProgressMonitor monitor) throws CoreException {
	}
	/**
	 * Requests that this builder forget any state it may be retaining regarding
	 * previously built states. Typically this means that the next time the
	 * builder runs, it will have to do a full build since it does not have any
	 * state upon which to base an incremental build.
	 */
	public final void forgetLastBuiltState() {
		super.forgetLastBuiltState();
	}
	/**
	 * Returns the resource delta recording the changes in the given project
	 * since the last time this builder was run. <code>null</code> is returned
	 * if no such delta is available. An empty delta is returned if no changes
	 * have occurred. If <code>null</code> is returned, clients should assume
	 * that unspecified changes have occurred and take the appropriate action.
	 * <p>
	 * The system reserves the right to trim old state in an effort to conserve
	 * space. As such, callers should be prepared to receive <code>null</code>
	 * even if they previously requested a delta for a particular project by
	 * returning that project from a <code>build</code> call.
	 * </p>
	 * <p>
	 * A non- <code>null</code> delta will only be supplied for the given
	 * project if either the result returned from the previous
	 * <code>build</code> included the project or the project is the one
	 * associated with this builder.
	 * </p>
	 * <p>
	 * If the given project was mentioned in the previous <code>build</code>
	 * and subsequently deleted, a non- <code>null</code> delta containing the
	 * deletion will be returned. If the given project was mentioned in the
	 * previous <code>build</code> and was subsequently created, the returned
	 * value will be <code>null</code>.
	 * </p>
	 * <p>
	 * A valid delta will be returned only when this method is called during a
	 * build. The delta returned will be valid only for the duration of the
	 * enclosing build execution.
	 * </p>
	 * 
	 * @return the resource delta for the project or <code>null</code>
	 */
	public final IResourceDelta getDelta(IProject project) {
		return super.getDelta(project);
	}
	/**
	 * Returns the project for which this builder is defined.
	 * 
	 * @return the project
	 */
	public final IProject getProject() {
		return super.getProject();
	}
	/**
	 * Returns whether the given project has already been built during this
	 * build iteration.
	 * <p>
	 * When the entire workspace is being built, the projects are built in
	 * linear sequence. This method can be used to determine if another project
	 * preceeds this builder's project in that build sequence. If only a single
	 * project is being built, then there is no build order and this method will
	 * always return <code>false</code>.
	 * </p>
	 * 
	 * @param project the project to check against in the current build order
	 * @return <code>true</code> if the given project has been built in this
	 * iteration, and <code>false</code> otherwise.
	 * @see #needRebuild
	 * @since 2.1
	 */
	public final boolean hasBeenBuilt(IProject project) {
		return super.hasBeenBuilt(project);
	}
	/**
	 * Returns whether an interrupt request has been made for this build.
	 * Background autobuild is interrupted when another thread tries to modify
	 * the workspace concurrently with the build thread. When this occurs, the
	 * build cycle is flagged as interrupted and the build will be terminated at
	 * the earliest opportunity. This method allows long running builders to
	 * respond to this interruption in a timely manner. Builders are not
	 * required to respond to interruption requests.
	 * <p>
	 * 
	 * @return <code>true</code> if the build cycle has been interrupted, and
	 * <code>false</code> otherwise.
	 * @since 3.0
	 */
	public final boolean isInterrupted() {
		return super.isInterrupted();
	}
	/**
	 * Indicates that this builder made changes that affect a project that
	 * preceeds this project in the currently executing build order, and thus a
	 * rebuild will be necessary.
	 * <p>
	 * This is an advanced feature that builders should use with caution. This
	 * can cause workspace builds to iterate until no more builders require
	 * rebuilds.
	 * </p>
	 * 
	 * @see #hasBeenBuilt
	 * @since 2.1
	 */
	public final void needRebuild() {
		super.needRebuild();
	}
	/**
	 * Sets initialization data for this builder.
	 * <p>
	 * This method is part of the <code>IExecutableExtension</code> interface.
	 * </p>
	 * <p>
	 * Subclasses are free to extend this method to pick up initialization
	 * parameters from the plug-in plug-in manifest (<code>plugin.xml</code>)
	 * file, but should be sure to invoke this method on their superclass.
	 * <p>
	 * For example, the following method looks for a boolean-valued parameter
	 * named "trace":
	 * 
	 * <pre>
	 * public void setInitializationData(IConfigurationElement cfig,
	 * 		String propertyName, Object data) throws CoreException {
	 * 	super.setInitializationData(cfig, propertyName, data);
	 * 	if (data instanceof Hashtable) {
	 * 		Hashtable args = (Hashtable) data;
	 * 		String traceValue = (String) args.get(&quot;trace&quot;);
	 * 		TRACING = (traceValue != null &amp;&amp; traceValue.equals(&quot;true&quot;));
	 * 	}
	 * }
	 * </pre>
	 * 
	 * </p>
	 */
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
	}
	/**
	 * Informs this builder that it is being started by the build management
	 * infrastructure. By the time this method is run, the builder's project is
	 * available and <code>setInitializationData</code> has been called. The
	 * default implementation should be called by all overriding methods.
	 * 
	 * @see #setInitializationData
	 */
	protected void startupOnInitialize() {
		// reserved for future use
	}
}