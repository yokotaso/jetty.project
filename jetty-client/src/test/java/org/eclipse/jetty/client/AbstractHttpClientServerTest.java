//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.client;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.SelectChannelConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestWatchman;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkMethod;

@RunWith(Parameterized.class)
public abstract class AbstractHttpClientServerTest
{
    @Parameterized.Parameters
    public static Collection<SslContextFactory[]> parameters()
    {
        return Arrays.asList(new SslContextFactory[]{null}, new SslContextFactory[]{new SslContextFactory()});
    }

    @Rule
    public final TestWatchman testName = new TestWatchman()
    {
        @Override
        public void starting(FrameworkMethod method)
        {
            super.starting(method);
            System.err.printf("Running %s.%s()%n",
                    method.getMethod().getDeclaringClass().getName(),
                    method.getName());
        }
    };

    protected SslContextFactory sslContextFactory;
    protected String scheme;
    protected Server server;
    protected HttpClient client;
    protected NetworkConnector connector;

    public AbstractHttpClientServerTest(SslContextFactory sslContextFactory)
    {
        this.sslContextFactory = sslContextFactory;
        this.scheme = sslContextFactory == null ? "http" : "https";
    }

    public void start(Handler handler) throws Exception
    {
        if (sslContextFactory != null)
        {
            sslContextFactory.setKeyStorePath("src/test/resources/keystore.jks");
            sslContextFactory.setKeyStorePassword("storepwd");
            sslContextFactory.setTrustStorePath("src/test/resources/truststore.jks");
            sslContextFactory.setTrustStorePassword("storepwd");
        }

        if (server == null)
            server = new Server();
        connector = new SelectChannelConnector(server, sslContextFactory);
        server.addConnector(connector);
        server.setHandler(handler);
        server.start();

        QueuedThreadPool executor = new QueuedThreadPool();
        executor.setName(executor.getName() + "-client");
        client = new HttpClient(sslContextFactory);
        client.setExecutor(executor);
        client.start();
    }

    @After
    public void dispose() throws Exception
    {
        if (client != null)
            client.stop();
        if (server != null)
            server.stop();
        server = null;
    }
}
