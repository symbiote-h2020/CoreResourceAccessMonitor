package eu.h2020.symbiote.cram.model;

import org.springframework.data.annotation.Id;

/**
 * Created by vasgl on 6/30/2017.
 */
public class CramPersistentVariables {

    @Id
    private String id;
    protected String variableName;

    public String getId() { return this.id; }
    public void setId(String id) { this.id = id; }
}
