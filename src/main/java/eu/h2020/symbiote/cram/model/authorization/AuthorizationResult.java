package eu.h2020.symbiote.cram.model.authorization;

/**
 * Created by mateuszl on 12.06.2017.
 */
public class AuthorizationResult {

    private final String message;
    private final boolean validated;

    public AuthorizationResult(String message, boolean validated) {
        this.message = message;
        this.validated = validated;
    }

    public String getMessage() { return message; }
    public boolean isValidated() { return validated; }
}
