import at.jku.isse.ecco.adapter.c.CReader;
import at.jku.isse.ecco.adapter.c.data.FunctionArtifactData;
import at.jku.isse.ecco.adapter.c.data.LineArtifactData;
import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.artifact.ArtifactData;
import at.jku.isse.ecco.storage.mem.dao.MemEntityFactory;
import at.jku.isse.ecco.tree.Node;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CReaderIntegrationTest {

    @Test
    public void readFileTest() throws URISyntaxException {
        String relativeResourceFolderPath = "C_SPL/simple_file";
        URI resourceFolderUri = Objects.requireNonNull(getClass().getClassLoader().getResource(relativeResourceFolderPath)).toURI();
        String resourceFolderPathString = Paths.get(resourceFolderUri).toString();
        Path resourceFolderPath = Paths.get(resourceFolderPathString);

        Set<Node.Op> nodes = readFolder(resourceFolderPath);

        assertEquals(1, nodes.size());
        Node.Op resultPluginNode = nodes.iterator().next();
        testSimpleFile(resultPluginNode);
    }

    @Test
    public void testGrammarBugHandling() throws URISyntaxException {
        // a buggy function will not be added as function node but as multiple line nodes

        String relativeResourceFolderPath = "C_SPL/buggy_file";
        URI resourceFolderUri = Objects.requireNonNull(getClass().getClassLoader().getResource(relativeResourceFolderPath)).toURI();
        String resourceFolderPathString = Paths.get(resourceFolderUri).toString();
        Path resourceFolderPath = Paths.get(resourceFolderPathString);

        Set<Node.Op> nodes = readFolder(resourceFolderPath);

        assertEquals(1, nodes.size());
        Node.Op resultPluginNode = nodes.iterator().next();
        List<Node.Op> pluginNodeChildren = (List<Node.Op>) resultPluginNode.getChildren();

        assertEquals(11, pluginNodeChildren.size());
        checkLineNode(pluginNodeChildren.get(0), "#include <stdio.h>");
        checkLineNode(pluginNodeChildren.get(1), "int main({");
        checkLineNode(pluginNodeChildren.get(2), "    printf(\"Base Product\\n\");");
        checkLineNode(pluginNodeChildren.get(3), "    // Feature A");
        checkLineNode(pluginNodeChildren.get(4), "    featureA();");
        checkLineNode(pluginNodeChildren.get(5), "    // Feature A || B");
        checkLineNode(pluginNodeChildren.get(6), "    featureAOrB();");
        checkLineNode(pluginNodeChildren.get(7), "    return 0;");
        checkLineNode(pluginNodeChildren.get(8), "}");

        Node.Op functionNode = pluginNodeChildren.get(9);
        checkFunctionNode(functionNode, "voidfeatureA()");
        List<Node.Op> functionLines = (List<Node.Op>) functionNode.getChildren();
        assertEquals(3, functionLines.size());
        checkLineNode(functionLines.get(0), "void featureA() {");
        checkLineNode(functionLines.get(1), "    printf(\"Hello, this is Feature A!\\n\");");
        checkLineNode(functionLines.get(2), "}");

        functionNode = pluginNodeChildren.get(10);
        checkFunctionNode(functionNode, "voidfeatureAOrB()");
        functionLines = (List<Node.Op>) functionNode.getChildren();
        assertEquals(3, functionLines.size());
        checkLineNode(functionLines.get(0), "void featureAOrB() {");
        checkLineNode(functionLines.get(1), "    printf(\"Hello, this is Feature A || B!\\n\");");
        checkLineNode(functionLines.get(2), "}");
    }

    @Test
    public void readMultipleFiles() throws URISyntaxException {
        String relativeResourceFolderPath = "C_SPL/multiple_files";
        URI resourceFolderUri = Objects.requireNonNull(getClass().getClassLoader().getResource(relativeResourceFolderPath)).toURI();
        String resourceFolderPathString = Paths.get(resourceFolderUri).toString();
        Path resourceFolderPath = Paths.get(resourceFolderPathString);

        Set<Node.Op> nodes = readFolder(resourceFolderPath);
        nodes.forEach(this::testSimpleFile);
    }

    private void testSimpleFile(Node.Op pluginNode){
        List<Node.Op> pluginNodeChildren = (List<Node.Op>) pluginNode.getChildren();

        assertEquals(4, pluginNodeChildren.size());
        checkLineNode(pluginNodeChildren.get(0), "#include <stdio.h>");

        Node.Op functionNode = pluginNodeChildren.get(1);
        checkFunctionNode(functionNode, "intmain()");
        List<Node.Op> functionLines = (List<Node.Op>) functionNode.getChildren();
        assertEquals(8, functionLines.size());
        checkLineNode(functionLines.get(0), "int main() {");
        checkLineNode(functionLines.get(1), "    printf(\"Base Product\\n\");");
        checkLineNode(functionLines.get(2), "    // Feature A");
        checkLineNode(functionLines.get(3), "    featureA();");
        checkLineNode(functionLines.get(4), "    // Feature A || B");
        checkLineNode(functionLines.get(5), "    featureAOrB();");
        checkLineNode(functionLines.get(6), "    return 0;");
        checkLineNode(functionLines.get(7), "}");

        functionNode = pluginNodeChildren.get(2);
        checkFunctionNode(functionNode, "voidfeatureA()");
        functionLines = (List<Node.Op>) functionNode.getChildren();
        assertEquals(3, functionLines.size());
        checkLineNode(functionLines.get(0), "void featureA() {");
        checkLineNode(functionLines.get(1), "    printf(\"Hello, this is Feature A!\\n\");");
        checkLineNode(functionLines.get(2), "}");

        functionNode = pluginNodeChildren.get(3);
        checkFunctionNode(functionNode, "voidfeatureAOrB()");
        assertEquals(3, functionLines.size());
        functionLines = (List<Node.Op>) functionNode.getChildren();
        checkLineNode(functionLines.get(0), "void featureAOrB() {");
        checkLineNode(functionLines.get(1), "    printf(\"Hello, this is Feature A || B!\\n\");");
        checkLineNode(functionLines.get(2), "}");
    }

    private Set<Node.Op> readFolder(Path folderPath){
        CReader reader = new CReader(new MemEntityFactory());
        Collection<Path> relativeFiles = this.getRelativeDirContent(reader, folderPath);
        Path[] relativeFileAr = relativeFiles.toArray(new Path[0]);
        return reader.read(folderPath, relativeFileAr);
    }

    private Collection<Path> getRelativeDirContent(CReader reader, Path dir){
        Map<Integer, String[]> prioritizedPatterns = reader.getPrioritizedPatterns();
        String[] patterns = prioritizedPatterns.values().iterator().next();
        Collection<PathMatcher> pathMatcher = new ArrayList<>();
        for (String p : patterns) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + p);
            pathMatcher.add(matcher);
        }

        Set<Path> fileSet = new HashSet<>();
        try (Stream<Path> pathStream = Files.walk(dir)) {
            pathStream.forEach( path -> {
                Boolean applicableFile = pathMatcher.stream().map(pm -> pm.matches(path)).reduce(Boolean::logicalOr).get();
                if (applicableFile) {
                    fileSet.add(path);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileSet.stream().map(dir::relativize).collect(Collectors.toList());
    }

    private void checkLineNode(Node.Op node, String line){
        assertNotNull(node);
        Artifact<?> artifact = node.getArtifact();
        ArtifactData artifactData = artifact.getData();
        assertTrue(artifactData instanceof LineArtifactData);
        assertEquals(line, ((LineArtifactData) artifactData).getLine());
    }

    private void checkFunctionNode(Node.Op node, String signature){
        assertNotNull(node);
        Artifact<?> artifact = node.getArtifact();
        ArtifactData artifactData = artifact.getData();
        assertTrue(artifactData instanceof FunctionArtifactData);
        assertEquals(signature, ((FunctionArtifactData) artifactData).getSignature());
    }
}
