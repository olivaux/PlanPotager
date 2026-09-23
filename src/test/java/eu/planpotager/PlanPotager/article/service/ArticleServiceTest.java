package eu.planpotager.PlanPotager.article.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import eu.planpotager.PlanPotager.article.dao.ArticleDAO;
import eu.planpotager.PlanPotager.article.domain.Article;
import eu.planpotager.PlanPotager.article.dto.ArticleDTO;
import eu.planpotager.PlanPotager.registry.domain.Family;
import eu.planpotager.PlanPotager.registry.domain.Species;
import eu.planpotager.PlanPotager.registry.domain.Type;
import eu.planpotager.PlanPotager.registry.domain.Variety;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleDAO articleDAO;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void getArticlesByVariety_shouldReturnDTOsForEveryArticleLinkedToVariety() {
        Family family = new Family("Solanaceae");
        Species tomato = new Species("Tomate", 0.3, 3, 5, 2, family, new Type("Légume"));
        Variety cherry = new Variety("Cerise", 0.2, 3, 5, 2, tomato);
        Article a1 = new Article("Bien planter la tomate cerise", "...", "https://example.com/1", cherry);
        Article a2 = new Article("Astuces d'arrosage", "...", "https://example.com/2", cherry);
        when(articleDAO.findByVarietyName("Cerise")).thenReturn(List.of(a1, a2));

        List<ArticleDTO> result = articleService.getArticlesByVariety("Cerise");

        assertThat(result).extracting(ArticleDTO::title)
                .containsExactlyInAnyOrder("Bien planter la tomate cerise", "Astuces d'arrosage");
    }

    @Test
    void getArticlesByVariety_shouldReturnEmptyList_whenVarietyHasNoArticle() {
        when(articleDAO.findByVarietyName("Inconnue")).thenReturn(List.of());

        List<ArticleDTO> result = articleService.getArticlesByVariety("Inconnue");

        assertThat(result).isEmpty();
    }
}
