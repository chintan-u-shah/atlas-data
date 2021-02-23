package com.wheelsup.jpa.audit.interceptor;

import com.wheelsup.jpa.audit.domain.audit.AuditLog;
import com.wheelsup.jpa.audit.domain.audit.AuditLogDetails;
import com.wheelsup.jpa.audit.interceptor.audit.Audited;
import com.wheelsup.jpa.audit.interceptor.audit_user.CurrentAuditUserProvider;
import com.wheelsup.jpa.audit.interceptor.field.AuditDiscriminatorField;
import com.wheelsup.jpa.audit.interceptor.field.AuditField;
import com.wheelsup.jpa.audit.interceptor.field.AuditInitialStateField;
import com.wheelsup.jpa.audit.interceptor.field.AuditUpdatedField;
import com.wheelsup.jpa.audit.service.utils.ModelReflectionUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.Type;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import java.beans.IntrospectionException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.wheelsup.jpa.audit.domain.audit.AuditLog.OperationType.*;
import static com.wheelsup.jpa.audit.service.utils.ModelValueUtils.getFormattedValue;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuditLogInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = -4687115248766564845L;

	private static Logger logger = Logger.getLogger(AuditLogInterceptor.class.getName());

	public static final String AUDIT_ADDITIONAL_INFO = "AUDIT_EXTRA_USER_NAME";

	@PersistenceContext
	private EntityManager entityManager;

	private final AuditLogInterceptorDelegate delegate;
	private final CurrentAuditUserProvider currentAuditUserProvider;

	@PostConstruct
	public void init() {
		StaticDelegateInterceptor.register(this);
	}

	private final SavableAuditLogs savableAuditLogs = new SavableAuditLogs();


	@Override
	public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (isActive()) {
			createInitialStateAuditLog(entity, state, propertyNames);
			createFinalStateAuditLog(entity, DELETE);
		}
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		logger.log(Level.ALL, "onSave. isActive() = " + isActive());
		if (isActive()) {
			createFinalStateAuditLog(entity, CREATE);
			createInitialStateAuditLog(entity, state, propertyNames);
		}

		return false;
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		if (isActive()) {
			createInitialStateAuditLog(entity, previousState, propertyNames);
			createUpdateAuditLog(entity, currentState, previousState, propertyNames);
		}

		return false;
	}

	@Override
	public void onCollectionUpdate(Object collection, Serializable parentId) throws CallbackException {
		if (isActive()) {
			addParentColumnValue(collection, parentId);
		}
	}

	@Override
	public void onCollectionRecreate(Object collection, Serializable parentId) throws CallbackException {
		if (isActive()) {
			addParentColumnValue(collection, parentId);
		}
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		synchronized (this) {
			if (savableAuditLogs.isNotEmpty()) {
				if (StringUtils.startsWith(Version.getVersionString(), "5.2")) {
					delegate.persistAuditLogHibernate5(savableAuditLogs);
				}
				else {
					persistAuditLogHibernate4();
				}
			}
		}
	}

	/**
	 * Creates an AuditLog, outside the session of the original transaction. Only works for older versions of Hibernate, because the transaction is kept open.
	 */
	void persistAuditLogHibernate4() {
		Session session = entityManager.unwrap(Session.class).getSessionFactory().openSession();
		try {
			for (SavableAuditLogs.SavableAuditLog savableAuditLog : savableAuditLogs.getUnique()) {
				if (savableAuditLog.isSavable()) {
					session.saveOrUpdate(savableAuditLog.getAuditLog());
				}
			}
		}
		catch (Throwable e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
		finally {
			session.flush();
			if (session.isOpen()) {
				session.disconnect();
			}
			savableAuditLogs.clear();
		}
	}

	private void createInitialStateAuditLog(final Object entity, Object[] state, String[] propertyNames) {
		try {
			if (ModelReflectionUtils.isEntityAuditable(entity.getClass())) {

				AuditLog auditLog = createAuditLogForEntity(entity);

				if (auditLog != null) {
					Set<AuditLogDetails> auditLogDetails = getInitialStateAuditLogDetails(auditLog, entity, propertyNames, state);
					if (CollectionUtils.isNotEmpty(auditLogDetails)) {
						auditLog.setOperationType(INITIAL_STATE);
						auditLog.setAuditLogDetails(auditLogDetails);
						auditLog.setAuditUserId(currentAuditUserProvider.getAudiUserId());
						auditLog.setAuditUserType(currentAuditUserProvider.getAudiUserType());

						savableAuditLogs.add(auditLog, entity, new SavableAuditLogs.UnicityChecker() {
							@Override
							public boolean isUnique() {
								return !operationAlreadyExists(entity, INITIAL_STATE);
							}
						});
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
	}

	private void createFinalStateAuditLog(final Object entity, final AuditLog.OperationType operationType) {
		try {
			if (ModelReflectionUtils.isEntityAuditable(entity.getClass())) {

				AuditLog auditLog = createAuditLogForEntity(entity);

				if (auditLog != null) {
					auditLog.setOperationType(operationType);

					savableAuditLogs.add(auditLog, entity, new SavableAuditLogs.UnicityChecker() {
						@Override
						public boolean isUnique() {
							return !operationAlreadyExists(entity, operationType);
						}
					});
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
	}

	private void createUpdateAuditLog(Object entity, Object[] currentState, Object[] previousState, String[] propertyNames) {
		try {
			if (ModelReflectionUtils.isEntityAuditable(entity.getClass())) {

				AuditLog auditLog = createAuditLogForEntity(entity);

				if (auditLog != null) {
					Set<AuditLogDetails> auditLogDetails = getUpdateAuditLogDetails(auditLog, entity, propertyNames, previousState, currentState);

					if (CollectionUtils.isNotEmpty(auditLogDetails)) {
						auditLog.setOperationType(UPDATE);
						auditLog.setAuditLogDetails(auditLogDetails);

						savableAuditLogs.add(auditLog, entity);
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
	}

	/**
	 * Because AuditLog contains AuditLogDetails as a Set, this loop only supports List representation;
	 * instead of using Sets, should use FetchType to force unique elements
	 */
	private void addParentColumnValue(Object collection, Serializable parentId) {
		try {
			if (PersistentBag.class.isInstance(collection)) {
				PersistentBag bag = PersistentBag.class.cast(collection);
				if (CollectionUtils.isNotEmpty(bag)) {
					for (Object entity : bag) {
						addAuditLogDetails(parentId, entity);
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
	}

	private void addAuditLogDetails(Serializable parentId, Object entity) throws Exception {
		if (ModelReflectionUtils.isEntityAuditable(entity.getClass())) {

			Audited.DependsOn dependsOn = ModelReflectionUtils.auditableDependsOn(entity.getClass());
			if (dependsOn != null) {

				String parentColumn = dependsOn.column();
				if (StringUtils.isNotEmpty(parentColumn)) {

					List<AuditLog> initialStateEntityAuditLogs = getInitialStateEntityAuditLogs(entity);

					for (AuditLog auditLog : initialStateEntityAuditLogs) {
						if (auditLog.getAuditLogDetails() == null) {
							auditLog.setAuditLogDetails(new TreeSet<>());
						}

						AuditLogDetails details = new AuditLogDetails();
						details.setColumnName(parentColumn);
						details.setCurrentValue(getFormattedValue(parentId, Long.class));

						auditLog.getAuditLogDetails().add(details);
					}
				}
			}
		}
	}

	private List<AuditLog> getInitialStateEntityAuditLogs(Object entity) {
		List<AuditLog> auditLogs = new ArrayList<>();

		String tableName = ModelReflectionUtils.getTableName(entity.getClass());
		if (StringUtils.isNotEmpty(tableName)) {
			for (SavableAuditLogs.SavableAuditLog savableAuditLog : savableAuditLogs.getAll()) {
				AuditLog sAuditLog = savableAuditLog.getAuditLog();
				Object sEntity = savableAuditLog.getEntity();

				if (sAuditLog.getOperationType() == INITIAL_STATE) {
					if (StringUtils.equals(tableName, sAuditLog.getTableName())) {
						if (Objects.equals(ModelReflectionUtils.getEntityId(entity), ModelReflectionUtils.getEntityId(sEntity))) {
							auditLogs.add(sAuditLog);
						}
					}
				}
			}
		}


		return auditLogs;
	}

	private Set<AuditLogDetails> getInitialStateAuditLogDetails(AuditLog auditLog, Object entity, String[] propertyNames, Object[] state) throws Exception {
		if (ArrayUtils.isNotEmpty(propertyNames) && ArrayUtils.isNotEmpty(state)) {
			List<? extends AuditField> initialStateAuditFields = getInitialStateAuditFields(entity, propertyNames, state);

			AuditDiscriminatorField discriminatorField = new AuditDiscriminatorField(entityManager.unwrap(Session.class).getSessionFactory().getClassMetadata(entity.getClass()));

			List<AuditField> auditFields = new ArrayList<>();
			auditFields.addAll(initialStateAuditFields);
			auditFields.add(discriminatorField);

			return createAuditLogDetails(auditFields);
		}

		return Collections.emptySet();
	}

	private Set<AuditLogDetails> getUpdateAuditLogDetails(AuditLog auditLog, Object entity, String[] propertyNames, Object[] previousState, Object[] currentState) throws Exception {
		if (ArrayUtils.isNotEmpty(propertyNames) && ArrayUtils.isNotEmpty(previousState) && ArrayUtils.isNotEmpty(currentState)) {

			List<? extends AuditField> auditFields = getAuditUpdatedFields(entity, propertyNames, previousState, currentState);

			return createAuditLogDetails(auditFields);
		}

		return Collections.emptySet();
	}

	private Set<AuditLogDetails> createAuditLogDetails(List<? extends AuditField> auditFields) throws Exception {
		Set<AuditLogDetails> auditLogDetails = new HashSet<>();

		for (AuditField auditField : auditFields) {
			if (auditField.toBeAudit()) {
				AuditLogDetails details = new AuditLogDetails();

				details.setColumnName(auditField.getColumnName());
				details.setPreviousValue(getFormattedValue(auditField.getPreviousValue(), auditField.getType()));
				details.setCurrentValue(getFormattedValue(auditField.getCurrentValue(), auditField.getType()));

				auditLogDetails.add(details);
			}
		}

		return auditLogDetails;
	}

	private List<AuditInitialStateField> getInitialStateAuditFields(Object entity, String[] propertyNames, Object[] state) {
		List<AuditInitialStateField> auditFields = new ArrayList<>();

		Map<String, Field> classFields = ModelReflectionUtils.getClassFields(entity.getClass());

		for (int i = 0; i < propertyNames.length; i++) {
			String propertyName = propertyNames[i];

			if (classFields.containsKey(propertyName)) {
				auditFields.add(new AuditInitialStateField(classFields.get(propertyName), state[i]));
			}
		}

		return auditFields;
	}

	private List<AuditUpdatedField> getAuditUpdatedFields(Object entity, String[] propertyNames, Object[] previousState, Object[] currentState) throws IntrospectionException {
		List<AuditUpdatedField> auditUpdatedFields = new ArrayList<>();

		Map<String, Field> classFields = ModelReflectionUtils.getClassFields(entity.getClass());

		for (int i = 0; i < propertyNames.length; i++) {
			String propertyName = propertyNames[i];

			if (classFields.containsKey(propertyName)) auditUpdatedFields.add(new AuditUpdatedField(classFields.get(propertyName), previousState[i], currentState[i]));
		}

		return auditUpdatedFields;
	}

	private AuditLog createAuditLogForEntity(Object entity) {
		String tableName = ModelReflectionUtils.getTableName(entity.getClass());

			if (StringUtils.isNotEmpty(tableName)) {
				AuditLog auditLog = new AuditLog();
				auditLog.setAuditDate(DateTime.now().toDate());
				auditLog.setAuditUserId(currentAuditUserProvider.getAudiUserId());
				auditLog.setTableName(tableName);
				auditLog.setAuditUserType(currentAuditUserProvider.getAudiUserType());
				auditLog.setAdditionalInfo(getAdditionalInfo());

				return auditLog;
			}

		return null;
	}

	private boolean operationAlreadyExists(Object entity, AuditLog.OperationType operationType) {

		String tableName = ModelReflectionUtils.getTableName(entity.getClass());
		if (StringUtils.isNotEmpty(tableName)) {

			Long entityId = ModelReflectionUtils.getEntityId(entity);
			if (entityId != null) {

				synchronized (this) {
					Session session = entityManager.unwrap(Session.class).getSessionFactory().openSession();
					try {
						Criteria criteria = session.createCriteria(AuditLog.class);

						Conjunction andClause = Restrictions.conjunction();
						andClause.add(Restrictions.eq("tableName", tableName));
						andClause.add(Restrictions.eq("entityId", entityId));
						andClause.add(Restrictions.eq("operationType", operationType));

						criteria.add(andClause);

						return CollectionUtils.isNotEmpty(criteria.list());
					}
					finally {
						if(session != null && session.isOpen()) {
							session.disconnect();
						}
					}
				}
			}
		}

		return false;
	}

	private String getAdditionalInfo() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

		if (requestAttributes != null) {
			HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

			if (request != null) {
				String additionalInfo = (String) request.getAttribute(AUDIT_ADDITIONAL_INFO);

				if (StringUtils.isNotEmpty(additionalInfo)) {
					return additionalInfo;
				}
			}
		}

		return null;
	}

	private boolean isActive() {
		return entityManager.unwrap(Session.class).getSessionFactory() != null;
	}
}
