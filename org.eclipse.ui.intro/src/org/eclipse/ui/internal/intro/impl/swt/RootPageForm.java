/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.intro.impl.swt;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.internal.intro.impl.*;
import org.eclipse.ui.internal.intro.impl.model.*;
import org.eclipse.ui.internal.intro.impl.util.*;

/**
 * A Composite that represents the Root Page. It is swapped in the main page
 * book in the FormIntroPartImplementation class.
 */
public class RootPageForm implements IIntroConstants {

    private FormToolkit toolkit;
    private IntroHomePage rootPage;
    private Form parentForm;
    private Label descriptionLabel;
    private PageStyleManager rootPageStyleManager;

    class PageComposite extends Composite {

        public PageComposite(Composite parent, int style) {
            super(parent, style);
        }

        // Do not allow composite to take wHint as-is - layout manager
        // can reject the hint and compute larger width.
        public Point computeSize(int wHint, int hHint, boolean changed) {
            return ((RootPageLayout) getLayout()).computeSize(this, wHint,
                    hHint, changed);
        }
    }

    class RootPageLayout extends Layout {

        // gap between link composite and description label.
        private int VERTICAL_SPACING = 20;

        private int LABEL_MARGIN_WIDTH = 5;

        /*
         * Custom layout for Root Page Composite.
         */
        protected Point computeSize(Composite composite, int wHint, int hHint,
                boolean flushCache) {
            int innerWHint = wHint;
            if (wHint != SWT.DEFAULT)
                innerWHint -= LABEL_MARGIN_WIDTH + LABEL_MARGIN_WIDTH;
            Control[] children = composite.getChildren();
            Point s1 = children[0].computeSize(SWT.DEFAULT, SWT.DEFAULT);
            Point s2 = children[1].computeSize(innerWHint, SWT.DEFAULT);
            s2.x += LABEL_MARGIN_WIDTH;
            int height = 2 * (s2.y + VERTICAL_SPACING + s1.y / 2);
            Point size = new Point(Math.max(s1.x, s2.x), height + 5);
            return size;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.swt.widgets.Layout#layout(org.eclipse.swt.widgets.Composite,
         *      boolean)
         */
        protected void layout(Composite composite, boolean flushCache) {
            Control[] children = composite.getChildren();
            Rectangle carea = composite.getClientArea();
            Control content = children[0];
            Control label = children[1];
            Point contentSize = content.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            Point labelSize = label.computeSize(carea.width - 2
                    - LABEL_MARGIN_WIDTH * 2, SWT.DEFAULT);
            content.setBounds(carea.width / 2 - contentSize.x / 2, carea.height
                    / 2 - contentSize.y / 2, contentSize.x, contentSize.y);
            label.setBounds(LABEL_MARGIN_WIDTH, content.getLocation().y
                    + contentSize.y + VERTICAL_SPACING, carea.width
                    - LABEL_MARGIN_WIDTH * 2, labelSize.y);
        }
    }

    private HyperlinkAdapter hyperlinkAdapter = new HyperlinkAdapter() {

        public void linkActivated(HyperlinkEvent e) {
            ImageHyperlink imageLink = (ImageHyperlink) e.getSource();
            IntroLink introLink = (IntroLink) imageLink.getData(INTRO_LINK);
            IntroURLParser parser = new IntroURLParser(introLink.getUrl());
            if (parser.hasIntroUrl()) {
                // execute the action embedded in the IntroURL
                parser.getIntroURL().execute();
                return;
            } else if (parser.hasProtocol()) {
                Util.openBrowser(introLink.getUrl());
                return;
            }
            DialogUtil.displayInfoMessage(imageLink.getShell(), IntroPlugin
                    .getString("HyperlinkAdapter.urlIs") //$NON-NLS-1$
                    + introLink.getUrl());
        }

        public void linkEntered(HyperlinkEvent e) {
            ImageHyperlink imageLink = (ImageHyperlink) e.getSource();
            IntroLink introLink = (IntroLink) imageLink.getData(INTRO_LINK);
            updateDescription(introLink.getText());
        }

        public void linkExited(HyperlinkEvent e) {
            // empty text on exit.
            updateDescription(""); //$NON-NLS-1$
        }

        private void updateDescription(String text) {
            descriptionLabel.setText(text);
            descriptionLabel.getParent().layout();
        }
    };

    /**
     *  
     */
    public RootPageForm(FormToolkit toolkit, IntroModelRoot modelRoot,
            Form parentForm) {
        this.toolkit = toolkit;
        this.rootPage = modelRoot.getHomePage();
        this.parentForm = parentForm;
    }

    /**
     * Create the form for the root page. Number of columns there is equal to
     * the number of links.
     * 
     * @param pageBook
     */
    public void createPartControl(ScrolledPageBook mainPageBook,
            SharedStyleManager shardStyleManager) {
        // first, create the root page style manager from shared style manager.
        rootPageStyleManager = new PageStyleManager(rootPage, shardStyleManager
                .getProperties());

        // Set title of Main form from root page title.
        parentForm.setText(rootPage.getTitle());

        // Composite for full root page. It has custom layout, and two
        // children: the content composite and the description label.
        Composite rootPageComposite = new PageComposite(mainPageBook
                .getContainer(), SWT.NULL);
        toolkit.adapt(rootPageComposite);

        mainPageBook.registerPage(rootPage.getId(), rootPageComposite);
        rootPageComposite.setLayout(new RootPageLayout());
        // Util.highlight(pageComposite, SWT.COLOR_DARK_CYAN);

        // create the contents composite in the center of the root page.
        createRootPageContent(rootPageComposite);

        // create description label for links description.
        descriptionLabel = createHoverLabel(rootPageComposite);

        // Clear memory. No need for style manager any more.
        rootPageStyleManager = null;
    }

    /**
     * Creates content of the root page.
     */
    private void createRootPageContent(Composite rootPageComposite) {
        // setup page composite/layout
        Composite contentComposite = toolkit.createComposite(rootPageComposite);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        contentComposite.setLayoutData(gd);
        AbstractIntroElement[] children = (AbstractIntroElement[]) rootPage
                .getChildrenOfType(AbstractIntroElement.GROUP
                        | AbstractIntroElement.LINK);
        int numChildren = children.length;
        GridLayout layout = new GridLayout();
        // separate links a bit more.
        layout.horizontalSpacing = 20;
        layout.verticalSpacing = 20;
        // set number of columns.
        int numColumns = rootPageStyleManager.getPageNumberOfColumns();
        numColumns = numColumns < 1 ? numChildren : numColumns;
        layout.numColumns = numColumns;
        contentComposite.setLayout(layout);
        for (int i = 0; i < children.length; i++) {
            if (children[i].getType() == AbstractIntroElement.GROUP)
                createGroupContent(contentComposite, (IntroGroup) children[i]);
            else if (children[i].getType() == AbstractIntroElement.LINK)
                createImageHyperlink(contentComposite, (IntroLink) children[i]);
        }
    }

    /**
     * Creates content of the root page.
     */
    private void createGroupContent(Composite parent, IntroGroup group) {
        // setup page composite/layout
        Composite contentComposite = toolkit.createComposite(parent);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gd.horizontalSpan = rootPageStyleManager.getColSpan(group);
        gd.verticalSpan = rootPageStyleManager.getRowSpan(group);
        contentComposite.setLayoutData(gd);
        AbstractIntroElement[] children = (AbstractIntroElement[]) group
                .getChildrenOfType(AbstractIntroElement.GROUP
                        | AbstractIntroElement.LINK);
        int numChildren = children.length;
        GridLayout layout = new GridLayout();
        // separate links a bit more.
        layout.horizontalSpacing = 20;
        // set number of columns.
        int numColumns = rootPageStyleManager.getNumberOfColumns(group);
        numColumns = numColumns < 1 ? numChildren : numColumns;
        layout.numColumns = numColumns;
        contentComposite.setLayout(layout);
        for (int i = 0; i < children.length; i++) {
            if (children[i].getType() == AbstractIntroElement.GROUP)
                createGroupContent(contentComposite, (IntroGroup) children[i]);
            else if (children[i].getType() == AbstractIntroElement.LINK)
                createImageHyperlink(contentComposite, (IntroLink) children[i]);
        }
    }

    /**
     * Creates an Image Hyperlink from an IntroLink. Model object is cached in
     * link.
     * 
     * @param body
     * @param link
     */
    private void createImageHyperlink(Composite parent, IntroLink link) {
        // create the container composite that will hold the imageHyperLink and
        // the label for the description.
        Composite container = toolkit.createComposite(parent);
        // Util.highlight(container, SWT.COLOR_CYAN);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        gd.horizontalSpan = rootPageStyleManager.getColSpan(link);
        gd.verticalSpan = rootPageStyleManager.getRowSpan(link);
        container.setLayoutData(gd);

        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        container.setLayout(layout);
        ImageHyperlink imageLink = toolkit.createImageHyperlink(container,
                SWT.NULL);
        imageLink.setImage(rootPageStyleManager.getImage(link, "link-icon", //$NON-NLS-1$
                ImageUtil.DEFAULT_ROOT_LINK));
        imageLink.setHoverImage(rootPageStyleManager.getImage(link,
                "hover-icon", null)); //$NON-NLS-1$
        // each link is centered in cell.
        gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        imageLink.setLayoutData(gd);
        // cache the intro link model object for description and URL.
        imageLink.setData(INTRO_LINK, link);
        imageLink.addHyperlinkListener(hyperlinkAdapter);

        // description label.
        Label linkLabel = toolkit.createLabel(container, link.getLabel());
        gd = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
        linkLabel.setFont(PageStyleManager.getDefaultFont());
        linkLabel.setLayoutData(gd);
    }

    /**
     * Creates a label to display the link description when you hover over a
     * hyperlink.
     * 
     * @param body
     */
    private Label createHoverLabel(Composite body) {
        Label label = toolkit.createLabel(body, "", SWT.WRAP); //$NON-NLS-1$
        String key = StringUtil.concat(rootPage.getId(), ".", "hover-text.fg")
                .toString();
        Color fg = rootPageStyleManager.getColor(toolkit, key);
        if (fg == null)
            fg = toolkit.getColors().getColor(FormColors.TITLE);
        label.setForeground(fg);
        label.setAlignment(SWT.CENTER);
        label.setFont(PageStyleManager.getDefaultFont());
        return label;
    }
}