package autorepair.testcasesgenerate;

import java.io.File;
import java.io.IOException;
import java.util.List;

import autorepair.patch.Patch;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.io.FileUtils;

public class ParseTest {


    public ParseTest(String originalTestSavePath, String sourceName, String targetName,
                     ModifyClass modifyClass, List<Patch> patchList) {
        this.originalTestSavePath = originalTestSavePath;
        this.sourceName = sourceName;
        this.targetName = targetName;
        this.modifyClass = modifyClass;
        this.patchList = patchList;
    }

    String originalTestSavePath;
    String sourceName;
    String targetName;
    ModifyClass modifyClass;
    List<Patch> patchList;

//    public static void main(String[] args) {
//        ParseTest parseTest = new ParseTest();
//        parseTest.parseAndSerialize("D:\\rep\\WebUIAutoRepair\\src\\main\\java\\testcases\\mantisbt\\model_based_dataset\\AddProfileTest.java");
//    }

    /**
     * parse a test, get its static information and serialize a JSON file
     *
     * @return EnhancedTestCase
     */
    public void parseAndSerialize() throws IOException {
        CompilationUnit cu = null;
        String clazz = originalTestSavePath + sourceName + ".java";
        try {
            cu = JavaParser.parse(new File(clazz));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        new MethodVisitor().visit(cu, clazz);

        new ClassVisitor().visit(cu, clazz);

        File fileMod = new File(originalTestSavePath + targetName + ".java");
        FileUtils.touch(fileMod);
        FileUtils.writeStringToFile(fileMod, cu.toString());
    }

    public static int getTestStartLine(String clazz) {
        CompilationUnit cu = null;
        try {
            cu = JavaParser.parse(new File(clazz));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        new MethodVisitor2().visit(cu, clazz);
        return line;
    }

    static int line;

    private static class MethodVisitor2 extends VoidVisitorAdapter<Object> {
        @Override
        public void visit(MethodDeclaration m, Object arg) {
            if (m.getAnnotations() != null && equalValue(m.getAnnotations(), "Test")) {
                    line = m.getBeginLine();
            }
        }
    }


    private class ClassVisitor extends VoidVisitorAdapter<Object> {
        @Override
        public void visit(ClassOrInterfaceDeclaration m, Object arg) {
            m.setName(targetName);
        }
    }


    /**
     * Simple visitor implementation for visiting MethodDeclaration nodes.
     */
    private class MethodVisitor extends VoidVisitorAdapter<Object> {
        @Override
        public void visit(MethodDeclaration m, Object arg) {

            if (m.getAnnotations() != null && equalValue(m.getAnnotations(), "Test")) {

                BlockStmt newBlockStmt = new BlockStmt();
                for (Statement statement : m.getBody().getStmts()) {
                    if (statement.getBeginLine() < modifyClass.getStartLine()) {
                        boolean repaired = false;
                        for (Patch patch : patchList) {
                            int codeline = Integer.parseInt(patch.getCodeLine().substring(patch.getCodeLine().lastIndexOf(":") + 1));
                            if (statement.getBeginLine() == codeline) {

                                String st = statement.toString();
                                if (st.contains("findElement")) {
                                    String start = "findElement(By.";
                                    StringBuilder code = new StringBuilder();
                                    code.append(st.substring(0, st.indexOf(start) + start.length()));
                                    code.append(patch.getIdentification().getHow()).append("(\"").append(patch.getIdentification().getValue()).append("\"))");
                                    code.append(st.substring(st.lastIndexOf("\"))") + "\"))".length()));
                                    ASTHelper.addStmt(newBlockStmt, new NameExpr(code.toString().replaceAll(";","")));
                                }
                                repaired = true;
//                            ASTHelper.addStmt(newBlockStmt, new NameExpr(patch.getCodeLine() + "\n"));
                            }
                        }
                        if (!repaired) {
                            System.out.println("no");
                            ASTHelper.addStmt(newBlockStmt, statement);
                        } else {
                            System.out.println("yes");
                        }
                    }
                }
                for (String str : modifyClass.getAddCode()) {
                    ASTHelper.addStmt(newBlockStmt, new NameExpr(str.replaceAll(";","") ));
                }
                m.setBody(newBlockStmt);
            }
        }
    }

    public static boolean equalValue(List<AnnotationExpr> annotations, String str) {
        for (AnnotationExpr annotationExpr : annotations) {
            String annotation = annotationExpr.toString();
            if (annotation.contains("(")) {
                annotation = annotation.substring(1, annotation.indexOf("("));
            }
            if (annotation.equals(str)) {
                return true;
            }
        }
        return false;
    }


}
