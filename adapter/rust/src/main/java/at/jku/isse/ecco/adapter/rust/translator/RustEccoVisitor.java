package at.jku.isse.ecco.adapter.rust.translator;


import at.jku.isse.ecco.EccoException;
import at.jku.isse.ecco.adapter.rust.antlr.RustParser;
import at.jku.isse.ecco.adapter.rust.antlr.RustParserBaseVisitor;
import at.jku.isse.ecco.adapter.rust.data.*;
import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.artifact.ArtifactData;
import at.jku.isse.ecco.dao.EntityFactory;
import at.jku.isse.ecco.featuretrace.FeatureTrace;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.util.Location;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.Token;
import org.logicng.formulas.Formula;

import java.nio.file.Path;
import java.util.*;

public class RustEccoVisitor extends RustParserBaseVisitor<Node.Op> {
    private static final String CONDITION_PROPERTY = "condition";
    private final Deque<Node.Op> nodeStack = new ArrayDeque<>();
    private final Node.Op pluginNode;
    private final String[] codeLines;
    private final EntityFactory entityFactory;
    private final Path path;
    private final String configuration;

    public RustEccoVisitor(Node.Op pluginNode, String[] codeLines, EntityFactory entityFactory, Path path, String configuration) {
        this.pluginNode = pluginNode;
        this.codeLines = codeLines;
        this.entityFactory = entityFactory;
        this.path = path;
        this.configuration = configuration;
        if (this.pluginNode == null) {
            throw new EccoException("Plugin node cannot be null.");
        }
        nodeStack.push(pluginNode);
    }

    public Node.Op translate(ParseTree tree) {
        return tree.accept(this);
    }

    /**
     * Visit a parse tree produced by {@link RustParser#crate}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    @Override
    public Node.Op visitCrate(RustParser.CrateContext ctx) {
        for (ParseTree parseTree : ctx.children) {
            parseTree.accept(this);
        }
        return this.pluginNode;
    }

    @Override
    public Node.Op visitVisibility(RustParser.VisibilityContext ctx) {
        //does not create LineNodes because writing pub is handled by RustWriter
        return createSimpleNode(new VisibilityArtifactData(getString(ctx)));
    }

    // Comments in module not tracked since they are not parsed
    @Override
    public Node.Op visitModule(RustParser.ModuleContext ctx) {
        StringBuilder sig = new StringBuilder();
        if (ctx.KW_UNSAFE() != null) {
            sig.append("unsafe ");
        }
        sig.append("mod ").append(getString(ctx.identifier()));
        if (ctx.SEMI() != null) {
            // module is just a declaration
            sig.append(";");
        } else {
            //module contains something
            sig.append(" {");
        }
        Node.Op moduleNode = createSimpleNode(new ModuleArtifactData(sig.toString()));

        // no semicolon means module contains items
        if (ctx.SEMI() == null) {
            nodeStack.push(moduleNode);
            ctx.item().forEach(item -> item.accept(this));
            nodeStack.pop();
            Artifact.Op<LineArtifactData> line = this.entityFactory.createArtifact(new LineArtifactData("}"));
            moduleNode.addChild(this.entityFactory.createOrderedNode(line));
        }
        return moduleNode;
    }

    @Override
    public Node.Op visitItem(RustParser.ItemContext ctx) {
        // If item has no children, the item is likely created by the antlr parser because of a parse error
        if (ctx.macroItem() == null && ctx.visItem() == null) {
            return null;
        }
        Artifact.Op<ItemArtifactData> item = this.entityFactory.createArtifact(new ItemArtifactData());
        Node.Op itemNode = createArtifactOrderedNodeAndAddToParent(item, nodeStack.peek());
        nodeStack.push(itemNode);

        // process outer attributes to get condition for feature trace
        // Item can have multiple outer attributes, so we look for conditions in all of them
        List<String> conditions = ctx.outerAttribute().stream()
                .map(attrCtx -> attrCtx.accept(this))
                .map(node -> node.getProperty(CONDITION_PROPERTY))
                .flatMap(Optional::stream)
                .map(Object::toString)
                .toList();
        String condition = conditions.isEmpty() ? "" : String.join(" & ", conditions); // to handle multiple conditions on an item

        Token stop = ctx.stop;
        // stop can be null in some cases, e.g., for macro items without a proper ending. So attempts to get stop from children
        if (stop == null) {
            if (ctx.macroItem() != null) {
                stop = ctx.macroItem().stop;
            } else if (ctx.visItem() != null) {
                stop = ctx.visItem().stop;
            }
        }
        // if still null throw exception
        if (stop == null) {
            throw new EccoException("Cannot determine end of ItemContext at " + ctx.start.getLine() + " in " + this.path.toAbsolutePath() + "\n with text: " + ctx.getText());
        }

        Location location = new Location(ctx.start.getLine(), stop.getLine(), this.path, this.configuration);
        itemNode.putProperty("Location", location);
        FeatureTrace nodeTrace = itemNode.getFeatureTrace();
        nodeTrace.buildProactiveConditionConjunction(condition);

        // visit rest of the children of RustParser.ItemContext if they are present
        if (ctx.macroItem() != null) ctx.macroItem().accept(this);
        if (ctx.visItem() != null) ctx.visItem().accept(this);
        nodeStack.pop();
        return itemNode;
    }

    @Override
    public Node.Op visitConstantItem(RustParser.ConstantItemContext ctx) {
        return createNodeWithLines(new ConstantArtifactData(), ctx);
    }

    @Override
    public Node.Op visitDocComment(RustParser.DocCommentContext ctx) {
        return createNodeWithLines(new DocArtifactData(), ctx);
    }

    @Override
    public Node.Op visitStatements(RustParser.StatementsContext ctx) {
        this.addLineNodesFromContext(nodeStack.peek(), ctx);

        // when super.visitStatements comes to a macro the tree looks like statement -> item -> macroItem(thus adding a item artifact)
        // which means going deeper in parseTree not needed here
        return nodeStack.peek();
    }

    @Override
    public Node.Op visitBlockExpression(RustParser.BlockExpressionContext ctx) {
        return createNodeWithLines(new BlockArtifactData(), ctx);
    }

    @Override
    public Node.Op visitFunction_(RustParser.Function_Context ctx) {
        Node.Op functionNode = createSimpleNode(new FunctionArtifactData(this.getFunctionSignature(ctx)));

        this.nodeStack.push(functionNode);
        // visit all childen as some may want to add children to function node
        Node.Op visited = super.visitFunction_(ctx);
        this.nodeStack.pop();
        return visited;
    }

    @Override
    public Node.Op visitStruct_(RustParser.Struct_Context ctx) {
        return createNodeWithLines(new StructArtifactData(), ctx);
    }

    @Override
    public Node.Op visitTypeAlias(RustParser.TypeAliasContext ctx) {
        return createNodeWithLines(new TypeAliasArtifactData(), ctx);
    }

    @Override
    public Node.Op visitTrait_(RustParser.Trait_Context ctx) {
        return createNodeWithLines(new TraitArtifactData(), ctx);
    }

    @Override
    public Node.Op visitExternCrate(RustParser.ExternCrateContext ctx) {
        return createNodeWithLines(new ExternCrateArtifactData(), ctx);
    }

    @Override
    public Node.Op visitStaticItem(RustParser.StaticItemContext ctx) {
        return createNodeWithLines(new StaticArtifactData(), ctx);
    }

    @Override
    public Node.Op visitOuterAttribute(RustParser.OuterAttributeContext ctx) {
        //if the outerAttribute is a comment only visit the comment
        if (ctx.docComment() != null) return visitDocComment(ctx.docComment());

        // Visit cfg attribute and convert to condition(formula)
        Optional<Formula> condition = extractCondition(ctx);

        Node.Op node = createNodeWithLines(new AttributeArtifactData(ctx.getText()), ctx);

        // Store condition in node to use it in parent item
        condition.map(Formula::toString).ifPresent(s -> node.putProperty(CONDITION_PROPERTY, s));
        return node;
    }

    /** 
     * Extract condition formula from outer attribute context if it contains cfg or cfg_attr
     * @param ctx the OuterAttributeContext
     * @return Optional containing the condition formula if present, otherwise empty
     */
    private Optional<Formula> extractCondition(RustParser.OuterAttributeContext ctx) {
        final RustParser.AttrContext attr = ctx.attr();
        if (attr == null) return Optional.empty();
        ConfigurationPredicateVisitor configVisitor = new ConfigurationPredicateVisitor();
        RustParser.CfgAttributeContext attrCtx = attr.cfgAttribute();
        RustParser.CfgAttrAttributeContext cfgAttrCtx = attr.cfgAttrAttribute();

        if (attrCtx != null) {
            // outer attribute has a cfg like: ![cfg(...)]
            return Optional.of(configVisitor.visitCfgAttribute(attrCtx));
        } else if (cfgAttrCtx != null) {
            // outer attribute has a cfg_attr like: ![cfg_attr(...)]
            return Optional.of(configVisitor.visitCfgAttrAttribute(cfgAttrCtx));
        }
        return Optional.empty();
    }

    @Override
    public Node.Op visitInnerAttribute(RustParser.InnerAttributeContext ctx) {
        return createNodeWithLines(new AttributeArtifactData(ctx.getText()), ctx);
    }

    @Override
    public Node.Op visitImplementation(RustParser.ImplementationContext ctx) {
        return createNodeWithLines(new ImplementationArtifactData(), ctx);
    }

    @Override
    public Node.Op visitUnion_(RustParser.Union_Context ctx) {
        return createNodeWithLines(new UnionArtifactData(), ctx);
    }

    @Override
    public Node.Op visitUseDeclaration(RustParser.UseDeclarationContext ctx) {
        return createNodeWithLines(new UseDeclarationArtifactData(), ctx);
    }

    @Override
    public Node.Op visitEnumeration(RustParser.EnumerationContext ctx) {
        Node.Op node = createSimpleNode(new EnumArtifactData());

        // Inside visitEnumeration
        int enumStartLine = ctx.getStart().getLine();
        int enumStartPos = ctx.getStart().getCharPositionInLine();
        int enumEndLine = ctx.getStop().getLine();

        if (ctx.enumItems() == null) {
            // No enum items, add all lines of the enum
            this.addLineNodesFromContext(node, ctx);
            return node;
        }
        int itemsStartLine = ctx.enumItems().getStart().getLine();
        int itemsEndLine = ctx.enumItems().getStop().getLine();

        // Add lines before enumItems
        // max value is handled my addLineNodes method
        this.addLineNodes(node, enumStartLine, itemsStartLine - 1, enumStartPos, Integer.MAX_VALUE);

        // Visit enumItems to add them as child nodes
        this.nodeStack.push(node);
        ctx.enumItems().enumItem().forEach(enumItemContext -> enumItemContext.accept(this));
        this.nodeStack.pop();

        // Add lines after enumItems
        this.addLineNodes(node, itemsEndLine+1, enumEndLine);

        return node;
    }

    @Override
    public Node.Op visitEnumItem(RustParser.EnumItemContext ctx) {
        // content of enumArtifact is not used, it is only used as an identifier for the artifact, so the ecco hashcode and equals work properly
        Node.Op node = createSimpleNode(new EnumItemArtifactData(getString(ctx.identifier())));

        int startLine = ctx.getStart().getLine();
        int endLine = ctx.getStop().getLine();
        this.addLineNodes(node, startLine, endLine);
        return node;
    }

    @Override
    public Node.Op visitMacroInvocationSemi(RustParser.MacroInvocationSemiContext ctx) {
        Node.Op node = createSimpleNode(new MacroInvocationArtifactData());

        int startLine = ctx.getStart().getLine();
        int endLine = ctx.getStop().getLine();
        // @TODO takes the whole line, could be improved to only take the macro invocation part
        this.addLineNodes(node, startLine, endLine);
        return node;
    }

    // Content of a MacroItems is TokenTrees which are not further parsed, so we just add the whole macro as line artifacts
    // Which means conditionals inside macros are not handled
    @Override
    public Node.Op visitMacroRulesDefinition(RustParser.MacroRulesDefinitionContext ctx) {
        String identifier = ctx.identifier().getText();
        Artifact.Op<MacroRulesArtifactData> item = this.entityFactory.createArtifact(new MacroRulesArtifactData(identifier));
        Node.Op node = createArtifactOrderedNodeAndAddToParent(item, this.nodeStack.peek());
        this.addLineNodesFromContext(node, ctx);

        return node;
    }

    /** Get the function signature as a string from the given Function_Context
     * @param ctx the Function_Context to extract the signature from
     * @return String representing the function signature
     */
    private String getFunctionSignature(RustParser.Function_Context ctx) {
        StringBuilder sig = new StringBuilder();

        // optional qualifiers
        if (ctx.functionQualifiers() != null) {
            // getString(ctx.functionQualifiers()) does not always work, since the stop variable can be null
            RustParser.FunctionQualifiersContext qualifiers = ctx.functionQualifiers();
            if (qualifiers.KW_CONST() != null) sig.append("const ");
            if (qualifiers.KW_ASYNC() != null) sig.append("async ");
            if (qualifiers.KW_UNSAFE() != null) sig.append("unsafe ");
            if (qualifiers.KW_EXTERN() != null) sig.append("extern ");
            if (qualifiers.abi() != null) sig.append(getString(qualifiers.abi())).append(" ");
        }
        // “fn” and name
        sig.append("fn").append(" ").append(getString(ctx.identifier()));

        // optional generics
        if (ctx.genericParams() != null) {
            sig.append(getString(ctx.genericParams()));
        }
        // parameters
        sig.append("(");
        if (ctx.functionParameters() != null) {
            sig.append(getString(ctx.functionParameters()));
        }
        sig.append(")");
        // optional return type
        if (ctx.functionReturnType() != null) {
            sig.append(" ").append(getString(ctx.functionReturnType()));
        }
        // optional where‐clause
        if (ctx.whereClause() != null) {
            sig.append(" ").append(getString(ctx.whereClause()));
        }
        // Add space before the function body
        sig.append(" ");
        return sig.toString();
    }

    /**
     * Add line artifacts for each line from startLine to endLine (inclusive) to the given parent node.
     * @param parentNode node to add line artifacts to
     * @param startLine line to start from
     * @param endLine line to end at
     */
    private void addLineNodes(Node.Op parentNode, int startLine, int endLine) {
        for (int i = startLine; i <= endLine; i++) {
            // -1 for 0 based index
            String codeLine = this.codeLines[i - 1];
            if (codeLine.isEmpty()) {
                continue;
            }
            Artifact.Op<LineArtifactData> lineArtifactData = this.entityFactory.createArtifact(new LineArtifactData(codeLine));
            createArtifactOrderedNodeAndAddToParent(lineArtifactData, parentNode);
        }
    }

    /**
     * Add line artifacts for each line from startLine to endLine (inclusive) to the given parent node.
     * Only the part of the first and last line between startPosition and endPosition is added.
     * @param parentNode node to add line artifacts to
     * @param startLine line to start from
     * @param endLine line to end at
     * @param startPosition
     * @param endPosition
     */
    private void addLineNodes(Node.Op parentNode, int startLine, int endLine, int startPosition, int endPosition) {
        if (startLine == endLine) {
            // Single line
            String codeLine = this.codeLines[startLine - 1];
            if (!codeLine.isEmpty()) {
                String extractedLine = codeLine.substring(startPosition, Math.min(endPosition, codeLine.length()));
                Artifact.Op<LineArtifactData> lineArtifactData = this.entityFactory.createArtifact(new LineArtifactData(extractedLine));
                createArtifactOrderedNodeAndAddToParent(lineArtifactData, parentNode);
            }
            return;
        }

        for (int i = startLine; i <= endLine; i++) {
            // -1 for 0 based index
            String codeLine = this.codeLines[i - 1];
            if (codeLine.isEmpty()) {
                continue;
            }
            String extractedLine;
            if (i == startLine) {
                // First line
                extractedLine = codeLine.substring(startPosition);
            } else if (i == endLine) {
                // Last line
                extractedLine = codeLine.substring(0, Math.min(endPosition, codeLine.length()));
            } else {
                // Middle lines
                extractedLine = codeLine;
            }
            Artifact.Op<LineArtifactData> lineArtifactData = this.entityFactory.createArtifact(new LineArtifactData(extractedLine));
            createArtifactOrderedNodeAndAddToParent(lineArtifactData, parentNode);
        }
    }

    /**
     * helper function to add all lines from a context as line artifacts to a parent node
     * @param parentNode node to add line artifacts to
     * @param ctx context to extract lines from
     */
    private void addLineNodesFromContext(Node.Op parentNode, ParserRuleContext ctx) {
        int startLine = ctx.start.getLine();
        int stopLine = ctx.stop.getLine();
        int startPos = ctx.start.getCharPositionInLine();
        int stopPos = ctx.stop.getCharPositionInLine() + ctx.stop.getText().length();

        this.addLineNodes(parentNode, startLine, stopLine, startPos, stopPos);
    }

    /**
     * Ordered artifact are assigned sequence numbers based on their order of occurrence.
     * This assigned sequence number is used as an additional means of identifying the child artifacts
     * @param artifact
     * @param parentNode
     * @return Some ArtifactData
     */
    private <T extends ArtifactData> Node.Op createArtifactOrderedNodeAndAddToParent(Artifact.Op<T> artifact, Node.Op parentNode) {
        Node.Op node = this.entityFactory.createOrderedNode(artifact);
        assert parentNode != null;
        parentNode.addChild(node);
        return node;
    }

    /** Create a simple node with the given artifact data and add it to the current parent node
     * @param artifactData the ArtifactData for the node
     * @return Node.Op containing the artifact
     */
    private <T extends ArtifactData> Node.Op createSimpleNode(T artifactData) {
        Artifact.Op<T> artifact = this.entityFactory.createArtifact(artifactData);
        return createArtifactOrderedNodeAndAddToParent(artifact, nodeStack.peek());
    }

    /** Create a node with line artifacts for each line in the given context
     * @param artifactData the ArtifactData for the main node
     * @param ctx the ParserRuleContext to extract lines from
     * @return Node.Op containing the main artifact and line artifacts as children
     */
    private <T extends ArtifactData> Node.Op createNodeWithLines(T artifactData, ParserRuleContext ctx) {
        Node.Op node = createSimpleNode(artifactData);
        this.addLineNodesFromContext(node, ctx);
        return node;
    }

    /**
     * Get the original source code string represented by the given parse tree context.
     * @param ctx the parse tree context
     * @return String of the original source code represented by the context
     */
    public String getString(ParserRuleContext ctx) {
        if (ctx == null) {
            return "";
        }

        int startPosition = ctx.start.getCharPositionInLine();
        int endPosition = ctx.stop.getCharPositionInLine() + ctx.stop.getText().length();
        int startLine = ctx.start.getLine();
        int stopLine = ctx.stop.getLine();

        if (startLine == stopLine) {
            return this.codeLines[startLine - 1].substring(startPosition, endPosition);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = startLine; i <= stopLine; i++) {
            String codeLine;
            // Handle 1 based line numbers and 0 based array index
            codeLine = this.codeLines[i - 1];
            if (codeLine.isEmpty()) {
                continue;
            }
            if (i == startLine) {
                // first Line
                sb.append(codeLine.substring(startPosition)).append("\n");
            } else if (i == stopLine) {
                // last Line
                sb.append(codeLine, 0, Math.min(endPosition, codeLine.length()));
            } else {
                // Middle line
                sb.append(codeLine);
            }
        }
        return sb.toString();
    }

}
