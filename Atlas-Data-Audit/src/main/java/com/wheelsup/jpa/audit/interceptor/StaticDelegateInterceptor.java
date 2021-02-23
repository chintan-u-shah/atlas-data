package com.wheelsup.jpa.audit.interceptor;

import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StaticDelegateInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = 8746438602867192014L;

	private static Map<Class<?>, Interceptor> interceptors = new HashMap<>();

	public static synchronized void register(Interceptor interceptor) {
		interceptors.put(interceptor.getClass(), interceptor);
	}

	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		for (Interceptor interceptor : interceptors.values()) {
			interceptor.onDelete(entity, id, state, propertyNames, types);
		}

		super.onDelete(entity, id, state, propertyNames, types);
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		for (Interceptor interceptor : interceptors.values()) {
			interceptor.onSave(entity, id, state, propertyNames, types);
		}

		return super.onSave(entity, id, state, propertyNames, types);
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		for (Interceptor interceptor : interceptors.values()) {
			interceptor.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
		}

		return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
	}

	@Override
	public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
		for (Interceptor interceptor : interceptors.values()) {
			interceptor.onCollectionRemove(collection, key);
		}

		super.onCollectionRemove(collection, key);
	}

	@Override
	public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
		for (Interceptor interceptor : interceptors.values()) {
			interceptor.onCollectionRecreate(collection, key);
		}

		super.onCollectionRecreate(collection, key);
	}

	@Override
	public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
		for (Interceptor interceptor : interceptors.values()) {
			interceptor.onCollectionUpdate(collection, key);
		}

		super.onCollectionUpdate(collection, key);
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		for (Interceptor interceptor : interceptors.values()) {
			interceptor.afterTransactionCompletion(tx);
		}

		super.afterTransactionCompletion(tx);
	}
}
