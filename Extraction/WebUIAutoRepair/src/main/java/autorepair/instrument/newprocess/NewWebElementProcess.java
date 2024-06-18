package autorepair.instrument.newprocess;

import autorepair.actions.WebElementAction;
import autorepair.crawl.Crawler;
import autorepair.match.MatchFactory;
import autorepair.patch.Record;
import autorepair.state.candidate.CandidateEvent;
import autorepair.state.candidate.CandidateExtractor;
import autorepair.state.edge.Event;
import autorepair.state.statematchine.StateMachineImpl;
import autorepair.state.vertex.StateVertex;
import autorepair.validate.ValidateException;
import autorepair.validate.ValidateFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.openqa.selenium.*;
import utils.UtilsXpath;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class NewWebElementProcess {
    public static String signature;
    public static Object webelementprocess(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                           WebDriver driver,
                                           ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        try {
            WebElement webElement = (WebElement) proceedingJoinPoint.getTarget();
            return proceedingJoinPoint.proceed();
        }catch (Exception e ){

        }
        WebElement webElement = (WebElement) proceedingJoinPoint.getTarget();

        StateVertex sourceStateVertex = newStateMachine.collectPageData(driver);
        newStateMachine.setSourceStateVertex(sourceStateVertex);
        Object object = null;
        Event oldEvent = oldStateMachine.getScriptSequence().getEvent(
                proceedingJoinPoint.getSourceLocation().toString(), proceedingJoinPoint.getSignature().getName()
        );
        signature=proceedingJoinPoint.getSourceLocation().getWithinType().getName();
        System.out.println("signature："+signature);

        try {
            String absoluteXpath = UtilsXpath.generateXPathForWebElement(webElement, "");

//            StateVertex targetStateVertex = newStateMachine.collectPageData(driver);
//            newStateMachine.setTargetStateVertex(targetStateVertex);
//            newStateMachine.setTargetStateVertex(newStateMachine.getTargetStateVertex());


            Event newEvent = newStateMachine.addWebElementEvent(driver, proceedingJoinPoint, absoluteXpath);
            newStateMachine.addEvent2ScriptSequence(newEvent);
            object = proceedingJoinPoint.proceed();


//            try {
//                boolean validateResult = true;
//                boolean validateResult2 = true;
//                System.out.println("verify result：" + validateResult);
//                newStateMachine.getExperiment().records.add(new Record(
//                        oldEvent.getAbsoluteXpath(), "", "",
//                        String.valueOf(validateResult), String.valueOf(validateResult2), String.valueOf(validateResult2), oldStateMachine.getSavePath() + oldEvent.getSourceVertexId() + File.separator + oldEvent.getElementId() + ".png",
//                        newStateMachine.getSavePath() + newEvent.getSourceVertexId() + File.separator + newEvent.getElementId() + ".png"
//                        , proceedingJoinPoint.getSignature().getName()
//                ));
//            } catch (NullPointerException nullPointerException) {
//                nullPointerException.printStackTrace();
//                newStateMachine.getExperiment().records.add(new Record(
//                        oldEvent.getAbsoluteXpath(), "", "",
//                        String.valueOf("null pointer"), "", String.valueOf("null pointer"), oldStateMachine.getSavePath() + oldEvent.getSourceVertexId() + File.separator + oldEvent.getElementId() + ".png",
//                        newStateMachine.getSavePath() + newEvent.getSourceVertexId() + File.separator + newEvent.getElementId() + ".png"
//                        , proceedingJoinPoint.getSignature().getName()
//                ));
//            }

        } catch (NoSuchElementException | ElementNotInteractableException exception) {

            String newXpath = MatchFactory.match(oldStateMachine, newStateMachine, oldEvent, driver);
            System.out.println("repaired xpath：" + oldEvent.getIdentification().getValue() + ":" + newXpath);
            WebElementAction.doWebElementActionHelper(driver.findElement(By.xpath(newXpath)),
                    oldEvent.getMethod(), Arrays.asList(proceedingJoinPoint.getArgs()));
        }

        return object;
    }
}
