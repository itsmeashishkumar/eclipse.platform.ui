/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.internal.e4.compatibility;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.ui.IPlaceholderFolderLayout;

public class ModeledPlaceholderFolderLayout implements IPlaceholderFolderLayout {

	protected MApplication application;
	protected MPartStack folderModel;
	protected ModeledPageLayout layout;

	public ModeledPlaceholderFolderLayout(ModeledPageLayout l, MApplication application,
			MPartStack stackModel) {
		this.application = application;
		folderModel = stackModel;
		layout = l;
	}

	public void addPlaceholder(String viewId) {
		MUIElement existingView = layout.findElement(layout.perspModel, viewId);
		if (existingView instanceof MPlaceholder) {
			existingView.getParent().getChildren().remove(existingView);
		}

		MStackElement viewModel = ModeledPageLayout.createViewModel(application, viewId, false,
				layout.page, layout.partService, layout.createReferences);
		folderModel.getChildren().add(viewModel);
	}

	public String getProperty(String id) {
		Object propVal = null;
		return propVal == null ? "" : propVal.toString(); //$NON-NLS-1$
	}

	public void setProperty(String id, String value) {
		// folderModel.setProperty(id, value);
	}

}
