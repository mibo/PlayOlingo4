package controllers;

import java.io.ByteArrayInputStream;
import java.util.*;

import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.processor.Processor;

import play.mvc.Http;

public class PlayODataHandler implements ODataHandler {
  private final ODataHandler handler;

  public PlayODataHandler(final OData odata, final ServiceMetadata serviceMetadata) {
    handler = odata.createRawHandler(serviceMetadata);
  }

  static HttpMethod extractMethod(final Http.Request httpRequest) throws ODataApplicationException {
    String method = httpRequest.method();
    try {
      HttpMethod httpRequestMethod = HttpMethod.valueOf(httpRequest.method());

      if (httpRequestMethod == HttpMethod.POST) {
        String xHttpMethod = httpRequest.getHeader(HttpHeader.X_HTTP_METHOD);
        String xHttpMethodOverride = httpRequest.getHeader(HttpHeader.X_HTTP_METHOD_OVERRIDE);

        if (xHttpMethod == null && xHttpMethodOverride == null) {
          return httpRequestMethod;
        } else if (xHttpMethod == null) {
          return HttpMethod.valueOf(xHttpMethodOverride);
        } else if (xHttpMethodOverride == null) {
          return HttpMethod.valueOf(xHttpMethod);
        } else {
          if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
            throw new ODataApplicationException(
                "Ambiguous X-HTTP-Methods" + xHttpMethod + " " + xHttpMethodOverride, 500, Locale.ENGLISH);
          }
          return HttpMethod.valueOf(xHttpMethod);
        }
      } else {
        return httpRequestMethod;
      }
    } catch (IllegalArgumentException e) {
      throw new ODataApplicationException(
          "Invalid HTTP method" + method, 500, Locale.ENGLISH);
    }
  }

  static void fillUriInformation(final ODataRequest odRequest, final Http.Request httpRequest, final String odataPath) {
    String rawRequestUri = httpRequest.host() + httpRequest.uri();

    String rawODataPath = "/" + odataPath;
    final String path = httpRequest.path().substring(0, rawODataPath.length());

    String rawBaseUri = httpRequest.host() + path;

    // XXX: BUG
    final Map<String, String[]> rawQueryString = httpRequest.queryString();
    StringBuilder queryString = new StringBuilder();
    for (Map.Entry<String, String[]> entry : rawQueryString.entrySet()) {
      for (String value: entry.getValue()) {
        if(queryString.length() > 1) {
          queryString.append("&");
        }
        queryString.append(entry.getKey()).append("=").append(value);
      }
    }

    odRequest.setRawQueryPath(queryString.toString());
    odRequest.setRawRequestUri(rawRequestUri);
    odRequest.setRawODataPath(rawODataPath);
    odRequest.setRawBaseUri(rawBaseUri);
  }

  static void copyHeaders(final ODataRequest odRequest, final Http.Request req) {
    Map<String, String[]> headers = req.headers();
    for (Map.Entry<String, String[]> header : headers.entrySet()) {
      List<String> headerValues = new ArrayList<>();
      Collections.addAll(headerValues, header.getValue());
      odRequest.addHeader(header.getKey(), headerValues);
    }
  }

  public ODataResponse processPlayRequest(Http.Request request, String odataPath) {
    ODataRequest odRequest = new ODataRequest();
    ODataResponse odResponse;

    try {
      odRequest.setBody(null);
      odRequest.setProtocol("HTTP/1.1");
      odRequest.setMethod(extractMethod(request));
      copyHeaders(odRequest, request);
      fillUriInformation(odRequest, request, odataPath);

      odResponse = process(odRequest);
      // ALL future methods after process must not throw exceptions!
    } catch (Exception e) {
      odResponse = new ODataResponse();
      odResponse.setContent(new ByteArrayInputStream(e.getMessage().getBytes()));
//      odResponse = handleException(odRequest, e);
      return null;
    }

    return odResponse;
  }

  @Override
  public ODataResponse process(ODataRequest request) {
    return handler.process(request);
  }

  @Override
  public void register(Processor processor) {
    handler.register(processor);
  }

  @Override
  public void register(OlingoExtension extension) {
    handler.register(extension);
  }

}
