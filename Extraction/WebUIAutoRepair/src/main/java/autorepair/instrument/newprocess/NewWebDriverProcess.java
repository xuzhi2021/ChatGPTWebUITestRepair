package autorepair.instrument.newprocess;

import autorepair.match.MatchFactory;
import autorepair.patch.Patch;
import autorepair.patch.Patches;
import autorepair.patch.Record;
import autorepair.state.datacollect.DomNodeInfo;
import autorepair.state.edge.Event;
import autorepair.state.edge.Identification;
import autorepair.state.statematchine.StateMachineImpl;
import autorepair.state.vertex.StateVertex;
import org.aspectj.lang.ProceedingJoinPoint;
import org.openqa.selenium.*;
import utils.UtilsDomNode;
import utils.UtilsProperties;
import utils.UtilsSeleniumHelper;
import utils.UtilsXpath;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class NewWebDriverProcess {
    public static String signature;
    public static Object findElementProcess(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                            WebDriver driver, ProceedingJoinPoint proceedingJoinPoint)
            throws Throwable {
        WebElement webElement = null;
        try {
            webElement = (WebElement) proceedingJoinPoint.proceed();
            return webElement;
        }catch (Exception e ){

        }
        boolean check = Boolean.parseBoolean(
                UtilsProperties.getConfigProperties().getProperty("check"));

        StateVertex sourceStateVertex = newStateMachine.collectPageData(driver);
        newStateMachine.setSourceStateVertex(sourceStateVertex);
        System.out.println(newStateMachine.getSavePath());
        Event oldEvent = oldStateMachine.getScriptSequence().getEvent(
                proceedingJoinPoint.getSourceLocation().toString(),
                proceedingJoinPoint.getSignature().getName()
        );
        signature=proceedingJoinPoint.getSourceLocation().getWithinType().getName();
        System.out.println("signature："+signature);
        boolean checkResult = true;
        String newXpath = "";
        boolean repair = false;
        try {
            webElement = (WebElement) proceedingJoinPoint.proceed();
        } catch (NoSuchElementException noSuchElementException) {

            if (Boolean.parseBoolean(UtilsProperties.getConfigProperties().getProperty("repair"))) {
                System.out.println(noSuchElementException.getMessage());
                String method = "findElement";
                newXpath = MatchFactory.match(oldStateMachine, newStateMachine, oldEvent, driver);
                System.out.println("repair result：" + newXpath);
                try {
                    webElement = driver.findElement(By.xpath(newXpath));
                } catch (NoSuchElementException noSuchElementException1) {
                    if (check) {
//                        DomNodeInfo domNodeInfo = null;
//                        try{
//                            Patches patches =
//                                    Patches.load(newStateMachine.getSavePath().replace("output","patches")
//                                            + "patch.json");
//                            for (Patch patch : patches.getPatches()){
//                                if (patch.getSignature().equals(proceedingJoinPoint.getSignature().toString()) &&
//                                        patch.getCodeLine().equals(proceedingJoinPoint.getSourceLocation().toString())){
//                                    domNodeInfo = patch.getDomNodeInfo();
//                                }
//                            }
//                        }catch (FileNotFoundException fileNotFoundException){
//
//                        }
                            newStateMachine.getExperiment().records.add(new Record(noSuchElementException1.getMessage(),
                                    oldEvent.getAbsoluteXpath(), oldStateMachine.getSavePath() +
                                    oldEvent.getSourceVertexId() +
                                    File.separator + oldEvent.getElementId() + ".png"
                                    , proceedingJoinPoint.getSignature().getName()));
                    }
                    throw noSuchElementException;
                }

//                boolean generate_patch_base = Boolean.parseBoolean(
//                        UtilsProperties.getConfigProperties().getProperty("generate_patch_base"));
//                DomNodeInfo repairDomNode = UtilsDomNode.getDomNodeElementByAbsoluteXpath(newXpath,
//                        newStateMachine.getSavePath() + sourceStateVertex.getStateVertexId() +
//                                File.separator);
//                if (generate_patch_base) {
//
//                    newStateMachine.getPatches().getPatches().add(new Patch(oldEvent.getCodeLine(),
//                            UtilsSeleniumHelper.getWebElementIdentification(webElement), repairDomNode
//                            , proceedingJoinPoint.getSignature()));
//                }


//                if (check) {
//                    checkResult = check(newStateMachine, proceedingJoinPoint, newXpath, sourceStateVertex);
//                }
//                repair = true;
            } else {
                System.out.println("no repair");
                throw noSuchElementException;
            }
        }
        String absoluteXpath = UtilsXpath.generateXPathForWebElement(webElement, "");

//        StateVertex targetStateVertex = newStateMachine.collectPageData(driver);
//        newStateMachine.setTargetStateVertex(targetStateVertex);
        newStateMachine.setTargetStateVertex(newStateMachine.getSourceStateVertex());


        labelElement(repair, newStateMachine, webElement);
        int index = UtilsDomNode.getElementByAbsoluteXpath(absoluteXpath, newStateMachine.getSavePath() + sourceStateVertex.getStateVertexId() +
                File.separator);
        UtilsSeleniumHelper.captureScreen(webElement,
                newStateMachine.getSavePath() + newStateMachine.getSourceStateVertex().getStateVertexId() +
                        File.separator + index + ".png");

        Event event = newStateMachine.addWebDriverFindElementEvent(driver, proceedingJoinPoint, absoluteXpath, webElement);
        newStateMachine.addEvent2ScriptSequence(event);



        return webElement;

    }

    static DomNodeInfo baseDomNode;

    public static boolean check(StateMachineImpl newStateMachine, ProceedingJoinPoint proceedingJoinPoint
            , String newXpath, StateVertex sourceStateVertex) throws IOException {
//        Patches patches =
//                Patches.load(newStateMachine.getSavePath().replace("output","patches")
//                        + "patch.json");
//        for (Patch patch : patches.getPatches()){
//            if (patch.getSignature().equals(proceedingJoinPoint.getSignature().toString()) &&
//                    patch.getCodeLine().equals(proceedingJoinPoint.getSourceLocation().toString())){
//                baseDomNode = patch.getDomNodeInfo();
//            }
//        }
//        System.out.println(baseDomNode.getXpath());
//        boolean checkResult = UtilsDomNode.checkDomNodeContainXpath(baseDomNode, newXpath,
//                newStateMachine.getSavePath() + sourceStateVertex.getStateVertexId() +
//                        File.separator);
//        System.out.println("check result：" + checkResult);
        return true;
    }

    public static void labelElement(Boolean repair, StateMachineImpl newStateMachine, WebElement webElement) {
        if (repair) {
            UtilsSeleniumHelper.labelScreenBlue(newStateMachine.getSavePath() + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "fullScreen.png",
                    webElement, newStateMachine.getSavePath() + "event" +
                            File.separator + newStateMachine.getScriptSequence().getEdges().size() + ".png");
        } else {
            UtilsSeleniumHelper.labelScreenRed(newStateMachine.getSavePath() + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "fullScreen.png",
                    webElement, newStateMachine.getSavePath() + "event" +
                            File.separator + newStateMachine.getScriptSequence().getEdges().size() + ".png");
        }
    }

}
