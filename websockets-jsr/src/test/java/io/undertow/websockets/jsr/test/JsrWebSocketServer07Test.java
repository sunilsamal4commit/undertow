/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.undertow.websockets.jsr.test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;

import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.test.util.TestClassIntrospector;
import io.undertow.servlet.test.util.TestResourceLoader;
import io.undertow.test.utils.DefaultServer;
import io.undertow.util.ConcreteIoFuture;
import io.undertow.websockets.jsr.bootstrap.WebSocketDeployer;
import io.undertow.websockets.jsr.bootstrap.WebSocketDeployment;
import io.undertow.websockets.jsr.bootstrap.WebSocketDeploymentInfo;
import io.undertow.websockets.utils.FrameChecker;
import io.undertow.websockets.utils.WebSocketTestClient;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
@RunWith(DefaultServer.class)
public class JsrWebSocketServer07Test {

    @org.junit.Test
    public void testBinaryWithByteBuffer() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                    @Override
                    public void onMessage(ByteBuffer message) {
                        ByteBuffer buf = ByteBuffer.allocate(message.remaining());
                        buf.put(message);
                        buf.flip();
                        try {
                            session.getBasicRemote().sendBinary(buf);
                        } catch (IOException e) {
                            e.printStackTrace();
                            cause.set(e);
                            latch.setException(e);
                        }
                    }
                });
            }
        }

        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new BinaryWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(BinaryWebSocketFrame.class, payload, latch));
        latch.get();
        Assert.assertNull(cause.get());
        client.destroy();
    }


    @org.junit.Test
    public void testBinaryWithByteArray() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();
        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                    @Override
                    public void onMessage(byte[] message) {
                        try {
                            session.getBasicRemote().sendBinary(ByteBuffer.wrap(message.clone()));
                        } catch (IOException e) {
                            e.printStackTrace();
                            cause.set(e);
                            latch.setException(e);
                        }
                    }
                });
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new BinaryWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(BinaryWebSocketFrame.class, payload, latch));
        latch.get();
        Assert.assertNull(cause.get());
        client.destroy();
    }

    @org.junit.Test
    public void testText() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        try {
                            session.getBasicRemote().sendText(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                            cause.set(e);
                            latch.setException(e);
                        }
                    }
                });
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new TextWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(TextWebSocketFrame.class, payload, latch));
        latch.get();
        Assert.assertNull(cause.get());
        client.destroy();
    }

    @org.junit.Test
    public void testBinaryWithByteBufferByCompletion() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<SendResult> sendResult = new AtomicReference<SendResult>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();
        final ConcreteIoFuture latch2 = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                    @Override
                    public void onMessage(ByteBuffer message) {
                        ByteBuffer buf = ByteBuffer.allocate(message.remaining());
                        buf.put(message);
                        buf.flip();
                        session.getAsyncRemote().sendBinary(buf, new SendHandler() {
                            @Override
                            public void onResult(SendResult result) {
                                sendResult.set(result);
                                if (result.getException() != null) {
                                    latch2.setException(new IOException(result.getException()));
                                } else {
                                    latch2.setResult(null);
                                }
                            }
                        });
                    }
                });
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new BinaryWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(BinaryWebSocketFrame.class, payload, latch));
        latch.get();
        latch2.get();

        SendResult result = sendResult.get();
        Assert.assertNotNull(result);
        Assert.assertNull(result.getException());

        client.destroy();
    }

    @org.junit.Test
    public void testTextByCompletion() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<SendResult> sendResult = new AtomicReference<SendResult>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();
        final ConcreteIoFuture latch2 = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        session.getAsyncRemote().sendText(message, new SendHandler() {
                            @Override
                            public void onResult(SendResult result) {
                                sendResult.set(result);
                                if (result.getException() != null) {
                                    latch2.setException(new IOException(result.getException()));
                                } else {
                                    latch2.setResult(null);
                                }
                            }
                        });
                    }
                });
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new TextWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(TextWebSocketFrame.class, payload, latch));
        latch.get();
        latch2.get();

        SendResult result = sendResult.get();
        Assert.assertNotNull(result);
        Assert.assertNull(result.getException());

        client.destroy();
    }

    @org.junit.Test
    public void testBinaryWithByteBufferByFuture() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Future<Void>> sendResult = new AtomicReference<>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                    @Override
                    public void onMessage(ByteBuffer message) {
                        ByteBuffer buf = ByteBuffer.allocate(message.remaining());
                        buf.put(message);
                        buf.flip();
                        sendResult.set(session.getAsyncRemote().sendBinary(buf));
                    }
                });
            }

        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new BinaryWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(BinaryWebSocketFrame.class, payload, latch));
        latch.get();

        Future<Void> result = sendResult.get();

        client.destroy();
    }

    @org.junit.Test
    public void testTextByFuture() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Future<Void>> sendResult = new AtomicReference<>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        sendResult.set(session.getAsyncRemote().sendText(message));
                    }
                });
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);

        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new TextWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(TextWebSocketFrame.class, payload, latch));
        latch.get();

        sendResult.get();

        client.destroy();
    }

    @org.junit.Test
    public void testBinaryWithByteArrayUsingStream() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();
        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
                    @Override
                    public void onMessage(byte[] message) {
                        try {
                            OutputStream out = session.getBasicRemote().getSendStream();
                            out.write(message);
                            out.flush();
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            cause.set(e);
                            latch.setException(e);
                        }
                    }
                });
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new BinaryWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(BinaryWebSocketFrame.class, payload, latch));
        latch.get();
        Assert.assertNull(cause.get());
        client.destroy();
    }

    @org.junit.Test
    public void testTextUsingWriter() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Whole<String>() {
                    @Override
                    public void onMessage(String message) {
                        try {
                            Writer writer = session.getBasicRemote().getSendWriter();
                            writer.write(message);
                            writer.flush();
                            writer.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                            cause.set(e);
                            latch.setException(e);
                        }
                    }
                });
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new TextWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(TextWebSocketFrame.class, payload, latch));
        latch.get();
        Assert.assertNull(cause.get());
        client.destroy();
    }

    @org.junit.Test
    public void testPingPong() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new PingWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(PongWebSocketFrame.class, payload, latch));
        latch.get();
        Assert.assertNull(cause.get());
        client.destroy();
    }

    @org.junit.Test
    public void testCloseFrame() throws Exception {
        final int code = 1000;
        final String reasonText = "TEST";
        final AtomicReference<CloseReason> reason = new AtomicReference<CloseReason>();
        ByteBuffer payload = ByteBuffer.allocate(reasonText.length() + 2);
        payload.putShort((short) code);
        payload.put(reasonText.getBytes("UTF-8"));
        payload.flip();

        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
            }

            @Override
            public void onClose(Session session, CloseReason closeReason) {
                reason.set(closeReason);
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new CloseWebSocketFrame(code, reasonText), new FrameChecker(CloseWebSocketFrame.class, payload.array(), latch));
        latch.get();
        Assert.assertEquals(code, reason.get().getCloseCode().getCode());
        Assert.assertEquals(reasonText, reason.get().getReasonPhrase());
        client.destroy();
    }


    @org.junit.Test
    public void testBinaryWithByteBufferAsync() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();

        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Partial<ByteBuffer>() {
                    @Override
                    public void onMessage(ByteBuffer message, boolean last) {
                        Assert.assertTrue(last);
                        ByteBuffer buf = ByteBuffer.allocate(message.remaining());
                        buf.put(message);
                        buf.flip();
                        try {
                            session.getBasicRemote().sendBinary(buf);
                        } catch (IOException e) {
                            e.printStackTrace();
                            cause.set(e);
                            latch.setException(e);
                        }

                    }
                });
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new BinaryWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(BinaryWebSocketFrame.class, payload, latch));
        latch.get();
        Assert.assertNull(cause.get());
        client.destroy();
    }

    @org.junit.Test
    public void testTextAsync() throws Exception {
        final byte[] payload = "payload".getBytes();
        final AtomicReference<Throwable> cause = new AtomicReference<Throwable>();
        final AtomicBoolean connected = new AtomicBoolean(false);
        final ConcreteIoFuture latch = new ConcreteIoFuture();
        class TestEndPoint extends Endpoint {
            @Override
            public void onOpen(final Session session, EndpointConfig config) {
                connected.set(true);
                session.addMessageHandler(new MessageHandler.Partial<String>() {
                    @Override
                    public void onMessage(String message, boolean last) {
                        Assert.assertTrue(last);
                        try {
                            session.getBasicRemote().sendText(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                            cause.set(e);
                            latch.setException(e);
                        }
                    }
                });
            }
        }
        WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
        WebSocketDeployment deployment = WebSocketDeployment.create(info);
        deployment.getContainer().addEndpoint(ServerEndpointConfig.Builder.create(TestEndPoint.class, "/").configurator(new InstanceConfigurator(new TestEndPoint())).build());

        DeploymentInfo builder = createDeploymentInfo();
        WebSocketDeployer.deploy(deployment, builder, getClass().getClassLoader());

        deployServlet(builder);

        WebSocketTestClient client = new WebSocketTestClient(getVersion(), new URI("ws://" + DefaultServer.getHostAddress("default") + ":" + DefaultServer.getHostPort("default") + "/"));
        client.connect();
        client.send(new TextWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), new FrameChecker(TextWebSocketFrame.class, payload, latch));
        latch.get();
        Assert.assertNull(cause.get());
        client.destroy();
    }

    protected WebSocketVersion getVersion() {
        return WebSocketVersion.V07;
    }


    private DeploymentInfo createDeploymentInfo() {
        return new DeploymentInfo()
                .setClassLoader(getClass().getClassLoader())
                .setContextPath("/")
                .setClassIntrospecter(TestClassIntrospector.INSTANCE)
                .setDeploymentName("websocket.war")
                .setResourceLoader(TestResourceLoader.NOOP_RESOURCE_LOADER);
    }


    private void deployServlet(final DeploymentInfo builder) throws ServletException {
        final PathHandler root = new PathHandler();
        final ServletContainer container = ServletContainer.Factory.newInstance();
        DeploymentManager manager = container.addDeployment(builder);
        manager.deploy();
        root.addPath(builder.getContextPath(), manager.start());

        DefaultServer.setRootHandler(root);
    }

    private static class InstanceConfigurator extends ServerEndpointConfig.Configurator {

        private final Object endpoint;

        private InstanceConfigurator(final Object endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public <T> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException {
            return (T) endpoint;
        }
    }

}