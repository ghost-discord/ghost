package com.github.coleb1911.ghost2.database.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "APPLICATION_META")
public class ApplicationMeta {
    @Id
    @Column(name = "ID", unique = true, nullable = false)
    private String id = "ghost2";

    @Column(name = "OPERATOR_ID", unique = true)
    private Long operatorId = -1L;

    public ApplicationMeta() {
    }

    public ApplicationMeta(long operatorId) {
        this.operatorId = operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public Long getOperatorId() {
        return operatorId;
    }
}
