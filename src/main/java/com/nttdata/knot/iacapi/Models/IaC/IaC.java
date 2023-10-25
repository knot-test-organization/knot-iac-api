package com.nttdata.knot.iacapi.Models.IaC;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IaC {

    private String name;
    private DatabaseDevOps database;
    private Serverless serverless;

    public IaC(String name, DatabaseDevOps database, Serverless serverless) {
        this.name = name;
        this.database = database;
        this.serverless = serverless;
    }

    public IaC() {

    }
}
