package pl.ks.profiling.safepoint.analyzer.standalone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pl.ks.profiling.safepoint.analyzer.commons.shared.KernelConfiguration;

@Configuration
@Import({
        KernelConfiguration.class
})
class AnalyzerStandaloneApplicationConfiguration {
    @Value("${gui.presentation.mode}")
    boolean presentationMode;

    @Bean
    PresentationFontProvider presentationFontProvider() {
        return new PresentationFontProvider(presentationMode);
    }
}
