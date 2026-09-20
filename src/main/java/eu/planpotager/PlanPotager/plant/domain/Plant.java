package eu.planpotager.PlanPotager.plant.domain;

import eu.planpotager.PlanPotager.registry.domain.Variety;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

// Divergence LDD documentee (specs/03_Conception_V1.0.md) : mappe sur la table SQL SeedPacket, pas Plant.
// BatchSize : les proxies d'un potager sont initialises par lots (IN) plutot qu'un SELECT par plante.
@BatchSize(size = 50)
@Entity
@Table(name = "SeedPacket")
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seedpacket")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "name_variety")
    private Variety variety;

    @Column(name = "brand")
    private String supplier;

    @Column(name = "email")
    private String userEmail;

    protected Plant() {
    }

    public Plant(Variety variety, String supplier, String userEmail) {
        this.variety = variety;
        this.supplier = supplier;
        this.userEmail = userEmail;
    }

    public Long getId() {
        return id;
    }

    public Variety getVariety() {
        return variety;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getUserEmail() {
        return userEmail;
    }
}
