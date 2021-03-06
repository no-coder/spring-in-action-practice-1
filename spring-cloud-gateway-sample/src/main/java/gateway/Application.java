package gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
@EnableConfigurationProperties(Application.UriConfiguration.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * 访问超时后的后备方案fallback
     * @return
     */
    @RequestMapping("/fallback")
    public Mono<String> fallback(){
        System.out.println("fallback...");
        return Mono.just("fallback");
    }

    /**
     * RouteLocator 用于创建路由
     * RouteLocatorBuilder 允许在路由中添加 predicate 和 filter，以便根据特定条件路由
     * UriConfiguration 从配置中获取URL
     * @param builder
     * @return
     */
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder, UriConfiguration uriConfiguration){
        String httpUri = uriConfiguration.getHttpbin();
        return builder.routes()
                .route(p -> p
                        .path("/get")
                        .filters(f -> f.addRequestHeader("Hello", "World"))
                        .uri("http://httpbin.org:80"))
                /**
                 * 使用curl 发起请求，header 中包含 Host: *.hystrix.com，否则请求不会被路由
                 * $ curl --dump-header - --header 'Host: www.hystrix.com' http://localhost:8080/delay/3
                 * 注：使用 --dump-header 查看响应的headers
                 * --dump-heaer 后面的 - 是为了告知 cURL 将headers 打印出来
                 */
                .route(p -> p
                        .host("*.hystrix.com")
                        .filters(f -> f.hystrix(config -> config
                                .setName("mycmd")
                                .setFallbackUri("forward:/fallback")))
                        .uri(httpUri))
                .build();
    }

    @ConfigurationProperties
    class UriConfiguration {
        private String httpbin = "http://httpbin.org:80";

        public String getHttpbin() {
            return httpbin;
        }

        public void setHttpbin(String httpbin) {
            this.httpbin = httpbin;
        }
    }
}
