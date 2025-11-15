package at.jku.isse.ecco.adapter.rust.data;

import at.jku.isse.ecco.artifact.ArtifactData;

/**
 * @param attribute String used to identify the attribute, not to write it.
 *                 Attributes has formats like "#[derive(Debug)]", "#[test]", etc.
 */
public record AttributeArtifactData(String attribute) implements ArtifactData {
}
