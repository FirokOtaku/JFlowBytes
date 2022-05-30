package firok.spring.jfb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.SpringfoxWebConfiguration;
import springfox.documentation.spring.web.SpringfoxWebMvcConfiguration;
import springfox.documentation.spring.web.WebMvcPropertySourcedRequestMappingHandlerMapping;
import springfox.documentation.spring.web.json.JacksonModuleRegistrar;
import springfox.documentation.spring.web.json.JsonSerializer;
import springfox.documentation.swagger.configuration.SwaggerCommonConfiguration;
import springfox.documentation.swagger2.configuration.Swagger2JacksonModule;
import springfox.documentation.swagger2.mappers.ServiceModelToSwagger2Mapper;
import springfox.documentation.swagger2.web.Swagger2ControllerWebMvc;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Configuration
@Import({ SpringfoxWebConfiguration.class, SpringfoxWebMvcConfiguration.class, SwaggerCommonConfiguration.class })
@ComponentScan(basePackages = {
        "springfox.documentation.swagger2.readers.parameter",
        "springfox.documentation.swagger2.mappers"
})
public class WebMVCConfigurer extends WebMvcConfigurationSupport
{
    @Bean
    public JacksonModuleRegistrar swagger2Module() {
        return new Swagger2JacksonModule();
    }

    @Bean
    public HandlerMapping swagger2ControllerMapping(
            Environment environment,
            DocumentationCache documentationCache,
            ServiceModelToSwagger2Mapper mapper,
            JsonSerializer jsonSerializer) {
        return new WebMvcPropertySourcedRequestMappingHandlerMapping(
                environment,
                new Swagger2ControllerWebMvc(environment, documentationCache, mapper, jsonSerializer));
    }

    /**
     * 发现如果继承了WebMvcConfigurationSupport，则在yml中配置的相关内容会失效。 需要重新指定静态资源
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico");
        registry.addResourceHandler("/static/**").addResourceLocations(
                "classpath:/static/");
        registry.addResourceHandler("swagger-ui.html").addResourceLocations(
                "classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations(
                "classpath:/META-INF/resources/webjars/");
        super.addResourceHandlers(registry);
    }
}
