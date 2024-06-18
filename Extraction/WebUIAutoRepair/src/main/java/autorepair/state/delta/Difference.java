package autorepair.state.delta;

import java.util.List;

public interface Difference {
    public List<Integer> getAddList();
    public List<Integer> getDeleteList();
    public List<Integer> getChangeNodeList();

}
