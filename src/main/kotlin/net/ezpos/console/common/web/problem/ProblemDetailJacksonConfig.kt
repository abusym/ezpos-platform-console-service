package net.ezpos.console.common.web.problem

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

@Configuration
class ProblemDetailJacksonConfig {
    @Bean
    @ConditionalOnMissingBean(ObjectMapper::class)
    fun objectMapper(builderProvider: ObjectProvider<Jackson2ObjectMapperBuilder>): ObjectMapper {
        val builder = builderProvider.ifAvailable
        return builder?.build() ?: ObjectMapper().findAndRegisterModules()
    }
}

