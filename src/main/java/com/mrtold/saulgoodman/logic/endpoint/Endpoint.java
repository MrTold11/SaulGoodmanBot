package com.mrtold.saulgoodman.logic.endpoint;

import com.mrtold.saulgoodman.Config;
import com.mrtold.saulgoodman.database.DatabaseConnector;
import com.mrtold.saulgoodman.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * @author Mr_Told
 */
public abstract class Endpoint {

    protected final Logger log = LoggerFactory.getLogger(Endpoint.class);

    protected final DatabaseConnector db = DatabaseConnector.getInstance();
    protected final Strings s = Strings.getInstance();
    protected final Config config = Config.getInstance();

    private Runnable onSuccess;
    private Consumer<String> onFailure;

    public abstract void execute();

    public Endpoint onSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public Endpoint onFailure(Consumer<String> onFailure) {
        this.onFailure = onFailure;
        return this;
    }

    public void exec(Runnable onSuccess, Consumer<String> onFailure) {
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        execute();
    }

    protected void onSuccessEP() {
        if (onSuccess != null)
            onSuccess.run();
    }

    protected void onFailureEP(String error) {
        if (onFailure != null)
            onFailure.accept(error);
    }

}
