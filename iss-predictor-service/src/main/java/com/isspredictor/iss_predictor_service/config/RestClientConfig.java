/**
 * Spring configuration: RestClient/WebClient beans, Resilience4j config, OpenAPI config.
 */
package com.isspredictor.iss_predictor_service.config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
// import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.http.client.ClientHttpRequestFactory;
// import org.springframework.web.client.RestClient;

// import java.time.Duration;

// /**
//  * Defines one {@link RestClient} bean per upstream API, each qualified by name and
//  * configured with its own connect/read timeout (see {@code application.yml}, under
//  * {@code iss.api.*.timeout-ms}). Keeping these separate — rather than one shared
//  * RestClient — means a slow Pollux Labs response can never starve the Open-Notify
//  * or Open-Meteo calls, and each upstream's timeout can be tuned independently.
//  */
// @Configuration
// public class RestClientConfig {

//     @Bean
//     public RestClient openNotifyRestClient(
//             @Value("${iss.api.open-notify.base-url}") String baseUrl,
//             @Value("${iss.api.open-notify.timeout-ms}") long timeoutMs) {
//         return buildClient(baseUrl, timeoutMs);
//     }

//     @Bean
//     public RestClient polluxLabsRestClient(
//             @Value("${iss.api.pollux-labs.base-url}") String baseUrl,
//             @Value("${iss.api.pollux-labs.timeout-ms}") long timeoutMs) {
//         return buildClient(baseUrl, timeoutMs);
//     }

//     @Bean
//     public RestClient openMeteoRestClient(
//             @Value("${iss.api.open-meteo.base-url}") String baseUrl,
//             @Value("${iss.api.open-meteo.timeout-ms}") long timeoutMs) {
//         return buildClient(baseUrl, timeoutMs);
//     }

//     private RestClient buildClient(String baseUrl, long timeoutMs) {
//         ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
//                 .withConnectTimeout(Duration.ofMillis(timeoutMs))
//                 .withReadTimeout(Duration.ofMillis(timeoutMs));
//         ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);

//         return RestClient.builder()
//                 .baseUrl(baseUrl)
//                 .requestFactory(factory)
//                 .build();
//     }
// }

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Defines one {@link RestClient} bean per upstream API, each qualified by name and
 * configured with its own connect/read timeout (see {@code application.yml}, under
 * {@code iss.api.*.timeout-ms}). Keeping these separate — rather than one shared
 * RestClient — means a slow Pollux Labs response can never starve the Open-Notify
 * or Open-Meteo calls, and each upstream's timeout can be tuned independently.
 * <p>
 * Uses {@link SimpleClientHttpRequestFactory} (a plain Spring Framework class,
 * not one of Spring Boot's newer request-factory builder APIs) deliberately -
 * its timeout setters are stable and well-documented across Spring Framework
 * versions, whereas Boot's {@code http.client} builder API surface changed
 * shape in ways not reflected consistently in publicly available docs at the
 * time this was written.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient openNotifyRestClient(
            @Value("${iss.api.open-notify.base-url}") String baseUrl,
            @Value("${iss.api.open-notify.timeout-ms}") long timeoutMs) {
        return buildClient(baseUrl, timeoutMs);
    }

    @Bean
    public RestClient polluxLabsRestClient(
            @Value("${iss.api.pollux-labs.base-url}") String baseUrl,
            @Value("${iss.api.pollux-labs.timeout-ms}") long timeoutMs) {
        return buildClient(baseUrl, timeoutMs);
    }

    @Bean
    public RestClient openMeteoRestClient(
            @Value("${iss.api.open-meteo.base-url}") String baseUrl,
            @Value("${iss.api.open-meteo.timeout-ms}") long timeoutMs) {
        return buildClient(baseUrl, timeoutMs);
    }

    private RestClient buildClient(String baseUrl, long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout((int) timeoutMs);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}