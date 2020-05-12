package it.flowing.complex.service;

import it.flowing.complex.model.QueryData;
import it.flowing.complex.model.SearchResult;
import it.flowing.complex.model.SearchType;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.rest.RestStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class ElasticServiceTest {

    @Inject
    private ElasticService elasticService;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addPackages(false,
                        "it.flowing.complex.service",
                        "it.flowing.complex.model")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void before() {
        elasticService.openConnection();
    }

    @After
    public void terminate() throws IOException {
        elasticService.closeConnection();
    }

    @Test(expected = NullPointerException.class)
    public void SearchShouldThrowErrorIfNullQueryBuilderProvided() throws Exception {
        elasticService.search(null);
    }

    @Test
    public void SearchShouldReturnAValidSearchResult() throws Exception {
        QueryData queryData = (new QueryData())
                .withSearchType(SearchType.MATCH_ALL_QUERY);

        SearchResult searchResult = elasticService.search(queryData);

        // About SearchResult
        assertNotNull(searchResult);

        // About Search Execution
        assertEquals(RestStatus.OK, searchResult.getStatus());
        assertFalse(searchResult.getTimedOut());
        assertNull(searchResult.getTerminatedEarly());
        assertNotEquals(0L, searchResult.getTook().duration());

        // About Shards Execution
        assertNotEquals(0, searchResult.getTotalShards());
        assertNotEquals(0, searchResult.getSuccessfulShards());
        assertEquals(0, searchResult.getFailedShards());

        // Hits
        assertEquals(4675L, searchResult.getNumHits());
        assertEquals(1.0f, searchResult.getMaxScore(), 0.1f);
        assertEquals(TotalHits.Relation.EQUAL_TO, searchResult.getHitsRelation());
    }

    @Test
    public void SearchWithFromShouldReturnTheRightSubsetOfResult() throws Exception {
        QueryData queryData = (new QueryData())
                .withSearchType(SearchType.MATCH_ALL_QUERY)
                .withFrom(Optional.of(4670));

        SearchResult searchResult = elasticService.search(queryData);
        assertEquals(5L, searchResult.getHits().size());
    }

    @Test
    public void SearchWithSizeShouldReturnTheRightSubsetOfResult() throws Exception {
        QueryData queryData = (new QueryData())
                .withSearchType(SearchType.MATCH_ALL_QUERY)
                .withSize(Optional.of(5));

        SearchResult searchResult = elasticService.search(queryData);
        assertEquals(5L, searchResult.getHits().size());
    }

    @Test
    public void SearchWithPaginationShouldReturnTheRightSubsetOfResult() throws Exception {
        QueryData queryData = (new QueryData())
                .withSearchType(SearchType.MATCH_ALL_QUERY)
                .withFrom(Optional.of(1))
                .withSize(Optional.of(2));

        SearchResult searchResult = elasticService.search(queryData);
        assertEquals(2L, searchResult.getHits().size());
    }

    @Test
    public void SearchWithTimeoutShouldReturnTimedOutStatus() throws Exception {
        QueryData queryData = (new QueryData())
                .withSearchType(SearchType.MATCH_ALL_QUERY)
                .withTimeout(Optional.of(new TimeValue(5, TimeUnit.MINUTES)));

        SearchResult searchResult = elasticService.search(queryData);
        assertFalse(searchResult.getTimedOut());

        // TODO: Verificare: impostando il timeout ad un valore basso, se la query impiega di più, getTimedOut ritorna false
    }

}
