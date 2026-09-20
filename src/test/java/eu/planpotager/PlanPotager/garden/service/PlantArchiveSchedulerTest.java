package eu.planpotager.PlanPotager.garden.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import eu.planpotager.PlanPotager.garden.dao.PlantArchiveDAO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlantArchiveSchedulerTest {

    @Mock
    private PlantArchiveDAO plantArchiveDAO;

    @InjectMocks
    private PlantArchiveScheduler scheduler;

    @Test
    void purgeExpiredArchives_shouldDeleteArchivesOlderThanOneYearInASingleQuery() {
        scheduler.purgeExpiredArchives();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.captor();
        verify(plantArchiveDAO).deleteArchivedBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isBefore(LocalDateTime.now().minusYears(1).plusMinutes(1));
        assertThat(cutoff.getValue()).isAfter(LocalDateTime.now().minusYears(1).minusMinutes(1));
    }
}
