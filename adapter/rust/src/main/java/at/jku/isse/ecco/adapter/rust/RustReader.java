package at.jku.isse.ecco.adapter.rust;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import at.jku.isse.ecco.adapter.ArtifactReader;
import at.jku.isse.ecco.service.listener.ReadListener;
import at.jku.isse.ecco.tree.Node;
import at.jku.isse.ecco.tree.Node.Op;

public class RustReader implements ArtifactReader<Path, Set<Node.Op>>{

    @Override
    public String getPluginId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPluginId'");
    }

    @Override
    public Map<Integer, String[]> getPrioritizedPatterns() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPrioritizedPatterns'");
    }

    @Override
    public Set<Op> read(Path base, Path[] input) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'read'");
    }

    @Override
    public Set<Op> read(Path[] input) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'read'");
    }

    @Override
    public void addListener(ReadListener listener) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addListener'");
    }

    @Override
    public void removeListener(ReadListener listener) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeListener'");
    }

}
