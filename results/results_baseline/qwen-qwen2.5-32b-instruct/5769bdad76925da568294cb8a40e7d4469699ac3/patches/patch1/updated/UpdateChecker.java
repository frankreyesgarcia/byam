package io.jenkins.tools.incrementals.lib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class UpdateChecker {

    private final Log log;
    private final List<String> repos;
    /** keys are {@code groupId:artifactId:currentVersion:branch} */
    private final Map<String, VersionAndRepo> cache = new HashMap<>();

    private final Map<String, String> groupIdCache = new HashMap<>();

    public UpdateChecker(Log log, List<String> repos) {
        this.log = log;
        this.repos = repos;
    }

    public @CheckForNull String findGroupId(String artifactId) throws IOException, InterruptedException {
        String cacheKey = artifactId;
        if (groupIdCache.containsKey(cacheKey)) {
            log.info("Group ID Cache hit on artifact ID: " + artifactId);
            return groupIdCache.get(cacheKey);
        }

        //TODO: implement to support non-Incremental formats
        // Needs to load UC JSON and query it like https://github.com/jenkinsci/docker/pull/668
        return null;
    }

    public @CheckForNull VersionAndRepo find(String groupId, String artifactId, String currentVersion, String branch) throws Exception {
        String cacheKey = groupId + ':' + artifactId + ':' + currentVersion + ':' + branch;
        if (cache.containsKey(cacheKey)) {
            log.info("Cache hit on updates to " + groupId + ":" + artifactId + ":" + currentVersion + " within " + branch);
            return cache.get(cacheKey);
        }
        VersionAndRepo result = doFind(groupId, artifactId, currentVersion, branch);
        cache.put(cacheKey, result);
        return result;
    }

    private SortedSet<VersionAndRepo> loadVersions(String groupId, String artifactId) throws Exception {
        SortedSet<VersionAndRepo> r = new TreeSet<>();
        for (String repo : repos) {
            String mavenMetadataURL = repo + groupId.replace('.', '/') + '/' + artifactId + "/maven-metadata.xml";
            Document doc;
            try {
                doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(mavenMetadataURL);
            } catch (FileNotFoundException x) {
                continue; // not even defined in this repo, fine
            }
            Element versionsE = theElement(doc, "versions", mavenMetadataURL);
            NodeList versionEs = versionsE.getElementsByTagName("version");
            for (int i = 0; i < versionEs.getLength(); i++) {
                r.add(new VersionAndRepo(groupId, artifactId, new ComparableVersion(versionEs.item(i).getTextContent()), repo));
            }
        }
        return r;
    }

    private static final class GitHubCommit {
        final String owner;
        final String repo;
        final String hash;
        GitHubCommit(String owner, String repo, String hash) {
            this.owner = owner;
            this.repo = repo;
            this.hash = hash;
        }
        @Override public String toString() {
            return "https://github.com/" + owner + '/' + repo + "/commit/" + hash;
        }
    }

    private static boolean isAncestor(GitHubCommit ghc, String branch) throws Exception {
        try {
            GHRepository repo = GitHub.connect().getRepository(ghc.owner + '/' + ghc.repo);
            GHCommit commit = repo.getCommit(ghc.hash);
            return commit.getSHA1().equals(repo.getBranch(branch).getSHA1());
        } catch (FileNotFoundException x) {
            return false;
        }
    }

    private static Element theElement(Document doc, String tagName, String url) throws Exception {
        return theElement(doc.getElementsByTagName(tagName), tagName, url);
    }

    private static Element theElement(Element parent, String tagName, String url) throws Exception {
        return theElement(parent.getElementsByTagName(tagName), tagName, url);
    }

    private static Element theElement(NodeList nl, String tagName, String url) throws Exception {
        if (nl.getLength() != 1) {
            throw new Exception("Could not find <" + tagName + "> in " + url);
        }
        return (Element) nl.item(0);
    }

    public static void main(String... argv) throws Exception {
        if (argv.length != 4) {
            throw new IllegalStateException("Usage: java " + UpdateChecker.class.getName() + " <groupId> <artifactId> <currentVersion> <branch>");
        }
        VersionAndRepo result = new UpdateChecker(
                message -> System.err.println(message),
                Arrays.asList("https://repo.jenkins-ci.org/releases/", "https://repo.jenkins-ci.org/incrementals/")).
            find(argv[0], argv[1], argv[2], argv[3]);
        if (result != null) {
            System.err.println("Found: " + result);
        } else {
            System.err.println("Nothing found.");
        }
    }

    public static final class VersionAndRepo implements Comparable<VersionAndRepo> {
        public final String groupId;
        public final String artifactId;
        public final ComparableVersion version;
        public final String repo;
        VersionAndRepo(String groupId, String artifactId, ComparableVersion version, String repo) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.repo = repo;
        }
        /** Sort by version descending. */
        @Override public int compareTo(VersionAndRepo o) {
            assert o.groupId.equals(groupId) && o.artifactId.equals(artifactId);
            return o.version.compareTo(version);
        }
        /** @return for example: {@code https://repo/net/nowhere/lib/1.23/} */
        public String baseURL() {
            return repo + groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/';
        }
        /**
         * @param type for example, {@code pom}
         * @return for example: {@code https://repo/net/nowhere/lib/1.23/lib-1.23.pom}
         */
        public String fullURL(String type) {
            return baseURL() + artifactId + '-' + version + '.' + type;
        }
        @Override public String toString() {
            return baseURL();
        }
    }

    public interface Log {
        void info(String message);
    }
}