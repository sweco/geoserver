package org.geoserver.rest.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.geoserver.rest.format.MapJSONFormat;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.restlet.resource.Representation;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import se.sweco.gis.search.SearchConfiguration;

/**
 * Forsmark search controller / proxy with permission filter.
 */
public class FkaSearchController extends AbstractController {

    private static String SECURITY_DEFAULT = "true";
    private static String SECURITY_FIELD_DEFAULT = "GRUPP";
    private static String CACHE_DEFAULT = "0";
    private static String SEARCH_SERVICE_HOST_DEFAULT = "127.0.0.1";
    private static String SEARCH_SERVICE_PORT_DEFAULT = "9300";

    private static boolean SECURITY_ENABLED = Boolean.valueOf(System.getProperty("se.sweco.gis.search.security.enabled", SECURITY_DEFAULT));
    private static String SECURITY_FIELD = System.getProperty("se.sweco.gis.search.security.field", SECURITY_FIELD_DEFAULT);
    private static int CACHE_SECONDS = Integer.valueOf(System.getProperty("se.sweco.gis.search.cache", CACHE_DEFAULT));
    private static String SEARCH_SERVICE_HOST = System.getProperty("se.sweco.gis.search.service.host", SEARCH_SERVICE_HOST_DEFAULT);
    private static int SEARCH_SERVICE_PORT = Integer.valueOf(System.getProperty("se.sweco.gis.search.service.port", SEARCH_SERVICE_PORT_DEFAULT));

    private static String ENCODING_UTF8 = "UTF-8";
    private static String REQUEST_ENCODING = ENCODING_UTF8;
    private static String RESPONSE_ENCODING = ENCODING_UTF8;
    private static String FIELD_ALL = "_all";
    private static Logger LOGGER = Logging.getLogger(FkaSearchController.class);

    private Client client;

    public FkaSearchController() throws UnknownHostException {
        setSupportedMethods(new String[] { METHOD_GET, METHOD_POST });
        setRequireSession(false);
        setCacheSeconds(CACHE_SECONDS);
        Settings settings = Settings.settingsBuilder().put("cluster.name", "forsmark-cartan").build();
        client = TransportClient.builder().settings(settings).build().
                addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(SEARCH_SERVICE_HOST), SEARCH_SERVICE_PORT));
    }

    @Override
    protected void finalize() throws Throwable {
        if (client != null) {
            client.close();
        }
    }

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest req,
            HttpServletResponse resp) throws Exception {
        Authentication auth = SecurityContextHolder.getContext() == null ?
                null : SecurityContextHolder.getContext().getAuthentication();

        if (HttpMethod.POST.equals(HttpMethod.valueOf(req.getMethod()))) {
            handlePost(auth, req, resp);
        } else {
            handleGet(auth, req, resp);
        }
        return null;
    }

    private void handlePost(Authentication auth, HttpServletRequest req,
            HttpServletResponse resp) {
        JSONObject json = getRequestJSON(req);
        SearchResponse response = null;
        if (json != null) {
            SearchRequestBuilder search = prepareSearchRequest(getSearchConfiguration(req));
            QueryBuilder unfiltered = getQueryBuilder(json);
            QueryBuilder q;
            if (SECURITY_ENABLED) {
                q = QueryBuilders.boolQuery().must(unfiltered).filter(getSecurityFilter(auth));
            } else {
                q = unfiltered;
            }
            search.setQuery(q);

            int from = json.optInt("from", -1);
            int size = json.optInt("size", -1);
            if (from > -1) {
                search.setFrom(from);
            }
            if (size > -1) {
                search.setSize(size);
            }
            response = search.execute().actionGet();
        }
        if (response != null) {
            resp.setStatus(HttpStatus.OK.value());
            resp.setContentType("application/json; charset=" + RESPONSE_ENCODING);
            writeResponse(response, resp);
        } else {
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            resp.setContentType("text/plain");
            OutputStream out = null;
            try {
                out = resp.getOutputStream();
                IOUtils.write("Search Controller failed", out);
                out.flush();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "handlePost exception", e);
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }

    private void writeResponse(SearchResponse response, HttpServletResponse resp) {
        OutputStream out = null;
        try {
            out = resp.getOutputStream();
            XContentBuilder builder = XContentFactory.jsonBuilder(out);
            builder.startObject();
            response.toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
            builder.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "writeResponse exception", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    private SearchRequestBuilder prepareSearchRequest(SearchConfiguration conf) {
        final SearchRequestBuilder search;
        if (!StringUtils.isBlank(conf.getIndex())) {
            search = client.prepareSearch(conf.getIndex());
        } else {
            search = client.prepareSearch();
        }
        if (!StringUtils.isBlank(conf.getType())) {
            search.setTypes(conf.getType());
        }
        return search;
    }

    private JSONObject getRequestJSON(HttpServletRequest req) {
        JSONObject json = null;
        InputStream in = null;
        try {
            in = req.getInputStream();
            json = (JSONObject) JSONSerializer.toJSON(IOUtils.toString(in, REQUEST_ENCODING));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "getRequestJSON exception", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return json;
    }

    private List<String> getRoles(Authentication auth) {
        List<String> roles = new ArrayList<String>();
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (!authority.getAuthority().equals(GeoServerRole.ANONYMOUS_ROLE.getAuthority())
                &&
                !authority.getAuthority().equals(GeoServerRole.AUTHENTICATED_ROLE.getAuthority())
                &&
                !authority.getAuthority().equals(GeoServerRole.ADMIN_ROLE.getAuthority())
                &&
                !authority.getAuthority().equals(GeoServerRole.GROUP_ADMIN_ROLE.getAuthority())) {
                roles.add(authority.getAuthority());
            }
        }
        return roles;
    }

    private QueryBuilder getSecurityFilter(Authentication auth) {
        // TODO: session attribute/cache? cookie = key GUID to cache, cache limited lifetime
        final List<String> roles = getRoles(auth);
        final BoolQueryBuilder securityFieldNotSet = QueryBuilders.boolQuery().
            mustNot(QueryBuilders.existsQuery(SECURITY_FIELD));
        if (roles.isEmpty()) {
            return securityFieldNotSet;
        }
        final BoolQueryBuilder filter = QueryBuilders.boolQuery();
        for (final String role : roles) {
            filter.should(QueryBuilders.termQuery(SECURITY_FIELD, role));
        }
        filter.should(securityFieldNotSet);
        return filter;
    }

    private QueryBuilder getQueryBuilder(JSONObject json) {
        final String field = StringUtils.trimToNull(json.optString("field"));
        final String query = StringUtils.trimToNull(json.getString("query"));
        final String analyzer = StringUtils.trimToNull(json.optString("analyzer", null));
        final BoolQueryBuilder q = QueryBuilders.boolQuery();
        
        // Combined query:
        //  A. use prefix matching depending on if single/multiple word search
        //     A1. single word: prefix
        //     A2. multiple word: match_phrase_prefix
        //  B. use match for letting synonyms match on type
        // 
        final boolean multiWord = StringUtils.contains(query, ' ');
        if (field != null) {
            if (multiWord) {
                q.should(getMatchPhrasePrefixQuery(field, query, analyzer));
            } else {
                q.should(getPrefixQuery(field, query));
            }
            q.should(getMatchQuery(field, query, analyzer));
        } else {
            if (multiWord) {
                q.should(getMatchPhrasePrefixQuery(query, analyzer));
            } else {
                q.should(getPrefixQuery(query));
            }
            q.should(getMatchQuery(query, analyzer));
        }
        return q;
    }

    private QueryBuilder getMatchPhrasePrefixQuery(String phrase, String analyzer) {
        return getMatchPhrasePrefixQuery(FIELD_ALL, phrase, analyzer);
    }

    private QueryBuilder getMatchPhrasePrefixQuery(String field, String phrase, String analyzer) {
        MatchQueryBuilder queryBuilder = QueryBuilders.matchPhrasePrefixQuery(field, phrase);
        if (analyzer != null) {
            queryBuilder.analyzer(analyzer);
        }
        return queryBuilder;
    }

    private QueryBuilder getMatchQuery(String phrase, String analyzer) {
        return getMatchQuery(FIELD_ALL, phrase, analyzer);
    }

    private QueryBuilder getMatchQuery(String field, String phrase, String analyzer) {
        MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery(field, phrase);
        if (analyzer != null) {
            queryBuilder.analyzer(analyzer);
        }
        return queryBuilder;
    }

    private QueryBuilder getPrefixQuery(String search) {
        return getPrefixQuery(FIELD_ALL, search);
    }

    private QueryBuilder getPrefixQuery(String field, String search) {
        PrefixQueryBuilder queryBuilder = QueryBuilders.prefixQuery(field, search);
        return queryBuilder;
    }

    private SearchConfiguration getSearchConfiguration(HttpServletRequest req) {
        SearchConfiguration conf = new SearchConfiguration();
        AntPathMatcher matcher = new AntPathMatcher();
        Map<String,String> templateValues = null;
        try {
            templateValues = matcher.extractUriTemplateVariables("/{index}/{type}/_search", req.getPathInfo());
        } catch (IllegalStateException e) {
            try {
                templateValues = matcher.extractUriTemplateVariables("/{index}/_search", req.getPathInfo());
            } catch (IllegalStateException ee) {
                // ignore
            }
        }
        if (templateValues != null) {
            conf.setIndex(templateValues.get("index"));
            conf.setType(templateValues.get("type"));
        }
        return conf;
    }

    private void handleGet(Authentication auth, HttpServletRequest req,
            HttpServletResponse resp) {
        Map<String,Object> data = new HashMap<String,Object>();
        data.put("name", auth.getName());
        data.put("roles", auth.getAuthorities());
        Representation repr = new MapJSONFormat().toRepresentation(data);
        resp.setContentType(repr.getMediaType().toString());
        resp.setContentLength((int) repr.getSize());
        resp.setStatus(HttpStatus.OK.value());
        OutputStream out = null;
        try {
            out = resp.getOutputStream();
            repr.write(out);
        } catch (IOException e) {
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            LOGGER.log(Level.WARNING, "handleGet exception", e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

}
