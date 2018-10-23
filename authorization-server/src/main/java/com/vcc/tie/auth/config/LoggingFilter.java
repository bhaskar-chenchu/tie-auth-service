package com.vcc.tie.auth.config;

import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private final Logger conversationLog = LoggerFactory.getLogger("tie.rest.conversation");
    private final List<String> ignoredUrlPatterns = new CopyOnWriteArrayList<>();

    public LoggingFilter(){
        ignoredUrlPatterns.add("/.*swagger.*");
        ignoredUrlPatterns.add("/api/actuator/.*");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
        } else {
            doFilterWrapped(wrapAsCached(request), wrapAsCached(response), filterChain);
        }
    }

    protected void doFilterWrapped(ContentCachingRequestWrapper cachingRequestWrapper, ContentCachingResponseWrapper contentCachingResponseWrapper, FilterChain filterChain) throws ServletException, IOException {
        try{
            filterChain.doFilter(cachingRequestWrapper, contentCachingResponseWrapper);
        }
        finally{
            if(!isUrlIgnored(cachingRequestWrapper.getRequestURI())){
                writeConversationLog(cachingRequestWrapper, contentCachingResponseWrapper);
            }
            contentCachingResponseWrapper.copyBodyToResponse();
        }
    }

    private boolean isUrlIgnored(String requestURI) {
        return ignoredUrlPatterns.stream()
               .anyMatch(regex -> requestURI.matches(regex));
    }


    private void writeConversationLog(ContentCachingRequestWrapper cachingRequestWrapper, ContentCachingResponseWrapper contentCachingResponseWrapper) {
        try{
            if(conversationLog.isInfoEnabled()){
                String content = createConversationLog(cachingRequestWrapper, contentCachingResponseWrapper);
                conversationLog.info(content);
            }
            else if(conversationLog.isWarnEnabled() && contentCachingResponseWrapper.getStatus() >= 400){
                String content = createConversationLog(cachingRequestWrapper, contentCachingResponseWrapper);
                conversationLog.warn(content);
            }
        }
        catch (Exception e){
            conversationLog.warn("Failed to log the http request - response!", e);
        }

    }

    private String createConversationLog(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {

        String requestContent = createRequestContent(request);
        String responseContent = createResponseContent(response);
        return requestContent+LB+LB+responseContent;
    }

    private String createResponseContent(ContentCachingResponseWrapper response) {
        StringBuilder builder = new StringBuilder();

        builder.append(RESPONSE_PREFIX).append("status: ").append(response.getStatus()).append(" ").append(HttpStatus.valueOf(response.getStatus()).name()).append(LB);
        String headerContent = getHeaderMap(response).stream()
                .map(HttpHeader::toString)
                .map(s -> RESPONSE_PREFIX+s)
                .collect(Collectors.joining(LB));
        if(!headerContent.isEmpty()){
            builder.append(LB).append(headerContent);
        }
        bodyAsString(response.getContentAsByteArray()).ifPresent(body -> builder.append(LB).append(body).append(LB));
        return builder.toString();
    }

    private String createRequestContent(ContentCachingRequestWrapper request) {

        StringBuilder builder = new StringBuilder(LB);

        builder.append(REQUEST_PREFIX).append(request.getMethod().toUpperCase()).append(" ").append(request.getRequestURI()).append(nullAsEmpty(request.getQueryString()));

        String headerContent = getHeaderMap(request).stream()
                        .map(HttpHeader::toString)
                        .map(s -> REQUEST_PREFIX+s)
                        .collect(Collectors.joining(LB));
        builder.append(LB).append(headerContent);

        bodyAsString(request.getContentAsByteArray()).ifPresent(body -> builder.append(LB).append(body));


        return builder.toString();
    }

    private Optional<String> bodyAsString(byte[] body){
      if(body == null || body.length == 0){
          return Optional.empty();
      }
        try {
            return Optional.of(new String(body, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            conversationLog.warn("machine does not support utf-8");
            return Optional.of(new String(body));
        }
    }

    private String nullAsEmpty(String s){
        return Optional.ofNullable(s).orElse("");
    }

    private static final String LB = System.getProperty("line.separator");
    private static final String REQUEST_PREFIX = ">> ";
    private static final String RESPONSE_PREFIX = "<< ";

    private List<HttpHeader> getHeaderMap(HttpServletRequest request){
        return Collections.list(request.getHeaderNames()).stream()
                .map( key -> new HttpHeader(key, Collections.list(request.getHeaders(key))))
                .collect(Collectors.toList());
    }

    private List<HttpHeader> getHeaderMap(HttpServletResponse response){

        return response.getHeaderNames().stream()
                .map( key -> new HttpHeader(key, response.getHeaders(key)))
                .collect(Collectors.toList());
    }

    @Builder
    @Getter
    private static class HttpHeader{
        private String key;
        @Builder.Default
        private Collection<String> value = new ArrayList<>();

        private String valueAsString(){
            if("authorization".equalsIgnoreCase(getKey().trim())){
                return "REDACTED";
            }
            return value.stream().collect(Collectors.joining(","));
        }

        public String toString(){
            return key+" : "+valueAsString();
        }
    }


    private ContentCachingResponseWrapper wrapAsCached(HttpServletResponse response) {
        if(ContentCachingResponseWrapper.class.isAssignableFrom(response.getClass())){
            return (ContentCachingResponseWrapper) response;
        }
        return new ContentCachingResponseWrapper(response);
    }

    private ContentCachingRequestWrapper wrapAsCached(HttpServletRequest request) {
        if(ContentCachingRequestWrapper.class.isAssignableFrom(request.getClass())){
            return (ContentCachingRequestWrapper) request;
        }
        return new ContentCachingRequestWrapper(request);
    }
}
