package autorepair.validate;

public class ValidateException extends Exception{
    @Override
    public String getMessage(){
        return "fail to validate";
    }
}
