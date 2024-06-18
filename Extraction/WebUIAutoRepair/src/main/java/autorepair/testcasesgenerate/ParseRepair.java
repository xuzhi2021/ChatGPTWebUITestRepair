package autorepair.testcasesgenerate;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

public class ParseRepair {
    public static void main(String[] args) {
        CompilationUnit cu = null;
        String clazz = "D:\\rep\\WebUIAutoRepair\\src\\main\\java\\testcases\\mantisbt\\model_based_dataset\\AddNewsTest.java";
        try {
            cu = JavaParser.parse(new File(clazz));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }

        new MethodVisitor().visit(cu, clazz);
    }

    private static class MethodVisitor extends VoidVisitorAdapter<Object> {
        @Override
        public void visit(MethodDeclaration m, Object arg) {
            BlockStmt body = m.getBody();
            for (Statement stmt : body.getStmts()) {

                Queue<Node> nodeQueue = new ArrayDeque<>(stmt.getChildrenNodes());
                while (!nodeQueue.isEmpty()){
                    Node node = nodeQueue.poll();
                    if (node.getBeginLine() == 32){
                        System.out.println(node);
                        System.out.println(node.getChildrenNodes());
                        System.out.println(node.getParentNode());
                        break;
                    }
                }
            }

        }
    }
}
