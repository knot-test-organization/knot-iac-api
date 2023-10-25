package com.nttdata.knot.iacapi.Controllers;

import com.nttdata.knot.iacapi.Models.IaC.IaC;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nttdata.knot.iacapi.Interfaces.IIacService;
import com.nttdata.knot.iacapi.Models.ComponentPackage.Component;
import com.nttdata.knot.iacapi.Models.GithubPackage.GithubFileRequest.DeleteGithubFileRequest;

@RestController
@RequestMapping("/iac")
public class IacController {

    private IIacService iacService;
    private static final Logger logger = LoggerFactory.getLogger(IaC.class);

    @Autowired
    public IacController(IIacService iacService) {
        this.iacService = iacService;

    }

    @PostMapping("/{org}/{area}/{product}")
    public ResponseEntity<Mono<IaC>> create(@PathVariable String org, @PathVariable String area, @PathVariable String product, @RequestBody Component component) throws JsonProcessingException {
        var iac = iacService.createIacAsync(org, area, product, component);
        return ResponseEntity.ok(iac);
    }

    @DeleteMapping("/{org}/{area}/{product}/{name}/{enviroment}")
    public ResponseEntity<Mono<DeleteGithubFileRequest>> delete(@PathVariable String org, @PathVariable String area, @PathVariable String product, @PathVariable String name,
            @PathVariable String enviroment) throws JsonProcessingException {

        var delete = iacService.deleteIacAsync(org, area, product, name, enviroment);
        logger.info("The component {} is being deleted", name);

        return ResponseEntity.ok(delete);
    }
    
    @PutMapping("/{org}/{area}/{product}")
    public  ResponseEntity<Mono<IaC>> update (@PathVariable String org, @PathVariable String area, @PathVariable String product, @RequestBody Component component) throws JsonProcessingException {
          
        var iac = iacService.updateIacAsync(org, area, product, component);
        logger.info("The component {} is being Updated", component.getName());

        return ResponseEntity.ok(iac);
    }

}
