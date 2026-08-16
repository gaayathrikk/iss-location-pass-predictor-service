package com.isspredictor.iss_predictor_service.config;

import com.isspredictor.iss_predictor_service.cache.TtlCache;
import com.isspredictor.iss_predictor_service.model.CloudForecast;
import com.isspredictor.iss_predictor_service.model.IssPosition;
import com.isspredictor.iss_predictor_service.model.PassPrediction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers one named {@link TtlCache} bean per upstream data source.
 * <p>
 * Three separate beans - rather than one shared cache - because each source
 * caches a different value type and needs its own TTL policy (position
 * changes every few seconds, pass predictions are stable for ~30 min,
 * weather for ~15 min; see application.yml). Spring auto-collects all three
 * into a {@code List<TtlCache<?,?>>} for {@link com.isspredictor.cache.CacheEvictionScheduler}
 * automatically - no extra wiring needed for that part.
 */
@Configuration
public class CacheConfig {

    @Bean(name = "positionCache")
    public TtlCache<String, IssPosition> positionCache() {
        return new TtlCache<>();
    }

    @Bean(name = "passCache")
    public TtlCache<String, List<PassPrediction>> passCache() {
        return new TtlCache<>();
    }

    @Bean(name = "weatherCache")
    public TtlCache<String, CloudForecast> weatherCache() {
        return new TtlCache<>();
    }
}