package com.github.coleb1911.ghost2.database.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "APPLICATION_META")
public class ApplicationMeta {
    @Id
    @Column(name = "KEY", unique = true, nullable = false)
    private String key = "ghost2";

    @Column(name = "OPERATOR_ID", unique = true)
    private Long operatorId = -1L;

    public ApplicationMeta() {
    }

    public ApplicationMeta(long operatorId) {
        this.key = "ghost2";
        this.operatorId = operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public Long getOperatorId() {
        return operatorId;
    }
}
