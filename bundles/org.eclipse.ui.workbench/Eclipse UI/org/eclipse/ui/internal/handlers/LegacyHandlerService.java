/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.internal.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.expressions.ElementHandler;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.commands.internal.HandlerServiceImpl;
import org.eclipse.e4.core.contexts.ContextFunction;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.internal.workbench.Activator;
import org.eclipse.e4.ui.internal.workbench.Policy;
import org.eclipse.e4.ui.workbench.modeling.ExpressionContext;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.e4.compatibility.E4Util;
import org.eclipse.ui.internal.expressions.AndExpression;
import org.eclipse.ui.internal.expressions.WorkbenchWindowExpression;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;
import org.eclipse.ui.services.ISourceProviderService;

/**
 * @since 3.5
 * 
 */
public class LegacyHandlerService implements IHandlerService {

	private static final String[] SELECTION_VARIABLES = { ISources.ACTIVE_CURRENT_SELECTION_NAME,
			ISources.ACTIVE_FOCUS_CONTROL_ID_NAME, ISources.ACTIVE_FOCUS_CONTROL_NAME,
			ISources.ACTIVE_MENU_EDITOR_INPUT_NAME, ISources.ACTIVE_MENU_NAME,
			ISources.ACTIVE_MENU_SELECTION_NAME };

	final static String LEGACY_H_ID = "legacy::handler::"; //$NON-NLS-1$

	static class HandlerSelectionFunction extends ContextFunction {

		private final String commandId;

		/**
		 * 
		 */
		public HandlerSelectionFunction(String commandId) {
			this.commandId = commandId;
		}

		@Override
		public Object compute(IEclipseContext context) {

			List<HandlerActivation> handlerActivations = (List<HandlerActivation>) context
					.get(LEGACY_H_ID + commandId);

			if (handlerActivations == null) {
				return null;
			}

			HandlerActivation bestActivation = null;

			ExpressionContext legacyEvalContext = new ExpressionContext(context);

			for (HandlerActivation handlerActivation : handlerActivations) {
				if (!handlerActivation.participating)
					continue;
				if (handlerActivation.evaluate(legacyEvalContext)) {
					if (bestActivation == null) {
						bestActivation = handlerActivation;
					} else {
						int comparison = bestActivation.compareTo(handlerActivation);
						if (comparison < 0) {
							bestActivation = handlerActivation;
						}
					}
				}
			}

			if (bestActivation != null) {
				return bestActivation.proxy;
			}

			// "super call"
			IEclipseContext parent = context.getParent();
			if (parent == null) {
				return null;
			}
			return parent.get(HandlerServiceImpl.H_ID + commandId);
		}
	}

	public static IHandlerActivation registerLegacyHandler(final IEclipseContext context,
			String id, final String cmdId, IHandler handler, Expression activeWhen) {
		ECommandService cs = (ECommandService) context.get(ECommandService.class.getName());
		Command command = cs.getCommand(cmdId);
		E4HandlerProxy handlerProxy = new E4HandlerProxy(command, handler);
		HandlerActivation activation = new HandlerActivation(context, cmdId, handler, handlerProxy,
				activeWhen);
		addHandlerActivation(activation);
		EHandlerService hs = context.get(EHandlerService.class);
		hs.activateHandler(cmdId, new HandlerSelectionFunction(cmdId));
		return activation;
	}

	static void addHandlerActivation(HandlerActivation eActivation) {
		List handlerActivations = (List) eActivation.context.getLocal(LEGACY_H_ID
				+ eActivation.getCommandId());
		if (handlerActivations == null) {
			handlerActivations = new ArrayList();
		} else {
			handlerActivations = new ArrayList(handlerActivations);
		}
		handlerActivations.add(eActivation);
		// setting this so that we trigger invalidations
		eActivation.context.set(LEGACY_H_ID + eActivation.getCommandId(), handlerActivations);
	}

	static void removeHandlerActivation(HandlerActivation eActivation) {
		List handlerActivations = (List) eActivation.context.getLocal(LEGACY_H_ID
				+ eActivation.getCommandId());
		if (handlerActivations == null) {
			handlerActivations = new ArrayList();
		} else {
			handlerActivations = new ArrayList(handlerActivations);
		}
		handlerActivations.remove(eActivation);
		// setting this so that we trigger invalidations
		eActivation.context.set(LEGACY_H_ID + eActivation.getCommandId(), handlerActivations);
	}

	private IEclipseContext eclipseContext;
	private IEvaluationContext evalContext;
	private Expression defaultExpression = null;

	public LegacyHandlerService(IEclipseContext context) {
		eclipseContext = context;
		evalContext = new ExpressionContext(eclipseContext);
		IWorkbenchWindow window = (IWorkbenchWindow) eclipseContext.get(IWorkbenchWindow.class
				.getName());
		if (window != null) {
			defaultExpression = new WorkbenchWindowExpression(window);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.services.IServiceWithSources#addSourceProvider(org.eclipse
	 * .ui.ISourceProvider)
	 */
	public void addSourceProvider(ISourceProvider provider) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.services.IServiceWithSources#removeSourceProvider(org.
	 * eclipse.ui.ISourceProvider)
	 */
	public void removeSourceProvider(ISourceProvider provider) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.services.IDisposable#dispose()
	 */
	public void dispose() {
		E4Util.message("LegacyHandlerService.dispose: should it do something?"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#activateHandler(org.eclipse.ui
	 * .handlers.IHandlerActivation)
	 */
	public IHandlerActivation activateHandler(IHandlerActivation activation) {
		HandlerActivation handlerActivation = (HandlerActivation) activation;
		handlerActivation.participating = true;
		addHandlerActivation(handlerActivation);
		return activation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#activateHandler(java.lang.String,
	 * org.eclipse.core.commands.IHandler)
	 */
	public IHandlerActivation activateHandler(String commandId, IHandler handler) {
		return activateHandler(commandId, handler, defaultExpression, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#activateHandler(java.lang.String,
	 * org.eclipse.core.commands.IHandler,
	 * org.eclipse.core.expressions.Expression)
	 */
	public IHandlerActivation activateHandler(String commandId, IHandler handler,
			Expression expression) {
		return activateHandler(commandId, handler, expression, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#activateHandler(java.lang.String,
	 * org.eclipse.core.commands.IHandler,
	 * org.eclipse.core.expressions.Expression, boolean)
	 */
	public IHandlerActivation activateHandler(String commandId, IHandler handler,
			Expression expression, boolean global) {
		if (global || defaultExpression == null) {
			return registerLegacyHandler(eclipseContext, commandId, commandId, handler, expression);
		}
		AndExpression andExpr = new AndExpression();
		andExpr.add(expression);
		andExpr.add(defaultExpression);
		return registerLegacyHandler(eclipseContext, commandId, commandId, handler, andExpr);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#activateHandler(java.lang.String,
	 * org.eclipse.core.commands.IHandler,
	 * org.eclipse.core.expressions.Expression, int)
	 */
	public IHandlerActivation activateHandler(String commandId, IHandler handler,
			Expression expression, int sourcePriorities) {
		return activateHandler(commandId, handler, expression, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#createExecutionEvent(org.eclipse
	 * .core.commands.Command, org.eclipse.swt.widgets.Event)
	 */
	public ExecutionEvent createExecutionEvent(Command command, Event event) {
		ExpressionContext legacy = new ExpressionContext(eclipseContext);
		ExecutionEvent e = new ExecutionEvent(command, Collections.EMPTY_MAP, event, legacy);
		return e;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#createExecutionEvent(org.eclipse
	 * .core.commands.ParameterizedCommand, org.eclipse.swt.widgets.Event)
	 */
	public ExecutionEvent createExecutionEvent(ParameterizedCommand command, Event event) {
		ExpressionContext legacy = new ExpressionContext(eclipseContext);
		ExecutionEvent e = new ExecutionEvent(command.getCommand(), command.getParameterMap(),
				event, legacy);
		return e;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#deactivateHandler(org.eclipse
	 * .ui.handlers.IHandlerActivation)
	 */
	public void deactivateHandler(IHandlerActivation activation) {
		// null is not allowed, but some people put it anyway :( see bug 326406
		if (activation != null) {
			HandlerActivation eActivation = (HandlerActivation) activation;
			eActivation.participating = false;
			removeHandlerActivation(eActivation);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#deactivateHandlers(java.util.
	 * Collection)
	 */
	public void deactivateHandlers(Collection activations) {
		Object[] array = activations.toArray();
		// set all activations to not be participating first so that they ignore
		// the upcoming context change events
		for (int i = 0; i < array.length; i++) {
			((HandlerActivation) array[i]).participating = false;
		}

		for (int i = 0; i < array.length; i++) {
			deactivateHandler((IHandlerActivation) array[i]);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#executeCommand(java.lang.String,
	 * org.eclipse.swt.widgets.Event)
	 */
	public Object executeCommand(String commandId, Event event) throws ExecutionException,
			NotDefinedException, NotEnabledException, NotHandledException {
		ECommandService cs = eclipseContext.get(ECommandService.class);
		final Command command = cs.getCommand(commandId);
		return executeCommand(ParameterizedCommand.generateCommand(command, null), event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#executeCommand(org.eclipse.core
	 * .commands.ParameterizedCommand, org.eclipse.swt.widgets.Event)
	 */
	public Object executeCommand(ParameterizedCommand command, Event event)
			throws ExecutionException, NotDefinedException, NotEnabledException,
			NotHandledException {
		EHandlerService hs = eclipseContext.get(EHandlerService.class);
		return hs.executeHandler(command);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#executeCommandInContext(org.eclipse
	 * .core.commands.ParameterizedCommand, org.eclipse.swt.widgets.Event,
	 * org.eclipse.core.expressions.IEvaluationContext)
	 */
	public Object executeCommandInContext(ParameterizedCommand command, Event event,
			IEvaluationContext context) throws ExecutionException, NotDefinedException,
			NotEnabledException, NotHandledException {
		if (context instanceof ExpressionContext) {
			EHandlerService hs = (EHandlerService) ((ExpressionContext) context).eclipseContext
					.get(EHandlerService.class.getName());
			return hs.executeHandler(command);
		}
		return executeCommand(command, event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#createContextSnapshot(boolean)
	 */
	public IEvaluationContext createContextSnapshot(boolean includeSelection) {
		IEclipseContext targetContext = eclipseContext.getActiveLeaf();
		IEvaluationContext tmpContext = getCurrentState();
		IEvaluationContext context = new ExpressionContext(targetContext.createChild());

		if (includeSelection) {
			for (String variable : SELECTION_VARIABLES) {
				copyVariable(context, tmpContext, variable);
			}
		}

		ISourceProviderService sp = eclipseContext.get(ISourceProviderService.class);
		for (ISourceProvider provider : sp.getSourceProviders()) {
			String[] names = provider.getProvidedSourceNames();
			for (String name : names) {
				if (!isSelectionVariable(name)) {
					copyVariable(context, tmpContext, name);
				}
			}
		}

		return context;
	}

	private boolean isSelectionVariable(String name) {
		for (String variable : SELECTION_VARIABLES) {
			if (variable.equals(name)) {
				return true;
			}
		}
		return false;
	}

	private void copyVariable(IEvaluationContext context, IEvaluationContext tmpContext, String var) {
		Object o = tmpContext.getVariable(var);
		if (o != null) {
			context.addVariable(var, o);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.handlers.IHandlerService#getCurrentState()
	 */
	public IEvaluationContext getCurrentState() {
		return evalContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.handlers.IHandlerService#readRegistry()
	 */
	public void readRegistry() {
		readDefaultHandlers();
		readHandlers();
	}

	private void readHandlers() {
		IExtensionRegistry registry = (IExtensionRegistry) eclipseContext
				.get(IExtensionRegistry.class.getName());
		IExtensionPoint extPoint = registry
				.getExtensionPoint(IWorkbenchRegistryConstants.EXTENSION_HANDLERS);
		IConfigurationElement[] elements = extPoint.getConfigurationElements();
		for (IConfigurationElement configElement : elements) {
			String commandId = configElement
					.getAttribute(IWorkbenchRegistryConstants.ATT_COMMAND_ID);
			if (commandId == null || commandId.length() == 0) {
				continue;
			}
			String defaultHandler = configElement
					.getAttribute(IWorkbenchRegistryConstants.ATT_CLASS);
			if ((defaultHandler == null)
					&& (configElement.getChildren(IWorkbenchRegistryConstants.TAG_CLASS).length == 0)) {
				continue;
			}
			Expression activeWhen = null;
			final IConfigurationElement[] awChildren = configElement
					.getChildren(IWorkbenchRegistryConstants.TAG_ACTIVE_WHEN);
			if (awChildren.length > 0) {
				final IConfigurationElement[] subChildren = awChildren[0].getChildren();
				if (subChildren.length != 1) {
					Activator.trace(Policy.DEBUG_CMDS,
							"Incorrect activeWhen element " + commandId, null); //$NON-NLS-1$
					continue;
				}
				final ElementHandler elementHandler = ElementHandler.getDefault();
				final ExpressionConverter converter = ExpressionConverter.getDefault();
				try {
					activeWhen = elementHandler.create(converter, subChildren[0]);
				} catch (CoreException e) {
					Activator.trace(Policy.DEBUG_CMDS,
							"Incorrect activeWhen element " + commandId, e); //$NON-NLS-1$
				}
			}
			registerLegacyHandler(eclipseContext, commandId, commandId,
					new org.eclipse.ui.internal.handlers.HandlerProxy(commandId, configElement,
							IWorkbenchRegistryConstants.ATT_CLASS), activeWhen);
		}
	}

	private void readDefaultHandlers() {
		IExtensionRegistry registry = (IExtensionRegistry) eclipseContext
				.get(IExtensionRegistry.class.getName());
		IExtensionPoint extPoint = registry
				.getExtensionPoint(IWorkbenchRegistryConstants.EXTENSION_COMMANDS);
		IConfigurationElement[] elements = extPoint.getConfigurationElements();
		for (IConfigurationElement configElement : elements) {
			String id = configElement.getAttribute(IWorkbenchRegistryConstants.ATT_ID);
			if (id == null || id.length() == 0) {
				continue;
			}
			String defaultHandler = configElement
					.getAttribute(IWorkbenchRegistryConstants.ATT_DEFAULT_HANDLER);
			if ((defaultHandler == null)
					&& (configElement.getChildren(IWorkbenchRegistryConstants.TAG_DEFAULT_HANDLER).length == 0)) {
				continue;
			}
			registerLegacyHandler(eclipseContext, id, id,
					new org.eclipse.ui.internal.handlers.HandlerProxy(id, configElement,
							IWorkbenchRegistryConstants.ATT_DEFAULT_HANDLER), null);

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.handlers.IHandlerService#setHelpContextId(org.eclipse.
	 * core.commands.IHandler, java.lang.String)
	 */
	public void setHelpContextId(IHandler handler, String helpContextId) {
		// TODO Auto-generated method stub

	}
}
