package eu.planpotager.PlanPotager.registry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

// BatchSize : les proxies de varietes sont initialises par lots (IN) plutot qu'un SELECT par variete.
@BatchSize(size = 50)
@Entity
@Table(name = "Variety")
public class Variety {

    @Id
    @Column(name = "name_variety")
    private String name;
    private Double radius;

    @Column(name = "plantation_start")
    private Integer plantationStart;

    @Column(name = "plantation_end")
    private Integer plantationEnd;

    @Column(name = "harvest_duration")
    private Integer harvestDuration;

    @ManyToOne
    @JoinColumn(name = "name_species")
    private Species species;

    protected Variety() {
    }

    public Variety(String name, Double radius, Integer plantationStart, Integer plantationEnd, Integer harvestDuration, Species species) {
        this.name = name;
        this.radius = radius;
        this.plantationStart = plantationStart;
        this.plantationEnd = plantationEnd;
        this.harvestDuration = harvestDuration;
        this.species = species;
    }

    public String getName() {
        return name;
    }

    public Double getRadius() {
        return radius;
    }

    public Integer getPlantationStart() {
        return plantationStart;
    }

    public Integer getPlantationEnd() {
        return plantationEnd;
    }

    public Integer getHarvestDuration() {
        return harvestDuration;
    }

    public Species getSpecies() {
        return species;
    }

    /**
     * Valeurs effectives : celles de la variété si renseignées, sinon héritées de l'espèce parente
     * (cf. sql/data.sql — la majorité des variétés ne surchargent que leur nom).
     */
    public Double getEffectiveRadius() {
        return radius != null ? radius : species.getRadius();
    }

    public int getEffectivePlantationStart() {
        return plantationStart != null ? plantationStart : species.getPlantationStart();
    }

    public int getEffectivePlantationEnd() {
        return plantationEnd != null ? plantationEnd : species.getPlantationEnd();
    }

    public int getEffectiveHarvestDuration() {
        return harvestDuration != null ? harvestDuration : species.getHarvestDuration();
    }
}
