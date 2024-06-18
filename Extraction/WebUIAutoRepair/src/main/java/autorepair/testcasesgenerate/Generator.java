package autorepair.testcasesgenerate;

import autorepair.patch.Patch;
import autorepair.state.edge.Event;
import autorepair.state.graph.StateFlowGraphImpl;
import autorepair.state.script.ScriptSequenceImpl;
import autorepair.state.statematchine.StateMachineImpl;
import autorepair.state.vertex.StateVertex;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Generator {
    static int count = 1000;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String javaPath = "D:\\rep\\WebUIAutoRepair\\src\\main\\java\\testcases\\mantisbt\\model_based_dataset\\";
        String className = "AddProfileTest";
        StateMachineImpl stateMachine = new StateMachineImpl(className,
                "D:\\rep\\WebUIAutoRepair\\output\\new\\testcases\\mantisbt\\model_based_dataset\\AddProfileTest\\");
        stateMachine.load(stateMachine.getSavePath());
        StateFlowGraphImpl stateFlowGraph = stateMachine.getStateFlowGraph();
        ScriptSequenceImpl scriptSequence = stateMachine.getScriptSequence();

        List<Patch> patchList = Patch.load(stateMachine.getSavePath() + "patch.json");
        int methodStartLine = ParseTest.getTestStartLine(javaPath + className + ".java");
        methodStartLine += 3;
        for (Event event : scriptSequence.getEdges()) {
            if (Integer.parseInt(event.getCodeLine().substring(event.getCodeLine().lastIndexOf(":") + 1)) < methodStartLine)
                continue;
            StateVertex sourceVertex = stateFlowGraph.getStateVertexById(event.getSourceVertexId());
            Set<Event> outEvents = stateFlowGraph.outgoingEdgesOf(sourceVertex);
            if (event.getMethod().equals("findElement")) continue;
            for (Event outEvent : outEvents) {
                if (!scriptSequence.getEdges().contains(outEvent)) {
                    count--;
                    ModifyClass modifyClass = new ModifyClass(Integer.parseInt(event.getCodeLine().substring(event.getCodeLine().lastIndexOf(":") + 1)));
                    modifyClass.addCode(generateCode(outEvent));

                    ParseTest parseTest = new ParseTest(javaPath,
                            className, className + count, modifyClass,
                            patchList);
                    parseTest.parseAndSerialize();
                    System.out.println(event.getCodeLine());
                    System.out.println(outEvent);
                }
                if (count == 0) {
                    break;
                }
            }
            if (count == 0) {
                break;
            }
        }
    }


    public static String generateCode(Event event) {
        StringBuilder code = new StringBuilder();
        code.append("driver.findElement(By.");
        code.append(event.getIdentification().getHow());
        code.append("(\"").append(event.getIdentification().getValue()).append("\"))");
        code.append(".").append(event.getMethod()).append("(");
        boolean isFirst = true;
        if (event.getArguments() != null)
            for (Object arg : event.getArguments()) {
                if (isFirst) {
                    isFirst = false;
                }else{
                    code.append(",");
                }
                code.append(arg);
            }
        code.append(");");
        return code.toString();
    }

}
