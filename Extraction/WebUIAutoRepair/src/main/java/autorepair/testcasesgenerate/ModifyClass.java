package autorepair.testcasesgenerate;

import java.util.ArrayList;
import java.util.List;

public class ModifyClass {
    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    private int startLine;

    public List<String> getAddCode() {
        return addCode;
    }

    public void setAddCode(List<String> addCode) {
        this.addCode = addCode;
    }

    private List<String> addCode;
    public ModifyClass(int startLine){
        this.startLine = startLine;
        this.addCode = new ArrayList<>();
    }

    public void addCode(String code){
        addCode.add(code);
    }

}
