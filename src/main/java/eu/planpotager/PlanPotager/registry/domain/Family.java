package eu.planpotager.PlanPotager.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Family")
public class Family {

    @Id
    @Column(name = "name_family")
    private String name;

    protected Family() {
    }

    public Family(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
