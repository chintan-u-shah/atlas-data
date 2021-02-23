package com.wheelsup.jpa.audit.domain.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wheelsup.jpa.audit.interceptor.audit.NotAudited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditEntry {

	@NotAudited
	@Column(insertable = true, updatable = false)
	@CreatedBy
	private String createUser;

	@NotAudited
	@Column(insertable = true, updatable = false)
	@CreatedDate
	private Date createDate;

	@NotAudited
	@Column(insertable = false, updatable = true)
	@LastModifiedBy
	private String updateUser;

	@NotAudited
	@Column(insertable = false, updatable = true)
	@LastModifiedDate
	private Date updateDate;

	@JsonIgnore
	public String getCreateUser() {
		return createUser;
	}

	@JsonIgnore
	public Date getCreateDate() {
		return createDate;
	}

	@JsonIgnore
	public String getUpdateUser() {
		return updateUser;
	}

	@JsonIgnore
	public Date getUpdateDate() {
		return updateDate;
	}
}