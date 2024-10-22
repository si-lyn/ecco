package at.jku.isse.ecco.adapter.c.translator;


public class FunctionStructure {
    private final int startLine;
    private final int endLine;
    private final String functionSignature;

    public FunctionStructure (int startLine, int endLine, String functionSignature){
        this.startLine = startLine;
        this.endLine = endLine;
        this.functionSignature = functionSignature;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getFunctionSignature() {
        return functionSignature;
    }
}