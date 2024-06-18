package autorepair.validate;

import autorepair.match.sustech.SameVersionMatch;
import autorepair.match.sustech.Sustech;
import autorepair.state.edge.Event;
import autorepair.state.statematchine.StateMachineImpl;
import utils.UtilsProperties;

import java.io.File;
import java.io.IOException;

public class ValidateFactory {

    public static boolean validate(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                   Event oldEvent, Event newEvent) throws Exception {
        String validateMethod = "none";
        try {
            validateMethod = UtilsProperties.getConfigProperties().getProperty("validate").trim();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        switch (validateMethod) {
            case "validateByUrl":
                return validateByUrl(oldStateMachine, newStateMachine, oldEvent, newEvent);
            case "validateByScreen":
                return validateByScreen(oldStateMachine, newStateMachine, oldEvent, newEvent);
            default:
                return true;
        }
    }


    public static boolean validateByUrl(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                        Event oldEvent, Event newEvent) {
        String oldSourceUrl = oldStateMachine.getStateFlowGraph()
                .getStateVertexById(oldEvent.getSourceVertexId()).getUrl();
        String oldTargetUrl = oldStateMachine.getStateFlowGraph()
                .getStateVertexById(oldEvent.getTargetVertexId()).getUrl();
        String newSourceUrl = newStateMachine.getStateFlowGraph()
                .getStateVertexById(newEvent.getSourceVertexId()).getUrl();
        String newTargetUrl = newStateMachine.getStateFlowGraph()
                .getStateVertexById(newEvent.getTargetVertexId()).getUrl();
        return oldSourceUrl.equals(newSourceUrl) && oldTargetUrl.equals(newTargetUrl);
    }


    public static boolean validateByScreen(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                           Event oldEvent, Event newEvent) throws Exception {
        String oldSourcePath = oldStateMachine.getSavePath() + oldEvent.getSourceVertexId() + File.separator;
        String oldTargetPath = oldStateMachine.getSavePath() + oldEvent.getTargetVertexId() + File.separator;
        String newSourcePath = newStateMachine.getSavePath() + newEvent.getSourceVertexId() + File.separator;
        String newTargetPath = newStateMachine.getSavePath() + newEvent.getTargetVertexId() + File.separator;
//        Sustech sustech = new Sustech();
//        sustech.match(oldSourcePath,newSourcePath);
//        double sourceRate = sustech.getMatchRate();
//        if (Double.compare(sourceRate, 0.5) < 0 ){
//            System.out.println(oldSourcePath);
//            System.out.println(newSourcePath);
//            System.out.println("Fail at source state" + sourceRate);
//            return false;
//        }
        Sustech sustech2 = new Sustech();
        sustech2.match(oldTargetPath, newTargetPath);
        double targetRate = sustech2.getMatchRate();
        if (Double.compare(targetRate, 0.5) < 0) {
            System.out.println(oldTargetPath);
            System.out.println(newTargetPath);
            System.out.println("Fail at target state" + targetRate);
            return false;
        }
        System.out.println(targetRate);
        return true;
    }

    public static boolean validateByDifference(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                               Event oldEvent, Event newEvent) throws IOException {
        String oldSourcePath = oldStateMachine.getSavePath() + oldEvent.getSourceVertexId() + File.separator;
        String oldTargetPath = oldStateMachine.getSavePath() + oldEvent.getTargetVertexId() + File.separator;
        String newSourcePath = newStateMachine.getSavePath() + newEvent.getSourceVertexId() + File.separator;
        String newTargetPath = newStateMachine.getSavePath() + newEvent.getTargetVertexId() + File.separator;
        System.out.println("verify path：" + oldSourcePath);
        System.out.println("verify path：" + oldTargetPath);
        System.out.println("verify path：" + newSourcePath);
        System.out.println("verify path：" + newTargetPath);

        SameVersionMatch oldmatch = new SameVersionMatch();
        oldmatch.sameVersionMatch(oldSourcePath, oldTargetPath);
        Delta oldDelta = oldmatch.getDelta();

        SameVersionMatch newmatch = new SameVersionMatch();
        newmatch.sameVersionMatch(newSourcePath, newTargetPath);
        Delta newDelta = newmatch.getDelta();

        Sustech sourceMatch = new Sustech();
        sourceMatch.match(oldSourcePath, newSourcePath);

        Sustech targetMatch = new Sustech();
        targetMatch.match(oldTargetPath, newTargetPath);

        int total = oldDelta.getSize() + newDelta.getSize();
        int matchCount = 0;
        int unMatchCount = 0;
        for (int index : oldDelta.getOldIndexList()) {
            if (!sourceMatch.getMatchResult().containsKey(index)) {
                unMatchCount++;
                continue;
            }
//            total++;
            if (newDelta.getOldIndexList().contains(sourceMatch.getMatchResult().get(index))) {
                matchCount++;
            } else {
                unMatchCount++;
            }
        }
        System.out.println("source match:" + matchCount);

        for (int index : oldDelta.getNewIndexList()) {
            if (!targetMatch.getMatchResult().containsKey(index)) {
                unMatchCount++;
                continue;
            }
            if (newDelta.getNewIndexList().contains(targetMatch.getMatchResult().get(index))) {
                matchCount++;
            } else {
                unMatchCount++;
            }
        }
        System.out.println("*********");
        System.out.println(oldDelta);
        System.out.println(newDelta);
        System.out.println(sourceMatch.getMatchResult());
        System.out.println(targetMatch.getMatchResult());
        System.out.println(matchCount);
        System.out.println(oldDelta.getSize());
        System.out.println("=======");


        double matchRate = ((double) matchCount) / Math.max(oldDelta.getSize(), newDelta.getSize());
        double weight = 0.4;
        if (Math.max(oldDelta.getSize(), newDelta.getSize()) == 0) {
            matchRate = 1;
            weight = 0;
        }
        matchRate = matchRate * weight + targetMatch.getMatchRate() * (1 - weight);
        return Double.compare(matchRate, 0.5) >= 0;
    }
}
