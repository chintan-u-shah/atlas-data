package com.wheelsup.jpa.audit.interceptor.field;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.SingleTableEntityPersister;

public class AuditDiscriminatorField extends AuditField {

	private final SingleTableEntityPersister singleTableEntityPersister;

	public AuditDiscriminatorField(ClassMetadata classMetadata) {
		super(AuditDiscriminatorField.class.getDeclaredFields()[0], null, null);
		if (classMetadata instanceof SingleTableEntityPersister) {
			this.singleTableEntityPersister = (SingleTableEntityPersister) classMetadata;
		} else {
			this.singleTableEntityPersister = null;
		}
	}

	@Override
	public boolean toBeAudit() throws Exception {

		return isAuditable() && getColumnName() != null && StringUtils.isNotEmpty((CharSequence) getCurrentValue());
	}

	@Override
	public boolean isAuditable() {
		return singleTableEntityPersister != null;
	}

	@Override
	public String getColumnName() {
		return singleTableEntityPersister.getDiscriminatorColumnName();
	}

	@Override
	public Object getCurrentValue() {
		return singleTableEntityPersister.getDiscriminatorValue();
	}
}
