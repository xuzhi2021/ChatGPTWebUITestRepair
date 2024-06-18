package testcases;

import config.UrlConfig;
import utils.UtilsProperties;

import java.io.IOException;

public class Constants {
    public static final String Mantisbt_ADMIN_USER_NAME = "administrator";
    public static final String Mantisbt_ADMIN_PASSWORD = "root";
    public static final String Claroline_ADMIN_USER_NAME = "admin";
    public static final String Claroline_ADMIN_PASSWORD = "123456";
    public static final String Collabtive_ADMIN_USER_NAME = "admin";
    public static final String Collabtive_ADMIN_PASSWORD = "123456";

    public static String getCollabtiveURL() {
        try {
            if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
                return UrlConfig.COLLABTIVE_NEW;
            } else {
                return UrlConfig.COLLABTIVE_OLD;
            }
        }catch (IOException ioException){
            return UrlConfig.COLLABTIVE_OLD;
        }
    }

    public static String getClarolineURL() {
        try {
            if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
                return UrlConfig.CLAROLINE_NEW;
            }
        }catch (IOException ioException){

        }
        return UrlConfig.CLAROLINE_OLD;
    }

    public static String getClarolineDBName() throws IOException {
        if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
            return "claroline1107";
        } else {
            return "claroline1115";
        }
    }

    public static String getAddressbookDBName() throws IOException {
        if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
                return "addressbook40";
        } else {
            return "addressbook61";
        }
    }


    public static String getMantisbtDBName() throws IOException {
        if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
            return "bugtracker120";
        } else {
            return "bugtracker118";
        }
    }


    public static String getCollabtiveDBName() throws IOException {
        if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
            return "collabtive10";
        } else {
            return "collabtive075";
        }
    }


    public static String getMRBSDBName() throws IOException {
        if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
            return "mrbs149";
        } else {
            return "mrbs1261";
        }
    }

    public static String getAddressBookUrl()  {
        try {
            if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
                return UrlConfig.ADDRESS_BOOK_NEW;
            } else {
                return UrlConfig.ADDRESS_BOOK_OLD;
            }
        }catch (IOException ioException){
        }
        return UrlConfig.ADDRESS_BOOK_OLD;
    }

    public static String getMantisUrl()   {
        try {
            if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
                return UrlConfig.MANTISBT_NEW;
            } else {
                return UrlConfig.MANTISBT_OLD;
            }
        }catch (IOException ioException){

        }
        return UrlConfig.MANTISBT_OLD;
    }

    public static String getMRBSUrl() throws IOException {
        if (UtilsProperties.getConfigProperties().getProperty("version").trim().equals("new")) {
            return UrlConfig.MRBS_NEW;
        } else {
            return UrlConfig.MRBS_OLD;
        }
    }



}
