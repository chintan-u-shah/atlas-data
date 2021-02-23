package com.wheelsup.jpa.audit.domain.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface AuditLogDetailsRepository extends JpaRepository<AuditLogDetails, Long> {


	@Query(
			value = "  SELECT distinct log.entityId " +
					"  FROM   AuditLogDetails details INNER JOIN AuditLog log ON details.auditLogId = log.auditLogId " +
					"  WHERE  " +
					"         log.operationType = 'INITIAL_STATE' " +
					"  AND    log.tableName = ?1 " +
					"  AND    details.columnName = ?2 " +
					"  AND    details.currentValue = ?3 ",
			nativeQuery = true
	)
	List<BigInteger> findInitialStateRemoteEntityIds(String tableName, String manyToOneColumnName, String manyToOneColumnValue);

	@Query(
			value = "  SELECT distinct details.currentValue " +
					"  FROM   AuditLogDetails details INNER JOIN AuditLog log ON details.auditLogId = log.auditLogId " +
					"  WHERE  " +
					"         log.operationType = 'INITIAL_STATE' " +
					"  AND    log.tableName = ?1 " +
					"  AND    log.entityId = ?2 " +
					"  AND    details.columnName = ?3 ",
			nativeQuery = true
	)

	String findInitialStateEntityColumnValue(String tableName, Long entityId, String columnName);
}
