package eu.planpotager.PlanPotager.garden.domain;

import eu.planpotager.PlanPotager.plant.domain.Plant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "PlantArchive")
public class PlantArchive {

    @Id
    @Column(name = "id_plant")
    private Long id;

    private int x;
    private int y;

    @Enumerated(EnumType.STRING)
    private PlantState state;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @ManyToOne
    @JoinColumn(name = "id_seedpacket")
    private Plant plant;

    @ManyToOne
    @JoinColumn(name = "id_garden", nullable = false)
    private Garden garden;

    protected PlantArchive() {
    }

    public PlantArchive(GardenPlant gardenPlant) {
        this.id = gardenPlant.getId();
        this.x = gardenPlant.getX();
        this.y = gardenPlant.getY();
        this.state = gardenPlant.getState();
        this.plant = gardenPlant.getPlant();
        this.garden = gardenPlant.getGarden();
        this.archivedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public PlantState getState() {
        return state;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public Plant getPlant() {
        return plant;
    }

    public Garden getGarden() {
        return garden;
    }
}
