package autorepair.state.delta;

import autorepair.match.sustech.Sustech;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DomNodeDifference implements Difference {
    private List<Integer> addList;
    private List<Integer> deleteList;
    private List<Integer> changeNodeList;

    public DomNodeDifference() {
        addList = new ArrayList<>();
        deleteList = new ArrayList<>();
        changeNodeList = new ArrayList<>();
    }

    @Override
    public List<Integer> getAddList() {
        return addList;
    }

    @Override
    public List<Integer> getDeleteList() {
        return deleteList;
    }

    @Override
    public List<Integer> getChangeNodeList() {
        return changeNodeList;
    }

}


