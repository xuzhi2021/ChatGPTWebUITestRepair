package autorepair.validate;

import java.util.List;

public class Delta {
    public List<Integer> getOldIndexList() {
        return oldIndexList;
    }

    public void setOldIndexList(List<Integer> oldIndexList) {
        this.oldIndexList = oldIndexList;
    }

    public List<Integer> getNewIndexList() {
        return newIndexList;
    }

    public void setNewIndexList(List<Integer> newIndexList) {
        this.newIndexList = newIndexList;
    }

    private List<Integer> oldIndexList;
    private List<Integer> newIndexList;

    public Delta(List<Integer> oldIndexList, List<Integer> newIndexList) {
        this.oldIndexList = oldIndexList;
        this.newIndexList = newIndexList;

    }

    public int getSize(){
        return oldIndexList.size() + newIndexList.size();
    }

    @Override
    public String toString() {
        return "Delta{" +
                "oldIndexList=" + oldIndexList +
                ", newIndexList=" + newIndexList +
                '}';
    }
}
