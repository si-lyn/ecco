package at.jku.isse.ecco.adapter.rust;

import at.jku.isse.ecco.adapter.ArtifactPlugin;
import com.google.inject.Module;

public class RustPlugin extends ArtifactPlugin {
    public static final String DESCRIPTION = "Adds support for ... artefacts";

    private final RustModule module = new RustModule();

    @Override
    public String getPluginId() {
        return RustPlugin.class.getName();
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public String getName() {
        return  RustPlugin.class.getSimpleName();
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}