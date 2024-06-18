package autorepair.testcasesgenerate;

public class ModifyLine {

    private int codeLine;

    public int getCodeLine() {
        return codeLine;
    }

    public void setCodeLine(int codeLine) {
        this.codeLine = codeLine;
    }

    public String getNewCode() {
        return newCode;
    }

    public void setNewCode(String newCode) {
        this.newCode = newCode;
    }

    private String newCode;

    public ModifyLine(int codeLine, String newCode) {
        this.codeLine = codeLine;
        this.newCode = newCode;
    }
}
