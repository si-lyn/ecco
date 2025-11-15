package at.jku.isse.ecco.adapter.rust.data;

import at.jku.isse.ecco.artifact.ArtifactData;

import java.io.BufferedWriter;
import java.io.IOException;

public record FunctionArtifactData(String signature) implements ArtifactData, RustWritable {

    @Override
    public void write(BufferedWriter bw) throws IOException {
        bw.write(this.signature);
    }
}
