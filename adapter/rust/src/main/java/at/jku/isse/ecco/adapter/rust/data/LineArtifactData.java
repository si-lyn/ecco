package at.jku.isse.ecco.adapter.rust.data;

import at.jku.isse.ecco.artifact.ArtifactData;

import java.io.BufferedWriter;
import java.io.IOException;

public record LineArtifactData(String line) implements ArtifactData, RustWritable {
    public void write(BufferedWriter bw) throws IOException {
        bw.write(this.line);
        bw.newLine();
    }
}
