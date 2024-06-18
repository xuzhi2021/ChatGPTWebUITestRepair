package autorepair.patch;

import config.Settings;
import utils.UtilsTxtLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShowExperiment {
    static StringBuilder html = new StringBuilder();
    static List<File> experimentFileList = new ArrayList<>();

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File(Settings.PROJECT_PATH + "experiment\\new\\testcases\\mrbs\\study\\");
        dfs(file);
        html.append("<html>\n" +
                "<body>" +
                "<style>.bg{background: #0f0}</style>\n" +
                "<div>");
        for (File temp : experimentFileList) {
            perExperiment(temp);
        }
        html.append("</div>\n" +
                "</body>\n" +
                "</html>");
        UtilsTxtLoader.save("D:\\rep\\WebUIAutoRepair\\show\\", "show.html", html.toString());
    }

    public static void dfs(File file) {
        for (File temp : file.listFiles()) {
            if (temp.isDirectory()) {
                dfs(temp);
            } else {
                experimentFileList.add(temp);
            }
        }
    }

    public static void perExperiment(File file) throws FileNotFoundException {
        Experiment experiment = Experiment.load(file.getAbsolutePath());
        System.out.println(experiment);


        addExperiment(experiment);

    }

    public static void addExperiment(Experiment experiment) {
        html.append("<hr style=\"width:2000PX;height:20px;\" color= \"blue\"></br>");
        html.append("<h1>" + experiment.scriptName + "----<font color=\"#FF0000\">" + experiment.algorithmName + "</font></h1>");
        int total = 0;
        int screenTrue = 0;
        int screenFalse = 0;
        int diffTrue = 0;
        int diffFalse = 0;
        for (int i = 0; i < experiment.records.size(); i++) {
            Record record = experiment.records.get(i);
            addRecord(record, i, experiment.algorithmName);

            if (record.repairLocator.startsWith("/html")) {
                total++;
            } else {
                if (!record.method.contains("getText")){
                    if (Boolean.parseBoolean(record.validaResult)) {
                        screenTrue++;
                    } else {
                        screenFalse++;
                    }
                    if (Boolean.parseBoolean(record.differenceResult)){
                        diffTrue++;
                    }else{
                        diffFalse++;
                    }
                }

            }
        }
        html.append("<strong>repair times</strong><font color=\"#FF0000\">" + total + "</font></br>");
        html.append("<strong>screen</strong><font color=\"#FF0000\">" + screenTrue + "/" + screenFalse + "</font></br>");
        html.append("<strong>screen</strong><font color=\"#FF0000\">" + diffTrue + "/" + diffFalse + "</font>");

    }

    public static void addRecord(Record record, int index, String experimentName) {
        String label = "";
        if (record.repairLocator != null && !record.repairLocator.equals("")) {
            label = "<font color = \"#FF0000\">*************</font>";
        }

    }

    public static boolean isB(String str) {
        if (str == null) return false;
        return str.equals("true") || str.equals("false");
    }

}
