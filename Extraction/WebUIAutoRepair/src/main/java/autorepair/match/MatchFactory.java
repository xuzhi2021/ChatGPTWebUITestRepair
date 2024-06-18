package autorepair.match;

import autorepair.instrument.newprocess.NewWebDriverProcess;
import autorepair.instrument.newprocess.NewWebElementProcess;
import autorepair.match.vista2.VISTA2;
import autorepair.match.water.WATER;
import autorepair.match.water.WATER2;
import autorepair.state.datacollect.DomNodeInfo;
import autorepair.state.datacollect.JsonProcess;
import autorepair.state.datacollect.PreDomNodeInfo;
import autorepair.state.edge.Event;
import autorepair.state.statematchine.StateMachineImpl;
import config.Settings;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.*;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.visitor.VoidVisitorAdapter;


public class MatchFactory {
    static String stm = null;
    static int lineNum = -1;
    static String ground_truth;
    static int gt_id = -1;
//    static int excelLineNum = 1;
//    static int excelLineNum_attr = 1;

    public static String match(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine, Event oldEvent, WebDriver driver) throws
            IOException {
        String matchMethod = "water";
        try {
            matchMethod = UtilsProperties.getConfigProperties().getProperty("match").trim();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        switch (matchMethod) {
            case "vista2":
                return matchByVista2(oldStateMachine, newStateMachine, oldEvent, driver);
            case "water2":
                return matchByWATER2(oldStateMachine, newStateMachine, oldEvent, driver);
//            case "chatGPT":
//                return matchByChatGPT(oldStateMachine, newStateMachine, oldEvent);
            default:
                // water(default)
                return matchByWATER(oldStateMachine, newStateMachine, oldEvent);
        }
    }


    public static String matchByVista2(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                       Event oldEvent, WebDriver driver) throws IOException {
        String newFullScreenPath = newStateMachine.getSavePath() + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator
                + "fullScreen.png";
        String oldFullScreenPath = oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "fullScreen.png";
        List<PreDomNodeInfo> oldPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "preDomNodeInfo.json");
        PreDomNodeInfo preDomNodeInfo = null;
        for (PreDomNodeInfo temp : oldPreDomNodeInfoList) {
            if (temp.getXpath().equals(oldEvent.getAbsoluteXpath())) {
                preDomNodeInfo = temp;
                break;
            }
        }
        String signature = NewWebDriverProcess.signature;
        if (signature == null)
            signature = NewWebElementProcess.signature;
        lineNum = Integer.parseInt(oldEvent.getCodeLine().split(":")[1]);
        System.out.println("line number:" + lineNum);
        String packageClassName = signature.replace(".", "\\");//testcases.Claroline_Test_Suite.model_based_dataset.po.Login

        String result = VISTA2.retrieveDomNode(oldFullScreenPath, newFullScreenPath, preDomNodeInfo, driver);
        storeCandidates2(preDomNodeInfo, packageClassName, lineNum, ground_truth, oldEvent, oldStateMachine, newStateMachine, 10, driver);
        storeGroundTruth(result, oldStateMachine, newStateMachine, oldEvent);
//        String verifiedResult = verifyXPath(preDomNodeInfo, result, oldStateMachine, newStateMachine, oldEvent,driver);
//
//        return verifiedResult;
//
        return result;
    }

    public static String verifyXPath(PreDomNodeInfo old, String result, StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                     Event oldEvent, WebDriver driver) {
        String signature = NewWebDriverProcess.signature;
        if (signature == null)
            signature = NewWebElementProcess.signature;
        lineNum = Integer.parseInt(oldEvent.getCodeLine().split(":")[1]);
        System.out.println("line number:" + lineNum);
        String packageClassName = signature.replace(".", "\\");//testcases.Claroline_Test_Suite.model_based_dataset.po.Login
        //String className=oldEvent.getCodeLine().split(":")[0];
        System.out.println("broken code lien:" + oldEvent.getCodeLine());
        //storeBrokenStatement(result, oldStateMachine, newStateMachine, oldEvent);
        String ResultPath = "\\ground_truth\\" + packageClassName + "_" + lineNum + ".txt";
        String gt = null;
        int gtID = -1;
        try {
            if (ResultPath.contains("\\po\\")) {
                String outerClass = newStateMachine.getScriptSequence().getClassName();
                ResultPath = ResultPath.replace(".txt", "_" + outerClass + "_all.txt");
                System.out.println(ResultPath);
            }
            File file = new File(ResultPath);

            BufferedReader br = new BufferedReader(new FileReader(file));
            String st;

            //String finalResult;
            while ((st = br.readLine()) != null) {
//                System.out.println(st);
                System.out.println("ground truth:" + st);
                ground_truth = st.trim();
                System.out.println("tool's result:" + result);
                if (result == null || !result.trim().equals(ground_truth)) {
                    System.out.println("fail");
                } else {
                    System.out.println("success");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //storeCandidates2(old, packageClassName, lineNum, gt,oldEvent,oldStateMachine, newStateMachine, 10,driver);
        if (ground_truth != null)
            return ground_truth;
        else {
            System.out.println("没有ground truth");
            return result;
        }
    }


    private static class MethodVisitor extends VoidVisitorAdapter<Object> {
        @Override
        public void visit(MethodDeclaration m, Object arg) {
            for (Statement stmt : m.getBody().getStmts()) {
                if (stmt.getBeginLine() == lineNum) {
                    stm = stmt.toString();
                    System.out.println(stm);
                    break;
                }
            }
        }
    }

    public static void storeBrokenStatement(String result, StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                            Event oldEvent) {
        String root = "\\src\\main\\java\\testcases\\";
        String signature = NewWebDriverProcess.signature;
        if (signature == null)
            signature = NewWebElementProcess.signature;
        lineNum = Integer.parseInt(oldEvent.getCodeLine().split(":")[1]);
        String packageClassName = signature.replace(".", "\\");//testcases.Claroline_Test_Suite.model_based_dataset.po.Login
        String className = oldEvent.getCodeLine().split(":")[0];

        String filePath = "\\src\\main\\java\\" + packageClassName + ".java";
        String outputPath = "\\output\\broken_statement\\" + packageClassName + "_" + lineNum + ".txt";
        CompilationUnit parse = null;
        try {
            parse = JavaParser.parse(new File(filePath));
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new MethodVisitor().visit(parse, null);
        String outerClass = newStateMachine.getScriptSequence().getClassName();
        if (outputPath.contains("po")) {//if (exist & ResultPath4.contains("po")) {
            outputPath = outputPath.replace("_all", "_" + outerClass + "_all");
        }
        File file = new File(outputPath);
        try {
            if (file.getParentFile().mkdirs()) {
                System.out.println("success");
            }
            if (file.createNewFile() || file.length() == 0) {
                StringBuilder sb = new StringBuilder();

                if (stm != null)
                    sb.append(stm);
                else
                    System.out.println("statement is null");

//                FileWriter writer=new FileWriter(file);
//                writer.write(sb.toString());
//                writer.close();

                FileOutputStream fos = new FileOutputStream(outputPath);
                fos.write(sb.toString().getBytes());

                fos.close();
                System.out.println("broken statement saved");
            }
        } catch (IOException e) {
            System.out.println("fail to create folder");
            e.printStackTrace();
        }
    }

    public static List<PreDomNodeInfo> xpathCandidates(String old_xpath, StateMachineImpl newStateMachine, int size) {
        List<PreDomNodeInfo> newPreDomNodeInfoList = null;
        try {
            newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                    + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<PreDomNodeInfo> candidates_small = new ArrayList<>();//size=5
        Collections.sort(newPreDomNodeInfoList, new Comparator<PreDomNodeInfo>() {
            public int compare(PreDomNodeInfo o1, PreDomNodeInfo o2) {
                String str1 = o1.getXpath();
                String str2 = o2.getXpath();
                int dist1 = getEditDistance(str1, old_xpath);
                int dist2 = getEditDistance(str2, old_xpath);
                return Integer.compare(dist1, dist2);//smallest com first
            }
        });
        for (int i = 0; i < newPreDomNodeInfoList.size(); i++) {
            if (candidates_small.size() == size)
                break;
            PreDomNodeInfo cur = newPreDomNodeInfoList.get(i);
            if (candidates_small.contains(cur) || (!cur.isDisplayed()))
                continue;
            candidates_small.add(newPreDomNodeInfoList.get(i));
        }
        return candidates_small;
    }

    public static List<PreDomNodeInfo> textXpathCandidates(Boolean old_isLeaf, String old_text, String old_xpath, StateMachineImpl newStateMachine, int size) {
        List<PreDomNodeInfo> newPreDomNodeInfoList = null;
        try {
            newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                    + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<PreDomNodeInfo> candidates_small = new ArrayList<>();//size=5
        if (old_text != null && (!old_text.equals("") && (!old_text.equals("''")))) {
            for (PreDomNodeInfo e : newPreDomNodeInfoList
            ) {
                if (old_text.equals(e.getText())) {
                    if ((old_isLeaf && e.isLeaf()) || (!old_isLeaf && !e.isLeaf())) {
                        candidates_small.add(e);

                    }
                }
            }
        }
        if (candidates_small.size() >= 10) {
            ArrayList<PreDomNodeInfo> result = new ArrayList<>();
            Collections.sort(candidates_small, new Comparator<PreDomNodeInfo>() {
                public int compare(PreDomNodeInfo o1, PreDomNodeInfo o2) {
                    String str1 = o1.getXpath();
                    String str2 = o2.getXpath();
                    int dist1 = getEditDistance(str1, old_xpath);
                    int dist2 = getEditDistance(str2, old_xpath);
                    return Integer.compare(dist1, dist2);//smallest com first
                }
            });
            for (int i = 0; i < candidates_small.size(); i++) {
                if (result.size() == size)
                    break;
                PreDomNodeInfo cur = candidates_small.get(i);
                if (result.contains(cur) || (!cur.isDisplayed()))
                    continue;
                if (old_isLeaf && (!cur.isLeaf()))
                    continue;
                if (!old_isLeaf && cur.isLeaf())
                    continue;
                result.add(candidates_small.get(i));
            }
            System.out.println("generate textXpath candidate successfully, 10 candidate has same text, size: " + result.size());
            return result;
        }
        Collections.sort(newPreDomNodeInfoList, new Comparator<PreDomNodeInfo>() {
            public int compare(PreDomNodeInfo o1, PreDomNodeInfo o2) {
                String str1 = o1.getXpath();
                String str2 = o2.getXpath();
                int dist1 = getEditDistance(str1, old_xpath);
                int dist2 = getEditDistance(str2, old_xpath);
                return Integer.compare(dist1, dist2);//smallest com first
            }
        });
        for (int i = 0; i < newPreDomNodeInfoList.size(); i++) {
            if (candidates_small.size() == size)
                break;
            PreDomNodeInfo cur = newPreDomNodeInfoList.get(i);
            if (candidates_small.contains(cur) || (!cur.isDisplayed()))
                continue;
            if (old_isLeaf && (!cur.isLeaf()))
                continue;
            if (!old_isLeaf && cur.isLeaf())
                continue;
            candidates_small.add(newPreDomNodeInfoList.get(i));
        }
        System.out.println("generate textXpath candidate successfully, size: " + candidates_small.size());
        return candidates_small;
    }

    public static List<PreDomNodeInfo> randomCandidates(String gt_xpath, StateMachineImpl newStateMachine, int size) {
        List<PreDomNodeInfo> newPreDomNodeInfoList = null;
        try {
            newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                    + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<PreDomNodeInfo> candidates_small = new ArrayList<>();//size=5
        for (PreDomNodeInfo c : newPreDomNodeInfoList
        ) {
            if (c.getXpath().equals(gt_xpath)) {
                candidates_small.add(c);

                break;
            }
        }
        int seed = Integer.parseInt(String.format("%ts", new Date()));
        Random rand = new Random(seed);
        ArrayList<Integer> ids = new ArrayList<>();
        while (ids.size() < size - 1) {

            int i1 = rand.nextInt(newPreDomNodeInfoList.size());
            boolean f1 = true;
            while (f1) {
                i1 = rand.nextInt(newPreDomNodeInfoList.size());
                if (newPreDomNodeInfoList.get(i1).isDisplayed() && (!ids.contains(i1))) {

                    break;
                }
            }
            ids.add(i1);
            candidates_small.add(newPreDomNodeInfoList.get(i1));
        }
//        Collections.shuffle(candidates);
        Collections.shuffle(candidates_small);
        return candidates_small;
    }

    public static List<PreDomNodeInfo> vistaAndWater(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                                     Event oldEvent, WebDriver driver, int size) throws IOException {
        List<PreDomNodeInfo> oldPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "preDomNodeInfo.json");
        String oldHtml = UtilsTxtLoader.readFile02(oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "temp.html");
        List<PreDomNodeInfo> newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
        PreDomNodeInfo preDomNodeInfo = null;
        for (PreDomNodeInfo temp : oldPreDomNodeInfoList) {
            if (temp.getXpath().equals(oldEvent.getAbsoluteXpath())) {
                preDomNodeInfo = temp;
                break;
            }
        }
        String newFullScreenPath = newStateMachine.getSavePath() + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator
                + "fullScreen.png";
        String oldFullScreenPath = oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "fullScreen.png";

        List<String> water = WATER2.retrieveWebElementFromDOMInfo2(newStateMachine.getDriver(),
                preDomNodeInfo, oldHtml, newPreDomNodeInfoList, size);
        List<String> vista = VISTA2.getCandidates(oldFullScreenPath, newFullScreenPath, preDomNodeInfo, driver, size / 2);
        ArrayList<PreDomNodeInfo> re = new ArrayList<>();
        System.out.println("water size:" + water.size() + ", vista size:" + vista.size());

        for (int i = 0; i < vista.size(); i++) {
            if (re.size() == size / 2)
                break;
            for (PreDomNodeInfo node : newPreDomNodeInfoList
            ) {
                if (node.getXpath().equals(vista.get(i)) && (!re.contains(node))) {
                    re.add(node);
                }
            }
        }
        for (int i = 0; i < water.size(); i++) {
            if (re.size() == size) {
                System.out.println("water+vista size: " + re.size());
                break;
            }
            for (PreDomNodeInfo node : newPreDomNodeInfoList
            ) {
                if (node.getXpath().equals(water.get(i)) && (!re.contains(node))) {
                    //!re.contains(node) to reduce duplication
                    re.add(node);
                }
            }
        }
        return re;
    }

    public static List<PreDomNodeInfo> VistaCandidates(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                                       Event oldEvent, WebDriver driver, int size) throws IOException {
        List<PreDomNodeInfo> oldPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "preDomNodeInfo.json");
//        String oldHtml = UtilsTxtLoader.readFile02(oldStateMachine.getSavePath() + File.separator
//                + oldEvent.getSourceVertexId() + File.separator + "temp.html");
        List<PreDomNodeInfo> newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
        PreDomNodeInfo preDomNodeInfo = null;
        for (PreDomNodeInfo temp : oldPreDomNodeInfoList) {
            if (temp.getXpath().equals(oldEvent.getAbsoluteXpath())) {
                preDomNodeInfo = temp;
                break;
            }
        }
        String newFullScreenPath = newStateMachine.getSavePath() + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator
                + "fullScreen.png";
        String oldFullScreenPath = oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "fullScreen.png";

        List<String> vista = VISTA2.getCandidates(oldFullScreenPath, newFullScreenPath, preDomNodeInfo, driver, size);
        ArrayList<PreDomNodeInfo> re = new ArrayList<>();
        System.out.println("vista candidate list size:" + vista.size());

        for (int i = 0; i < vista.size(); i++) {
            if (re.size() == size)
                break;
            for (PreDomNodeInfo node : newPreDomNodeInfoList
            ) {
                if (node.getXpath().equals(vista.get(i)) && (!re.contains(node))) {
                    re.add(node);
                }
            }
        }

        return re;
    }


    public static List<PreDomNodeInfo> WaterCandidates(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                                       Event oldEvent, WebDriver driver, int size) throws IOException {
        List<PreDomNodeInfo> oldPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "preDomNodeInfo.json");
        String oldHtml = UtilsTxtLoader.readFile02(oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "temp.html");
        List<PreDomNodeInfo> newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
        PreDomNodeInfo preDomNodeInfo = null;
        for (PreDomNodeInfo temp : oldPreDomNodeInfoList) {
            if (temp.getXpath().equals(oldEvent.getAbsoluteXpath())) {
                preDomNodeInfo = temp;
                break;
            }
        }
//        String newFullScreenPath = newStateMachine.getSavePath() + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator
//                + "fullScreen.png";
//        String oldFullScreenPath = oldStateMachine.getSavePath() + File.separator
//                + oldEvent.getSourceVertexId() + File.separator + "fullScreen.png";

        List<String> water = WATER2.retrieveWebElementFromDOMInfo2(newStateMachine.getDriver(),
                preDomNodeInfo, oldHtml, newPreDomNodeInfoList, size);
        ArrayList<PreDomNodeInfo> re = new ArrayList<>();
        System.out.println("water size:" + water.size());

        for (int i = 0; i < water.size(); i++) {
            if (re.size() == size) {
                System.out.println("water candidate list size: " + re.size());
                break;
            }
            for (PreDomNodeInfo node : newPreDomNodeInfoList
            ) {
                if (node.getXpath().equals(water.get(i)) && (!re.contains(node))) {
                    re.add(node);
                }
            }
        }
        return re;
    }

    public static List<PreDomNodeInfo> generateRandomCandidate(PreDomNodeInfo old, String packageClassName, int lineNum, String gt_xpath, Event oldEvent, StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine, int size, WebDriver driver) {
        List<PreDomNodeInfo> newPreDomNodeInfoList = null;
        try {
            newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                    + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

//        ArrayList<PreDomNodeInfo> candidates_small = new ArrayList<>();//if want to add groundtruth in random candidate list
//        for (PreDomNodeInfo c : newPreDomNodeInfoList
//        ) {
//            if (c.getXpath().equals(gt_xpath)) {
//                candidates_small.add(c);
//
//                break;
//            }
//        }
        ArrayList<PreDomNodeInfo> result = new ArrayList<>();
        Random rand = new Random();
        ArrayList<Integer> ids = new ArrayList<>();
        while (ids.size() < size) {

            int i1 = rand.nextInt(newPreDomNodeInfoList.size());
            while (!newPreDomNodeInfoList.get(i1).isDisplayed()) {
                i1 = rand.nextInt(newPreDomNodeInfoList.size());//reduce the invisible element
            }
            while (ids.contains(i1)) {
                i1 = rand.nextInt(newPreDomNodeInfoList.size());//reduce duplication
            }
            ids.add(i1);
            result.add(newPreDomNodeInfoList.get(i1));
        }
        return result;


    }

    public static void storeCandidates(PreDomNodeInfo old, String packageClassName, int lineNum, String gt_xpath, Event oldEvent, StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine, int size, WebDriver driver) {
        long start_time = System.currentTimeMillis();
        List<PreDomNodeInfo> newPreDomNodeInfoList = null;//

        List<PreDomNodeInfo> xpath_candidates = new ArrayList<>();
        xpath_candidates = xpathCandidates(old.getXpath(), newStateMachine, size);
//        Collections.shuffle(xpath_candidates);


        String output3 = "\\output\\chatgpt10_0520\\xpath\\";
        String ResultPath3 = output3 + packageClassName + "_" + lineNum + "_all.txt";
        StringBuilder sb3 = new StringBuilder();
        String beginning = "Here is an element and its information. It's extracted from the old version web page.";
        String mid = "Now I want to find out which element is most similar with this old element. And here are candidate elements and their information extracted from the new version.";

        sb3.append(beginning + old.getWholeInfo() + "\n" + mid);
        File file = new File(ResultPath3);
        try {
            if (file.getParentFile().mkdirs()) {
                System.out.println("success");
            }
            if (file.createNewFile() || file.length() == 0) {

                CompilationUnit parse = null;
                for (PreDomNodeInfo e : xpath_candidates
                ) {
                    sb3.append(e.getWholeInfo() + "\n");
                }
                sb3.append("Select the most similar element from candidates according to all these information.");
                FileOutputStream fos = new FileOutputStream(ResultPath3);
                fos.write(sb3.toString().getBytes());

                fos.close();
                System.out.println("question saved");
                String className = packageClassName.split("\\\\")[packageClassName.split("\\\\").length - 1];
                String fileName = className + "_" + lineNum;
                long end_time = System.currentTimeMillis();
                System.out.printf("generate candidates time：%d ms.", (end_time - start_time));
                calculateSimilarity(old, xpath_candidates, fileName, output3);
            }
        } catch (IOException e) {
            System.out.println("fail 1");
            e.printStackTrace();
        }

        long start_time2 = System.currentTimeMillis();
        List<PreDomNodeInfo> vw_candidates = new ArrayList<>();
        try {
            vw_candidates = vistaAndWater(oldStateMachine, newStateMachine, oldEvent, driver, size);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("vista water candidates fail");
        }
        for (PreDomNodeInfo o : vw_candidates
        ) {
            System.out.println(o.getWholeInfo());
        }
        //Collections.shuffle(vw_candidates);
        String output4 = "\\output\\chatgpt10_0520\\vw\\";
        String ResultPath4 = output4 + packageClassName + "_" + lineNum + "_all.txt";


        StringBuilder sb4 = new StringBuilder();

        sb4.append(beginning + old.getWholeInfo() + "\n" + mid);
        File file2 = new File(ResultPath4);
        try {
            if (file2.getParentFile().mkdirs()) {
                System.out.println("success");
            }
            if (file2.createNewFile() || file2.length() == 0) {

                CompilationUnit parse = null;
                for (PreDomNodeInfo e : vw_candidates
                ) {
                    sb4.append(e.getWholeInfo() + "\n");
                }
                sb4.append("Give me the most similar element's id according to all these information.");
                FileOutputStream fos = new FileOutputStream(ResultPath4);
                fos.write(sb4.toString().getBytes());

                fos.close();
                System.out.println("question saved");
                String className = packageClassName.split("\\\\")[packageClassName.split("\\\\").length - 1];
                String fileName = className + "_" + lineNum;
                long end_time = System.currentTimeMillis();
                System.out.printf("generate candidates time：%d ms.", (end_time - start_time2));
                calculateSimilarity(old, vw_candidates, fileName, output4);
            }
        } catch (IOException e) {
            System.out.println("fail2");
            e.printStackTrace();
        }



    }

    public static void storeCandidates2(PreDomNodeInfo old, String packageClassName, int lineNum, String gt_xpath, Event oldEvent, StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine, int size, WebDriver driver) {
        //storeSpecificCandidates(old, packageClassName, lineNum, gt_xpath, oldEvent, oldStateMachine, newStateMachine, size, driver, "random");
        storeSpecificCandidates(old, packageClassName, lineNum, gt_xpath, oldEvent, oldStateMachine, newStateMachine, size, driver, "vista");
        storeSpecificCandidates(old, packageClassName, lineNum, gt_xpath, oldEvent, oldStateMachine, newStateMachine, size, driver, "water");
//        storeSpecificCandidates(old,packageClassName,lineNum,gt_xpath,oldEvent,oldStateMachine,newStateMachine,size,driver,"vw");
        storeSpecificCandidates(old, packageClassName, lineNum, gt_xpath, oldEvent, oldStateMachine, newStateMachine, size, driver, "xpath");



    }

    public static void storeSpecificCandidates(PreDomNodeInfo old, String packageClassName, int lineNum, String gt_xpath, Event oldEvent, StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine, int size, WebDriver driver, String method) {
        long start_time2 = System.currentTimeMillis();

        List<PreDomNodeInfo> candidates = new ArrayList<>();



//        String beginning = "Here is an element and its information. It's extracted from the old version web page.";
//        String mid = "Now I want to find out which element is most similar with this old element. And here are candidate elements and their information extracted from the new version.";
        String beginning = "Target element: ";
        String mid = "Candidate elements: ";


        String outerClass = newStateMachine.getScriptSequence().getClassName();


        String output4 = "\\output\\chatgpt10_1114\\" + method + "\\";
        String ResultPath4 = output4 + packageClassName + "_" + lineNum + "_all.txt";
        String output5 = "\\output\\chatgpt10_1114_allinfo\\" + method + "\\";
        String ResultPath5 = output5 + packageClassName + "_" + lineNum + "_all.txt";

        StringBuilder sb4 = new StringBuilder();
        StringBuilder sb5 = new StringBuilder();
        sb4.append(beginning + old.getWholeInfo() + "\n" + mid);
        sb5.append(beginning + old.getWholeInfo() + "\n" + mid);
        boolean exist = false;
        if (ResultPath4.contains("\\po\\")) {//if (exist & ResultPath4.contains("po")) {
            ResultPath4 = ResultPath4.replace("_all", "_" + outerClass + "_all");
            ResultPath5 = ResultPath5.replace("_all", "_" + outerClass + "_all");
        }
        File file2 = new File(ResultPath4);
        try {
            if (file2.getParentFile().mkdirs()) {
                System.out.println("create folder successfylly");
            } else {
                System.out.println("fail to create folder, folder exist");
            }
            if (file2.createNewFile() || file2.length() == 0) {

                if (method.equals("random")) {
                    candidates = generateRandomCandidate(old, packageClassName, lineNum, gt_xpath, oldEvent, oldStateMachine, newStateMachine, size, driver);
                } else if (method.equals("xpath")) {
                    candidates = xpathCandidates(old.getXpath(), newStateMachine, size);
                } else if (method.equals("textxpath")) {
                    candidates = textXpathCandidates(old.isLeaf(), old.getText(), old.getXpath(), newStateMachine, size);
                } else if (method.equals("vista")) {
                    try {
                        candidates = VistaCandidates(oldStateMachine, newStateMachine, oldEvent, driver, size);
                    } catch (IOException e) {
                        System.out.println("vista fail to generate candidates");
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        System.out.println("vista fail to generate candidates");
                        e.printStackTrace();
                    }
                } else if (method.equals("water")) {
                    try {
                        candidates = WaterCandidates(oldStateMachine, newStateMachine, oldEvent, driver, size);
                    } catch (IOException e) {
                        System.out.println("water fail to generate candidates");
                        e.printStackTrace();
                    }
                } else if (method.equals("vw")) {
                    try {
                        candidates = vistaAndWater(oldStateMachine, newStateMachine, oldEvent, driver, size);
                    } catch (IOException e) {
                        System.out.println("vw fail to generate candidates");
                        e.printStackTrace();
                    }

                }else {
                    System.out.println("use undefined method to generate candidate, fail to generate");
                    return;
                }

                CompilationUnit parse = null;
                for (PreDomNodeInfo e : candidates
                ) {
                    sb4.append(e.getWholeInfo() + "\n");
                }
                long end_time = System.currentTimeMillis();
                double seconds = (end_time / 1000.0) % 60;
                String secondsString = String.format("%.3f", seconds);
                System.out.printf(method + " generate candidates consumed time: " + secondsString);
                sb4.append("\n"+"Time: "+secondsString);
                FileOutputStream fos = new FileOutputStream(ResultPath4);
                fos.write(sb4.toString().getBytes());

                fos.close();
                System.out.println("question saved");
                String className = packageClassName.split("\\\\")[packageClassName.split("\\\\").length - 1];
                String fileName = className + "_" + lineNum;

                //calculateSimilarity(old, candidates, fileName,output4);
            } else {
                exist = true;
                System.out.println("fail to create file, file exists");

            }
        } catch (IOException e) {
            exist = true;
            System.out.println("fail to create file, IOException");
            e.printStackTrace();
        }
        File file5 = new File(ResultPath5);
        try {
            if (file5.getParentFile().mkdirs()) {
                System.out.println("create folder successfylly");
            } else {
                System.out.println("fail to create folder, folder exist");
            }
            if (file5.createNewFile() || file5.length() == 0) {

                CompilationUnit parse = null;
                for (PreDomNodeInfo e : candidates
                ) {
                    sb5.append(e.toString() + "\n");
                }
                sb4.append("Give me the most similar element's id according to all these information.");
                FileOutputStream fos = new FileOutputStream(ResultPath5);
                fos.write(sb5.toString().getBytes());

                fos.close();
                System.out.println("question saved");
                String className = packageClassName.split("\\\\")[packageClassName.split("\\\\").length - 1];
                String fileName = className + "_" + lineNum;
                long end_time = System.currentTimeMillis();
                System.out.printf(method + " generate candidates consumed time：%d ms.", (end_time - start_time2));
                //calculateSimilarity(old, candidates, fileName,output4);
            } else {
                exist = true;
                System.out.println("fail to create file, file exists");

            }
        } catch (IOException e) {
            exist = true;
            System.out.println("fail to create file, IOException");
            e.printStackTrace();
        }

    }


    public static void calculateSimilarity(PreDomNodeInfo target, List<PreDomNodeInfo> candidates, String fileName, String output) {
        System.out.println("start calculating similarities, candidate list size: " + candidates.size());
        //int[][] ids = new int[9][5];
        StringBuilder[] ids = new StringBuilder[9];
        for (int i = 0; i < 9; i++) {
            ids[i] = new StringBuilder();
        }
        //属性
        //id
//        int id1 = -1;
//        int min_s1 = Integer.MAX_VALUE;
        for (PreDomNodeInfo c : candidates
        ) {
            if (target.getId().equals(c.getId())) {
//                id1 = c.getElementId();
//                min_s1 = getEditDistance(target.getId(), c.getId());
                ids[1].append(c.getElementId() + ",");
            }
            if (target.getName().equals(c.getName())) {
                ids[2].append(c.getElementId() + ",");
            }
            if (target.getText().equals(c.getText())) {
                ids[3].append(c.getElementId() + ",");
            }
            if (target.getTagName().equals(c.getTagName())) {
                ids[4].append(c.getElementId() + ",");
            }
            if (target.getLinkText().equals(c.getLinkText())) {
                ids[5].append(c.getElementId() + ",");
            }

        }

        int id6 = -1;
        int min_s6 = Integer.MAX_VALUE;
        for (PreDomNodeInfo c : candidates
        ) {
            int dif = Math.abs(target.getHeight() * target.getWidth() - c.getHeight() * c.getWidth());
            if (dif < min_s6) {
                min_s6 = dif;
                id6 = c.getElementId();
            }
        }
        ids[6].append(id6);
        //xpath
        int id7 = -1;
        int max_s7 = -1;
        for (PreDomNodeInfo c : candidates
        ) {
            if (longestPrefix(target.getXpath(), c.getXpath()) > max_s7) {
                max_s7 = longestPrefix(target.getXpath(), c.getXpath());
                id7 = c.getElementId();
            }
        }
        ids[7].append(id7);
        //location
        int id8 = -1;
        int min_s8 = Integer.MAX_VALUE;
        for (PreDomNodeInfo c : candidates
        ) {
            int dif = Math.abs(target.getX() - c.getX()) + Math.abs(target.getY() - c.getY());
            if (dif < min_s8) {
                min_s8 = dif;
                id8 = c.getElementId();
            }
        }
        ids[8].append(id8);

        int excelLineNum = -1;
        try {

            FileInputStream file = new FileInputStream(output + "similarities.xlsx");
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet("Sheet1");
            if (sheet == null) {
                sheet = workbook.createSheet("Sheet1");
            }

            for (int i = 0; i < 2000; i++) {
                XSSFRow row = sheet.getRow(i);

                // Check if the row is null or if all cells in the row are blank
                if (row == null || row.getPhysicalNumberOfCells() == 0 || row.getLastCellNum() == -1) {
                    System.out.println("Row " + i + " is blank");
                    Row row1 = sheet.createRow(i);
                    Cell cell = row1.createCell(0);
                    cell.setCellValue("---");
                    excelLineNum = i + 1;
                    break;
                }
            }


            String attrs[] = {"fileName", "id", "name", "text", "tagName", "linkText", "site", "xpath", "location", "ground_truth"};
            if (excelLineNum == 1) {
                Row row00 = sheet.createRow(0);
                for (int i = 0; i <= 9; i++) {
                    Cell cell00 = row00.createCell(i);
                    cell00.setCellValue(attrs[i]);
                }
            }

            // Create a row and cell
            Row row = sheet.createRow(excelLineNum);
            Cell cell = row.createCell(0);
//            System.out.println("file name in row 1 :"+fileName);
//            System.out.println(ids.toString());
            // Set the cell's value
            cell.setCellValue(fileName);
            for (int i = 1; i <= 8; i++) {
                //Row row0 = sheet.createRow(excelLineNum);
                Cell cell0 = row.createCell(i);
                cell0.setCellValue(ids[i].toString());
                //System.out.println("attr:"+attrs[i]+"  similar elementId:"+ids[i]);
            }
            Cell cell0 = row.createCell(9);
            cell0.setCellValue(ground_truth);

            FileOutputStream outputStream = new FileOutputStream(output + "similarities.xlsx");
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();
            System.out.println("Excel file (similarity) written successfully.");
        } catch (Exception e) {
            System.out.println("Error writing Excel file: " + e.getMessage());
            e.printStackTrace();
        }
        excelLineNum++;



        try {

            FileInputStream file1 = new FileInputStream(output + "attributes.xlsx");
            XSSFWorkbook workbook1 = new XSSFWorkbook(file1);
            XSSFSheet sheet1 = workbook1.getSheet("Sheet1");
            if (sheet1 == null) {
                sheet1 = workbook1.createSheet("Sheet1");
            }
            int excelLineNum_attr = -1;
            for (int i = 0; i < 1048575; i++) {
                XSSFRow row = sheet1.getRow(i);

                // Check if the row is null or if all cells in the row are blank
                if (row == null || row.getPhysicalNumberOfCells() == 0 || row.getLastCellNum() == -1) {
                    System.out.println("Row " + i + " is blank");
                    Row row1 = sheet1.createRow(i);
                    Cell cell = row1.createCell(0);
                    cell.setCellValue("---");
                    excelLineNum_attr = i + 1;
                    break;
                }
            }


            String attrs[] = {"numericId", "id", "name", "text", "tagName", "linkText", "site", "xpath", "location"};
            if (excelLineNum_attr == 1) {
                Row row00 = sheet1.createRow(0);
                for (int i = 1; i <= 9; i++) {
                    Cell cell00 = row00.createCell(i);
                    cell00.setCellValue(attrs[i - 1]);
                }
            }
            for (int i = 0; i < candidates.size() + 1; i++) {
                PreDomNodeInfo cur = null;
                Row row1 = sheet1.createRow(excelLineNum_attr);//target
                if (i < 1) {
                    cur = target;
                    Cell cell0 = row1.createCell(0);
                    cell0.setCellValue(fileName);
                } else {
                    cur = candidates.get(i - 1);
                }

                Cell cell1 = row1.createCell(1);
                cell1.setCellValue(cur.getElementId());
                Cell cell2 = row1.createCell(2);
                cell2.setCellValue(cur.getId());
                Cell cell3 = row1.createCell(3);
                cell3.setCellValue(cur.getName());
                Cell cell4 = row1.createCell(4);
                cell4.setCellValue(cur.getText());
                Cell cell5 = row1.createCell(5);
                cell5.setCellValue(cur.getTagName());
                Cell cell6 = row1.createCell(6);
                cell6.setCellValue(cur.getLinkText());
                Cell cell7 = row1.createCell(7);
                cell7.setCellValue(cur.getHeight() * cur.getWidth());
                Cell cell8 = row1.createCell(8);
                cell8.setCellValue(cur.getXpath());
                Cell cell9 = row1.createCell(9);
                cell9.setCellValue(cur.getX() + "," + cur.getY());

                excelLineNum_attr++;
            }

            Row row1 = sheet1.createRow(excelLineNum_attr);
            Cell cell = row1.createCell(0);
            cell.setCellValue("---");
            excelLineNum_attr++;

            FileOutputStream outputStream = new FileOutputStream(output + "attributes.xlsx");
            workbook1.write(outputStream);
            workbook1.close();
            System.out.println("Excel file (attribute) written successfully.");
        } catch (Exception e) {
            System.out.println("Error writing Excel file (attribute): " + e.getMessage());
            e.printStackTrace();
        }


    }

    public static int longestPrefix(String s1, String s2) {
        int len = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (i >= s2.length())
                break;
            if (s1.charAt(i) == s2.charAt(i)) {
                len++;
            }
        }
        return len;
    }

    public static double getSimilarity(String str1, String str2) {
        double editDistance = getEditDistance(str1, str2);
        double semanticSimilarity = getSemanticSimilarity(str1, str2);
        double similarity = (1.0 / (1.0 + editDistance)) * semanticSimilarity;
        return similarity;
    }

    private static double getSemanticSimilarity(String str1, String str2) {
        HashSet<String> words1 = getWords(str1);
        HashSet<String> words2 = getWords(str2);
        double intersection = 0.0;
        double union = 0.0;
        for (String word : words1) {
            if (words2.contains(word)) {
                intersection++;
            }
            union++;
        }
        for (String word : words2) {
            if (!words1.contains(word)) {
                union++;
            }
        }
        double similarity = intersection / union;
        return similarity;
    }

    private static HashSet<String> getWords(String str) {
        HashSet<String> words = new HashSet<>();
        String[] tokens = str.split("\\s+");
        for (String token : tokens) {
            words.add(token.toLowerCase());
        }
        return words;
    }

    private static int getEditDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i][j - 1], Math.min(dp[i - 1][j], dp[i - 1][j - 1]));
                }
            }
        }
        return dp[str1.length()][str2.length()];
    }


    public static void storeGroundTruth(String result, StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                        Event oldEvent) throws IOException {
        //for ground truth
        System.out.println("storing ground truth， signature is" + NewWebDriverProcess.signature);
        String signature = NewWebDriverProcess.signature;
        if (signature == null)
            signature = NewWebElementProcess.signature;
        lineNum = Integer.parseInt(oldEvent.getCodeLine().split(":")[1]);
        String packageClassName = signature.replace(".", "\\");//testcases.Claroline_Test_Suite.model_based_dataset.po.Login
        String className = oldEvent.getCodeLine().split(":")[0];
        String dataSetName = "Claroline_Test_Suite";
        // addressbook , Claroline_Test_Suite , collabtive , mantisbt , mrbs

        String filePath = "\\src\\main\\java\\" + packageClassName + ".java";
        String outputPath = "\\output\\ground_truth\\" + packageClassName + "_" + lineNum + ".txt";
        File file = new File(outputPath);
        try {
            if (file.getParentFile().mkdirs()) {
                System.out.println("create folder successfully");
            }
            if (file.createNewFile() || file.length() == 0) {//else file exists, do nothing
                StringBuilder sb = new StringBuilder();
                CompilationUnit parse = null;
                try {
                    parse = JavaParser.parse(new File(filePath));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                new MethodVisitor().visit(parse, null);
                sb.append("class name:" + className + "\n");
                sb.append("code line number:" + lineNum + "\n");
                sb.append("expect xpath:" + result + "\n");
                List<PreDomNodeInfo> newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                        + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
                for (PreDomNodeInfo temp : newPreDomNodeInfoList
                ) {
                    if (temp.getXpath().equals(result)) {
                        System.out.println("codeline:");
                        System.out.println(oldEvent.getCodeLine());
                        System.out.println("complete info:" + temp.getWholeInfo());
                        sb.append("complete info:" + temp.getWholeInfo() + "\n");
                        //System.out.println(temp.toString());
                        System.out.println("textual info:" + temp.getTextInfo());
                        sb.append("textual info:" + temp.getTextInfo() + "\n");
                        //System.out.println(temp.getTextInfo());
                        System.out.println("dom info:" + temp.getDomInfo());
                        sb.append("dom info:" + temp.getDomInfo() + "\n");
                        //System.out.println(temp.getDomInfo());
                        System.out.println();
                    }
                }
//                FileWriter writer=new FileWriter(file);
//                writer.write(sb.toString());
//                writer.close();

                FileOutputStream fos = new FileOutputStream(outputPath);
                fos.write(sb.toString().getBytes());

                fos.close();
                System.out.println("ground truth successfully");
            }
        } catch (IOException e) {
            System.out.println("fail to create file or folder");
            e.printStackTrace();
        }

    }


    public static String matchByWATER2(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                       Event oldEvent, WebDriver driver) throws IOException {
        List<PreDomNodeInfo> oldPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "preDomNodeInfo.json");
        String oldHtml = UtilsTxtLoader.readFile02(oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "temp.html");
        List<PreDomNodeInfo> newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
        PreDomNodeInfo preDomNodeInfo = null;
        for (PreDomNodeInfo temp : oldPreDomNodeInfoList) {
            if (temp.getXpath().equals(oldEvent.getAbsoluteXpath())) {
                preDomNodeInfo = temp;
                break;
            }
        }
//        if (preDomNodeInfo == null) {
//            return "";
//        }

        String signature = NewWebDriverProcess.signature;
        if (signature == null)
            signature = NewWebElementProcess.signature;
        lineNum = Integer.parseInt(oldEvent.getCodeLine().split(":")[1]);
        System.out.println("line number:" + lineNum);
        String packageClassName = signature.replace(".", "\\");//testcases.Claroline_Test_Suite.model_based_dataset.po.Login
        storeCandidates2(preDomNodeInfo, packageClassName, lineNum, ground_truth, oldEvent, oldStateMachine, newStateMachine, 10, newStateMachine.getDriver());

        WebElement webElement = WATER2.retrieveWebElementFromDOMInfo(newStateMachine.getDriver(),
                preDomNodeInfo, oldHtml, newPreDomNodeInfoList);
        if (webElement != null) {
            String re1 = UtilsXpath.generateXPathForWebElement(webElement, "");
            //System.out.println("water2 repaired result "+re1);
            //storeGroundTruth(re1,oldStateMachine,newStateMachine,oldEvent);
            //storeBrokenStatement(re1,oldStateMachine,newStateMachine,oldEvent);
            //return re1;
            return verifyXPath(preDomNodeInfo, re1, oldStateMachine, newStateMachine, oldEvent, driver);
        } else {
            //return null;
            return verifyXPath(preDomNodeInfo, null, oldStateMachine, newStateMachine, oldEvent, driver);
        }
        //(PreDomNodeInfo old, String result, StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
        //                                     Event oldEvent,WebDriver driver)
//        if (webElement != null) {
//            return UtilsXpath.generateXPathForWebElement(webElement, "");
//        } else {
//            return "";
//        }
    }

    public static String matchByWATER(StateMachineImpl oldStateMachine, StateMachineImpl newStateMachine,
                                      Event oldEvent) throws IOException {
        List<PreDomNodeInfo> oldPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(oldStateMachine.getSavePath() + File.separator
                + oldEvent.getSourceVertexId() + File.separator + "preDomNodeInfo.json");
        List<PreDomNodeInfo> newPreDomNodeInfoList = JsonProcess.readPreDomNodeInfoJson(newStateMachine.getSavePath() + File.separator
                + newStateMachine.getSourceStateVertex().getStateVertexId() + File.separator + "preDomNodeInfo.json");
        PreDomNodeInfo preDomNodeInfo = null;
        for (PreDomNodeInfo temp : oldPreDomNodeInfoList) {
            if (temp.getXpath().equals(oldEvent.getAbsoluteXpath())) {
                preDomNodeInfo = temp;
                break;
            }
        }
        if (preDomNodeInfo == null) {
            return "";
        }
        PreDomNodeInfo newPreDomNode = WATER.getNodeByLocator(preDomNodeInfo, newPreDomNodeInfoList);
        if (newPreDomNode != null) {
            return newPreDomNode.getXpath();
        } else {
            return "";
        }
    }


}
