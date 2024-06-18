package autorepair.state.candidate;

import autorepair.match.sustech.Sustech;
import autorepair.state.Constants;
import autorepair.state.datacollect.DomNodeInfo;
import autorepair.state.datacollect.JsonProcess;
import autorepair.state.datacollect.PreDomNodeInfo;
import autorepair.state.delta.DomNodeDifference;
import autorepair.state.edge.EventType;
import autorepair.state.edge.Identification;
import org.openqa.selenium.WebElement;
import utils.UtilsDomNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CandidateExtractor {
    List<WebElementEventType> eventTypes;
    public CandidateExtractor(){
        eventTypes = new ArrayList<>();
        eventTypes.add(WebElementEventType.click);
        eventTypes.add(WebElementEventType.sendKeys);
    }

    public static List<CandidateEvent> extractCandidateElement(String savePath) throws IOException {
        List<DomNodeInfo> domNodeInfos = JsonProcess.readDomInfoJson(savePath + "domNodeInfo.json");
        List<CandidateEvent > candidateEvents = new ArrayList<>();
        for (DomNodeInfo domNodeInfo : domNodeInfos){
            if (domNodeInfo.getElementType() == Constants.CONTAINER) continue;
             PreDomNodeInfo preDomNodeInfo = UtilsDomNode.getDeepestPreDomNode(domNodeInfo,savePath);
             if (preDomNodeInfo.getXpath().contains(":")) continue;
             if (preDomNodeInfo.getText().equals("Logout")) continue;
             candidateEvents.add(new CandidateEvent(new Identification(Identification.How.xpath,preDomNodeInfo.getXpath()),
                     WebElementEventType.click,null,preDomNodeInfo.getElementId()));
            if (preDomNodeInfo.getTagName().equals("input")){
                List<Object> strings = new ArrayList<>();
                strings.add("1");
                candidateEvents.add(new CandidateEvent(new Identification(Identification.How.xpath,preDomNodeInfo.getXpath()),
                        WebElementEventType.sendKeys,strings,preDomNodeInfo.getElementId()));
            }
        }
        return  candidateEvents;
    }

    public static List<CandidateEvent> extractCandidateByDelta(String oldSavePath, String newSavePath) throws IOException {
        Sustech sustech = new Sustech();
        sustech.match(oldSavePath,newSavePath);
        DomNodeDifference domNodeDifference = sustech.getDomNodeDifference();
        List<DomNodeInfo> domNodeInfos = JsonProcess.readDomInfoJson(newSavePath + "domNodeInfo.json");
        List<CandidateEvent > candidateEvents = new ArrayList<>();
        for (DomNodeInfo domNodeInfo : domNodeInfos){
            if (domNodeInfo.getElementType() == Constants.CONTAINER) continue;
            if (!domNodeDifference.getAddList().contains(domNodeInfo.getNewElementId())) continue;
            PreDomNodeInfo preDomNodeInfo = UtilsDomNode.getDeepestPreDomNode(domNodeInfo,newSavePath);
            if (preDomNodeInfo.getXpath().contains(":")) continue;
            if (preDomNodeInfo.getText().equals("Logout")) continue;
            candidateEvents.add(new CandidateEvent(new Identification(Identification.How.xpath,preDomNodeInfo.getXpath()),
                    WebElementEventType.click,null,preDomNodeInfo.getElementId()));
            if (preDomNodeInfo.getTagName().equals("input")){
                List<Object> strings = new ArrayList<>();
                strings.add("1");
                candidateEvents.add(new CandidateEvent(new Identification(Identification.How.xpath,preDomNodeInfo.getXpath()),
                        WebElementEventType.sendKeys,strings,preDomNodeInfo.getElementId()));
            }
        }
        return  candidateEvents;
    }

}
