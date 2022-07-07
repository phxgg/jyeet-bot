package bot.dto;

public class Response {
    private int code;
    private String message;
    private String error;
    private Object data;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public Object getData() {
        return data;
    }
}
