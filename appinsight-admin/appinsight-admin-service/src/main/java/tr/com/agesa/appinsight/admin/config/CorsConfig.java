package tr.com.agesa.appinsight.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Node'daki {@code @fastify/cors { origin: true }} karşılığı — isteğin Origin'ini yansıtır.
 * Web portal (5173) doğrudan bu servise gidebilsin diye açık bırakılmıştır;
 * prod profilinde daraltılmalıdır.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
