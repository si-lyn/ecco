package at.jku.isse.ecco.adapter.c.translator;

import at.jku.isse.ecco.adapter.c.data.FunctionArtifactData;
import at.jku.isse.ecco.adapter.c.data.LineArtifactData;
import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.tree.Node;

import java.nio.file.Path;
import java.util.*;

public class CEccoTranslator {

    private List<FunctionStructure> functionStructures;
    private String[] codeLines;
    private EntityFactory entityFactory;
    private Path path;

    public CEccoTranslator(String[] codeLines,
                           EntityFactory entityFactory,
                           Path path){
        this.codeLines = codeLines;
        this.entityFactory = entityFactory;
        this.functionStructures = new LinkedList<>();
        this.path = path;
    }

    public void addFunctionStructure(int start, int end, String functionSignature){
        this.functionStructures.add(new FunctionStructure(start, end, functionSignature));
    }

    public void addChildrenToPluginNode(Node.Op pluginNode){
        this.sortFunctionStructures();

        int startLine = 1;
        for(FunctionStructure functionStructure : this.functionStructures){
            this.addLineNodes(pluginNode, startLine, functionStructure.getStartLine() - 1);
            Node.Op functionNode = this.createFunctionNode(functionStructure);
            pluginNode.addChild(functionNode);
            startLine = functionStructure.getEndLine() + 1;
        }

        if (this.functionStructures.size() > 0) {
            this.addLineNodes(pluginNode, startLine, this.codeLines.length);
        }
    }

    private void sortFunctionStructures(){
        Comparator<FunctionStructure> compareByStart = Comparator.comparingInt(FunctionStructure::getStartLine);
        this.functionStructures.sort(compareByStart);
    }

    private void addLineNodes(Node.Op parentNode, int startLine, int endLine){
        for(int i = startLine; i <= endLine; i++){
            String codeLine = this.codeLines[i - 1];
            if (codeLine.isEmpty()){
                continue;
            }

            Artifact.Op<LineArtifactData> lineArtifactData = this.entityFactory.createArtifact(new LineArtifactData(codeLine));
            Node.Op lineNode = this.entityFactory.createNode(lineArtifactData);
            parentNode.addChild(lineNode);
        }
    }

    private Node.Op createFunctionNode(FunctionStructure functionStructure){
        Artifact.Op<FunctionArtifactData> data = this.entityFactory.createArtifact(new FunctionArtifactData(functionStructure.getFunctionSignature()));
        Node.Op functionNode = this.entityFactory.createOrderedNode(data);
        this.addLineNodes(functionNode, functionStructure.getStartLine(), functionStructure.getEndLine());
        return functionNode;
    }
}
