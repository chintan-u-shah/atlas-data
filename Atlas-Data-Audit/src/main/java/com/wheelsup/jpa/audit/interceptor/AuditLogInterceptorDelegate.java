package com.wheelsup.jpa.audit.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This delegate is for opening new Transactions within the AuditLogInterceptor, due to how Spring proxies work, this method cannot be in the same file.
 */
@Component
public class AuditLogInterceptorDelegate {
	@PersistenceContext
	private EntityManager entityManager;

	private static Logger logger = Logger.getLogger(AuditLogInterceptor.class.getName());

	/**
	 * Creates an AuditLog, outside the session of the original transaction. In Hibernate 5.2+, the session's transaction has already committed, so a new one has to be opened.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void persistAuditLogHibernate5(SavableAuditLogs savableAuditLogs) {
		try {
			for (SavableAuditLogs.SavableAuditLog savableAuditLog : savableAuditLogs.getUnique()) {
				if (savableAuditLog.isSavable()) {
					entityManager.persist(savableAuditLog.getAuditLog());
				}
			}
		}
		catch (Throwable e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
		finally {
			savableAuditLogs.clear();
		}
	}
}
