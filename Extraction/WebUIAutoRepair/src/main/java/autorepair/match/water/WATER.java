package autorepair.match.water;

import autorepair.state.datacollect.DomNodeInfo;
import autorepair.state.datacollect.PreDomNodeInfo;

import java.util.List;

public class WATER {

    public static PreDomNodeInfo getNodeByLocator(PreDomNodeInfo oldPreDomNodeInfo, List<PreDomNodeInfo> newPreDomNodeInfoList) {
        for (PreDomNodeInfo preDomNodeInfo : newPreDomNodeInfoList) {
            if (!oldPreDomNodeInfo.getId().isEmpty() && !preDomNodeInfo.getId().isEmpty() && oldPreDomNodeInfo.getId().equals(preDomNodeInfo.getId())) {
                return preDomNodeInfo;
            }
            if (!oldPreDomNodeInfo.getXpath().isEmpty() && !preDomNodeInfo.getXpath().isEmpty() && oldPreDomNodeInfo.getXpath().equals(preDomNodeInfo.getXpath())) {
                System.out.println("get element by getXpath:" + oldPreDomNodeInfo.getXpath());

                return preDomNodeInfo;
            }
            if (oldPreDomNodeInfo.getAttributes().get("class") != null && preDomNodeInfo.getAttributes().get("class") != null &&
                    oldPreDomNodeInfo.getAttributes().get("class").equals(preDomNodeInfo.getAttributes().get("class"))) {
                System.out.println("get element by class:" + oldPreDomNodeInfo.getAttributes().get("class"));

                return preDomNodeInfo;
            }
            if (!oldPreDomNodeInfo.getText().isEmpty() && !preDomNodeInfo.getText().isEmpty() && oldPreDomNodeInfo.getText().equals(preDomNodeInfo.getText())) {
                System.out.println("get element by text:" + oldPreDomNodeInfo.getText());
                return preDomNodeInfo;
            }
            if (!oldPreDomNodeInfo.getName().isEmpty() && !preDomNodeInfo.getName().isEmpty() && oldPreDomNodeInfo.getName().equals(preDomNodeInfo.getName())) {
                return preDomNodeInfo;
            }

        }
        for (PreDomNodeInfo preDomNodeInfo : newPreDomNodeInfoList) {
            if (getSimilarityScore(oldPreDomNodeInfo, preDomNodeInfo).compareTo(0.5) > 0) {
                System.out.println("get element by similarity:" + oldPreDomNodeInfo.getText());

                return preDomNodeInfo;
            }
        }
        return null;
    }


    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    public static int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];
        for (int i = 0; i <= str1.length(); i++)
            distance[i][0] = i;
        for (int j = 0; j <= str2.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= str1.length(); i++)
            for (int j = 1; j <= str2.length(); j++)
                distance[i][j] = minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));
        return distance[str1.length()][str2.length()];
    }

    private static Double getSimilarityScore(PreDomNodeInfo a, PreDomNodeInfo b) {
        double alpha = 0.9;
        double rho, rho1, rho2 = 0;
        if (a.getTagName().equals(b.getTagName())) {
            double levDist = computeLevenshteinDistance(a.getXpath(), b.getXpath());
            rho1 = 1 - levDist / Math.max(a.getXpath().length(), b.getXpath().length());
            if (Math.abs(a.getX() - b.getX()) <= 5 && Math.abs((a.getX() + a.getWidth()) - (b.getX() - b.getWidth())) <= 5
                    && Math.abs(a.getY() - b.getY()) <= 5 && Math.abs((a.getY() + a.getHeight()) - (b.getY() - b.getHeight())) <= 5) {
                rho2 = rho2 + 1;
            }
            rho2 = rho2 / 2;
            rho = (rho1 * alpha + rho2 * (1 - alpha));
            return rho;
        }
        return 0.0;
    }

}
