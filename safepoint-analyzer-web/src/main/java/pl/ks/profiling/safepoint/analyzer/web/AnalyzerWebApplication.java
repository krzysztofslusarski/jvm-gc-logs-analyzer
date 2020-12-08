package pl.ks.profiling.safepoint.analyzer.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AnalyzerWebApplication {
	public static void main(String[] args) {
		SpringApplication.run(AnalyzerWebApplication.class, args);
	}
}
