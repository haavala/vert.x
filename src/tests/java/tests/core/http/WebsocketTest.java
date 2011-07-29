package tests.core.http;

import org.nodex.core.buffer.Buffer;
import org.nodex.core.buffer.DataHandler;
import org.nodex.core.http.HttpClient;
import org.nodex.core.http.HttpClientConnectHandler;
import org.nodex.core.http.HttpClientConnection;
import org.nodex.core.http.HttpServer;
import org.nodex.core.http.HttpServerConnectHandler;
import org.nodex.core.http.HttpServerConnection;
import org.nodex.core.http.Websocket;
import org.nodex.core.http.WebsocketConnectHandler;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import tests.Utils;
import tests.core.TestBase;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * User: timfox
 * Date: 26/07/2011
 * Time: 20:30
 */
public class WebsocketTest extends TestBase {
  @BeforeClass
  public void setUp() {
  }

  @AfterClass
  public void tearDown() {

  }

  @Test
  public void testWSBinary() throws Exception {
    testWS(true);
    throwAssertions();
  }

  @Test
  public void testWSString() throws Exception {
    testWS(false);
    throwAssertions();
  }

  private void testWS(final boolean binary) throws Exception {
    final String host = "localhost";
    final boolean keepAlive = true;
    final String path = "/some/path";
    final int port = 8181;

    HttpServerConnectHandler serverH = new HttpServerConnectHandler() {
      public void onConnect(final HttpServerConnection conn) {
        conn.websocketConnect(new WebsocketConnectHandler() {
          public boolean onConnect(final Websocket ws) {
            azzert(path.equals(ws.uri));
            ws.data(new DataHandler() {
              public void onData(Buffer data) {
                //Echo it back
                ws.writeBuffer(data);
              }
            });
            return true;
          }
        });
      }
    };
    final CountDownLatch latch = new CountDownLatch(1);

    HttpClientConnectHandler clientH = new HttpClientConnectHandler() {
      public void onConnect(final HttpClientConnection conn) {
        conn.upgradeToWebSocket(path, new WebsocketConnectHandler() {
          public boolean onConnect(Websocket ws) {
            final Buffer received = Buffer.newDynamic(0);
            ws.data(new DataHandler() {
              public void onData(Buffer data) {
                received.append(data);
                if (received.length() == 1000) {
                  latch.countDown();
                  conn.close();
                }
              }
            });
            final Buffer sent = Buffer.newDynamic(0);
            for (int i = 0; i < 10; i++) {
              String str = Utils.randomAlphaString(100);
              try {
                Buffer buff = Buffer.newWrapped(str.getBytes("UTF-8"));
                sent.append(buff);
                if (binary) {
                  ws.writeBinaryFrame(buff);
                } else {
                  ws.writeTextFrame(str);
                }
              } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                azzert(false);
              }
            }
            return true;
          }
        });
        conn.closed(new Runnable() {
          public void run() {
            latch.countDown();
          }
        });
      }
    };

    HttpServer server = HttpServer.createServer(serverH).listen(port, host);

    HttpClient client = HttpClient.createClient().setKeepAlive(keepAlive).connect(port, host, clientH);

    azzert(latch.await(5, TimeUnit.SECONDS));
    awaitClose(server);
  }
}