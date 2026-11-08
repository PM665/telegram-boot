package io.github.pm665.telegramboot.admin

import io.github.pm665.telegramboot.admin.security.TelegramAdminProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver
import org.thymeleaf.templatemode.TemplateMode

@Configuration
@EnableConfigurationProperties(TelegramAdminProperties::class)
@ComponentScan(
    basePackages = [
        "io.github.pm665.telegramboot.admin",
    ],
)
class TelegramBootAdminConfiguration {
    @Bean
    fun telegramBootAdminTemplatesResolver(): SpringResourceTemplateResolver =
        SpringResourceTemplateResolver().apply {
            this.prefix = "classpath:/telegram-boot-admin/templates/"
            this.suffix = ".html"
            this.order = 10
            this.templateMode = TemplateMode.HTML
            this.characterEncoding = "UTF-8"
            this.checkExistence = true
        }

    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    class TelegramBootAdminWebMvcConfig : WebMvcConfigurer {
        override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
            registry
                .addResourceHandler("/tg-boot-admin/**")
                .addResourceLocations("classpath:/telegram-boot-admin/static/")
                .setCachePeriod(3600)
                .resourceChain(true)
        }

//        override fun addViewControllers(registry: ViewControllerRegistry) {
//            registry
//                .addViewController("/")
//                .setViewName("forward:/admin")
//            registry.setOrder(Ordered.HIGHEST_PRECEDENCE)
//        }
    }
}
