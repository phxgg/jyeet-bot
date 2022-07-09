package bot.dto;

public class Response {
    private int code;
    private String message;
    private String error;
    private Object data;

    public Response(int code, String message, String error, Object data) {
        this.code = code;
        this.message = message;
        this.error = error;
        this.data = data;
    }

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
