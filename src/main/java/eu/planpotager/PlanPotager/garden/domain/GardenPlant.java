package eu.planpotager.PlanPotager.garden.domain;

import eu.planpotager.PlanPotager.plant.domain.Plant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;


@Entity
@Table(name = "Plant")
public class GardenPlant {

    public static final int MAX_MATRIX_SIZE = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plant")
    private Long id;
    private int x;
    private int y;

    // Nombre de legumes de la plante en colonnes (X) et en lignes (Y) : 1 x 1 = un seul legume (un cercle a l'ecran).
    @Column(name = "matrix_x")
    private int matrixX = 1;

    @Column(name = "matrix_y")
    private int matrixY = 1;

    @Enumerated(EnumType.STRING)
    private PlantState state;

    @Column(name = "date_planted")
    private LocalDate datePlanted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_seedpacket")
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_garden", nullable = false)
    private Garden garden;

    protected GardenPlant() {
    }

    public GardenPlant(Garden garden, Plant plant, int x, int y) {
        this.garden = garden;
        this.plant = plant;
        this.x = x;
        this.y = y;
        this.state = PlantState.A_PLANTER;
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

    public void setPosition(int x, int y) {
        if (state != PlantState.A_PLANTER) {
            throw new IllegalStateException("Plant position cannot change once planted");
        }
        this.x = x;
        this.y = y;
    }

    public int getMatrixX() {
        return matrixX;
    }

    public int getMatrixY() {
        return matrixY;
    }

    public void setMatrix(int matrixX, int matrixY) {
        if (!isValidMatrixSize(matrixX) || !isValidMatrixSize(matrixY)) {
            throw new IllegalArgumentException("Matrix size must be between 1 and " + MAX_MATRIX_SIZE);
        }
        this.matrixX = matrixX;
        this.matrixY = matrixY;
    }

    public static boolean isValidMatrixSize(int size) {
        return size >= 1 && size <= MAX_MATRIX_SIZE;
    }

    public PlantState getState() {
        return state;
    }

    public void setState(PlantState newState) {
        if (!state.canTransitionTo(newState)) {
            throw new IllegalStateException("Cannot transition plant from " + state + " to " + newState);
        }
        if (newState == PlantState.PLANTEE) {
            this.datePlanted = LocalDate.now();
        }
        this.state = newState;
    }

    public LocalDate getDatePlanted() {
        return datePlanted;
    }

    public Plant getPlant() {
        return plant;
    }

    public Garden getGarden() {
        return garden;
    }
}
