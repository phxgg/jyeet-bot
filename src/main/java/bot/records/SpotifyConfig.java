package bot.records;

public class SpotifyConfig {
    public String clientId;
    public String clientSecret;
    public String countryCode;

    public SpotifyConfig() {

    }

    public SpotifyConfig(String clientId, String clientSecret, String countryCode) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.countryCode = countryCode;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

}
