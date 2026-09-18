package eu.planpotager.PlanPotager.article.service;

import eu.planpotager.PlanPotager.article.dao.ArticleDAO;
import eu.planpotager.PlanPotager.article.dto.ArticleDTO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

    private final ArticleDAO articleDAO;

    public ArticleService(ArticleDAO articleDAO) {
        this.articleDAO = articleDAO;
    }

    public List<ArticleDTO> getArticlesByVariety(String varietyName) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
