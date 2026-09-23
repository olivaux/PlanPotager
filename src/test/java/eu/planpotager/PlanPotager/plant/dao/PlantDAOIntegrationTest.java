package eu.planpotager.PlanPotager.plant.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.planpotager.PlanPotager.TestcontainersConfiguration;
import eu.planpotager.PlanPotager.plant.domain.Plant;
import eu.planpotager.PlanPotager.registry.domain.Family;
import eu.planpotager.PlanPotager.registry.domain.Species;
import eu.planpotager.PlanPotager.registry.domain.Type;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import eu.planpotager.PlanPotager.user.domain.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class PlantDAOIntegrationTest {

    private static final String EMAIL = "jane.doe@example.com";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlantDAO plantDAO;

    @Test
    void save_shouldPersistSeedPacket_whenVarietyAndUserExist() {
        Variety variety = persistVarietyChain("Tomate Cerise");
        entityManager.persist(new User(EMAIL));

        Plant saved = plantDAO.save(new Plant(variety, "Graines du Midi", EMAIL));
        entityManager.flush();

        List<Plant> found = plantDAO.findByUserEmail(EMAIL);
        assertThat(found).extracting(Plant::getId).containsExactly(saved.getId());
    }

    @Test
    void save_shouldThrowDataIntegrityViolation_whenUserEmailDoesNotExist() {
        Variety variety = persistVarietyChain("Tomate Cerise");

        assertThatThrownBy(() -> plantDAO.save(new Plant(variety, "Graines du Midi", "inconnu@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_shouldThrowDataIntegrityViolation_whenVarietyDoesNotExist() {
        entityManager.persist(new User(EMAIL));
        Species species = persistSpeciesChain();
        Variety unknownVariety = new Variety("Variete Inconnue", 0.2, 3, 5, 2, species);

        assertThatThrownBy(() -> plantDAO.save(new Plant(unknownVariety, "Graines du Midi", EMAIL)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Variety persistVarietyChain(String varietyName) {
        Species species = persistSpeciesChain();
        Variety variety = new Variety(varietyName, 0.2, 3, 5, 2, species);
        entityManager.persist(variety);
        return variety;
    }

    private Species persistSpeciesChain() {
        Type type = new Type("Legume");
        entityManager.persist(type);
        Family family = new Family("Solanaceae");
        entityManager.persist(family);
        Species species = new Species("Tomate", 0.3, 3, 5, 2, family, type);
        entityManager.persist(species);
        return species;
    }
}