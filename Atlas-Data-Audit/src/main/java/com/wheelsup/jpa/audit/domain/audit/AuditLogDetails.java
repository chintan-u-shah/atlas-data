package com.wheelsup.jpa.audit.domain.audit;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Entity
public class AuditLogDetails implements Serializable {

	private static final long serialVersionUID = 1576478259806627697L;

	@Id
	@GeneratedValue
	@Column(name = "auditLogDetailsId", unique = true)
	private Long id;

	private String columnName;

	private String previousValue;

	private String currentValue;


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		AuditLogDetails that = (AuditLogDetails) o;

		return Objects.equals(this.columnName, that.columnName) &&
				Objects.equals(this.previousValue, that.previousValue) &&
				Objects.equals(this.currentValue, that.currentValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnName, previousValue, currentValue);
	}
}
