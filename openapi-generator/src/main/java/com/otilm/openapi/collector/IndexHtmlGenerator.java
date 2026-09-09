package com.otilm.openapi.collector;

import com.otilm.openapi.config.loader.GroupsConfigLoader;
import com.otilm.openapi.config.model.GroupConfiguration;
import com.otilm.openapi.config.model.GroupsConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates index.html from an index.html.template file, injecting the API list arrays (coreApis, connectorApis,
 * messagingApis, protocolApis) derived from groups.yaml in place of the {@code // placeholder for the list of APIS}
 * marker line.
 * <p>
 * Each group with an {@code indexCategory} field ({@code core}, {@code connector}, {@code messaging}, or
 * {@code protocol}) is included in the corresponding JavaScript array. Groups without the field (e.g. cert-manager,
 * ilm-core, legacy variants) are ignored. Groups with an unrecognized {@code indexCategory} value cause the generator
 * to fail.
 * <p>
 * The display name for each entry is resolved in the following order:
 * <ol>
 * <li>{@code navLabel} – when present, used verbatim</li>
 * <li>{@code title} – used after stripping a trailing {@code " API"} suffix</li>
 * <li>{@code id} – fallback when title is also absent</li>
 * </ol>
 * <p>
 * Arguments:
 * <ul>
 * <li>args[0] – Path to groups.yaml</li>
 * <li>args[1] – Path to index.html.template (read-only input)</li>
 * <li>args[2] – Path to index.html (written as output)</li>
 * </ul>
 */
public class IndexHtmlGenerator {
    private static final Logger log = LoggerFactory.getLogger(IndexHtmlGenerator.class);

    /**
     * Category names in the order they appear in the JS block.
     */
    private static final List<String> CATEGORY_ORDER = List.of("core", "connector", "messaging", "protocol");

    /**
     * Maps indexCategory value → JS variable name.
     */
    private static final Map<String, String> VAR_NAMES = Map
            .of("core", "coreApis", "connector", "connectorApis", "messaging", "messagingApis", "protocol",
                    "protocolApis");

    /**
     * Matches the single placeholder line {@code // placeholder for the list of APIS}, including any leading
     * whitespace, which marks the insertion point for the generated arrays block.
     */
    private static final Pattern ARRAYS_PATTERN = Pattern.compile("([ \\t]*)// placeholder for the list of APIS");

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            log.error("Usage: IndexHtmlGenerator <groups.yaml> <index.html.template> <index.html>");
            System.exit(1);
        }

        Path groupsYamlPath = Paths.get(args[0]);
        Path templateHtmlPath = Paths.get(args[1]);
        Path indexHtmlPath = Paths.get(args[2]);

        validatePaths(groupsYamlPath, templateHtmlPath, indexHtmlPath);

        GroupsConfig config = loadConfig(groupsYamlPath);
        Map<String, List<GroupConfiguration>> buckets = bucketByCategory(config.getGroups());
        String newBlock = buildJsBlock(buckets);
        updateIndexHtml(templateHtmlPath, indexHtmlPath, newBlock);
        printSummary(buckets);
    }

    private static void validatePaths(Path groupsYamlPath, Path templateHtmlPath, Path indexHtmlPath) {
        if (!Files.exists(groupsYamlPath)) {
            log.error("Error: groups.yaml not found at {}", groupsYamlPath);
            System.exit(1);
        }
        if (!Files.exists(templateHtmlPath)) {
            log.error("Error: index.html.template not found at {}", templateHtmlPath);
            System.exit(1);
        }
        if (indexHtmlPath.getParent() != null && !Files.exists(indexHtmlPath.getParent())) {
            log.error("Error: output directory does not exist: {}", indexHtmlPath.getParent());
            System.exit(1);
        }
    }

    private static GroupsConfig loadConfig(Path groupsYamlPath) {
        try {
            return GroupsConfigLoader.withoutExtensionResolution().loadFromFilesystem(groupsYamlPath.toString());
        } catch (IOException e) {
            log.error("Error: failed to load groups.yaml from {}", groupsYamlPath);
            System.exit(1);
            return null;
        }
    }

    /**
     * Groups the given list by {@code indexCategory}, preserving {@link #CATEGORY_ORDER} and sorting each bucket
     * alphabetically by title.
     */
    private static Map<String, List<GroupConfiguration>> bucketByCategory(List<GroupConfiguration> groups) {
        Map<String, List<GroupConfiguration>> buckets = new LinkedHashMap<>();
        for (String cat : CATEGORY_ORDER) {
            buckets.put(cat, new ArrayList<>());
        }
        for (GroupConfiguration group : groups) {
            String cat = group.getIndexCategory();
            if (cat == null) {
                continue;
            }
            if (!buckets.containsKey(cat)) {
                log
                        .error("Error: group '{}' has unknown indexCategory '{}'. Allowed values: {}", group.getId(),
                                cat, CATEGORY_ORDER);
                System.exit(1);
            }
            buckets.get(cat).add(group);
        }
        buckets
                .values()
                .forEach(list -> list.sort(Comparator.comparing(g -> g.getTitle() != null ? g.getTitle() : g.getId())));
        return buckets;
    }

    /**
     * Builds the {@code // placeholder for the list of APIS} JavaScript block from the bucketed groups.
     */
    private static String buildJsBlock(Map<String, List<GroupConfiguration>> buckets) {
        StringBuilder js = new StringBuilder();
        js.append("// placeholder for the list of APIS\n");
        for (String cat : CATEGORY_ORDER) {
            js.append("    var ").append(VAR_NAMES.get(cat)).append(" = [\n");
            for (GroupConfiguration g : buckets.get(cat)) {
                String name = resolveNavName(g);
                js
                        .append("      {\"name\": ")
                        .append(jsonString(name))
                        .append(", \"url\": ")
                        .append(jsonString(g.getId() + ".html"))
                        .append("},\n");
            }
            js.append("    ];\n\n");
        }
        return js.toString().stripTrailing();
    }

    static String resolveNavName(GroupConfiguration g) {
        if (g.getNavLabel() != null && !g.getNavLabel().isBlank()) {
            return g.getNavLabel();
        }
        String rawName = g.getTitle() != null ? g.getTitle() : g.getId();
        return rawName.endsWith(" API") ? rawName.substring(0, rawName.length() - 4) : rawName;
    }

    /**
     * Reads {@code templateHtmlPath}, replaces the placeholder line with {@code newBlock}, and writes the result to
     * {@code indexHtmlPath}. The original indentation of the placeholder line is preserved.
     */
    private static void updateIndexHtml(Path templateHtmlPath, Path indexHtmlPath, String newBlock) throws IOException {
        String html = Files.readString(templateHtmlPath);
        Matcher m = ARRAYS_PATTERN.matcher(html);
        if (!m.find()) {
            log.error("Error: could not find the '// placeholder for the list of APIS' line in index.html.");
            System.exit(1);
        }
        String indent = m.group(1);
        String indented = newBlock
                .lines()
                .map(line -> line.isBlank() ? line : indent + line)
                .collect(Collectors.joining("\n"));
        Files.writeString(indexHtmlPath, html.substring(0, m.start()) + indented + html.substring(m.end()));
    }

    private static void printSummary(Map<String, List<GroupConfiguration>> buckets) {
        log.info("index.html updated:");
        for (String cat : CATEGORY_ORDER) {
            String category = VAR_NAMES.get(cat);
            int count = buckets.get(cat).size();
            log.info("  {}: {} entries", category, count);
        }
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
