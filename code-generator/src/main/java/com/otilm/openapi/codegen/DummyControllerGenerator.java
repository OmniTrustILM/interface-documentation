package com.otilm.openapi.codegen;

import com.otilm.openapi.config.loader.GroupsConfigLoader;
import com.otilm.openapi.config.model.GroupsConfig;
import com.otilm.openapi.config.model.SecurityConfiguration;
import com.otilm.openapi.config.util.ClassNameResolver;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates dummy controller implementations from the groups.yaml configuration. This is a standalone tool that runs
 * during the Maven build to create @RestController classes that implement the interfaces defined in groups.yaml.
 * <p>
 * Uses reflection to inspect interfaces and generate proper method implementations. Validates that each interface
 * extends one of the three base security controllers. Annotates generated classes with @SecuritySchemeCategory
 * metadata.
 * <p>
 */
public class DummyControllerGenerator {
    private static final Logger log = LoggerFactory.getLogger(DummyControllerGenerator.class);

    private static final String PACKAGE_NAME = "com.otilm.openapi.generated";
    private static final String HEADING_SEPARATOR = "======================================================================";

    private final GroupsConfigLoader configLoader;
    private SecuritySchemeExtractor securitySchemeExtractor;

    public DummyControllerGenerator() {
        this.configLoader = new GroupsConfigLoader();
    }

    public static void main(String[] args) throws ClassNotFoundException, IOException {
        if (args.length != 2) {
            log.error("Usage: DummyControllerGenerator <groups.yaml path> <output directory>");
            System.exit(1);
        }

        String groupsYamlPath = args[0];
        String outputDir = args[1];

        printHeader(groupsYamlPath, outputDir);

        DummyControllerGenerator generator = new DummyControllerGenerator();
        generator.generate(groupsYamlPath, outputDir);
    }

    /**
     * Main generation method that orchestrates the entire process.
     */
    public void generate(String groupsYamlPath, String outputDir) throws ClassNotFoundException, IOException {
        // Load configuration
        GroupsConfig groupsConfig = configLoader.loadFromFilesystem(groupsYamlPath);
        if (groupsConfig == null) {
            throw new IOException("Failed to load configuration from " + groupsYamlPath);
        }

        // security configuration
        SecurityConfiguration securityConfig = groupsConfig.getSecurity();
        this.securitySchemeExtractor = new SecuritySchemeExtractor(securityConfig);

        // interface configuration
        Set<String> allInterfaces = groupsConfig
                .getGroups()
                .stream()
                .flatMap(g -> g.getInterfaces().stream())
                .collect(Collectors.toSet());
        int groupCount = groupsConfig.getGroups().size();

        log.info("Found {} groups with {} unique interfaces", groupCount, allInterfaces.size());

        // Generate implementations
        GenerationResult result = generateAllControllers(allInterfaces, outputDir);

        printSummary(result);

        if (result.failCount > 0) {
            System.exit(1);
        }
    }

    /**
     * Generates dummy controllers for all interfaces.
     */
    private GenerationResult generateAllControllers(Set<String> interfaces, String outputDir) {
        GeneratedSourceWriter generatedSourceWriter = new GeneratedSourceWriter(outputDir, PACKAGE_NAME);
        int successCount = 0;
        int failCount = 0;

        for (String interfaceFqn : interfaces) {
            try {
                generateDummyController(interfaceFqn, generatedSourceWriter);
                successCount++;
            } catch (ClassNotFoundException | IOException e) {
                log.error("❌ Failed to generate dummy for {}: {}", interfaceFqn, e.getMessage());
                failCount++;
            }
        }

        return new GenerationResult(successCount, failCount);
    }

    /**
     * Generates a single dummy controller implementation for the given interface. Validates that the interface extends
     * one of the three base security controllers.
     */
    private void generateDummyController(String interfaceFqn, GeneratedSourceWriter generatedSourceWriter)
            throws ClassNotFoundException, IOException {
        Class<?> interfaceClass = loadInterfaceClass(interfaceFqn);

        String baseSecurityClass = securitySchemeExtractor.determineBaseSecurityClass(interfaceClass);
        List<String> securitySchemes = securitySchemeExtractor.getSecuritySchemesForBaseClass(baseSecurityClass);

        // Generate implementation code with unique naming
        String implClassName = ClassNameResolver.generateImplementationClassName(interfaceClass);
        TypeResolver typeResolver = new TypeResolver(interfaceClass);
        CodeGenerator codeGenerator = new CodeGenerator(typeResolver, PACKAGE_NAME, implClassName, baseSecurityClass,
                securitySchemes);
        String sourceCode = codeGenerator.generateImplementation(interfaceClass);

        generatedSourceWriter.writeImplementation(implClassName, sourceCode);

        int methodCount = countNonObjectMethods(interfaceClass);
        String baseClassName = baseSecurityClass.substring(baseSecurityClass.lastIndexOf('.') + 1);
        log.info("✓ Generated {} ({} methods, {})", implClassName, methodCount, baseClassName);
    }

    /**
     * Loads an interface class by its fully qualified name using reflection.
     */
    private Class<?> loadInterfaceClass(String interfaceFqn) throws ClassNotFoundException {
        try {
            return Class.forName(interfaceFqn);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(
                    "Interface not found: " + interfaceFqn + ". Make sure the interfaces JAR is on the classpath.", e);
        }
    }

    /**
     * Counts the number of methods in the interface (excluding Object methods).
     */
    private int countNonObjectMethods(Class<?> interfaceClass) {
        int count = 0;
        for (var method : interfaceClass.getMethods()) {
            if (!method.getDeclaringClass().equals(Object.class)) {
                count++;
            }
        }
        return count;
    }

    private static void printHeader(String configPath, String outputDir) {
        log.info(HEADING_SEPARATOR);
        log.info("Dummy Controller Generator");
        log.info(HEADING_SEPARATOR);
        log.info("Configuration: {}", configPath);
        log.info("Output directory: {}", outputDir);
    }

    private static void printSummary(GenerationResult result) {
        log.info(HEADING_SEPARATOR);
        log.info("✅ Successfully generated {} dummy controller classes", result.successCount);
        if (result.failCount > 0) {
            log.error("❌ Failed to generate {} classes", result.failCount);
        }
        log.info(HEADING_SEPARATOR);
    }

    private record GenerationResult(int successCount, int failCount) {
    }
}
