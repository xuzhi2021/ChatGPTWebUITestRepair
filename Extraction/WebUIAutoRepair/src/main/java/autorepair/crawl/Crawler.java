package autorepair.crawl;


import autorepair.actions.WebElementAction;
import autorepair.state.candidate.CandidateEvent;
import autorepair.state.candidate.CandidateExtractor;
import autorepair.state.edge.Event;
import autorepair.state.graph.StateFlowGraphImpl;
import autorepair.state.statematchine.StateMachineImpl;
import autorepair.state.vertex.StateVertex;
import autorepair.state.vertex.StateVertexImpl;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.yaml.snakeyaml.scanner.Constant;
import testcases.Constants;

import javax.swing.plaf.nimbus.State;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;



public class Crawler {

    public StateVertex currentStateVertex;
    public StateMachineImpl stateMachine;
    public StateFlowGraphImpl stateFlowGraph;
    public int initStateNumber;
    public int maxStateNumber;
    public Queue<CandidateEvent> candidateEventQueue;
    public int maxTry;

    public Crawler(StateMachineImpl stateMachine, int maxStateNumber, int maxTry) {
        this.stateFlowGraph = stateMachine.getStateFlowGraph();
        this.currentStateVertex = stateMachine.getSourceStateVertex();
        this.stateMachine = stateMachine;
        initStateNumber = stateFlowGraph.getAllState().size();
        this.maxStateNumber = maxStateNumber;
        candidateEventQueue = new ArrayDeque<CandidateEvent>();
        this.maxTry = maxTry;
    }

    public void crawl() throws Exception {
        System.out.println("开始爬虫");
        System.out.println(stateFlowGraph);
        // 进行爬虫
        long start = System.currentTimeMillis();
        long current = System.currentTimeMillis();
        StateVertex stateVertex = stateFlowGraph.addStateVertex(stateMachine.getDriver(), stateMachine.getSavePath());
        extractElementByStateVertex(stateVertex);
        while (((stateFlowGraph.getAllState().size() - initStateNumber) < maxStateNumber)
                && ((current - start) < 1000000) && (!candidateEventQueue.isEmpty()) && maxTry > 0 ) {
            maxTry--;
            CandidateEvent candidateEvent = candidateEventQueue.poll();

            goToTargetState(candidateEvent.getSourceStateVertex());

            try {
                candidateEvent.execute(stateMachine.getDriver());
            } catch (NoSuchElementException noSuchElementException) {
                noSuchElementException.printStackTrace();
            }

            StateVertex newState = stateFlowGraph.addStateVertex(stateMachine.getDriver(), stateMachine.getSavePath());
            Event event = stateFlowGraph.addEvent(candidateEvent.getSourceStateVertex(), newState,
                    candidateEvent, candidateEvent.getElementId());
            extractElementByStateVertex(newState);
            closeMoreWindow(stateMachine.getDriver());

            current = System.currentTimeMillis();
            if (candidateEventQueue.isEmpty()) {
                System.out.println("=========null");
            }
            if (((stateFlowGraph.getAllState().size() - initStateNumber) >= maxStateNumber)) {
                System.out.println("====state size full");
            }
            if (((current - start) >= 1000000)) {
                System.out.println("====timeout");
            }
        }

        System.out.println("over");
        System.out.println(stateFlowGraph);
        goToTargetState(stateMachine.getTargetStateVertex());
    }

    public void crawlBydelta(StateMachineImpl oldStateMachine, Event oldEvent) throws Exception {
        System.out.println("start");
        System.out.println(stateFlowGraph);

        long start = System.currentTimeMillis();
        long current = System.currentTimeMillis();
        StateVertex stateVertex = stateFlowGraph.addStateVertex(stateMachine.getDriver(), stateMachine.getSavePath());
        extractElementByStateVertexDelta(oldStateMachine, oldStateMachine.getStateFlowGraph().getStateVertexById(oldEvent.getSourceVertexId()), stateVertex);
        while (((stateFlowGraph.getAllState().size() - initStateNumber) < maxStateNumber)
                && ((current - start) < 1000000) && (!candidateEventQueue.isEmpty())) {
            CandidateEvent candidateEvent = candidateEventQueue.poll();

            goToTargetState(candidateEvent.getSourceStateVertex());

            try {
                candidateEvent.execute(stateMachine.getDriver());
            } catch (NoSuchElementException noSuchElementException) {
                noSuchElementException.printStackTrace();
            }

            StateVertex newState = stateFlowGraph.addStateVertex(stateMachine.getDriver(), stateMachine.getSavePath());
            Event event = stateFlowGraph.addEvent(candidateEvent.getSourceStateVertex(), newState,
                    candidateEvent, candidateEvent.getElementId());
            System.out.println("add Event:" + event);
            extractElementByStateVertex(newState);
            closeMoreWindow(stateMachine.getDriver());

            current = System.currentTimeMillis();
            if (candidateEventQueue.isEmpty()) {
                System.out.println("=========null");
            }
            if (((stateFlowGraph.getAllState().size() - initStateNumber) >= maxStateNumber)) {
                System.out.println("====state size full");
            }
            if (((current - start) >= 1000000)) {
                System.out.println("====timeout");
            }
        }

        System.out.println("over");
        System.out.println(stateFlowGraph);
        goToTargetState(stateMachine.getTargetStateVertex());
    }


    public void closeMoreWindow(WebDriver driver) {
        Set<String> windowHandles = driver.getWindowHandles();
        boolean isFirst = true;
        for (String handle : windowHandles) {
            if (!isFirst) {
                driver.switchTo().window(handle);
                driver.close();
            } else {
                isFirst = false;
            }
        }
        windowHandles = driver.getWindowHandles();
        for (String handle : windowHandles) {
            driver.switchTo().window(handle);
        }
    }

    public void extractElementByStateVertexDelta(StateMachineImpl oldStateMachine, StateVertex oldStateVertex,
                                                 StateVertex newStateVertex) throws IOException {

        List<CandidateEvent> candidateEvents = CandidateExtractor.extractCandidateByDelta(
                oldStateMachine.getSavePath() + oldStateVertex.getStateVertexId() + File.separator,
                stateMachine.getSavePath() + newStateVertex.getStateVertexId() + File.separator);
        for (CandidateEvent candidateEvent : candidateEvents) {
            candidateEvent.setSourceStateVertex(newStateVertex);
            candidateEventQueue.add(candidateEvent);
        }
    }

    public void extractElementByStateVertex(StateVertex stateVertex) throws IOException {
        if (!stateVertex.getUrl().startsWith(Constants.getMantisUrl())) return;


        List<CandidateEvent> candidateEvents = CandidateExtractor.extractCandidateElement(
                stateMachine.getSavePath() + stateVertex.getStateVertexId() + File.separator);
        for (CandidateEvent candidateEvent : candidateEvents) {
            candidateEvent.setSourceStateVertex(stateVertex);
            candidateEventQueue.add(candidateEvent);
        }
    }

    public void goToTargetState(StateVertex targetVertex) throws Exception {

        List<Event> rootPath = new ArrayList<>();
        try {
            rootPath = stateFlowGraph.getShortestPath(stateFlowGraph.getStateVertexById(0), targetVertex);
        } catch (NullPointerException nullPointerException) {
            System.out.println("no road");
        }

        Stack<String> urlStack = new Stack<>();
        urlStack.push(targetVertex.getUrl());
        for (Event rootEvent : rootPath) {
            urlStack.push(stateFlowGraph.getStateVertexById(rootEvent.getSourceVertexId()).getUrl());
        }

        StateVertex lastVertex = null;
        while (!urlStack.isEmpty()) {
            stateMachine.getDriver().get(urlStack.pop());
            StateVertex currentVertex = stateFlowGraph.addStateVertex(stateMachine.getDriver(), stateMachine.getSavePath());
            try {
                List<Event> shortestPath = stateFlowGraph.getShortestPath(currentVertex, targetVertex);
                for (Event event : shortestPath) {

                    WebElementAction.doAction(stateMachine.getDriver(), event);
                }

                lastVertex = stateFlowGraph.addStateVertex(stateMachine.getDriver(), stateMachine.getSavePath());
            } catch (NullPointerException nullPointerException) {
                System.out.println("no road");
            }
            if (targetVertex.equals(lastVertex)) {
                break;
            }
        }
        if (targetVertex.equals(lastVertex)) {
            System.out.println("can't go back");
        }

    }

    public void reset() {

    }

}
