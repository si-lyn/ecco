package at.jku.isse.ecco.adapter.rust.data;

import at.jku.isse.ecco.artifact.ArtifactData;
import lombok.Data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Objects;

@Data
public class AttributeArtifactData implements ArtifactData {
    // String used to identify the attribute, not to write it
    private final String attribute; // e.g., "#[derive(Debug)]", "#[test]", etc.
}
