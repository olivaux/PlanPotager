package eu.planpotager.PlanPotager.article.dao;

import eu.planpotager.PlanPotager.article.domain.Article;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleDAO extends JpaRepository<Article, Long> {

    List<Article> findByVarietyName(String varietyName);
}
