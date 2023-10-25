package com.nttdata.knot.iacapi.Interfaces;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.nttdata.knot.iacapi.Models.ComponentPackage.Component;
import com.nttdata.knot.iacapi.Models.GithubPackage.GithubFileRequest.DeleteGithubFileRequest;
import com.nttdata.knot.iacapi.Models.IaC.IaC;
import reactor.core.publisher.Mono;


public interface IIacService {
    

    Mono<IaC> createIacAsync(String org, String area, String product, Component component) throws JsonProcessingException;

    Mono<DeleteGithubFileRequest> deleteIacAsync(String org, String area, String product, String ComponentName, String enviroment);

    Mono<IaC> updateIacAsync(String org, String area, String product, Component component) throws JsonProcessingException;


    
}
