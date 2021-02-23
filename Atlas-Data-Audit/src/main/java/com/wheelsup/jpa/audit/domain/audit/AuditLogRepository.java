package com.wheelsup.jpa.audit.domain.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Repository
@Transactional
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {


	List<AuditLog> findAllByTableNameAndEntityIdAndOperationTypeInOrderByAuditDateDesc(String tableName, long entityId, Collection<AuditLog.OperationType> operationTypes);

	List<AuditLog> findAllByTableNameAndEntityIdInAndOperationTypeInOrderByAuditDateDesc(String tableName, Collection<Long> entityIds, Collection<AuditLog.OperationType> operationTypes);

	long countByTableNameAndEntityIdInAndOperationTypeIn(String tableName, Collection<Long> entityIds, Collection<AuditLog.OperationType> operationTypes);

	long countByTableNameAndEntityIdAndOperationTypeIn(String tableName, long entityId, Collection<AuditLog.OperationType> operationTypes);

	List<AuditLog> findByOperationType(AuditLog.OperationType operationType);

	List<AuditLog> findAllByTableNameAndEntityIdAndAuditDateBetween(String tableName, long entityId, Date fromDate, Date toDate);

	List<AuditLog> findAllByAuditDateBetweenAndOperationTypeIn(Date fromDate, Date toDate, Collection<AuditLog.OperationType> operationTypes);
}
