package com.vackosar.gitflowincrementalbuild.boundary;

import static com.vackosar.gitflowincrementalbuild.utils.PluginUtils.extractPluginConfigValue;
import static com.vackosar.gitflowincrementalbuild.utils.PluginUtils.separatePattern;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.vackosar.gitflowincrementalbuild.control.Property;

@Singleton
public class Configuration {

    private static final String PLUGIN_KEY = "com.vackosar.gitflowincrementalbuilder:gitflow-incremental-builder";

    private static final String MAKE_UPSTREAM = "make-upstream";

    private final boolean enabled;
    private final Path key;
    private final String referenceBranch;
    private final String baseBranch;
    private final boolean uncommited;
    private final boolean untracked;
    private final boolean makeUpstream;
    private final boolean skipTestsForNotImpactedModules;
    private final boolean buildAll;
    private final boolean compareToMergeBase;
    private final boolean fetchBaseBranch;
    private final boolean fetchReferenceBranch;
    private final Path outputFile;
    private final boolean writeChanged;
    private final String ignoreChangedPattern;

    private final Set<MavenProject> ignoredProjects;

    @Inject
    public Configuration(MavenSession session) throws IOException {

        try {
            makeUpstream = MAKE_UPSTREAM.equals(session.getRequest().getMakeBehavior());
            Plugin plugin = session.getTopLevelProject().getPlugin(PLUGIN_KEY);
            // check properties
            checkProperties(session.getTopLevelProject().getProperties());
            checkPluginConfiguration(plugin);
            checkProperties(System.getProperties());
            checkProperties(session.getUserProperties());
            // parse into configuration
            enabled = Boolean.valueOf(Property.enabled.getValue());
            key = parseKey(session, Property.repositorySshKey.getValue());
            referenceBranch = Property.referenceBranch.getValue();
            baseBranch = Property.baseBranch.getValue();
            uncommited = Boolean.valueOf(Property.uncommited.getValue());
            untracked = Boolean.valueOf(Property.untracked.getValue());
            skipTestsForNotImpactedModules = Boolean.valueOf(Property.skipTestsForNotImpactedModules.getValue());
            buildAll = Boolean.valueOf(Property.buildAll.getValue());
            compareToMergeBase = Boolean.valueOf(Property.compareToMergeBase.getValue());
            fetchReferenceBranch = Boolean.valueOf(Property.fetchReferenceBranch.getValue());
            fetchBaseBranch = Boolean.valueOf(Property.fetchBaseBranch.getValue());
            outputFile = parseOutputFile(session, Property.outputFile.getValue());
            writeChanged = Boolean.valueOf(Property.writeChanged.getValue());
            ignoreChangedPattern = Property.ignoreChanged.getValue();
            ignoredProjects = getIgnoredProjects(session, ignoreChangedPattern);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private Path parseKey(MavenSession session, String keyOptionValue) throws IOException {
        Path pomDir = session.getTopLevelProject().getBasedir().toPath();
        if (keyOptionValue != null && !keyOptionValue.isEmpty()) {
            return pomDir.resolve(keyOptionValue).toAbsolutePath().toRealPath().normalize();
        }
        return null;
    }

    private Set<MavenProject> getIgnoredProjects(MavenSession session, String ignoreChangedPattern) {
        if (Strings.isNullOrEmpty(ignoreChangedPattern)) {
            return Collections.emptySet();
        }
        List<String> patterns = separatePattern(ignoreChangedPattern);
        final PatternIncludesArtifactFilter filter = new PatternIncludesArtifactFilter(patterns);
        return session.getProjects().stream()
                .filter(p -> filter.include(p.getArtifact()))
                .collect(Collectors.toSet());
    }

    private Path parseOutputFile(MavenSession session, String outputFileValue) throws IOException {
        Path pomDir = session.getTopLevelProject().getBasedir().toPath();
        if (outputFileValue != null && !outputFileValue.isEmpty()) {
            return pomDir.resolve(outputFileValue).toAbsolutePath().normalize();
        }
        return null;
    }

    private void checkPluginConfiguration(Plugin plugin) {
        if (null != plugin) {
            Arrays.stream(Property.values())
                    .forEach(p -> p.setValue(extractPluginConfigValue(p.name(), plugin)));
        }
    }

    private void checkProperties(Properties properties) throws MavenExecutionException {
        try {
            properties.stringPropertyNames().stream()
                    .filter(s -> s.startsWith(Property.PREFIX))
                    .map(s -> s.replaceFirst(Property.PREFIX, ""))
                    .map(Property::valueOf)
                    .forEach(p -> p.setValue(properties.getProperty(p.fullName())));
        } catch (IllegalArgumentException e) {
            throw new MavenExecutionException("Invalid invalid GIB property found. Allowed properties: \n" + Property
                    .exemplifyAll(), e);
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public Optional<Path> key() {
        return Optional.ofNullable(key);
    }

    public String referenceBranch() {
        return referenceBranch;
    }

    public String baseBranch() {
        return baseBranch;
    }

    public boolean uncommited() {
        return uncommited;
    }

    public boolean untracked() {
        return untracked;
    }

    public boolean makeUpstream() {
        return makeUpstream;
    }

    public boolean skipTestsForNotImpactedModules() {
        return skipTestsForNotImpactedModules;
    }

    public boolean buildAll() {
        return buildAll;
    }

    public boolean compareToMergeBase() {
        return compareToMergeBase;
    }

    public boolean fetchBaseBranch() {
        return fetchBaseBranch;
    }

    public boolean fetchReferenceBranch() {
        return fetchReferenceBranch;
    }

    public Optional<Path> outputFile() {
        return Optional.ofNullable(outputFile);
    }

    public boolean writeChanged() {
        return writeChanged;
    }

    public String ignoreChangedPattern() {
        return ignoreChangedPattern;
    }

    public Set<MavenProject> getIgnoredProjects() {
        return ignoredProjects;
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "enabled=" + enabled +
                ", key=" + key +
                ", referenceBranch='" + referenceBranch + '\'' +
                ", baseBranch='" + baseBranch + '\'' +
                ", uncommited=" + uncommited +
                ", untracked=" + untracked +
                ", makeUpstream=" + makeUpstream +
                ", skipTestsForNotImpactedModules=" + skipTestsForNotImpactedModules +
                ", buildAll=" + buildAll +
                ", compareToMergeBase=" + compareToMergeBase +
                ", fetchBaseBranch=" + fetchBaseBranch +
                ", fetchReferenceBranch=" + fetchReferenceBranch +
                ", outputFile=" + outputFile +
                ", writeChanged=" + writeChanged +
                ", ignoreChangedPattern='" + ignoreChangedPattern + '\'' +
                '}';
    }
}
