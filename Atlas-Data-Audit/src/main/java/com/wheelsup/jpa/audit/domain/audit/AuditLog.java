package com.wheelsup.jpa.audit.domain.audit;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.joda.time.DateTime;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
@Getter

@Setter
@Entity
@Table(catalog = "${audit-log.db-schema}", name = "AuditLog")
public class AuditLog implements Serializable {
	private static final long serialVersionUID = -2077863529318997579L;

	public enum OperationType {
		CREATE, UPDATE, DELETE, INITIAL_STATE
	}

	@Id
	@GeneratedValue
	@Column(name = "auditLogId", unique = true)
	private Long id;

	private Date auditDate;

	private Long auditUserId;

	@Enumerated(EnumType.STRING)
	private AuditUserType auditUserType;

	@Enumerated(EnumType.STRING)
	private OperationType operationType;

	private String tableName;

	private Long entityId;

	private String additionalInfo;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "auditLogId")
	@Fetch(FetchMode.SUBSELECT)
	@OrderBy("columnName")
	private Set<AuditLogDetails> auditLogDetails;


	public void setAuditDate(Date auditDate) {
		this.auditDate = (auditDate == null ? null : new DateTime(auditDate).secondOfMinute().roundFloorCopy().toDate());
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		AuditLog that = (AuditLog) o;

		boolean whenInitialState = OperationType.INITIAL_STATE.equals(this.operationType) && OperationType.INITIAL_STATE.equals(that.operationType);
		boolean whenCreate = OperationType.CREATE.equals(this.operationType) && OperationType.CREATE.equals(that.operationType);
		boolean whenDelete = OperationType.DELETE.equals(this.operationType) && OperationType.DELETE.equals(that.operationType);
		boolean whenUpdate = OperationType.UPDATE.equals(this.operationType) && OperationType.UPDATE.equals(that.operationType);

		if (whenInitialState || whenCreate || whenDelete) {
			return Objects.equals(this.tableName, that.tableName) &&
					Objects.equals(this.entityId, that.entityId);
		}

		if (whenUpdate) {
			boolean sameUserUpdatingSameEntity =
					Objects.equals(this.tableName, that.tableName) &&
							Objects.equals(this.entityId, that.entityId) &&
							Objects.equals(this.auditUserId, that.auditUserId) &&
							Objects.equals(this.auditUserType, that.auditUserType);

			if (sameUserUpdatingSameEntity) {
				int thisAuditLogDetailsSize = (CollectionUtils.isEmpty(this.auditLogDetails) ? 0 : this.auditLogDetails.size());
				int thatAuditLogDetailsSize = (CollectionUtils.isEmpty(that.auditLogDetails) ? 0 : that.auditLogDetails.size());

				if (thisAuditLogDetailsSize == thatAuditLogDetailsSize) {
					for (AuditLogDetails details : this.auditLogDetails) {
						if (!that.auditLogDetails.contains(details)) {
							return false;
						}
					}

					return true;
				}
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		boolean whenInitialState = OperationType.INITIAL_STATE.equals(this.operationType);
		boolean whenCreate = OperationType.CREATE.equals(this.operationType);
		boolean whenDelete = OperationType.DELETE.equals(this.operationType);
		boolean whenUpdate = OperationType.UPDATE.equals(this.operationType);

		if (whenInitialState || whenCreate || whenDelete) {
			return Objects.hash(operationType, tableName, entityId);
		}

		if (whenUpdate) {
			int hashcode = Objects.hash(auditUserId, auditUserType, operationType, tableName, entityId);

			if (!CollectionUtils.isEmpty(auditLogDetails)) {
				for (AuditLogDetails details : auditLogDetails) {
					hashcode += details.hashCode();
				}
			}

			return hashcode;
		}

		return Objects.hash(auditDate, auditUserId, auditUserType, operationType, tableName, entityId);
	}
}
