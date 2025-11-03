package at.jku.isse.ecco.adapter.rust.data;

import at.jku.isse.ecco.artifact.ArtifactData;
import lombok.Data;

import java.io.BufferedWriter;
import java.io.IOException;

@Data
public class VisibilityArtifactData implements ArtifactData, RustWritable {
    private final String visibility; // e.g., "pub", "pub(crate)", etc.

    @Override
    public void write(BufferedWriter bw) throws IOException {
        bw.write(visibility + " ");
    }
}
