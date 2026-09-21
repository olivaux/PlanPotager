package eu.planpotager.PlanPotager.garden.domain;

import eu.planpotager.PlanPotager.plant.domain.Plant;
import eu.planpotager.PlanPotager.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Garden")
public class Garden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_garden")
    private Long id;
    private String name;

    @Column(name = "location_longitude")
    private Double longitude;

    @Column(name = "location_latitude")
    private Double latitude;

    private Double score;

    @ManyToOne
    @JoinColumn(name = "email")
    private User user;

    @OneToMany(mappedBy = "garden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GardenPlant> gardenPlants = new ArrayList<>();

    protected Garden() {
    }

    public Garden(String name, Double longitude, Double latitude, User user) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public User getUser() {
        return user;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public List<GardenPlant> getGardenPlants() {
        return gardenPlants;
    }

    public GardenPlant addPlant(Plant plant, int x, int y) {
        GardenPlant gardenPlant = new GardenPlant(this, plant, x, y);
        gardenPlants.add(gardenPlant);
        return gardenPlant;
    }

    public GardenPlant findGardenPlant(Long gardenPlantId) {
        return gardenPlants.stream()
                .filter(gardenPlant -> gardenPlant.getId().equals(gardenPlantId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Plant not found in the garden"));
    }

    public void updatePlantPosition(Long gardenPlantId, int newX, int newY) {
        GardenPlant gardenPlant = findGardenPlant(gardenPlantId);
        gardenPlant.setPosition(newX, newY);
    }

    public GardenPlant updatePlantMatrix(Long gardenPlantId, int matrixX, int matrixY) {
        GardenPlant gardenPlant = findGardenPlant(gardenPlantId);
        gardenPlant.setMatrix(matrixX, matrixY);
        return gardenPlant;
    }

    public void setPlantState(Long gardenPlantId, PlantState state) {
        GardenPlant gardenPlant = findGardenPlant(gardenPlantId);
        gardenPlant.setState(state);
    }

    public void removePlant(Long gardenPlantId) {
        GardenPlant gardenPlant = findGardenPlant(gardenPlantId);
        gardenPlants.remove(gardenPlant);
    }

}
