package controllers;

import olingo.ETagSupport;
import olingo.MetadataETagSupport;
import olingo.data.DataProvider;
import olingo.processor.TechnicalActionProcessor;
import olingo.processor.TechnicalBatchProcessor;
import olingo.processor.TechnicalEntityProcessor;
import olingo.processor.TechnicalPrimitiveComplexProcessor;
import olingo.provider.EdmTechProvider;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.edmx.EdmxReferenceInclude;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataContent;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.core.serializer.utils.CircleStreamBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Application extends Controller {

//    public Result index() {
//        return ok(index.render("Your new application is ready."));
//    }
  public Result index() {
    // Serves this stream with 200 OK
//    return ok("Up and running -> " + Calendar.getInstance().getTime());
    return ok(index.apply(
        "Up and running -> " + Calendar.getInstance().getTime(),
        "http://localhost:9000/odata.svc/ESAllPrim?$format=json"));
  }

  public Result chunks() {
    // Prepare a chunked text stream
    Chunks<String> chunks = StringChunks.whenReady(JavaStream::registerOutChannelSomewhere);

    // Serves this stream with 200 OK
    return ok(chunks);
  }


  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(Application.class);
  /**
   * <p>ETag for the service document and the metadata document</p>
   * <p>We use the same field for service-document and metadata-document ETags.
   * It must change whenever the corresponding document changes.
   * We don't know when someone changed the EDM in a way that changes one of these
   * documents, but we do know that the EDM is defined completely in code and that
   * therefore any change must be deployed, resulting in re-loading of this class,
   * giving this field a new and hopefully unique value.</p>
   */
  private static final String metadataETag = "W/\"" + UUID.randomUUID() + "\"";

  public Result odata(String odataPath) {
    try {
      OData odata = OData.newInstance();
      EdmxReference reference = new EdmxReference(URI.create("../v4.0/cs02/vocabularies/Org.OData.Core.V1.xml"));
      reference.addInclude(new EdmxReferenceInclude("Org.OData.Core.V1", "Core"));
      final ServiceMetadata serviceMetadata = odata.createServiceMetadata(
          new EdmTechProvider(),
          Collections.singletonList(reference),
          new MetadataETagSupport(metadataETag));

//      HttpSession session = request.getSession(true);
//      DataProvider dataProvider = (DataProvider) session.getAttribute(DataProvider.class.getName());
      DataProvider dataProvider = null;
        dataProvider = new DataProvider(odata, serviceMetadata.getEdm());

//      ODataHttpHandler handler = odata.createHandler(serviceMetadata);
//      ODataHandler handler = new ODataHandler(odata, serviceMetadata, null);
      PlayODataHandler handler = new PlayODataHandler(odata, serviceMetadata);

      // Register processors.
      handler.register(new TechnicalEntityProcessor(dataProvider, serviceMetadata));
      handler.register(new TechnicalPrimitiveComplexProcessor(dataProvider, serviceMetadata));
      handler.register(new TechnicalActionProcessor(dataProvider, serviceMetadata));
      handler.register(new TechnicalBatchProcessor(dataProvider));
      // Register helpers.
      handler.register(new ETagSupport());
//      handler.register(new DefaultDebugSupport());
      // Process the request.
//      handler.process(request, response);
//      req.
      ODataResponse resp = handler.processPlayRequest(request(), odataPath);
      InputStream content = resp.getContent();
      if(content == null) {
        ODataContent odc = resp.getODataContent();
        CircleStreamBuffer csb = new CircleStreamBuffer();
        odc.write(csb.getOutputStream());
        content = csb.getInputStream();
      }
//      if(content == null) {
//        ODataContent odc = resp.getODataContent();
//        content = Channels.newInputStream(odc.getChannel());
//      }
      response().setContentType(resp.getHeader("Content-Type"));

      return ok(content);
    } catch (final RuntimeException e) {
      LOG.error("Server Error", e);
      return badRequest();
    }

  }

  static class JavaStream {

    public static void registerOutChannelSomewhere(Chunks.Out<String> out) {
      try {
        out.write(new Date().toString() + ": kiki\n");
        TimeUnit.MILLISECONDS.sleep(2000);
        out.write(new Date().toString() + ": foo->" + generateData(100) + "\n");
        TimeUnit.MILLISECONDS.sleep(2000);
        out.write("bar->" + new Date().toString());
        out.close();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public static String generateData(final int len) {
      Random random = new Random();
      StringBuilder b = new StringBuilder(len);
      for (int j = 0; j < len; j++) {
        b.append((char) ('A' + random.nextInt('Z' - 'A' + 1)));
      }
      return b.toString();
    }
  }
}
