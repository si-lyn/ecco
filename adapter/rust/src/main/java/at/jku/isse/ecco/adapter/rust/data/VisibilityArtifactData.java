package at.jku.isse.ecco.adapter.rust.data;

import at.jku.isse.ecco.artifact.ArtifactData;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @param visibility e.g., "pub", "pub(crate)", etc.
 */
public record VisibilityArtifactData(String visibility) implements ArtifactData, RustWritable {
    @Override
    public void write(BufferedWriter bw) throws IOException {
        bw.write(visibility + " ");
    }
}
