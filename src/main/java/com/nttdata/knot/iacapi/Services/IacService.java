package com.nttdata.knot.iacapi.Services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.nttdata.knot.iacapi.Models.GithubPackage.GithubFileResponse.GetGithubFileResponse;
import com.nttdata.knot.iacapi.Models.IaC.DatabaseDevOps;
import com.nttdata.knot.iacapi.Models.IaC.IaC;
import com.nttdata.knot.iacapi.Models.IaC.Serverless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.nttdata.knot.iacapi.Interfaces.IIacService;
import com.nttdata.knot.iacapi.Interfaces.IGithubService;
import com.nttdata.knot.iacapi.Models.ComponentPackage.Component;
import com.nttdata.knot.iacapi.Models.ComponentPackage.Env;
import com.nttdata.knot.iacapi.Models.GithubPackage.GithubFileRequest.Committer;
import com.nttdata.knot.iacapi.Models.GithubPackage.GithubFileRequest.CreateGithubFileRequest;
import com.nttdata.knot.iacapi.Models.GithubPackage.GithubFileRequest.DeleteGithubFileRequest;

import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class IacService implements IIacService {

    private String repoName = "knot-onboarding-resources";
    private IGithubService githubService;
    private final Logger logger = LoggerFactory.getLogger(IacService.class);

    public IacService(IGithubService githubService) {
        this.githubService = githubService;
    }

    @Override
    public Mono<IaC> createIacAsync(String org, String area, String product, Component component)
            throws JsonProcessingException {

        // set the DatabaseDevOps class
        DatabaseDevOps databaseDevOps = null;
        Serverless serverless = null;

        if (component.getBbdd() != null && component.getBbdd().isEnabled()) {

            // set DatabaseDevOps class
            databaseDevOps = new DatabaseDevOps(
                    component.getBbdd().isEnabled(),
                    component.getBbdd().getType(),
                    component.getBbdd().getTier(),
                    component.getBbdd().getVersion(),
                    component.getBbdd().getName(),
                    component.getBbdd().getAdmin(),
                    component.getBbdd().getAdminPass());
        }

        if (component.getServerless().getCreateFunctionApp()) {
            serverless = component.getServerless();
            serverless.setApplicationStack(component.getTechnology());
        }

        // populate IaC object
        IaC iac = new IaC(component.getId(), databaseDevOps, serverless);

        // prepare the verticals values commit
        var values_iac = prepareValueForCommit(component, iac);

        // push the values of each enviroment of the vertical in
        // knot-onboarding-resources
        for (Env environment : component.getEnvironments()) {
            if (environment.isEnabled()) {
                this.githubService.createGithubFileAsync(values_iac, repoName,
                        "products/" + org + "/" + area + "/" + product + "/" + component.getId() + "/iac/"
                                + environment.getEnvPath() + "/values.yaml")
                        .block();
            }
        }

        return Mono.just(iac);
    }

    @Override
    public Mono<DeleteGithubFileRequest> deleteIacAsync(String org, String area, String product, String ComponentName,
            String enviroment) {

        // get the file to delete
        var valuesFile = this.githubService.getGithubFileAsync(repoName,
                "products/" + org + "/" + area + "/" + product + "/" + ComponentName + "/iac/" + enviroment
                        + "/values.yaml")
                .block();

        // set the commit
        Committer committer = new Committer();
        committer.setEmail("41898282+github-actions[bot]@users.noreply.github.com");
        committer.setName("github-actions[bot]");

        DeleteGithubFileRequest deleteGithubFileRequest = new DeleteGithubFileRequest();
        deleteGithubFileRequest
                .setMessage("Removing IaC vertical into a Component, with name " + ComponentName);
        deleteGithubFileRequest.setCommitter(committer);
        deleteGithubFileRequest.setSha(valuesFile.getSha());

        // delete the file
        this.githubService.deleteGithubFileAsync(deleteGithubFileRequest,
                repoName,
                "products/" + org + "/" + area + "/" + product + "/" + ComponentName + "/iac/" + enviroment
                        + "/values.yaml")
                .block();

        return Mono.just(deleteGithubFileRequest);
    }

    @Override
    public Mono<IaC> updateIacAsync(String org, String area, String product, Component component)
            throws JsonProcessingException {
        // set the DatabaseDevOps class
        DatabaseDevOps databaseDevOps = null;
        Serverless serverless = null;

        if (component.getBbdd() != null && component.getBbdd().isEnabled()) {

            // set DatabaseDevOps class
            databaseDevOps = new DatabaseDevOps(
                    component.getBbdd().isEnabled(),
                    component.getBbdd().getType(),
                    component.getBbdd().getTier(),
                    component.getBbdd().getVersion(),
                    component.getBbdd().getName(),
                    component.getBbdd().getAdmin(),
                    component.getBbdd().getAdminPass());
        }

        // populate IaC object
        IaC iac = new IaC(component.getId(), databaseDevOps, serverless);

        for (Env environment : component.getEnvironments()) {
            if (environment.isEnabled()) {
                GetGithubFileResponse valuesFile = null;

                try {
                    // get the file to update
                    valuesFile = this.githubService.getGithubFileAsync(repoName,
                            "products/" + org + "/" + area + "/" + product + "/" + component.getId() + "/iac/"
                                    + environment.getEnvPath() + "/values.yaml")
                            .block();
                } catch (WebClientResponseException.NotFound ex) {

                    logger.error("File not found: " + ex.getMessage());
                }

                // prepare the verticals values commit ans set the SHA
                var values_iac = prepareValueForCommit(component, iac);

                if (valuesFile != null) {

                    values_iac.setSha(valuesFile.getSha());
                }

                // push the values to the repository
                this.githubService.createGithubFileAsync(values_iac, repoName,
                        "products/" + org + "/" + area + "/" + product + "/" + component.getId() + "/iac/"
                                + environment.getEnvPath() + "/values.yaml")
                        .block();

            }
        }
        return Mono.just(iac);
    }

    // serialize content of values and prepare a commit
    private CreateGithubFileRequest prepareValueForCommit(Component component, Object vertical)
            throws JsonProcessingException {

        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
        ObjectMapper objectMapper = new ObjectMapper(yamlFactory);

        String verticalInBase64String = Base64.getEncoder()
                .encodeToString(objectMapper
                        .writeValueAsString(vertical).getBytes(StandardCharsets.UTF_8));

        Committer committer = new Committer();
        committer.setEmail("41898282+github-actions[bot]@users.noreply.github.com");
        committer.setName("github-actions[bot]");

        CreateGithubFileRequest createGithubFileRequest = new CreateGithubFileRequest();
        createGithubFileRequest.setMessage("Add new IaC vertical into a Component, with name " + component.getName());
        createGithubFileRequest.setCommitter(committer);
        createGithubFileRequest.setContent(verticalInBase64String);

        return createGithubFileRequest;

    }
}
