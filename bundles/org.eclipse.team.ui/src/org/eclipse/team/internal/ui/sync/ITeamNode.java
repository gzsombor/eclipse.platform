package org.eclipse.team.internal.ui.sync;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;

/**
 * Interface for a sync node that responds to team commands.
 */
public interface ITeamNode extends IDiffElement {
	// Possible values for the change direction
	static final int INCOMING = Differencer.RIGHT;
	static final int OUTGOING = Differencer.LEFT;
	static final int CONFLICTING = Differencer.CONFLICTING;
	static final int NO_CHANGE = Differencer.NO_CHANGE;
	
	/**
	 * Returns the change direction for this resource.  One of:
	 * INCOMING, OUTGOING, CONFLICTING, NO_CHANGE.
	 */
	public int getChangeDirection();
	
	/**
	 * Returns the core resource represented by this node.
	 */
	public IResource getResource();
}
