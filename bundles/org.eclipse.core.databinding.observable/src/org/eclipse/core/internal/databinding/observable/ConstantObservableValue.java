/*******************************************************************************
 * Copyright (c) 2005, 2015 Matt Carter and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Matt Carter - initial API and implementation (bug 212518)
 *     Matthew Hall - bug 212518, 146397, 249526
 *     Stefan Xenos <sxenos@gmail.com> - Bug 335792
 *******************************************************************************/
package org.eclipse.core.internal.databinding.observable;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IStaleListener;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.Assert;

/**
 * An immutable {@link IObservableValue}.
 *
 * @param <T>
 *            the type of the value being observed
 *
 * @see WritableValue
 */
public class ConstantObservableValue<T> implements IObservableValue<T> {
	final Realm realm;
	final T value;
	final Object type;

	/**
	 * Construct a constant value of the given type, in the default realm.
	 *
	 * @param value
	 *            immutable value
	 * @param type
	 *            type
	 */
	public ConstantObservableValue(T value, Object type) {
		this(Realm.getDefault(), value, type);
	}

	/**
	 * Construct a constant value of the given type, in the given realm.
	 *
	 * @param realm
	 *            Realm
	 * @param value
	 *            immutable value
	 * @param type
	 *            type
	 */
	public ConstantObservableValue(Realm realm, T value, Object type) {
		Assert.isNotNull(realm, "Realm cannot be null"); //$NON-NLS-1$
		this.realm = realm;
		this.value = value;
		this.type = type;
		ObservableTracker.observableCreated(this);
	}

	@Override
	public Object getValueType() {
		return type;
	}

	@Override
	public T getValue() {
		ObservableTracker.getterCalled(this);
		return value;
	}

	@Override
	public void setValue(T value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addValueChangeListener(IValueChangeListener<? super T> listener) {
		// ignore
	}

	@Override
	public void removeValueChangeListener(IValueChangeListener<? super T> listener) {
		// ignore
	}

	@Override
	public void addChangeListener(IChangeListener listener) {
		// ignore
	}

	@Override
	public void addDisposeListener(IDisposeListener listener) {
		// ignore
	}

	@Override
	public void addStaleListener(IStaleListener listener) {
		// ignore
	}

	@Override
	public boolean isDisposed() {
		return false;
	}

	@Override
	public void dispose() {
		// nothing to dispose
	}

	@Override
	public Realm getRealm() {
		return realm;
	}

	@Override
	public boolean isStale() {
		return false;
	}

	@Override
	public void removeChangeListener(IChangeListener listener) {
		// ignore
	}

	@Override
	public void removeDisposeListener(IDisposeListener listener) {
		// ignore
	}

	@Override
	public void removeStaleListener(IStaleListener listener) {
		// ignore
	}
}