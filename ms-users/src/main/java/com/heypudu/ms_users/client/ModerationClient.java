package com.heypudu.ms_users.client;

import com.heypudu.ms_users.dto.response.AuthorReportCountResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

@Component
public class ModerationClient {

    private static final Logger log = LoggerFactory.getLogger(ModerationClient.class);

    private final RestClient postsRestClient;
    private final RestClient interactionsRestClient;

    public ModerationClient(@Qualifier("postsRestClient") RestClient postsRestClient,
                            @Qualifier("interactionsRestClient") RestClient interactionsRestClient) {
        this.postsRestClient = postsRestClient;
        this.interactionsRestClient = interactionsRestClient;
    }

    public List<AuthorReportCountResponse> getPostReportCounts(String jwt) {
        return fetch(postsRestClient, "/api/v1/internal/posts/reports/counts", jwt, "ms-posts");
    }

    public List<AuthorReportCountResponse> getCommentReportCounts(String jwt) {
        return fetch(interactionsRestClient, "/api/v1/internal/comment-reports/counts", jwt, "ms-interactions");
    }

    private List<AuthorReportCountResponse> fetch(RestClient client,
                                                  String uri,
                                                  String jwt,
                                                  String service) {
        try {
            AuthorReportCountResponse[] counts = client.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .retrieve()
                    .body(AuthorReportCountResponse[].class);

            return counts != null ? Arrays.asList(counts) : List.of();
        } catch (RestClientException ex) {
            log.warn("No se pudieron obtener los conteos de reportes desde {}: {}",
                    service, ex.getMessage());
            return List.of();
        }
    }
}
