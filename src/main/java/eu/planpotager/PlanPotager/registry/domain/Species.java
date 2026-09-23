package eu.planpotager.PlanPotager.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Species")
public class Species {

    @Id
    @Column(name = "name_species")
    private String name;
    private Double radius;

    @Column(name = "plantation_start")
    private int plantationStart;

    @Column(name = "plantation_end")
    private int plantationEnd;

    @Column(name = "harvest_duration")
    private int harvestDuration;

    @ManyToOne
    @JoinColumn(name = "name_family")
    private Family family;

    @ManyToOne
    @JoinColumn(name = "name_type")
    private Type type;

    @OneToMany(mappedBy = "species")
    private List<Variety> varieties = new ArrayList<>();

    protected Species() {
    }

    public Species(String name, Double radius, int plantationStart, int plantationEnd, int harvestDuration, Family family, Type type) {
        this.name = name;
        this.radius = radius;
        this.plantationStart = plantationStart;
        this.plantationEnd = plantationEnd;
        this.harvestDuration = harvestDuration;
        this.family = family;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Double getRadius() {
        return radius;
    }

    public int getPlantationStart() {
        return plantationStart;
    }

    public int getPlantationEnd() {
        return plantationEnd;
    }

    public int getHarvestDuration() {
        return harvestDuration;
    }

    public Family getFamily() {
        return family;
    }

    public Type getType() {
        return type;
    }

    public List<Variety> getVarieties() {
        return varieties;
    }
}
