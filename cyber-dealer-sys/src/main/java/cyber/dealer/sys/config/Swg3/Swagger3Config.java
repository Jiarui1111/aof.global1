package cyber.dealer.sys.config.Swg3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class Swagger3Config {

    //http://localhost:8082/user1/login 首先使用这个操作
    //http://localhost:8082/swagger-ui/index.html  然后登录swg
    @Bean
    public Docket desertsApi(){
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("cyber.dealer.sys.controller"))
                .paths(PathSelectors.any())
                .build()
                .groupName("default")
                .enable(true);
    }

    private ApiInfo apiInfo(){
        return new ApiInfoBuilder()
                .title("微服务（GMS） API说明文档")
                .description("微服务（GMS） API说明文档")
                .contact(new Contact("GMS", "https://", "@qq.com"))
                .termsOfServiceUrl("https://www.zybuluo.com/mdeditor#2281023-full-reader")
                .version("1.0")
                .build();
    }
}
