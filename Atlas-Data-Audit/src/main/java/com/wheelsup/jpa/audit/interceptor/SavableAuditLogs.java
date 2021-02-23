package com.wheelsup.jpa.audit.interceptor;

import com.wheelsup.jpa.audit.domain.audit.AuditLog;
import com.wheelsup.jpa.audit.service.utils.ModelReflectionUtils;

import java.util.*;

public class SavableAuditLogs {

	private final List<SavableAuditLog> savableAuditLogs = new ArrayList<>();

	public synchronized void add(AuditLog auditLog, Object entity) {
		savableAuditLogs.add(new SavableAuditLog(auditLog, entity, UnicityChecker.NULL));
	}

	public synchronized void add(AuditLog auditLog, Object entity, UnicityChecker checker) {
		savableAuditLogs.add(new SavableAuditLog(auditLog, entity, checker));
	}

	public synchronized Collection<SavableAuditLog> getUnique() {
		Set<SavableAuditLog> uniqueAuditLogs = new HashSet<>();

		for (SavableAuditLog savableAuditLog : savableAuditLogs) {
			if (savableAuditLog.resetEntityId().getAuditLog().getEntityId() != null) {
				uniqueAuditLogs.add(savableAuditLog);
			}
		}

		return uniqueAuditLogs;
	}

	public synchronized void clear() {
		savableAuditLogs.clear();
	}

	public synchronized boolean isEmpty() {
		return savableAuditLogs.isEmpty();
	}

	public synchronized boolean isNotEmpty() {
		return !savableAuditLogs.isEmpty();
	}

	public synchronized List<SavableAuditLog> getAll() {
		return savableAuditLogs;
	}

	public static class SavableAuditLog {

		private final Object entity;
		private final AuditLog auditLog;
		private final UnicityChecker checker;


		SavableAuditLog(AuditLog auditLog, Object entity, UnicityChecker checker) {
			this.auditLog = auditLog;
			this.entity = entity;
			this.checker = checker;
		}

		SavableAuditLog resetEntityId() {
			auditLog.setEntityId(ModelReflectionUtils.getEntityId(entity));

			return this;
		}

		boolean isSavable() {
			return checker.isUnique();
		}

		public AuditLog getAuditLog() {
			return auditLog;
		}

		public Object getEntity() {
			return entity;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			SavableAuditLog that = (SavableAuditLog) o;
			return Objects.equals(auditLog, that.auditLog);
		}

		@Override
		public int hashCode() {
			return Objects.hash(auditLog);
		}
	}

	interface UnicityChecker {
		boolean isUnique();

		UnicityChecker NULL = new UnicityChecker() {

			@Override
			public boolean isUnique() {
				return true;
			}
		};
	}
}
