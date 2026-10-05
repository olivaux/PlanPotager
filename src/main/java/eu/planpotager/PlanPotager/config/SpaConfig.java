package eu.planpotager.PlanPotager.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

// Vue Router gere le routage cote client (ex: /profile) : ces chemins ne correspondent a aucun
// fichier statique ni endpoint /api/**, donc sans ce fallback Spring Boot renvoie 404 au lieu de
// servir index.html et laisser Vue Router prendre le relais.
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource requestedResource = location.createRelative(resourcePath);
                    if (requestedResource.exists() && requestedResource.isReadable()) {
                        return requestedResource;
                    }
                    if (resourcePath.startsWith("api/")) {
                        return null;
                    }
                    return new ClassPathResource("/static/index.html");
                }
            });
    }
}
